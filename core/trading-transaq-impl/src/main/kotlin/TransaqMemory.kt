package org.cryptolosers.transaq

import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.Transaction
import org.cryptolosers.transaq.xml.callback.*
import org.cryptolosers.transaq.xml.callback.internal.FortsPosition
import org.cryptolosers.transaq.xml.callback.internal.MoneyPosition
import org.cryptolosers.transaq.xml.callback.internal.SecPosition
import org.cryptolosers.transaq.xml.callback.internal.UnitedLimits
import java.math.BigDecimal
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference


class TransaqMemory {

    // technical
    var responseServerStatuses: BlockingQueue<ServerStatus> = LinkedBlockingQueue()

    // not technical
    var markets: ConcurrentMap<Long, Markets.Market> = ConcurrentHashMap()
    var boards: MutableList<Boards.Board> = CopyOnWriteArrayList()
    var tickerSecCodeMarketMap: MutableMap<SecCodeMarket, TransaqTickerInfo> = ConcurrentHashMap()
    var tickerSecCodeBoardMap: MutableMap<SecCodeBoard, TransaqTickerInfo> = ConcurrentHashMap()
    var tickerMap: MutableMap<Ticker, TransaqTickerInfo> = ConcurrentHashMap()

    var priceMap: ConcurrentMap<Ticker, TransaqPriceInfo> = ConcurrentHashMap()
    var orderBookMap: ConcurrentMap<Ticker, OrderBook> = ConcurrentHashMap()

    var clients: MutableList<Client> = CopyOnWriteArrayList()
    var portfolioTpluses: AtomicReference<PortfolioTplus> = AtomicReference(PortfolioTplus())
    var wallet: AtomicReference<Wallet> = AtomicReference()

    var secPositionMap: MutableMap<Ticker, SecPosition> = ConcurrentHashMap()
    var fortsPositionMap: MutableMap<Ticker, FortsPosition> = ConcurrentHashMap()
    var moneyPositionMap: MutableMap<Ticker, MoneyPosition> = ConcurrentHashMap()
    var positions: AtomicReference<List<Position>> = AtomicReference(ArrayList())

    var unitedLimits: MutableList<UnitedLimits> = CopyOnWriteArrayList()

    var orders: AtomicReference<Orders> = AtomicReference(Orders())
    var ordersMapped: AtomicReference<List<IOrder>> = AtomicReference(ArrayList())
    var transactions: AtomicReference<Transactions> = AtomicReference(Transactions())

    // listeners
    var priceChangesListenerMap: MutableMap<Ticker, (PriceInfo) -> Unit> = ConcurrentHashMap()

    fun reset() {
        //TODO
    }
}

class Transactions {
    var ordersMap: ConcurrentHashMap<String, Transaction> = ConcurrentHashMap()
}