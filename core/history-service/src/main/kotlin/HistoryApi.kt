package org.cryptolosers.history

import java.time.LocalDate
import java.time.LocalDateTime

interface HistoryApi {

    fun tickers(): List<HistoryTickerId>

    fun downloadHistory(
        tickerId: HistoryTickerId,
        timeframe: Timeframe,
        startDate: LocalDate,
        endDate: LocalDate,
    )

    fun downloadFullHistory(
        tickerId: HistoryTickerId,
        timeframe: Timeframe
    )

    fun readCandles(
        tickerId: HistoryTickerId,
        timeframe: Timeframe,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<HistoryCandle>

    fun readFullCandles(
        tickerId: HistoryTickerId,
        timeframe: Timeframe
    ): List<HistoryCandle>

    fun runOnCandles(
        tickerId: HistoryTickerId,
        timeframe: Timeframe,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        onNextCandle: (HistoryCandle) -> Unit
    )

    fun runOnTicks(
        tickerId: HistoryTickerId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        onNextTick: (HistoryTick) -> Unit
    )

}