package org.cryptolosers.transaq.handlers

import mu.KotlinLogging
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.SecCodeBoard
import org.cryptolosers.transaq.TransaqMemory
import org.cryptolosers.transaq.connector.concurrent.Transaction
import org.cryptolosers.transaq.xml.callback.Orders
import java.math.BigDecimal

class OrdersHandler(val memory: TransaqMemory) {
    private val logger = KotlinLogging.logger {}

    fun handle(orders: Orders) {
        runCatching {
            handleInternal(orders)
        }.getOrElse {
            logger.error(it) { "Can not handle orders" }
        }
    }

    private fun handleInternal(orders: Orders) {
        // map to transaq model
        val memoryOrders = memory.orders.get()
        if (memoryOrders.order == null) {
            memoryOrders.order = mutableListOf()
        }
        if (memoryOrders.stoporder == null) {
            memoryOrders.stoporder = mutableListOf()
        }
        if (orders.order != null) {
            for ((oIndex, o) in orders.order.withIndex()) {
                if (o.status != "cancelled" || o.status != "disabled") {
                    memoryOrders.order.add(o)
                }
                for (mo in memoryOrders.order) {
                    if (o.transactionid == mo.transactionid && (o.status != "cancelled" || o.status != "disabled")) { // may be orderno
                        memoryOrders.order[oIndex] = o
                    } else if (o.transactionid == mo.transactionid && (o.status != "cancelled" || o.status != "disabled")) {
                        memoryOrders.order[oIndex] = null
                    } else {

                    }
                }
            }
        }
        memoryOrders.order.removeIf { it == null }

        if (orders.stoporder != null) {
            for ((oIndex, o) in orders.stoporder.withIndex()) {
                if (o.status != "cancelled" || o.status != "disabled") {
                    memoryOrders.stoporder.add(o)
                }
                for (mo in memoryOrders.stoporder) {
                    if (o.transactionid == mo.transactionid && (o.status != "cancelled" || o.status != "disabled")) {
                        memoryOrders.stoporder[oIndex] = o
                    } else if (o.transactionid == mo.transactionid && (o.status == "cancelled" || o.status != "disabled")) {
                        memoryOrders.stoporder[oIndex] = null
                    } else {

                    }
                }
            }
        }
        memoryOrders.stoporder?.removeIf { it == null }

        memory.orders.set(memoryOrders)

        // signalAll threads which called tradingApi.sendOrder and was blocked
        // because tradingApi.sendOrder is needed confirmation message from Transaq
        if (orders.order != null) {
            for (o in orders.order) {
                if (o.orderno != null) {
                    Transaction.signalAll(o.transactionid.toString(), o, memory.transactions.get())
                }
            }
        }
        if (orders.stoporder != null) {
            for (o in orders.stoporder) {
                Transaction.signalAll(o.transactionid.toString(), o, memory.transactions.get())
            }
        }

        // map to Api model (mapped)
        val limitOrdersMapped = memoryOrders.order.mapNotNull {
            runCatching {
                mapLimitOrder(it, memoryOrders)
            }.getOrElse {
                logger.error(it) { "Can not map stop order" }
                null
            }
        }
        val stopOrdersMapped= memoryOrders.stoporder.mapNotNull {
            runCatching {
                if (it.stoploss != null && it.takeprofit == null) {
                    mapStopLossOrder(it, memoryOrders)
                }
                else if (it.stoploss == null && it.takeprofit != null) {
                    mapTakeProfitOrder(it, memoryOrders)
                } else if (it.stoploss != null && it.takeprofit != null) {
                    StopAndTakeprofitOrder(
                        stopOrder = mapStopLossOrder(it, memoryOrders),
                        takeprofitOrder = mapTakeProfitOrder(it, memoryOrders)
                    )
                } else {
                    throw IllegalStateException("Can not find stopLoss or takeProfit")
                }
            }.getOrElse {
                logger.error(it) { "Can not map takeProfit order" }
                null
            }
        }
        memory.ordersMapped.set(limitOrdersMapped + stopOrdersMapped)
    }

    private fun mapLimitOrder(order: org.cryptolosers.transaq.xml.callback.internal.Order, memoryOrders: Orders): LimitOrder {
        val ticker = memory.tickerSecCodeBoardMap[SecCodeBoard(
            secCode = order.seccode,
            board = order.board
        )]!!.tickerInfo.ticker
        return LimitOrder(
            ticker = ticker,
            size = order.quantity,
            orderDirection = getOrderDirection(order.buysell),
            price = order.price
        )
    }

    private fun mapStopLossOrder(stopOrder: org.cryptolosers.transaq.xml.callback.internal.StopOrder, memoryOrders: Orders): StopOrder {
        val ticker = memory.tickerSecCodeBoardMap[SecCodeBoard(
            secCode = stopOrder.seccode,
            board = stopOrder.board
        )]!!.tickerInfo.ticker
        val slippage = if (stopOrder.stoploss.orderprice != null) {
            (stopOrder.stoploss.orderprice - stopOrder.stoploss.activationprice).abs()
        } else {
            BigDecimal.ZERO
        }

        return StopOrder(
            ticker = ticker,
            size = getSizeStopOrder(stopOrder, memoryOrders),
            orderDirection = getOrderDirection(stopOrder.buysell),
            activationPrice = stopOrder.stoploss.activationprice,
            slippage = slippage
        )
    }

    private fun mapTakeProfitOrder(stopOrder: org.cryptolosers.transaq.xml.callback.internal.StopOrder, memoryOrders: Orders): TakeprofitOrder {
        val ticker = memory.tickerSecCodeBoardMap[SecCodeBoard(
            secCode = stopOrder.seccode,
            board = stopOrder.board
        )]!!.tickerInfo.ticker
        return TakeprofitOrder(
            ticker = ticker,
            size = getSizeTakeProfitrder(stopOrder, memoryOrders),
            orderDirection = getOrderDirection(stopOrder.buysell),
            activationPrice = stopOrder.takeprofit.activationprice,
        )
    }

    private fun getOrderDirection(buysell: String): OrderDirection {
        return when (buysell) {
            "B" -> OrderDirection.BUY
            "S" -> OrderDirection.SELL
            else -> throw IllegalStateException("Can not map buysell")
        }
    }

    private fun getSizeStopOrder(stopOrder: org.cryptolosers.transaq.xml.callback.internal.StopOrder, memoryOrders: Orders): Long {
        return if (stopOrder.stoploss.quantity.toLongOrNull() != null) {
            stopOrder.stoploss.quantity.toLong()
        } else {
            val limitOrder = memoryOrders.order.firstOrNull { limitOrder -> limitOrder.orderno == stopOrder.linkedorderno }
                ?: throw IllegalStateException("Can not find original order for stop order by linkedorderno")
            limitOrder.quantity
        }
    }

    private fun getSizeTakeProfitrder(stopOrder: org.cryptolosers.transaq.xml.callback.internal.StopOrder, memoryOrders: Orders): Long {
        return if (stopOrder.takeprofit.quantity.toLongOrNull() != null) {
            stopOrder.takeprofit.quantity.toLong()
        } else {
            val limitOrder = memoryOrders.order.firstOrNull { limitOrder -> limitOrder.orderno == stopOrder.linkedorderno }
                ?: throw IllegalStateException("Can not find original order for stop order by linkedorderno")
            limitOrder.quantity
        }
    }
}