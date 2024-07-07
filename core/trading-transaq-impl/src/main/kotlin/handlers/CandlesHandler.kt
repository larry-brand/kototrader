package org.cryptolosers.transaq.handlers

import mu.KotlinLogging
import org.cryptolosers.trading.model.Candle
import org.cryptolosers.trading.model.Timeframe
import org.cryptolosers.transaq.SecCodeBoard
import org.cryptolosers.transaq.TickerTimeframe
import org.cryptolosers.transaq.TransaqCandles
import org.cryptolosers.transaq.TransaqMemory
import org.cryptolosers.transaq.xml.callback.Candles
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CandlesHandler(val memory: TransaqMemory) {
    private val logger = KotlinLogging.logger {}

    fun handle(candles: Candles) {
        runCatching {
            handleInternal(candles)
        }.getOrElse {
            logger.error(it) { "Can not handle candles" }
        }
    }

    private fun handleInternal(candles: Candles) {
        val ticker = memory.tickerSecCodeBoardMap[SecCodeBoard(
            secCode = candles.seccode,
            board = candles.board
        )]!!.tickerInfo.ticker
        val timeframe = when (candles.period) {
            1L -> Timeframe.MIN1
            2L -> Timeframe.MIN5
            3L -> Timeframe.MIN15
            4L -> Timeframe.HOUR1
            5L -> Timeframe.DAY1
            else -> throw IllegalStateException("Timeframe is not supported")
        }
        val tickerTimeframe = TickerTimeframe(ticker = ticker, timeframe = timeframe)
        memory.candlesMap.computeIfAbsent(tickerTimeframe) {
            TransaqCandles(mutableListOf())
        }

        val candlesMapped = candles.candle.mapNotNull {
            runCatching {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS")
                val timestamp = LocalDateTime.from(formatter.parse(it.date))
                Candle(
                    timestamp = timestamp,
                    openPrice = it.open,
                    highPrice = it.high,
                    lowPrice = it.low,
                    closePrice = it.close,
                    volume = it.volume
                )
            }.getOrDefault(null)
        }

        val memoryCandles = memory.candlesMap[tickerTimeframe]!!
        memoryCandles.candles.addAll(candlesMapped)

        // статус 2 это продолжение следует (будет еще порция)
        if (candles.status != 2L ) {
            TransaqCandles.signalAll(tickerTimeframe, memory)
        }
    }
}