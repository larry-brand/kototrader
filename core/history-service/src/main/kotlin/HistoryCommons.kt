package org.cryptolosers.history

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

const val LOCAL_DATE_PATTERN = "yyyyMMdd"
const val LOCAL_DATE_ONLY_FILENAME_PATTERN = "yyyy-MM-dd"
const val LOCAL_DATE_TIME_PATTERN = "yyyyMMdd HHmmss"

const val LOCAL_DATE_FRIENDLY_PATTERN = "yyyy-MM-dd"
const val LOCAL_DATE_TIME_FRIENDLY_PATTERN = "yyyy-MM-dd HH:mm:ss"

enum class Timeframe(val str: String) {

    TIKS("tiks"), MIN1("M1"), MIN5("M5"), MIN15("M15"), MIN30("M30"), HOUR1("H1"), DAY1("D1");

    override fun toString(): String {
        return str
    }
}

data class HistoryTickerId(val name: String)

data class HistoryCandle(
    val timestamp: LocalDateTime,
    val openPrice: BigDecimal,
    val highPrice: BigDecimal,
    val lowPrice: BigDecimal,
    val closePrice: BigDecimal,
    val volume: Long
) {
    override fun toString(): String {
        val timestampString = DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN).withZone(ZoneOffset.UTC).format(timestamp)
        return "$timestampString ${openPrice.toPlainString()} ${highPrice.toPlainString()} ${lowPrice.toPlainString()} ${closePrice.toPlainString()} $volume"
    }
}

data class HistoryTick(
    val timestamp: Instant,
    val price: BigDecimal,
    val volume: Int
) {
    override fun toString(): String {
        val timestampString = DateTimeFormatter.ofPattern("HHmmss").withZone(ZoneOffset.UTC).format(timestamp)
        return "$timestampString ${price.toPlainString()} $volume"
    }
}