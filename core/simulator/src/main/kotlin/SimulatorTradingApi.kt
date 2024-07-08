package org.cryptolosers.simulator

import mu.KotlinLogging
import org.cryptolosers.history.HistoryApi
import org.cryptolosers.history.HistoryTickerId
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.math.absoluteValue


class SimulatorTradingApi(money: BigDecimal, val historyApi: HistoryApi): TradingApi {

    var now = LocalDateTime.now()
    var nowPrice = BigDecimal(0)
    private val comissionPunkts = BigDecimal(0.04)
    private val wallet = Wallet(money)
    private var positionBR: Position? = null
    var dials = 0
    private val logger = KotlinLogging.logger {}

    override suspend fun getAllTickers(): List<TickerInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getPrice(ticker: Ticker): PriceInfo {
        return PriceInfo(nowPrice, nowPrice, nowPrice)
    }

    override suspend fun subscribePriceChanges(ticker: Ticker, priceChangesListener: (PriceInfo) -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderBook(ticker: Ticker): OrderBook {
        TODO("Not yet implemented")
    }

    override suspend fun getCandles(ticker: Ticker, timeframe: Timeframe, startTimestamp: LocalDateTime, endTimestamp: LocalDateTime): List<Candle> {
        TODO("Not yet implemented")
    }

    override suspend fun getLastCandles(ticker: Ticker, timeframe: Timeframe, candlesCount: Int, session: Session): List<Candle> {
        val sessionStarted = now.withHour(10).withMinute(0)
        val sessionEnd = now.withHour(23).withMinute(59)
        val historyTimeframe = when (timeframe) {
            Timeframe.MIN1 -> org.cryptolosers.history.Timeframe.MIN1
            Timeframe.MIN5 -> org.cryptolosers.history.Timeframe.MIN5
            Timeframe.MIN15 -> org.cryptolosers.history.Timeframe.MIN15
            Timeframe.HOUR1 -> org.cryptolosers.history.Timeframe.HOUR1
            Timeframe.DAY1 -> org.cryptolosers.history.Timeframe.DAY1
            else -> throw IllegalStateException("Unsupported timeframe")
        }
        val candles = historyApi.readCandles(HistoryTickerId(ticker.symbol), historyTimeframe, sessionStarted, now)
        val takeSize = if (candles.size >= candlesCount) {
            candles.size
        } else {
            logger.warn { "Can not load candles with requested size, probably invalid size" }
            candlesCount
        }
        return candles.takeLast(takeSize).map {
            Candle(
                timestamp = it.timestamp,
                openPrice = it.openPrice,
                highPrice = it.highPrice,
                lowPrice = it.lowPrice,
                closePrice = it.closePrice,
                volume = it.volume
            )
        }
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