package org.cryptolosers.trading

import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.time.Instant

interface TradingApi {
    /** tickers data */
    suspend fun getAllTickers(): List<TickerInfo>


    /** quotes data */
    suspend fun getPrice(ticker: Ticker): BigDecimal
    suspend fun getOrderBook(ticker: Ticker): OrderBook
    suspend fun getCandles(ticker: Ticker, periodicity: Timeframe, startTimestamp: Instant, endTimestamp: Instant)


    /** orders commands */
    suspend fun sendOrder(order: IOrder)
    suspend fun removeOrder(orderId: Long)


    /** operations */
    suspend fun getOpenPosition(ticker: Ticker): Position?
    suspend fun getAllOpenPositions(): List<Position>
    suspend fun getOrders(ticker: Ticker): List<IOrder>
    suspend fun getAllOrders(): List<IOrder>
    suspend fun getOperations(ticker: Ticker): List<Operation>
    suspend fun getAllOperations(): List<Operation>
    suspend fun getWallet(): Wallet
}