package org.cryptolosers.samples.signals

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.cryptolosers.commons.toStringWithSign
import org.cryptolosers.indicators.DraftAlert
import org.cryptolosers.indicators.VolumeAlerts
import org.cryptolosers.indicators.getCandlesCount
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
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
    val moexWatchList = listOf("ROSN", "SBER", "LKOH", "GAZP", "NVTK", "LNZL", "SVCB")

    thread {
        runBlocking {
            while (true) {
                val volumeAlerts = VolumeAlerts()
                logger.info { "\nЗапуск индикаторов:" }
                val startTime = System.currentTimeMillis()
                val executorService = Executors.newFixedThreadPool(5)

                val printSignals = Collections.synchronizedList(ArrayList<DraftAlert>())
                moexWatchList.forEach { t ->
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
                                Timeframe.MIN15,
                                getCandlesCount(ticker.ticker, Timeframe.MIN15),
                                Session.CURRENT_AND_PREVIOUS
                            )
                            val alert = volumeAlerts.isBigVolume(candles)
                            if (alert != null) {
                                printSignals.add(DraftAlert(ticker, alert))
                            }
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

                logger.info { "Volume signals at 10:15, 15min bar: " }
                printSignals.forEach {
                    println("${it.ticker.shortDescription}(${it.ticker.ticker.symbol}) " +
                            "volume: ${it.alert.volumeX?.toStringWithSign()}%, " +
                             "price: ${it.alert.pricePercentage?.toStringWithSign()}%")
                }
                logger.info { "Время работы загрузки свечей: " + ((System.currentTimeMillis().toDouble() - startTime) / 1000).toBigDecimal().setScale(3, RoundingMode.HALF_DOWN) + " сек" }

                delay(3000)
            }
        }
    }
}