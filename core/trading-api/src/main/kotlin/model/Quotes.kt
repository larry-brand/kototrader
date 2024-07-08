package org.cryptolosers.trading.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class PriceInfo(val lastPrice: BigDecimal, val bidPrice: BigDecimal, val askPrice: BigDecimal)

data class OrderBook(val ticker: Ticker, val ask: List<BigDecimal>, val bid: List<BigDecimal>)

data class OrderBookEntry(val price: BigDecimal, val size: Int)

data class Candle(
    val timestamp: LocalDateTime,
    val openPrice: BigDecimal,
    val highPrice: BigDecimal,
    val lowPrice: BigDecimal,
    val closePrice: BigDecimal,
    val volume: Long
)

enum class Timeframe {
    TIKS, MIN1, MIN5, MIN15, HOUR1, DAY1
}

enum class Session {
    CURRENT_AND_PREVIOUS, CURRENT
}