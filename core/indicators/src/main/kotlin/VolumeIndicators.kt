package org.cryptolosers.indicators

import org.cryptolosers.indicators.VolumeIndicators.Companion.days
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class VolumeIndicators(val tradingApi: ViewTradingApi) {

    companion object {
        val days = 22 * 2 // days for calculate volume
        val bigVolume = 3 // big volume more median
    }

    suspend fun isBigVolumeOnStartSession(ticker: Ticker, timeframe: Timeframe): IndicatorResult {
        val now = LocalDate.now().minusDays(1)
        val candles = tradingApi.getLastCandles(ticker, timeframe, getCandlesCount(timeframe), Session.CURRENT_AND_PREVIOUS)
        if (candles.size <= 10) {
            return IndicatorResult(
                isSignal = false,
                volumeChangeFromMedianPercentageInCandle = null,
                priceChangePercentageInCandle = null
            )
        }
        val startSessionCandles = candles.filter { it.timestamp.hour == 10 && it.timestamp.minute == 0 }
        val medianVolume = findMedian(startSessionCandles.map { it.volume })
        if (candles.none { it.timestamp.hour == 10 && it.timestamp.minute == 0 && it.timestamp.toLocalDate() == now}) {
            return IndicatorResult(
                isSignal = false,
                volumeChangeFromMedianPercentageInCandle = null,
                priceChangePercentageInCandle = null
            )
        }

        val startCurrentSessionCandle = candles.first { it.timestamp.hour == 10 && it.timestamp.minute == 0 && it.timestamp.toLocalDate() ==now}
        var prevStartCurrentSessionCandleIndex = if (candles.indexOf(startCurrentSessionCandle) != 0) (candles.indexOf(startCurrentSessionCandle) - 1) else 0
        var prevStartCurrentSessionCandle: Candle
        do {
            prevStartCurrentSessionCandle = candles[prevStartCurrentSessionCandleIndex]
            prevStartCurrentSessionCandleIndex--
        } while (prevStartCurrentSessionCandle.timestamp.toLocalDate().isEqual(now) && prevStartCurrentSessionCandleIndex >= 0)

        val bigVolumes = startSessionCandles.filter { it.volume > bigVolume * medianVolume }

//        bigVolumes.forEach {
//            println(ticker.symbol + " " +  it)
//        }

        val scale = 2
        val volumeChangeFromMedianPercentageInCandle = startCurrentSessionCandle.let {
            ((it.volume.toDouble() - medianVolume.toDouble()) / medianVolume.toDouble() * 100).toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN)
        }
        val priceChangePercentageInCandle = startCurrentSessionCandle.let {
            ((it.closePrice.toDouble() - prevStartCurrentSessionCandle.closePrice.toDouble()) / prevStartCurrentSessionCandle.closePrice.toDouble() * 100).toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN)
        }
        return IndicatorResult(
            isSignal = bigVolumes.contains(startCurrentSessionCandle),
            volumeChangeFromMedianPercentageInCandle = volumeChangeFromMedianPercentageInCandle,
            priceChangePercentageInCandle = priceChangePercentageInCandle
        )
    }

    suspend fun isBigVolumeOnStartSession(candles: List<Candle>): IndicatorResult {
        val now = LocalDate.now().minusDays(1)
        if (candles.size <= 10) {
            return IndicatorResult(
                isSignal = false,
                volumeChangeFromMedianPercentageInCandle = null,
                priceChangePercentageInCandle = null
            )
        }
        val startSessionCandles = candles.filter { it.timestamp.hour == 10 && it.timestamp.minute == 0 }
        if (startSessionCandles.isEmpty()) {
            println("startSessionCandles empty")
            return IndicatorResult(
                isSignal = false,
                volumeChangeFromMedianPercentageInCandle = null,
                priceChangePercentageInCandle = null
            )
        }
        val medianVolume = findMedian(startSessionCandles.map { it.volume })
        if (candles.none { it.timestamp.hour == 10 && it.timestamp.minute == 0 && it.timestamp.toLocalDate() == now}) {
            return IndicatorResult(
                isSignal = false,
                volumeChangeFromMedianPercentageInCandle = null,
                priceChangePercentageInCandle = null
            )
        }

        val startCurrentSessionCandle = candles.first { it.timestamp.hour == 10 && it.timestamp.minute == 0 && it.timestamp.toLocalDate() == now}
        var prevStartCurrentSessionCandleIndex = if (candles.indexOf(startCurrentSessionCandle) != 0) (candles.indexOf(startCurrentSessionCandle) - 1) else 0
        var prevStartCurrentSessionCandle: Candle
        do {
            prevStartCurrentSessionCandle = candles[prevStartCurrentSessionCandleIndex]
            prevStartCurrentSessionCandleIndex--
        } while (prevStartCurrentSessionCandle.timestamp.toLocalDate().isEqual(now) && prevStartCurrentSessionCandleIndex >= 0)

        val bigVolumes = startSessionCandles.filter { it.volume > bigVolume * medianVolume }

//        bigVolumes.forEach {
//            println(ticker.symbol + " " +  it)
//        }

        val scale = 2
        val volumeChangeFromMedianPercentageInCandle = startCurrentSessionCandle.let {
            ((it.volume.toDouble() - medianVolume.toDouble()) / medianVolume.toDouble() * 100).toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN)
        }
        val priceChangePercentageInCandle = startCurrentSessionCandle.let {
            ((it.closePrice.toDouble() - prevStartCurrentSessionCandle.closePrice.toDouble()) / prevStartCurrentSessionCandle.closePrice.toDouble() * 100).toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN)
        }
        return IndicatorResult(
            isSignal = bigVolumes.contains(startCurrentSessionCandle),
            volumeChangeFromMedianPercentageInCandle = volumeChangeFromMedianPercentageInCandle,
            priceChangePercentageInCandle = priceChangePercentageInCandle
        )
    }

    suspend fun isBigVolume(ticker: Ticker, timeframe: Timeframe): Boolean {
        val candles = tradingApi.getLastCandles(ticker, timeframe, getCandlesCount(timeframe), Session.CURRENT_AND_PREVIOUS)
        val medianVolume = findMedian(candles.map { it.volume })
        val lastSessionCandle = candles.lastOrNull { it.timestamp.toLocalDate() == LocalDate.now() }

        val bigVolumes = candles.filter { it.volume > bigVolume * medianVolume }
        bigVolumes.forEach {
            println(ticker.symbol + " " +  it)
        }
        return bigVolumes.contains(lastSessionCandle)
    }

    suspend fun isBigVolumeOnStartSessionWithPulse(ticker: Ticker, timeframe: Timeframe): Boolean {
        TODO()
    }

    suspend fun isBigVolumeWithPulse(ticker: Ticker, timeframe: Timeframe): Boolean {
        TODO()
    }

}

