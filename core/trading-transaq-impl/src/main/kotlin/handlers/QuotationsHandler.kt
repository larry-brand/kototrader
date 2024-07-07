package org.cryptolosers.transaq.handlers

import mu.KotlinLogging
import org.cryptolosers.trading.model.PriceInfo
import org.cryptolosers.transaq.SecCodeBoard
import org.cryptolosers.transaq.TransaqMemory
import org.cryptolosers.transaq.TransaqPriceInfo
import org.cryptolosers.transaq.xml.callback.Quotations
import java.math.BigDecimal

class QuotationsHandler(val memory: TransaqMemory) {
    private val logger = KotlinLogging.logger {}

    fun handle(quotations: Quotations) {
        runCatching {
            handleInternal(quotations)
        }.getOrElse {
            logger.error(it) { "Can not handle quotations" }
        }
    }

    private fun handleInternal(quotations: Quotations) {
        if (quotations.quotation != null) {
            val ticker = memory.tickerSecCodeBoardMap[SecCodeBoard(
                secCode = quotations.quotation.seccode,
                board = quotations.quotation.board
            )]!!.tickerInfo.ticker
            val price = PriceInfo(
                lastPrice = quotations.quotation.last ?: quotations.quotation.offer, //quotations.quotation.last can be null
                bidPrice = quotations.quotation.bid,
                askPrice = quotations.quotation.offer
            )
            memory.priceMap[ticker]!!.priceInfo = price
            memory.priceMap[ticker]!!.subscribed = true
            TransaqPriceInfo.signalAll(ticker, memory)

            memory.priceChangesListenerMap[ticker]?.let { priceChangesListener ->
                priceChangesListener(price)
            }
        }
    }
}