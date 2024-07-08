package org.cryptolosers.history.file

import org.cryptolosers.commons.file.ProjectHomeFile
import org.cryptolosers.history.*
import java.io.BufferedReader
import java.io.File.separator
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

class HistoryFile : ProjectHomeFile {
    val periodicity: Timeframe

    companion object {
        const val DOT_DATA = ".data"
        const val PROVIDER = "finam"

        fun wrapFilenameMinHourDay(tickerId: HistoryTickerId, periodicity: Timeframe, year: Int, extension: String): String {
            if (periodicity == Timeframe.TIKS) throw IllegalArgumentException("Constructor is not supported for tiks")
            return PROVIDER + separator + tickerId.name + separator + periodicity + separator + year + extension
        }

        fun wrapFilenameTiks(tickerId: HistoryTickerId, periodicity: Timeframe, date: LocalDate, extension: String): String {
            if (periodicity != Timeframe.TIKS) throw IllegalArgumentException("Constructor is not supported for day, hour, min, sec. Only for tiks")
            val dateString = date.format(DateTimeFormatter.ofPattern(LOCAL_DATE_ONLY_FILENAME_PATTERN))
            return PROVIDER + separator + tickerId.name + separator + periodicity + separator +  date.year + separator + dateString +  extension
        }
    }

    constructor(tickerId: HistoryTickerId, periodicity: Timeframe, date: LocalDate): super(wrapFilenameTiks(tickerId, periodicity, date, DOT_DATA)) {
        this.periodicity = periodicity
    }
    constructor(tickerId: HistoryTickerId, periodicity: Timeframe, year: Int): super(wrapFilenameMinHourDay(tickerId, periodicity, year, DOT_DATA)) {
        this.periodicity = periodicity
    }

    fun supportedNowDate(): Boolean  {
        if (periodicity != Timeframe.TIKS) {
            if (LocalDate.now().year == filename.split(".").firstOrNull()?.toInt()) {
                return true
            }

        } else {
            if (LocalDate.now().year == filename.split("-")[0].toInt()
                && LocalDate.now().monthValue == filename.split("-")[1].toInt()
                && LocalDate.now().dayOfMonth == filename.split("-")[2].toInt()) {
                return true
            }
        }
        return false
    }

    fun ifDateNowExists(): Boolean {
        if (periodicity != Timeframe.TIKS) {
            file.useLines { lines ->
                lines.forEach { line ->
                    val dateString = line.split(" ")[0]
                    val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
                    if (date == getPreviousWorkingDay(LocalDate.now())) {
                        return true
                    }
                }
            }
        } else { // 1min , 1hour, 1day
            file.useLines { lines ->
                lines.forEach { line ->
                    val dateString = line.split(" ")[0]
                    val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
                    if (date == getPreviousWorkingDay(LocalDate.now())) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun forEachCandle(startDate: LocalDateTime, endDate: LocalDateTime, action: (HistoryCandle) -> Unit) {
        val candles: MutableList<HistoryCandle> = mutableListOf()
        bufferedReader().forEachLine {
            val words = it.split(" ")
            val iDate = LocalDateTime.parse("${words[0]} ${words[1]}", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN))
            if ((iDate.isAfter(startDate) || iDate == startDate) && (iDate.isBefore(endDate) || iDate == endDate)) {
                candles.add(
                    HistoryCandle(
                        LocalDateTime.parse("${words[0]} ${words[1]}", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN)),
                        BigDecimal(words[2]),
                        BigDecimal(words[3]),
                        BigDecimal(words[4]),
                        BigDecimal(words[5]),
                        words[6].toLong()
                    )
                )
            }
        }
        candles.forEach { action(it) }
    }

    fun getPreviousWorkingDay(date: LocalDate): LocalDate {
        val dayOfWeek = DayOfWeek.of(date.get(ChronoField.DAY_OF_WEEK));
        when (dayOfWeek) {
            DayOfWeek.SATURDAY -> return date.minus(1, ChronoUnit.DAYS)
            DayOfWeek.SUNDAY -> return date.minus(2, ChronoUnit.DAYS)
        }
        return date
    }

    fun exists(): Boolean = file.exists()

    fun bufferedReader(): BufferedReader {
        return file.bufferedReader()
    }

}