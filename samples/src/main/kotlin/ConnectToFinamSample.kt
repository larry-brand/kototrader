package org.cryptolosers.samples

import kotlinx.coroutines.runBlocking
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.FinamFutureInstrument
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import kotlin.concurrent.thread

/**
 * Sample
 * Connect to Finam via Transaq connector and print balance, positions, orders. It is not modify anything.
 * Before run:
 * - copy config/terminalConfig-example.json to config/terminalConfig.json
 * - insert your login and password (of Finam Transaq connector) to config/terminalConfig.json
 */
suspend fun main() {
    val conn  = Connector(TransaqConnector())
    conn.connect()
    val tradingApi = conn.tradingApi()

    thread {
        runBlocking {
            val tickers = tradingApi.getAllTickers()
                .filter { it.ticker.symbol.startsWith("Si") && it.type == FinamFutureInstrument }
            println("Tickers:")
            tickers.forEach {
                println(it)
            }

            println("Positions:")
            tradingApi.getAllOpenPositions().forEach {
                println(it)
            }

            println("Orders:")
            tradingApi.getAllOrders().forEach {
                println(it)
            }

            tradingApi.subscribePriceChanges(Ticker("SiZ4", Exchanges.MOEX_FORTS)) {
                println("Subscribed $it")
            }

            println("LastCandles:")
            tradingApi.getLastCandles(Ticker("SiZ4", Exchanges.MOEX_FORTS), Timeframe.MIN15, 10, Session.CURRENT_AND_PREVIOUS).forEach {
                println(it)
            }

//            println("Price:")
//            tradingApi.getPrice(Ticker("SiZ4", Exchange_MOEX_FORTS)).let {
//                println(it)
//            }
        }
    }

    conn.abort()
}