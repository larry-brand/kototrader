package org.cryptolosers.trading

import org.cryptolosers.trading.model.*
import java.time.LocalDateTime

interface ViewTradingApi {
    /** tickers data */
    suspend fun getAllTickers(): List<TickerInfo>

    /** quotes data */
    suspend fun getPrice(ticker: Ticker): PriceInfo
    suspend fun subscribePriceChanges(ticker: Ticker, priceChangesListener: (PriceInfo) -> Unit)
    //TODO: add suspend fun unsubscribePriceChanges(ticker: Ticker)
    suspend fun getOrderBook(ticker: Ticker): OrderBook
    suspend fun getCandles(ticker: Ticker, timeframe: Timeframe, startTimestamp: LocalDateTime, endTimestamp: LocalDateTime): List<Candle>
    suspend fun getLastCandles(ticker: Ticker, timeframe: Timeframe, candlesCount: Int, session: Session): List<Candle>


}