package org.cryptolosers.transaq

import org.cryptolosers.commons.utils.JAXBUtils
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.checkResult
import org.cryptolosers.transaq.connector.jna.TXmlConnector
import org.cryptolosers.transaq.xml.command.GetHistoryData
import org.cryptolosers.transaq.xml.command.Subscribe
import org.cryptolosers.transaq.xml.command.Unsubscribe
import org.cryptolosers.transaq.xml.command.internal.Security
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class TransaqTradingApi(val memory: TransaqMemory): TradingApi {

    override suspend fun getAllTickers(): List<TickerInfo> {
        return memory.tickerMap.values.map{ it.tickerInfo }.toList()
    }

    override suspend fun getPrice(ticker: Ticker): PriceInfo {
        if (memory.priceChangesListenerMap[ticker] == null) {
            subscribePrice(ticker)
        }

        val price = memory.priceMap[ticker]!!
        val result = if (!price.subscribed) {
            price.await()
        } else {
            price.getFilledPriceInfo()
        }

        if (memory.priceChangesListenerMap[ticker] == null) {
            unsubscribePrice(ticker)
            price.subscribed = false
        }
        return result
    }

    override suspend fun subscribePriceChanges(ticker: Ticker, priceChangesListener: (PriceInfo) -> Unit) {
        memory.priceChangesListenerMap[ticker] = priceChangesListener
        subscribePrice(ticker)
    }

    override suspend fun getOrderBook(ticker: Ticker): OrderBook {
        TODO("Not yet implemented")
    }

    override suspend fun getCandles(ticker: Ticker, timeframe: Timeframe, startTimestamp: LocalDateTime, endTimestamp: LocalDateTime): List<Candle> {
        TODO("Not yet implemented")
    }

    override suspend fun getLastCandles(ticker: Ticker, timeframe: Timeframe, candlesCount: Int, session: Session): List<Candle> {
        val tickerInfo = memory.tickerMap[ticker] ?: throw IllegalStateException("Can not find ticker")
        val getHistoryData = GetHistoryData()
        val security = Security()
        security.seccode = tickerInfo.secCode
        security.board = tickerInfo.board
        getHistoryData.security = security
        //TODO: remove hardcode
        when (timeframe) {
            Timeframe.MIN1 -> getHistoryData.period = "1"
            Timeframe.MIN5 -> getHistoryData.period = "2"
            Timeframe.MIN15 -> getHistoryData.period = "3"
            Timeframe.HOUR1 -> getHistoryData.period = "4"
            Timeframe.DAY1 -> getHistoryData.period = "5"
            else -> throw IllegalStateException("Timeframe is not supported")
        }
        getHistoryData.count = candlesCount.toLong()
        getHistoryData.reset = true

        // clear candles in memory because reset = true
        val tickerTimeframe = TickerTimeframe(ticker = ticker, timeframe = timeframe)
        val candlesBefore = memory.candlesMap[tickerTimeframe]
        if (candlesBefore != null) {
            memory.candlesMap[tickerTimeframe] = null
        }

        val subscribeXml = JAXBUtils.marshall(getHistoryData)
        val subscribeResultXml = TXmlConnector.sendCommand(subscribeXml)
        checkResult(subscribeResultXml, "GET CANDLES (GET HISTORY DATA)")


        Thread.sleep(3000) //TODO
        val candles = memory.candlesMap[tickerTimeframe]!!
        if (candles.size >= candlesCount) {
            when (session) {
                Session.CURRENT_AND_PREVIOUS -> return candles.takeLast(candlesCount)
                Session.CURRENT -> {
                    val now = LocalDateTime.now(ZoneId.of("Europe/Moscow"))
                    val sessionStarted = now.withHour(10).withMinute(0)
                    return candles.takeLast(candlesCount).filter { it.timestamp >= sessionStarted }
                }
            }
        } else {
            throw IllegalStateException("Can not load candles, probably invalid size")
        }

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

    private fun subscribePrice(ticker: Ticker) {
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
    }

    private fun unsubscribePrice(ticker: Ticker) {
        val tickerInfo = memory.tickerMap[ticker] ?: throw IllegalStateException("Can not find ticker")
        val unsubscribe = Unsubscribe()
        val quotations = Subscribe.Quotations()
        val security = Security()
        security.seccode = tickerInfo.secCode
        security.board = tickerInfo.board
        quotations.security = listOf(security)
        unsubscribe.quotations = quotations
        val unsubscribeXml = JAXBUtils.marshall(unsubscribe)
        val unsubscribeResultXml = TXmlConnector.sendCommand(unsubscribeXml)
        checkResult(unsubscribeResultXml, "UNSUBSCRIBE PRICE (QUITATIONS)")
    }
}