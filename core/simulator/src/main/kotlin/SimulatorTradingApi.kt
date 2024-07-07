package org.cryptolosers.simulator

import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.time.Instant
import kotlin.math.absoluteValue


class SimulatorTradingApi(money: BigDecimal): TradingApi {

    var nowPrice = BigDecimal(0)
    private val comissionPunkts = BigDecimal(0.04)
    private val wallet = Wallet(money)
    private var positionBR: Position? = null
    var dials = 0

    override suspend fun getAllTickers(): List<TickerInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getPrice(ticker: Ticker): PriceInfo {
        return PriceInfo(nowPrice, nowPrice, nowPrice)
    }

    override suspend fun getOrderBook(ticker: Ticker): OrderBook {
        TODO("Not yet implemented")
    }

    override suspend fun getCandles(ticker: Ticker, periodicity: Timeframe, startTimestamp: Instant, endTimestamp: Instant) {
        TODO("Not yet implemented")
    }

    override suspend fun sendOrder(order: IOrder) {
        if (order is MarketOrder) {
            if (positionBR == null) { // new position
                val pType = if (order.orderDirection == OrderDirection.SELL) {
                    PositionType.SHORT
                } else {
                    PositionType.LONG
                }
                positionBR = Position(order.ticker, order.size, pType, nowPrice)
                wallet.balance -= (getPrice(order.ticker).lastPrice * order.size.toBigDecimal()).abs()
            } else {
                var orderSizeWithSigh = order.size
                if (order.orderDirection == OrderDirection.SELL) {
                    orderSizeWithSigh = - order.size
                }
                if (positionBR!!.size * orderSizeWithSigh > 0) { // 1 2    -2 -1
                    TODO()
                } else if (positionBR!!.size > 0 && orderSizeWithSigh < 0 && (positionBR!!.size.absoluteValue == orderSizeWithSigh.absoluteValue) ) { // 1 -1
                    var diffPunkts = (nowPrice - positionBR!!.openPrice!!)
                    positionBR = null
                    wallet.equity += (diffPunkts - comissionPunkts).times(orderSizeWithSigh.absoluteValue.toBigDecimal()) * BigDecimal(7.0) * BigDecimal(100)
                    dials++
                } else if (positionBR!!.size < 0 && orderSizeWithSigh > 0 && (positionBR!!.size.absoluteValue == orderSizeWithSigh.absoluteValue)) { // -1  1
                    var diffPunts = (- nowPrice + positionBR!!.openPrice!!)
                    positionBR = null
                    wallet.equity += (diffPunts - comissionPunkts ).times(orderSizeWithSigh.absoluteValue.toBigDecimal())* BigDecimal(7.0) * BigDecimal(100)
                    dials++
                } else {
                    TODO()
                }
            }
        } else {
            TODO()
        }
    }

    override suspend fun removeOrder(orderId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun getOpenPosition(ticker: Ticker): Position? {
        return positionBR
    }

    override suspend fun getAllOpenPositions(): List<Position> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrders(ticker: Ticker): List<IOrder> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllOrders(): List<IOrder> {
        TODO("Not yet implemented")
    }

    override suspend fun getOperations(ticker: Ticker): List<Operation> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllOperations(): List<Operation> {
        TODO("Not yet implemented")
    }

    override suspend fun getWallet(): Wallet {
        if (positionBR != null)
            wallet.equity = wallet.balance + (getPrice(positionBR!!.ticker).lastPrice * positionBR!!.size.toBigDecimal()).abs()
        return wallet
    }
}