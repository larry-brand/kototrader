package org.cryptolosers.trading.model

/** ticker=BR exchangeId="MOEX"  internalId=34 */
data class Ticker(val symbol: SymbolName, val exchange: ExchangeName? = null)

data class TickerInfo (
    val ticker: Ticker,
    val shortDescription: String,
    val type: InstrumentType,
//    val currency: Currency
)

typealias SymbolName = String
typealias ExchangeName = String

object Exchanges {
    val MOEX: ExchangeName = "ММВБ"
    val MOEX_FORTS: ExchangeName = "FORTS"
    val NASDAQ: ExchangeName = "NASDAQ"
    val NYSE: ExchangeName = "NYSE"
}

typealias InstrumentType = String