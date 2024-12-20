package org.cryptolosers.samples.signals

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.cryptolosers.commons.moexAllTickers
import org.cryptolosers.commons.toStringWith_
import org.cryptolosers.indicators.findMedian
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * Sample
 * Connect to Finam via Transaq connector and get signals for buy or sell.
 * Before run:
 * - copy config/terminalConfig-example.json to config/terminalConfig.json
 * - insert your login and password (of Finam Transaq connector) to config/terminalConfig.json
 */
suspend fun main() {
    val conn  = Connector(TransaqConnector())
    conn.connect()
    val tradingApi: ViewTradingApi = conn.tradingApi()
    val logger = KotlinLogging.logger {}

//    val moexAll = listOf("SVCB")

    thread {
        runBlocking {
            logger.info { "\nЗапуск получения ликвидных:" }
            val startTime = System.currentTimeMillis()
            val executorService = Executors.newFixedThreadPool(5)

            val printTickers = Collections.synchronizedList(ArrayList<TickerWithAverageMoney>())
            moexAllTickers.forEach { t ->
                executorService.submit {
                    runBlocking {
                        val tickers = tradingApi.getAllTickers()
                            .filter { it.ticker.symbol == t && it.ticker.exchange == Exchanges.MOEX }
                        if (tickers.isEmpty()) {
                            logger.warn { "can not find ticker: $t" }
                            return@runBlocking
                        }
                        if (tickers.size > 1) {
                            logger.warn {"find more than one tickers: $t, found: $tickers" }
                            return@runBlocking
                        }
                        val ticker = tickers.first()
                        val candles = tradingApi.getLastCandles(
                            ticker.ticker,
                            Timeframe.DAY1,
                            30,
                            Session.CURRENT_AND_PREVIOUS
                        )
                        if (candles.size < 20) {
                            logger.warn {"Мало свечек: $ticker" }
                            return@runBlocking
                        }

                        val tradedMoneyDay = findMedian(candles.map { it.volume * ((it.closePrice + it.openPrice) * ticker.lotSize.toBigDecimal() / BigDecimal(2)).toLong() })
                        printTickers.add(TickerWithAverageMoney(
                            ticker = ticker,
                            tradedMoneyDay = tradedMoneyDay
                        ))
                    }
                }
            }

            executorService.shutdown()
            val finished = executorService.awaitTermination(3, TimeUnit.MINUTES)
            if (finished) {
                logger.info { "All tasks finished successfully." }
            } else {
                logger.error { "Timeout reached before all tasks completed." }
            }


            val MIN_LIMIT = 20_000_000
            logger.info { "\ntradedMoneyDay only liquid for copy-past: " }
            printTickers.sortedByDescending { it.tradedMoneyDay }.filter { it.tradedMoneyDay > MIN_LIMIT } .forEach {
                println(it.ticker.ticker.symbol)
            }

            logger.info { "\ntradedMoneyDay: " }
            printTickers.sortedByDescending { it.tradedMoneyDay }.forEach {
                println("${it.ticker.shortDescription}(${it.ticker.ticker.symbol}) " +
                        "tradedMoneyDay: ${it.tradedMoneyDay.toStringWith_()}")
            }
            logger.info { "Время работы загрузки свечей: " + ((System.currentTimeMillis().toDouble() - startTime) / 1000).toBigDecimal().setScale(3, RoundingMode.HALF_DOWN) + " сек" }

            logger.info { "\ntradedMoneyDay only liquid: " }
            printTickers.sortedByDescending { it.tradedMoneyDay }.filter { it.tradedMoneyDay > MIN_LIMIT } .forEachIndexed { index, it ->
                println("$index ${it.ticker.shortDescription}(${it.ticker.ticker.symbol}) " +
                        "tradedMoneyDay: ${it.tradedMoneyDay.toStringWith_()}")
            }

            logger.info { "Количество акций: ${printTickers.sortedByDescending { it.tradedMoneyDay }.filter { it.tradedMoneyDay > MIN_LIMIT }.size}" }
        }
    }
}

data class TickerWithAverageMoney(
    val ticker: TickerInfo,
    val tradedMoneyDay: Long
)