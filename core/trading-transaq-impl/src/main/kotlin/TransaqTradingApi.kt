package org.cryptolosers.transaq

import org.cryptolosers.commons.utils.JAXBUtils
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.checkResult
import org.cryptolosers.transaq.connector.jna.TXmlConnector
import org.cryptolosers.transaq.xml.command.Subscribe
import org.cryptolosers.transaq.xml.command.Unsubscribe
import org.cryptolosers.transaq.xml.command.internal.Security
import java.time.Instant

class TransaqTradingApi(val memory: TransaqMemory): TradingApi {

    override suspend fun getAllTickers(): List<TickerInfo> {
        return memory.tickerMap.values.map{ it.tickerInfo }.toList()
    }

    override suspend fun getPrice(ticker: Ticker): PriceInfo {
        val tickerInfo = memory.tickerMap[ticker] ?: throw IllegalStateException("Can not find ticker")
        memory.priceMap.computeIfAbsent(ticker) { TransaqPriceInfo() }
        val subscribe = Subscribe()
        val quotations = Subscribe.Quotations()
        val security = Security()
        security.seccode = tickerInfo.secCode
        security.board = tickerInfo.board
        quotations.security = listOf(security)
        subscribe.quotations = quotations

        val subscribeXml = JAXBUtils.marshall(subscribe)
        val subscribeResultXml = TXmlConnector.sendCommand(subscribeXml)
        checkResult(subscribeResultXml, "SUBSCRIBE PRICE (QUITATIONS)")

        val price = memory.priceMap[ticker]!!
        val result = if (!price.subscribed) {
            price.await()
        } else {
            price.getFilledPriceInfo()
        }

        val unsubscribe = Unsubscribe()
        unsubscribe.quotations = quotations
        val unsubscribeXml = JAXBUtils.marshall(unsubscribe)
        val unsubscribeResultXml = TXmlConnector.sendCommand(unsubscribeXml)
        checkResult(unsubscribeResultXml, "UNSUBSCRIBE PRICE (QUITATIONS)")
        return result
    }

    override suspend fun subscribePriceChanges(ticker: Ticker, priceChangesListener: (PriceInfo) -> Unit) {
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
        return memory.ordersMapped.get()
    }

    override suspend fun getOperations(ticker: Ticker): List<Operation> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllOperations(): List<Operation> {
        TODO("Not yet implemented")
    }

    override suspend fun getWallet(): Wallet {
        return memory.wallet.get()
    }
}