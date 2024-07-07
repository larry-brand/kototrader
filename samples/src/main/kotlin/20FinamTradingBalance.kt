package org.cryptolosers.samples

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.Exchange_MOEX_FORTS
import org.cryptolosers.trading.model.Ticker
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
                .filter { it.ticker.symbol.startsWith("BR") && it.type == FinamFutureInstrument }
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

            println("Price:")
            conn.tradingApi().getPrice(Ticker("BRJ5", Exchange_MOEX_FORTS)).let {
                println(it)
            }
        }
    }

    conn.abort()
}