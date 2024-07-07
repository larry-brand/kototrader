package org.cryptolosers.transaq

import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.time.Instant

class TransaqTradingApi(val memory: TransaqMemory): TradingApi {

    override suspend fun getAllTickers(): List<TickerInfo> {
        return memory.tickerMap.values.map{ it.tickerInfo }.toList()
    }

    override suspend fun getPrice(ticker: Ticker): BigDecimal {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderBook(ticker: Ticker): OrderBook {
        TODO("Not yet implemented")
    }

    override suspend fun getCandles(ticker: Ticker, periodicity: Timeframe, startTimestamp: Instant, endTimestamp: Instant) {
        TODO("Not yet implemented")
    }

    override suspend fun sendOrder(order: IOrder) {
        TODO("Not yet implemented")
    }

    override suspend fun removeOrder(orderId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun getOpenPosition(ticker: Ticker): Position? {
        TODO("Not yet implemented")
    }

    override suspend fun getAllOpenPositions(): List<Position> {
        return memory.positions.get()
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
        TODO("Not yet implemented")
    }
}