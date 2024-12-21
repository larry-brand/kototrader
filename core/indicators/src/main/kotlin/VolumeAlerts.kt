package org.cryptolosers.indicators

import org.cryptolosers.commons.fortsMap
import org.cryptolosers.indicators.VolumeAlerts.Companion.days
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class VolumeAlerts(val tradingApi: ViewTradingApi) {

    companion object {
        val days = 22 * 2 // days for calculate volume
        val bigVolume = 3 // big volume more median
    }

    suspend fun isBigVolume(candles: List<Candle>): AlertResult {
        if (candles.size <= 10) {
            return AlertResult(
                isSignal = false,
                volumeX = null,
                pricePercentage = null
            )
        }
        if (candles.isEmpty()) {
            println("candles empty")
            return AlertResult(
                isSignal = false,
                volumeX = null,
                pricePercentage = null
            )
        }
        val medianVolume = findMedian(candles.map { it.volume })

        val lastCandle = candles.last()
        val prevCandleIndex = if (candles.indexOf(lastCandle) != 0) (candles.indexOf(lastCandle) - 1) else 0
        val prevCandle = candles[prevCandleIndex]

        val isSignal =  lastCandle.volume > bigVolume * medianVolume

        val scale = 2
        val volumeChangeFromMedianXInCandle = lastCandle.let {
            ((it.volume.toDouble() / medianVolume.toDouble())).toBigDecimal().setScale(1, RoundingMode.HALF_DOWN)
        }
        val priceChangePercentageInCandle = lastCandle.let {
            ((it.closePrice.toDouble() - prevCandle.closePrice.toDouble()) / prevCandle.closePrice.toDouble() * 100).toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN)
        }
        return AlertResult(
            isSignal = isSignal,
            volumeX = volumeChangeFromMedianXInCandle,
            pricePercentage = priceChangePercentageInCandle,
            details = "lastCandle: $lastCandle , prevCandle: $prevCandle"
        )
    }

    suspend fun isBigVolumeOnStartSession(candles: List<Candle>): AlertResult {
        val now = LocalDate.now().minusDays(1)
        if (candles.size <= 10) {
            return AlertResult(
                isSignal = false,
                volumeX = null,
                pricePercentage = null
            )
        }
        val startSessionCandles = candles.filter { it.timestamp.hour == 10 && it.timestamp.minute == 0 }
        if (startSessionCandles.isEmpty()) {
            println("startSessionCandles empty")
            return AlertResult(
                isSignal = false,
                volumeX = null,
                pricePercentage = null
            )
        }
        val medianVolume = findMedian(startSessionCandles.map { it.volume })
        if (candles.none { it.timestamp.hour == 10 && it.timestamp.minute == 0 && it.timestamp.toLocalDate() == now}) {
            return AlertResult(
                isSignal = false,
                volumeX = null,
                pricePercentage = null
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

        val scale = 2
        val volumeChangeFromMedianXInCandle = startCurrentSessionCandle.let {
            ((it.volume.toDouble() / medianVolume.toDouble())).toBigDecimal().setScale(1, RoundingMode.HALF_DOWN)
        }
        val priceChangePercentageInCandle = startCurrentSessionCandle.let {
            ((it.closePrice.toDouble() - prevStartCurrentSessionCandle.closePrice.toDouble()) / prevStartCurrentSessionCandle.closePrice.toDouble() * 100).toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN)
        }
        return AlertResult(
            isSignal = bigVolumes.contains(startCurrentSessionCandle),
            volumeX = volumeChangeFromMedianXInCandle,
            pricePercentage = priceChangePercentageInCandle
        )
    }



    suspend fun isBigVolumeOnStartSessionWithPulse(ticker: Ticker, timeframe: Timeframe): Boolean {
        TODO()
    }

    suspend fun isBigVolumeWithPulse(ticker: Ticker, timeframe: Timeframe): Boolean {
        TODO()
    }

}

fun getCandlesCount(ticker: Ticker, timeframe: Timeframe): Int {
    val summaryDays = if (ticker.exchange == Exchanges.MOEX_FORTS) {
        var futureTradingDays = ChronoUnit.DAYS.between(fortsMap[ticker.symbol], LocalDate.now()).toInt() + 1
        if (futureTradingDays < 0) {
            futureTradingDays = 0
        }
        futureTradingDays
    } else {
        days
    }

    return when (timeframe) {
        Timeframe.MIN5 -> { 14 * 12 * summaryDays } //session hours * count in hour * days
        Timeframe.MIN15 -> { 14 * 4 * summaryDays }
        Timeframe.HOUR1 -> { 14 * 1 * summaryDays }
        Timeframe.DAY1 -> { summaryDays }
        else -> { TODO("timeframe not supported yet") }
    }

}

data class AlertResult(
    val isSignal: Boolean,
    val volumeX: BigDecimal?,
    val pricePercentage: BigDecimal?,
    //val priceChangePercentageInDay: Double
    val details: String = ""
)

data class TickerWithAlert (
    val ticker: TickerInfo,
    val indicator: AlertResult
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