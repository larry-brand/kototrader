package org.cryptolosers.transaq

import mu.KotlinLogging
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.command.executor.SendOrderCommandExecutor
import org.cryptolosers.transaq.connector.concurrent.sendCommandAndCheckResult
import org.cryptolosers.transaq.xml.command.*
import org.cryptolosers.transaq.xml.command.internal.Security
import java.time.LocalDateTime
import java.time.ZoneId

class TransaqTradingService(val memory: TransaqMemory): TradingApi {
    private val sendOrderCommandExecutor = SendOrderCommandExecutor(memory)
    private val logger = KotlinLogging.logger {}

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
        var getLastCandlesRetries = 0
        var candles = emptyList<Candle>()
        while (getLastCandlesRetries < 3 && candles.isEmpty()) {
            if (getLastCandlesRetries >= 1) {
                logger.warn { "Try to load candles again, previous time- downloaded size: ${candles.size} , ticker: $ticker, callRetries: $getLastCandlesRetries" }
            }
            candles = getLastCandlesInternal(ticker, timeframe, candlesCount, session)
            getLastCandlesRetries++
        }

        if (candles.isEmpty()) {
            logger.error { "Can not load candles with requested size, downloaded size: ${candles.size} , ticker: $ticker, callRetries: $getLastCandlesRetries" }
        } else if (candles.size < 10) {
            logger.warn { "Not liquid ticker, downloaded size: ${candles.size} , ticker: $ticker" }
        }

        return candles
    }

    override suspend fun sendOrder(order: IOrder) {
        sendOrderCommandExecutor.sendOrder(order)
    }

    override suspend fun removeOrder(orderId: Long) {
        val order = memory.orders.get().order.firstOrNull { it.orderno == orderId }
        val stopOrder = memory.orders.get().stoporder.firstOrNull { it.activeorderno == orderId }
        if (order != null) {
            val cancelOrder = CancelOrder()
            cancelOrder.transactionid = order.transactionid.toString()
            sendCommandAndCheckResult(cancelOrder)
        } else if (stopOrder != null) {
            val cancelStopOrder = CancelStopOrder()
            cancelStopOrder.transactionid = stopOrder.transactionid.toString()
            sendCommandAndCheckResult(cancelStopOrder)
        } else {
            throw IllegalStateException("Order not found")
        }
    }

    override suspend fun getOpenPosition(ticker: Ticker): Position? {
        return memory.positions.get().firstOrNull { it.ticker == ticker }
    }

    override suspend fun getAllOpenPositions(): List<Position> {
        return memory.positions.get()
    }

    override suspend fun getOrders(ticker: Ticker): List<IOrder> {
        return memory.ordersMapped.get().filter {
            when (it) {
                is LimitOrder -> { it.ticker == ticker }
                is StopOrder -> { it.ticker == ticker }
                is TakeprofitOrder -> { it.ticker == ticker }
                is StopAndTakeprofitOrder -> { it.stopOrder.ticker == ticker }
                else -> throw IllegalStateException("unknown order")

            }
        }
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

    private suspend fun getLastCandlesInternal(ticker: Ticker, timeframe: Timeframe, candlesCount: Int, session: Session): List<Candle> {
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
        memory.candlesMap.computeIfAbsent(tickerTimeframe) {
            TransaqCandles(mutableListOf())
        }
        memory.candlesMap[tickerTimeframe]!!.candles.clear()

        sendCommandAndCheckResult(getHistoryData)

        val candles = memory.candlesMap[tickerTimeframe]!!.await()
        val takeSize = if (candles.size >= candlesCount) {
            candles.size
        } else {
//            if (candles.size < 10) {
//                logger.warn { "Can not load candles with requested size, downloaded size: ${candles.size} , ticker: $ticker" }
//            }
            candlesCount
        }
        when (session) {
            Session.CURRENT_AND_PREVIOUS -> return candles.takeLast(takeSize).sortedBy { it.timestamp }
            Session.CURRENT -> {
                val now = LocalDateTime.now(ZoneId.of("Europe/Moscow"))
                val sessionStarted = now.withHour(10).withMinute(0)
                return candles.takeLast(candlesCount).filter { it.timestamp >= sessionStarted }.sortedBy { it.timestamp }
            }
        }

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

        sendCommandAndCheckResult(subscribe)
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

        sendCommandAndCheckResult(unsubscribe)
    }
}