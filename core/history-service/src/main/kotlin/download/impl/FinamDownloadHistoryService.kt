package org.cryptolosers.history.download.impl

import com.google.gson.Gson
import mu.KotlinLogging
import org.cryptolosers.commons.file.ConfigFile
import org.cryptolosers.commons.file.ConfigFile.Companion.FINAM_DOWNLOAD_HISTORY_CRED_FILE
import org.cryptolosers.commons.file.ConfigFile.Companion.FINAM_DOWNLOAD_HISTORY_FILE
import org.cryptolosers.history.*
import org.cryptolosers.history.download.api.DownloadHistoryApi
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FinamDownloadHistoryService : DownloadHistoryApi {

    private val logger = KotlinLogging.logger {}

    override fun tickers(): List<HistoryTickerId> {
        val finamTickersFile = ConfigFile(FINAM_DOWNLOAD_HISTORY_FILE).readText()
        val finamTickers = Gson().fromJson(finamTickersFile, FinamDownloadHistoryConfig::class.java).tickers
        return finamTickers.map { HistoryTickerId(it.tickerName) }
    }

    override fun download(
        tickerId: HistoryTickerId,
        periodicity: Timeframe,
        startDate: LocalDate,
        endDate: LocalDate
    ): ByteArray {
        logger.debug { "Called api.download, ticker=${tickerId.name} startDate=$startDate endDate=$endDate" }
        val finamTickersFile = ConfigFile(FINAM_DOWNLOAD_HISTORY_FILE).readText()
        val finamTicker = Gson().fromJson(finamTickersFile, FinamDownloadHistoryConfig::class.java).tickers
            .find { tickerId.name == it.tickerName } ?: throw IllegalArgumentException("Ticker not found in $FINAM_DOWNLOAD_HISTORY_FILE, please add it")

        // Usually it works without token
        var token = ""
        if (ConfigFile(FINAM_DOWNLOAD_HISTORY_CRED_FILE).exists()) {
            val finalCredentialsFile = ConfigFile(FINAM_DOWNLOAD_HISTORY_CRED_FILE).readText()
            val finamCredentials = Gson().fromJson(finalCredentialsFile, FinamDownloadHistoryCredentialsConfig::class.java)
            token = finamCredentials.token
        }

        val finamDateFormat = "dd.MM.yyyy"
        val finamPeriodicity = when (periodicity) {
            Timeframe.TIKS -> "1"
            Timeframe.MIN1 -> "2"
            Timeframe.MIN5 -> "3"
            Timeframe.MIN15 -> "5"
            Timeframe.MIN30 -> "6"
            Timeframe.HOUR1 -> "7"
            Timeframe.DAY1 -> "8"
        }
        val finamDatf = when (periodicity) {
            Timeframe.TIKS -> "6"
            else -> "1"
        }

        val url = "https://export.finam.ru/export9.out?" +
                "market=${finamTicker.market}&em=${finamTicker.em}&token=${token}&code=${finamTicker.code}" +
                "&apply=0&df=${startDate.dayOfMonth}&mf=${startDate.monthValue - 1}&yf=${startDate.year}" +
                "&from=${startDate.format(DateTimeFormatter.ofPattern(finamDateFormat))}" +
                "&dt=${endDate.dayOfMonth}&mt=${endDate.monthValue - 1}&yt=${endDate.year}" +
                "&to=${endDate.format(DateTimeFormatter.ofPattern(finamDateFormat))}" +
                "&p=$finamPeriodicity&f=OUTPUT_FILE&e=.txt&cn=${finamTicker.code}&dtf=1" +
                "&tmf=1&MSOR=1&mstime=on&mstimever=1&sep=5&sep2=1&datf=$finamDatf"
        logger.info { "Downloading... : $url" }

        return inputStream(url).readBytes()
    }

    override fun parseBytesToCandles(data: ByteArray): List<HistoryCandle> {
        val result: MutableList<HistoryCandle> = mutableListOf()
        val streamReader = InputStreamReader(ByteArrayInputStream(data), StandardCharsets.UTF_8)
        val bufferedReader = BufferedReader(streamReader)

        var line: String?
        while (bufferedReader.readLine().also { line = it } != null && line?.isNotBlank() == true) {
            val elements = line?.split(" ")!!
            val candle = HistoryCandle(
                timestamp = LocalDateTime.parse(
                    "${elements[2]} ${elements[3]}", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN)),
                openPrice = elements[4].toBigDecimal().stripTrailingZeros(),
                highPrice = elements[5].toBigDecimal().stripTrailingZeros(),
                lowPrice = elements[6].toBigDecimal().stripTrailingZeros(),
                closePrice = elements[7].toBigDecimal().stripTrailingZeros(),
                volume = elements[8].toLong()
            )
            result.add(candle)
        }
        return result
    }

    override fun parseBytesToTiks(data: ByteArray): List<HistoryTick> {
        val result: MutableList<HistoryTick> = mutableListOf()
        val streamReader = InputStreamReader(ByteArrayInputStream(data), StandardCharsets.UTF_8)
        val bufferedReader = BufferedReader(streamReader)

        var line: String?
        while (bufferedReader.readLine().also { line = it } != null && line?.isNotBlank() == true) {
            val elements = line?.split(" ")!!
            val candle = HistoryTick(
                timestamp = LocalDateTime.parse(
                    "${elements[2]} ${elements[3]}", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN)
                ).atZone(ZoneOffset.UTC).toInstant(),
                price = elements[4].toBigDecimal().stripTrailingZeros(),
                volume = elements[5].toInt()
            )
            result.add(candle)
        }
        return result
    }

    fun inputStream(url: String): InputStream {
        return URL(url).openStream()
    }


}