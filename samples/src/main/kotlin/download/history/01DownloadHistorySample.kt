package org.cryptolosers.samples.download.history

import org.cryptolosers.history.HistoryService
import org.cryptolosers.history.HistoryTickerId
import org.cryptolosers.history.LOCAL_DATE_FRIENDLY_PATTERN
import org.cryptolosers.history.Timeframe
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Sample
 * Download full history from Finam to C:\Users\username\.koto-trader\finam\Si\1 day
 */
fun main() {
    val service = HistoryService()
    //service.downloadFullHistory(HistoryTickerId("Si"), Timeframe.MIN1)
    val startDate = LocalDate.parse("2024-06-01", DateTimeFormatter.ofPattern(LOCAL_DATE_FRIENDLY_PATTERN))
    val endDate = LocalDate.parse("2024-06-29", DateTimeFormatter.ofPattern(LOCAL_DATE_FRIENDLY_PATTERN))
    service.downloadHistory(HistoryTickerId("Si"), Timeframe.MIN1, startDate, endDate)
}