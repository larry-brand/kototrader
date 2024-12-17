package org.cryptolosers.trading.model

import java.math.BigDecimal

interface IOrder {
}

abstract class AbstractOrder(
    open val ticker: Ticker,
    open val size: Long,
    open val orderDirection: OrderDirection,
) : IOrder

enum class OrderDirection {
    BUY, SELL
}

data class MarketOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
) : AbstractOrder(ticker, size, orderDirection)

data class LimitOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
    val price: BigDecimal
) : AbstractOrder(ticker, size, orderDirection)

data class StopOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
    val activationPrice: BigDecimal,
    val slippage: BigDecimal,
) : AbstractOrder(ticker, size, orderDirection)

data class TakeprofitOrder(
    override val ticker: Ticker,
    override val size: Long,
    override val orderDirection: OrderDirection,
    val activationPrice: BigDecimal,
    //TODO: добавить поля: коррекция, защитный спред
) : AbstractOrder(ticker, size, orderDirection)

data class StopAndTakeprofitOrder(
    val stopOrder: StopOrder,
    val takeprofitOrder: TakeprofitOrder
) : IOrder