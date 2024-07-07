package org.cryptolosers.trading.model

import java.math.BigDecimal

interface IOrder {
}

abstract class AbstractOrder(
    open val ticker: Ticker,
    open val size: Long,
    open val orderDirection: OrderDirection,
    open val orderId: String? = null
) : IOrder

enum class OrderDirection {
    BUY, SELL
}

data class MarketOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
    override val orderId: String? = null
) : AbstractOrder(ticker, size, orderDirection, orderId)

data class LimitOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
    override val orderId: String? = null,
    val price: BigDecimal
) : AbstractOrder(ticker, size, orderDirection, orderId)

data class StopOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
    val activationPrice: BigDecimal,
    val slippage: BigDecimal,
    override val orderId: String? = null
) : AbstractOrder(ticker, size, orderDirection, orderId)

data class TakeprofitOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
    val activationPrice: BigDecimal,
    //TODO: добавить поля: коррекция, защитный спред
    override val orderId: String? = null
) : AbstractOrder(ticker, size, orderDirection, orderId)

data class StopAndTakeprofitOrder(
    val stopOrder: StopOrder,
    val takeprofitOrder: TakeprofitOrder
) : IOrder