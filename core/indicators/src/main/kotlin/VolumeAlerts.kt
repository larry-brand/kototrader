package org.cryptolosers.indicators

import mu.KotlinLogging
import org.cryptolosers.commons.fortsMap
import org.cryptolosers.indicators.VolumeAlerts.Companion.days
import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class VolumeAlerts() {
    private val logger = KotlinLogging.logger {}

    companion object {
        val days = 2 * 22 // days for calculate volume
    }

    suspend fun isBigVolume(candles: List<Candle>): BigVolumeAlertResult {
        val medianVolume = findMedian(candles.map { it.volume })

        val lastCandle = candles.last()
        val prevCandleIndex = if (candles.indexOf(lastCandle) != 0) (candles.indexOf(lastCandle) - 1) else 0
        val prevCandle = candles[prevCandleIndex]

        val volumeX = lastCandle.let {
            ((it.volume.toDouble() / medianVolume.toDouble())).toBigDecimal().setScale(1, RoundingMode.HALF_DOWN)
        }
        val pricePercentage = lastCandle.let {
            ((it.closePrice.toDouble() - prevCandle.closePrice.toDouble()) / prevCandle.closePrice.toDouble() * 100).toBigDecimal().setScale(2, RoundingMode.HALF_DOWN)
        }

        return BigVolumeAlertResult(
            volumeX = volumeX,
            pricePercentage = pricePercentage,
            lastCandleVolume = lastCandle.volume,
            medianVolume = medianVolume,
            details = "lastCandle: $lastCandle , prevCandle: $prevCandle, medianVolume: $medianVolume"
        )

    }

//    suspend fun isBigVolumeOnStartSession(candles: List<Candle>, now: LocalDate): AlertResult? {
//        if (candles.size <= 10) {
//            logger.warn { "Слишком мало свечек: ${candles.size}" }
//            return null
//        }
//        val startSessionCandles = candles.filter { it.timestamp.hour == 10 && it.timestamp.minute == 0 }
//        if (startSessionCandles.isEmpty()) {
//            logger.warn { "нет свечь начала дня" }
//            return null
//        }
//        val medianVolume = findMedian(startSessionCandles.map { it.volume })
//        val startCurrentSessionCandle = candles.firstOrNull { it.timestamp.hour == 10 && it.timestamp.minute == 0 && it.timestamp.toLocalDate() == now}
//        if (startCurrentSessionCandle == null) {
//            logger.warn { "нет свечи начала текущего дня" }
//            return null
//        }
//
//        var prevStartCurrentSessionCandleIndex = if (candles.indexOf(startCurrentSessionCandle) != 0) (candles.indexOf(startCurrentSessionCandle) - 1) else 0
//        var prevStartCurrentSessionCandle: Candle
//        do {
//            prevStartCurrentSessionCandle = candles[prevStartCurrentSessionCandleIndex]
//            prevStartCurrentSessionCandleIndex--
//        } while (prevStartCurrentSessionCandle.timestamp.toLocalDate().isEqual(now) && prevStartCurrentSessionCandleIndex >= 0)
//
//        val bigVolumes = startSessionCandles.filter { it.volume > bigVolume * medianVolume }
//        val isAlert = bigVolumes.contains(startCurrentSessionCandle)
//
//        if (isAlert) {
//            val scale = 2
//            val volumeX = startCurrentSessionCandle.let {
//                ((it.volume.toDouble() / medianVolume.toDouble())).toBigDecimal().setScale(1, RoundingMode.HALF_DOWN)
//            }
//            val pricePercentage = startCurrentSessionCandle.let {
//                ((it.closePrice.toDouble() - prevStartCurrentSessionCandle.closePrice.toDouble()) / prevStartCurrentSessionCandle.closePrice.toDouble() * 100).toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN)
//            }
//            return AlertResult(
//                volumeX = volumeX,
//                pricePercentage = pricePercentage,
//                details = "startCurrentSessionCandle: $startCurrentSessionCandle, median volume: $medianVolume"
//            )
//        } else {
//            return null
//        }
//
//    }



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

data class BigVolumeAlertResult(
    val volumeX: BigDecimal,
    val pricePercentage: BigDecimal,
    //val pricePercentageInDay: BigDecimal,
    internal val lastCandleVolume: Long,
    internal val medianVolume: Long,
    val details: String = ""
)

fun BigVolumeAlertResult.isAlert(volumeXMedian: BigDecimal): Boolean {
    return lastCandleVolume.toBigDecimal() > volumeXMedian * medianVolume.toBigDecimal()
}

data class TickerWithAlert (
    val ticker: TickerInfo,
    val alert: BigVolumeAlertResult
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