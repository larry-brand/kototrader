package org.cryptolosers.samples.download.history

import com.google.gson.Gson
import org.cryptolosers.history.*
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Sample
 * Download full history from Finam to C:\Users\username\.koto-trader\finam\Si\1 day
 */
fun main() {
    val service = HistoryService()
    val startDate = LocalDateTime.parse("20210108 100100", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN))
    val endDate = LocalDateTime.parse("20220110 220100", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN))
    val candles = service.readCandles(HistoryTickerId("Si"), Timeframe.DAY1,  startDate, endDate)
    println(candles.size)

    var ohlcv: MutableList<MutableList<BigDecimal>> = mutableListOf()
    candles.forEach {
        ohlcv.add(
            mutableListOf(
                it.timestamp.atZone(ZoneId.systemDefault()).toEpochSecond().toBigDecimal(),
                it.openPrice,
                it.highPrice,
                it.lowPrice,
                it.closePrice,
                it.volume.toBigDecimal()
            )
        )
    }


    val v = VueCanle()
    v.ohlcv = ohlcv
    val strResult = Gson().toJson(v, VueCanle::class.java)
    println(strResult)
    File("C:\\Projects\\koto-trader\\frontend\\src\\data.json").writeText(strResult)

}

class VueCanle() {
    var ohlcv: MutableList<MutableList<BigDecimal>>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VueCanle

        if (ohlcv != other.ohlcv) return false

        return true
    }

    override fun hashCode(): Int {
        return ohlcv?.hashCode() ?: 0
    }


}