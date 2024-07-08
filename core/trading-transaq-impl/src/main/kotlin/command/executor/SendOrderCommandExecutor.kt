package org.cryptolosers.transaq.command.executor

import mu.KotlinLogging
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.TransaqMemory
import org.cryptolosers.transaq.connector.concurrent.Transaction
import org.cryptolosers.transaq.connector.concurrent.sendCommandAndCheckResult
import org.cryptolosers.transaq.xml.command.NewOrder
import org.cryptolosers.transaq.xml.command.NewStopOrder
import org.cryptolosers.transaq.xml.command.internal.Security
import org.cryptolosers.transaq.xml.command.internal.Stoploss
import org.cryptolosers.transaq.xml.command.internal.Takeprofit
import java.math.BigDecimal

class SendOrderCommandExecutor(val memory: TransaqMemory) {
    private val logger = KotlinLogging.logger {}

    suspend fun sendOrder(order: IOrder) {
        if (order is MarketOrder) {
            if (order.size <= 0) {
                throw IllegalStateException("invalid size")
            }
            val newOrder = NewOrder()
            val security = Security()
            val tickerInfo = memory.tickerMap[order.ticker]!!
            security.seccode = tickerInfo.secCode
            security.board = tickerInfo.board
            newOrder.security = security

            val client = memory.clients.first { it.market == tickerInfo.market }
            newOrder.client = client.id
            newOrder.union = client.union

            newOrder.bymarket = ""
            newOrder.quantity = order.size
            newOrder.buysell = when(order.orderDirection) {
                OrderDirection.BUY -> "B"
                OrderDirection.SELL -> "S"
            }

            val result = sendCommandAndCheckResult(newOrder)
            logger.info { "Try to send Market order: $order" }
            // wait order
            val transaction = Transaction.from(result.transactionid.toString(), memory.transactions.get())
            transaction.await()
            logger.info { "Sent Market order successfully: $order" }

        } else if (order is LimitOrder) {
            if (order.size <= 0) {
                throw IllegalStateException("invalid size")
            }
            val newOrder = NewOrder()
            val security = Security()
            val tickerInfo = memory.tickerMap[order.ticker]!!
            security.seccode = tickerInfo.secCode
            security.board = tickerInfo.board
            newOrder.security = security

            val client = memory.clients.first { it.market == tickerInfo.market }
            newOrder.client = client.id
            newOrder.union = client.union

            newOrder.price = order.price.toString()
            newOrder.quantity = order.size
            newOrder.buysell = when(order.orderDirection) {
                OrderDirection.BUY -> "B"
                OrderDirection.SELL -> "S"
            }

            val result = sendCommandAndCheckResult(newOrder)
            logger.info { "Try to send Limit order: $order" }
            // wait order
            val transaction = Transaction.from(result.transactionid.toString(), memory.transactions.get())
            transaction.await()
            logger.info { "Sent Limit order successfully: $order" }

        } else if (order is StopOrder) {
            if (order.size <= 0) {
                throw IllegalStateException("invalid size")
            }
            if (order.slippage < BigDecimal.ZERO) {
                throw IllegalStateException("invalid slippage")
            }
            val newOrder = NewStopOrder()
            val security = Security()
            val tickerInfo = memory.tickerMap[order.ticker]!!
            security.seccode = tickerInfo.secCode
            security.board = tickerInfo.board
            newOrder.security = security

            val client = memory.clients.first { it.market == tickerInfo.market }
            newOrder.client = client.id
            newOrder.union = client.union

            newOrder.buysell = when(order.orderDirection) {
                OrderDirection.BUY -> "B"
                OrderDirection.SELL -> "S"
            }

            newOrder.stoploss = Stoploss()
            newOrder.stoploss.activationprice = order.activationPrice.toString()
            newOrder.stoploss.quantity = order.size
            if (order.slippage == BigDecimal.ZERO) {
                newOrder.stoploss.bymarket = ""
            } else {
                val price = when(order.orderDirection) {
                    OrderDirection.BUY -> order.activationPrice + order.slippage
                    OrderDirection.SELL -> order.activationPrice - order.slippage
                }
                newOrder.stoploss.orderprice = price.toString()
            }

            val result = sendCommandAndCheckResult(newOrder)
            logger.info { "Try to send Stop order: $order" }
            // wait order
            val transaction = Transaction.from(result.transactionid.toString(), memory.transactions.get())
            transaction.await()
            logger.info { "Sent Stop order successfully: $order" }

        } else if (order is TakeprofitOrder) {
            if (order.size <= 0) {
                throw IllegalStateException("invalid size")
            }
            val newOrder = NewStopOrder()
            val security = Security()
            val tickerInfo = memory.tickerMap[order.ticker]!!
            security.seccode = tickerInfo.secCode
            security.board = tickerInfo.board
            newOrder.security = security

            val client = memory.clients.first { it.market == tickerInfo.market }
            newOrder.client = client.id
            newOrder.union = client.union

            newOrder.buysell = when(order.orderDirection) {
                OrderDirection.BUY -> "B"
                OrderDirection.SELL -> "S"
            }

            newOrder.takeprofit = Takeprofit()
            newOrder.takeprofit.activationprice = order.activationPrice.toString()
            newOrder.takeprofit.quantity = order.size
            newOrder.takeprofit.bymarket = ""

            val result = sendCommandAndCheckResult(newOrder)
            logger.info { "Try to send Takeprofit order: $order" }
            // wait order
            val transaction = Transaction.from(result.transactionid.toString(), memory.transactions.get())
            transaction.await()
            logger.info { "Sent Takeprofit order successfully: $order" }

        } else if (order is StopAndTakeprofitOrder) {
            if (order.stopOrder.size <= 0) {
                throw IllegalStateException("invalid size")
            }
            if (order.stopOrder.slippage < BigDecimal.ZERO) {
                throw IllegalStateException("invalid slippage")
            }
            if (order.takeprofitOrder.size <= 0) {
                throw IllegalStateException("invalid size")
            }
            if (order.stopOrder.ticker != order.takeprofitOrder.ticker) {
                throw IllegalStateException("invalid ticker")
            }
            if (order.stopOrder.orderDirection != order.takeprofitOrder.orderDirection) {
                throw IllegalStateException("invalid order direction")
            }
            val newOrder = NewStopOrder()
            val security = Security()
            val tickerInfo = memory.tickerMap[order.stopOrder.ticker]!!
            security.seccode = tickerInfo.secCode
            security.board = tickerInfo.board
            newOrder.security = security

            val client = memory.clients.first { it.market == tickerInfo.market }
            newOrder.client = client.id
            newOrder.union = client.union

            newOrder.buysell = when(order.stopOrder.orderDirection) {
                OrderDirection.BUY -> "B"
                OrderDirection.SELL -> "S"
            }

            newOrder.stoploss = Stoploss()
            newOrder.stoploss.activationprice = order.stopOrder.activationPrice.toString()
            newOrder.stoploss.quantity = order.stopOrder.size
            if (order.stopOrder.slippage == BigDecimal.ZERO) {
                newOrder.stoploss.bymarket = ""
            } else {
                val price = when(order.stopOrder.orderDirection) {
                    OrderDirection.BUY -> order.stopOrder.activationPrice + order.stopOrder.slippage
                    OrderDirection.SELL -> order.stopOrder.activationPrice - order.stopOrder.slippage
                }
                newOrder.stoploss.orderprice = price.toString()
            }

            newOrder.takeprofit = Takeprofit()
            newOrder.takeprofit.activationprice = order.takeprofitOrder.activationPrice.toString()
            newOrder.takeprofit.quantity = order.takeprofitOrder.size
            newOrder.takeprofit.bymarket = ""

            val result = sendCommandAndCheckResult(newOrder)
            logger.info { "Try to send StopAndTakeProfit order: $order" }
            // wait order
            val transaction = Transaction.from(result.transactionid.toString(), memory.transactions.get())
            transaction.await()
            logger.info { "Sent StopAndTakeProfit order successfully: $order" }
        } else {
            throw IllegalStateException("Unsupported order")
        }
    }
}