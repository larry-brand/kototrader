package org.cryptolosers.transaq

import org.cryptolosers.commons.utils.JAXBUtils
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.checkResult
import org.cryptolosers.transaq.connector.jna.TXmlConnector
import org.cryptolosers.transaq.xml.command.Subscrube
import org.cryptolosers.transaq.xml.command.internal.Security
import java.math.BigDecimal
import java.time.Instant

class TransaqTradingApi(val memory: TransaqMemory): TradingApi {

    override suspend fun getAllTickers(): List<TickerInfo> {
        return memory.tickerMap.values.map{ it.tickerInfo }.toList()
    }

    override suspend fun getPrice(ticker: Ticker): PriceInfo {
        val tickerInfo = memory.tickerMap[ticker] ?: throw IllegalStateException("Can not find ticker")
        if (memory.priceMap[ticker] == null) {
            memory.priceMap[ticker] = TransaqPriceInfo(PriceInfo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
        }
        val subscrube = Subscrube()
        val quotations = Subscrube.Quotations()
        val security = Security()
        security.seccode = tickerInfo.secCode
        security.board = tickerInfo.board
        quotations.security = listOf(security)
        subscrube.quotations = quotations

        val subscrubeXml = JAXBUtils.marshall(subscrube)
        val subscrubeResultXml = TXmlConnector.sendCommand(subscrubeXml)
        checkResult(subscrubeResultXml, "SUBSCRIBE PRICE (QUITATIONS)")
        return memory.priceMap[ticker]!!.await()
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
        return memory.ordersMapped.get()
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