package org.cryptolosers.transaq

import mu.KotlinLogging
import org.cryptolosers.commons.utils.JAXBUtils
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.Transaction
import org.cryptolosers.transaq.connector.jna.TCallback
import org.cryptolosers.transaq.xml.callback.*
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

class TransaqTradingApiTCallback(val memory: TransaqMemory) : TCallback {

    override fun invoke(response: String) {
        try {
            if (response.startsWith("<pits>") || response.contains("<news_header>")
                || response.startsWith("<candlekinds>") || response.startsWith("<trades>")
                || response.startsWith("<union") || response.startsWith("<overnight")) {
                logger.debug { "Response wont be handled, it is not supported: $response" }
                return
            }

            val resp: Any = JAXBUtils.unmarshall(response, jaxbContext)

            if (resp is ServerStatus) {
                memory.responseServerStatuses.put(resp)
            } else if (resp is Markets) { // Доступные рынки
                resp.market.forEach { memory.markets[it.id] = it }
            } else if (resp is Boards) { //Справочник режимов торгов
                memory.boards.clear()
                memory.boards.addAll(resp.board)
            } else if (resp is Securities) { // Список инструментов
                handleSecirity(resp)
            } else if (resp is SecInfoUpd) { //Обновление информации по инструменту
                //TODO add handle
            } else if (resp is Positions) { //Позиции
                handlePosition(resp)
            } else if (resp is Client) { //Клиентские счета
                //TODO: to map
                memory.clients.add(resp)
            } else if (resp is PortfolioTplus) { //Клиентский портфель Т+
                memory.portfolioTpluses.set(resp)
            } else if (resp is Quotes) { //Глубина рынка по инструменту(ам), т.е. стакан
                //handleQuotes(resp)
            } else if (resp is Quotations) { //Котировки по инструменту(ам)
                //handleQuotations(resp)
            } else if (resp is Orders) { //Заявка(и) клиента
                handleOrders(resp)
            } else if (resp is Messages) {
                logger.info { resp.toString() }
            }
            //TODO:
//            else if (resp is Trades) {
//            }
            else  {
                logger.info {"Response is not handled, may be not instanced $response" }
            }
        } catch (e: JAXBException) {
            logger.warn { "Parsing is not supported or error: ${response.substring(0, 20)}" }
        } catch (e: InterruptedException) {
            logger.error(e) { "Thread was interrupted for: ${response.substring(0, 20)}" }
        } catch (e: RuntimeException) {
            logger.error(e) { "Parsing is not supported or error: ${response.substring(0, 20)}" }
            //e.printStackTrace()
        }
    }

    private fun handleSecirity(responseObj: Any) {
        val securities: Securities = responseObj as Securities
        for (s in securities.security) {
            try {
                // Secid действителен в течение сессии, постоянным уникальным ключом
                // инструмента между сессиями является Seccode+Market
                val marketName = memory.markets[s.market!!.toLong()]!!.market!!
                val key = Ticker(symbol = s.seccode, exchange = marketName)
//                val currency = Currency.getInstance("RUB") // TODO

                val info = TickerInfo(key, s.shortname!!, s.sectype!!)
                memory.tickerMap[key] = TransaqTickerInfo(info, s.decimals!!, s.minstep!!, s.lotsize!!, s.point_cost!!)
            } catch (e: RuntimeException) {
                logger.error(e) { "Can not handle security $s" }
            }
        }
    }

