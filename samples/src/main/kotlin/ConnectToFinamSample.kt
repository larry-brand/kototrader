package org.cryptolosers.samples

import kotlinx.coroutines.runBlocking
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.Exchange_MOEX_FORTS
import org.cryptolosers.trading.model.Session
import org.cryptolosers.trading.model.Ticker
import org.cryptolosers.trading.model.Timeframe
import org.cryptolosers.transaq.FinamFutureInstrument
import org.cryptolosers.transaq.connector.concurrent.InternalTransaqConnector
import kotlin.concurrent.thread

/**
 * Sample
 * Connect to Finam via Transaq connector and print balance, positions, orders. It is not modify anything.
 * Before run:
 * - copy config/terminalConfig-example.json to config/terminalConfig.json
 * - insert your login and password (of Finam Transaq connector) to config/terminalConfig.json
 */
suspend fun main() {
    val conn  = Connector(InternalTransaqConnector())
    conn.connect()

    thread {
        runBlocking {
            val tickers = conn.tradingApi().getAllTickers()
                .filter { it.ticker.symbol.startsWith("Si") && it.type == FinamFutureInstrument }
            println("Tickers:")
            tickers.forEach {
                println(it)
            }

            println("Positions:")
            conn.tradingApi().getAllOpenPositions().forEach {
                println(it)
            }

            println("Orders:")
            conn.tradingApi().getAllOrders().forEach {
                println(it)
            }

            conn.tradingApi().subscribePriceChanges(Ticker("SiU4", Exchange_MOEX_FORTS)) {
                println("Subscribed $it")
            }

            println("LastCandles:")
            conn.tradingApi().getLastCandles(Ticker("SiU4", Exchange_MOEX_FORTS), Timeframe.MIN5, 3, Session.CURRENT_AND_PREVIOUS).forEach {
                println(it)
            }

//            println("Price:")
//            conn.tradingApi().getPrice(Ticker("SiU4", Exchange_MOEX_FORTS)).let {
//                println(it)
//            }
        }
    }

    conn.abort()
}