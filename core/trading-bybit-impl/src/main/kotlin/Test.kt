package org.cryptolosers.bybit

import kotlinx.coroutines.*
import org.cryptolosers.trading.model.Exchanges
import org.cryptolosers.trading.model.Ticker
import org.cryptolosers.trading.model.Timeframe

fun main(): Unit = runBlocking {

    val api = ByBitViewTradingApi()
    val candles = api.getLastCandles(Ticker("TONUSDT", Exchanges.Bybit), Timeframe.MIN5, candlesCount = 5)
    candles.forEach { println(it) }
}