    private fun handlePosition(responseObj: Any) {
        val positions: Positions = responseObj as Positions
        if (positions.sec_position != null) {
            for (p in positions.sec_position) {
                memory.markets[p.market]?.let {
                    memory.secPositionMap.put(Ticker(p.seccode, it.market), p)
                }
            }
        }
        if (positions.forts_position != null) {
            for (p in positions.forts_position) {
                memory.markets[p.markets.markets.first().markets]?.let {
                    memory.fortsPositionMap.put(Ticker(p.seccode, it.market), p)
                }
            }
        }
        if (positions.money_position != null) {
            for (p in positions.money_position) {
                memory.markets[p.markets.market.first().market]?.let {
                    memory.moneyPositionMap.put(Ticker(p.asset, it.market), p)
                }
            }
        }
        if (positions.united_limits != null) {
            memory.unitedLimits.clear()
            memory.unitedLimits.addAll(positions.united_limits)
        }
        // copy secPosition, fortsPosition to positions
        val thisNewPositions: MutableList<Position> = ArrayList<Position>()
        for (p in memory.secPositionMap) {
            if (p.value.saldo != 0L) {
                thisNewPositions.add(Position(Ticker(p.key.symbol, p.key.exchange), p.value.saldo))
            }
        }
        for (p in memory.fortsPositionMap) {
            if (p.value.totalnet != 0L) {
                thisNewPositions.add(Position(Ticker(p.key.symbol, p.key.exchange), p.value.totalnet))
            }
        }
        for (m in memory.moneyPositionMap) {
            thisNewPositions.add(Position(Ticker(m.key.symbol, m.key.exchange), m.value.saldo.toLong()))
        }
        for (u in memory.unitedLimits) {
            val newWallet = Wallet(
                balance = u.equity,
                margin = u.requirements,
                freeMargin = u.free
            )
            memory.wallet.set(newWallet)
        }
        memory.positions.set(thisNewPositions)
    }

//    private fun handleQuotes(responseObj: Any) {
//        val quotes: Quotes = responseObj as Quotes
//        val tickerSet: MutableSet<Ticker> = HashSet<Ticker>()
//        if (quotes.getQuote() != null) {
//            for (q in quotes.getQuote()) {
//                val ticker: Ticker = memory.tickerWithBoardMap.get(TickerWithBoard(q.getSeccode(), q.getBoard()))
//                if (!memory.orderBookMap.containsKey(ticker)) {
//                    memory.orderBookMap[ticker] = OrderBook()
//                }
//                tickerSet.add(ticker)
//                val orderBook: OrderBook? = memory.orderBookMap[ticker]
//                if (q.getBuy() != null && q.getBuy() !== 0L && q.getBuy() > 0) {
//                    orderBook.removeByPrice(q.getPrice())
//                    orderBook.getBid().add(OrderBookEntry(q.getPrice(), q.getBuy().longValue()))
//                    Collections.sort(orderBook.getBid())
//                } else if (q.getSell() != null && q.getSell() !== 0L && q.getSell() > 0) {
//                    orderBook.removeByPrice(q.getPrice())
//                    orderBook.getAsk().add(OrderBookEntry(q.getPrice(), q.getSell().longValue()))
//                    Collections.sort(orderBook.getAsk())
//                } else {
//                    //error
//                    orderBook.removeByPrice(q.getPrice())
//                }
//            }
//        }
//        for (ticker in tickerSet) {
//            applyForChannelListener(WalletChannel()) { memory.orderBookMap[ticker] }
//        }
//    }
//
//    private fun handleQuotations(responseObj: Any) {
//        val quotations: Quotations = responseObj as Quotations
//        val q: Quotations.Quotation = quotations.getQuotation()
//        if (q != null) {
//            val ticker: Ticker = memory.tickerWithBoardMap.get(TickerWithBoard(q.getSeccode(), q.getBoard()))
//            if (!memory.priceMap.containsKey(ticker)) {
//                memory.priceMap[ticker] = Price()
//            }
//            val price: Price? = memory.priceMap[ticker]
//            price.setTicker(ticker)
//            if (q.getBid() != null) {
//                price.setBidPrice(q.getBid())
//            }
//            if (q.getOffer() != null) {
//                price.setAskPrice(q.getOffer())
//            }
//            if (q.getLast() != null) {
//                price.setLastPrice(q.getLast())
//            }
//            if (price.getLastPrice() != null && price.getAskPrice() != null && price.getLastPrice().compareTo(price.getAskPrice()) >= 0) {
//                price.setLastPrice(price.getAskPrice())
//            }
//            if (price.getLastPrice() != null && price.getBidPrice() != null && price.getLastPrice().compareTo(price.getBidPrice()) === -1) {
//                price.setLastPrice(price.getBidPrice())
//            }
//            if (price.getLastPrice() == null && price.getAskPrice() != null) {
//                price.setLastPrice(price.getAskPrice())
//            }
//            try {
//                memory.subscriptionEventQueue.put(PriceChannel(ticker))
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//
//            // notify listener
//            applyForChannelListener(WalletChannel(), Supplier<*> { price })
//        }
//    }
//

    private fun handleOrders(responseObj: Any) {
        val orders: Orders = responseObj as Orders
        val memoryOrders = memory.orders.get()
        if (orders.order != null) {
            for ((oIndex, o) in orders.order.withIndex()) {
                for (mo in memoryOrders.order) {
                    if (o.orderno == mo.orderno) {
                        memoryOrders.order[oIndex] = o
                    }
                }
            }
        }

        if (orders.stoporder != null) {
            for ((oIndex, o) in orders.stoporder.withIndex()) {
                for (mo in memoryOrders.stoporder) {
                    if (o.transactionid == mo.transactionid) {
                        memoryOrders.stoporder[oIndex] = o
                    }
                }
            }
        }
        memory.orders.set(memoryOrders)

        //memory.orders = orders;
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
    }
//
//    private fun applyForChannelListener(iChannel: IChannel, applier: Supplier<*>) {
//        //TODO
//    }
//
//    private fun getMarketById(id: Long?): String {
//        var marketName = "Unknown"
//        if (id == null) {
//            return marketName
//        }
//        for (m in memory.markets) {
//            if (m.getId().equals(id)) {
//                marketName = m.getMarket()
//                break
//            }
//        }
//        return marketName
//    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private var jaxbContext: JAXBContext? = null

        init {
            try {
                jaxbContext = JAXBContext.newInstance(
                    ServerStatus::class.java,
                    Markets::class.java, Boards::class.java, Securities::class.java,
                    SecInfoUpd::class.java, Positions::class.java, Client::class.java, PortfolioTplus::class.java, Quotations::class.java, Quotes::class.java,
                    Orders::class.java, Messages::class.java
                )
            } catch (e: JAXBException) {
                logger.error(e) { "Can not init JAXBContext" }
            }
        }
    }
}