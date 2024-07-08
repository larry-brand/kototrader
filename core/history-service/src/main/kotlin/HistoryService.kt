package org.cryptolosers.history

import org.cryptolosers.history.download.api.DownloadHistoryApi
import org.cryptolosers.history.file.HistoryFile
import org.cryptolosers.history.file.HistoryTmpFile
import org.cryptolosers.history.file.PropertiesFile
import org.cryptolosers.history.download.impl.FinamDownloadHistoryService
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import mu.KotlinLogging

class HistoryService: HistoryApi {

    private val expectedFirstTradingYear = 1990
    private val msDelayBetweenDownloadCalls = 200L
    private val maxRetriesDownloadCalls = 10
    private val api: DownloadHistoryApi = FinamDownloadHistoryService()
    private val logger = KotlinLogging.logger {}
    @Volatile
    private var downloading = false

    companion object {
        private const val MMDD_FIRST = "0101"
        private const val MMDD_LAST = "1231"
    }

    override fun tickers(): List<HistoryTickerId> {
        return api.tickers()
    }

    override fun downloadHistory(
        tickerId: HistoryTickerId,
        timeframe: Timeframe,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (timeframe == Timeframe.TIKS) {
            throw IllegalArgumentException()
        }
        downloadHistoryMinHourDayInternal(startDate, endDate, tickerId, timeframe)
    }

    override fun downloadFullHistory(
        tickerId: HistoryTickerId,
        timeframe: Timeframe
    ) {
        if (timeframe != Timeframe.TIKS)
            downloadFullHistoryMinHourDay(tickerId, timeframe)
        else
            downloadFullHistoryTiks(tickerId)
    }

