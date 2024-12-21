package org.cryptolosers.bybit

import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.model.*
import java.time.LocalDateTime

class ByBitViewTradingApi: ViewTradingApi {

    val byBitCandlesHandler = ByBitCandlesHandler()

    override suspend fun getAllTickers(): List<TickerInfo> {
        return listOf(
            TickerInfo(
                ticker = Ticker("BTCUSDT", Exchanges.Bybit),
                shortDescription = "Bitcoin",
                type = "Crypto",
                lotSize = 0
            ),
            TickerInfo(
                ticker = Ticker("TONUSDT", Exchanges.Bybit),
                shortDescription = "Ton",
                type = "Crypto",
                lotSize = 0
            )
        )
    }

    override suspend fun getPrice(ticker: Ticker): PriceInfo {
        TODO("Not yet implemented")
    }

    override suspend fun subscribePriceChanges(ticker: Ticker, priceChangesListener: (PriceInfo) -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderBook(ticker: Ticker): OrderBook {
        TODO("Not yet implemented")
    }

    override suspend fun getCandles(
        ticker: Ticker,
        timeframe: Timeframe,
        startTimestamp: LocalDateTime,
        endTimestamp: LocalDateTime
    ): List<Candle> {
        TODO("Not yet implemented")
    }

    override suspend fun getLastCandles(
        ticker: Ticker,
        timeframe: Timeframe,
        candlesCount: Int,
        session: Session
    ): List<Candle> {
        if (session == Session.CURRENT) {
            TODO()
        }
        val interval = when(timeframe) {
            Timeframe.MIN1 -> "1"
            Timeframe.MIN5 -> "5"
            Timeframe.MIN15 -> "15"
            Timeframe.HOUR1 -> "60"
            Timeframe.DAY1 -> "D"
            else -> throw IllegalStateException("Timeframe not supported")
        }
        return byBitCandlesHandler.getCandles(symbol = ticker.symbol, interval = interval, limit = candlesCount)
    }
}