fun getCandlesCount(timeframe: Timeframe): Int {
    return when (timeframe) {
        Timeframe.MIN5 -> { 14 * 12 * days } //session hours * count in hour * days
        Timeframe.MIN15 -> { 14 * 4 * days }
        Timeframe.HOUR1 -> { 14 * 1 * days }
        Timeframe.DAY1 -> { days }
        else -> { TODO("timeframe not supported yet") }
    }
}

data class IndicatorResult(
    val isSignal: Boolean,
    val volumeChangeFromMedianPercentageInCandle: BigDecimal?,
    val priceChangePercentageInCandle: BigDecimal?,
    //val priceChangePercentageInDay: Double
)

data class TickerWithIndicator (
    val ticker: TickerInfo,
    val indicator: IndicatorResult
)

fun findMedian(numbers: List<Long>): Long {
    // Сортируем список
    val sortedNumbers = numbers.sorted()

    val size = sortedNumbers.size
    return if (size % 2 == 1) {
        // Если количество элементов нечётное, медиана - это средний элемент
        sortedNumbers[size / 2]
    } else {
        // Если количество элементов чётное, медиана - это среднее значение двух центральных элементов
        val midIndex = size / 2
        (sortedNumbers[midIndex - 1] + sortedNumbers[midIndex]) / 2
    }
}