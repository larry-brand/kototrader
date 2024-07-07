package org.cryptolosers.trading

import org.cryptolosers.trading.model.*
import java.time.LocalDateTime

interface TradingApi {
    /** tickers data */
    suspend fun getAllTickers(): List<TickerInfo>


    /** quotes data */
    suspend fun getPrice(ticker: Ticker): PriceInfo
    suspend fun subscribePriceChanges(ticker: Ticker, priceChangesListener: (PriceInfo) -> Unit)
    suspend fun getOrderBook(ticker: Ticker): OrderBook
    suspend fun getCandles(ticker: Ticker, timeframe: Timeframe, startTimestamp: LocalDateTime, endTimestamp: LocalDateTime): List<Candle>
    suspend fun getLastCandles(ticker: Ticker, timeframe: Timeframe, candlesCount: Int, session: Session): List<Candle>


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