    override fun readCandles(
        tickerId: HistoryTickerId,
        timeframe: Timeframe,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<HistoryCandle> {
        if (timeframe == Timeframe.TIKS) {
            throw IllegalArgumentException()
        }
        // force download 1 year if start date is current year
        if (startDate.year == LocalDateTime.now().year) {
            downloadHistoryMinHourDayInternal(startDate.toLocalDate(), LocalDate.now(), tickerId, timeframe)
        }
        val years = ChronoUnit.YEARS.between(
            startDate.atZone(ZoneId.systemDefault()),
            endDate.atZone(ZoneId.systemDefault())
        )
        var candles = mutableListOf<HistoryCandle>()
        for (i in 0..years) {
            val iYear = startDate.toLocalDate().plusYears(i).year
            //TODO: read with buffer
            HistoryFile(tickerId, timeframe, iYear).forEachCandle(startDate, endDate) {
                candles.add(it)
            }
        }
        return candles
    }

    override fun readFullCandles(tickerId: HistoryTickerId, timeframe: Timeframe): List<HistoryCandle> {
        if (timeframe == Timeframe.TIKS) {
            throw IllegalArgumentException()
        }
        val firstTradingDay = firstTradingDay(tickerId)
        val years = ChronoUnit.YEARS.between(
            firstTradingDay,
            LocalDate.now()
        )
        var candles = mutableListOf<HistoryCandle>()
        for (i in 0..years) {
            val iYear = firstTradingDay.plusYears(i).year
            //TODO: read with buffer
            HistoryFile(tickerId, timeframe, iYear).forEachCandle(firstTradingDay.atStartOfDay(), LocalDateTime.now()) {
                candles.add(it)
            }
        }
        return candles
    }

    override fun runOnCandles(
        tickerId: HistoryTickerId,
        timeframe: Timeframe,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        onNextCandle: (HistoryCandle) -> Unit
    ) {
        if (timeframe == Timeframe.TIKS) {
            throw IllegalArgumentException()
        }
        val years = ChronoUnit.YEARS.between(
            startDate.atZone(ZoneId.systemDefault()),
            endDate.atZone(ZoneId.systemDefault())
        )

        for (i in 0..years) {
            val iYear = startDate.toLocalDate().plusYears(i).year
            HistoryFile(tickerId, timeframe, iYear).forEachCandle(startDate, endDate) {
                onNextCandle(it)
            }
        }
    }

    override fun runOnTicks(
        tickerId: HistoryTickerId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        onNextTick: (HistoryTick) -> Unit
    ) {
        TODO()
    }

    private fun downloadFullHistoryMinHourDay(
        tickerId: HistoryTickerId,
        timeframe: Timeframe
    ) {
        if (timeframe == Timeframe.TIKS) {
            throw IllegalArgumentException("Use downloadFullHistoryTiks instead")
        }
        val firstTradingDay = firstTradingDay(tickerId)
        downloadHistoryMinHourDayInternal(firstTradingDay, LocalDate.now(), tickerId, timeframe)
    }

    private fun downloadFullHistoryTiks(
        tickerId: HistoryTickerId
    ) {
        val firstTradingDay = firstTradingDay(tickerId)

        val days = firstTradingDay.until(LocalDate.now(), ChronoUnit.DAYS)

        for (i in 0..days) {
            val iDate = firstTradingDay.plusDays(i)
            val expFile = HistoryFile(tickerId, Timeframe.TIKS, iDate)
            if (!expFile.exists() || (expFile.supportedNowDate() && !expFile.ifDateNowExists())) { //TODO
                val bytes = runWithRetries(maxRetriesDownloadCalls) {
                    api.download(tickerId, Timeframe.TIKS, iDate, iDate)
                }
                val candles = api.parseBytesToTiks(bytes)
                val tmpFile = HistoryTmpFile(tickerId, Timeframe.TIKS, iDate)
                tmpFile.save(candles)
                tmpFile.renameToOkFile()
            }
        }
    }

    private fun downloadHistoryMinHourDayInternal(startDate: LocalDate, endDate: LocalDate, tickerId: HistoryTickerId, timeframe: Timeframe) {
        try {
            if (downloading) throw IllegalStateException("Downloading could not be parallel ")
            if (timeframe == Timeframe.TIKS) {
                throw IllegalArgumentException("Use downloadHistoryTiks instead")
            }
            downloading = true

            for (iYear in startDate.year ..endDate.year) {
                val expFile = HistoryFile(tickerId, timeframe, iYear)
                if (!expFile.exists() || (expFile.supportedNowDate() && !expFile.ifDateNowExists())) {
                    val firstDateOfYear = LocalDate.parse("$iYear$MMDD_FIRST", DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
                    val lastDateOfYear = LocalDate.parse("$iYear$MMDD_LAST", DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))

                    val bytes = runWithRetries(maxRetriesDownloadCalls) {
                        api.download(tickerId, timeframe, firstDateOfYear, lastDateOfYear)
                    }
                    val candles = api.parseBytesToCandles(bytes)
                    if (candles.isNotEmpty()) {
                        val tmpFile = HistoryTmpFile(tickerId, timeframe, iYear)
                        tmpFile.save(candles)
                        tmpFile.renameToOkFile()
                    }
                } else {
                    logger.debug { "Data for ticker=${tickerId.name} [$startDate-$endDate] won't be downloaded, because data exists" }
                }
            }
        } finally {
            downloading = false
        }

    }

    private fun firstTradingDay(tickerId: HistoryTickerId): LocalDate {
        val key = HistoryFile.PROVIDER + "." + tickerId.name + ".firstDay"
        var firstDay = PropertiesFile().readProperties()[key]
        if (firstDay == null) {
            for (iYear in expectedFirstTradingYear..LocalDate.now().year) {
                val firstDateOfYear = LocalDate.parse("$iYear$MMDD_FIRST", DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
                val lastDateOfYear = LocalDate.parse("$iYear$MMDD_LAST", DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
                val bytes = runWithRetries(maxRetriesDownloadCalls) {
                    api.download(tickerId, Timeframe.DAY1, firstDateOfYear, lastDateOfYear)
                }
                val candles = api.parseBytesToCandles(bytes)
                if (candles.isNotEmpty()) {
                    val date = candles.first().timestamp.toLocalDate()
                    val dateString = date.format(DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
                    PropertiesFile().saveProperties(key, dateString)
                    return date
                }
            }
            throw IllegalStateException("first trading day can not parsed")
        } else {
            return LocalDate.parse(firstDay.toString(), DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
        }
    }

    private fun <T> runWithRetries(maxRetries: Int, task: () -> T): T {
        var count = 0
        while (count < maxRetries) {
            try {
                return task()
            } catch (e: IOException) {
                // print only for finam provider
                if (!File("finam-download-history-credentials.json").exists()) {
                    println("If you get authorization token error, please create file finam-download-history-credentials.json")
                }
                if (++count >= maxRetries)
                    throw e
            }
            Thread.sleep(msDelayBetweenDownloadCalls)
        }
        throw IllegalStateException()
    }
}