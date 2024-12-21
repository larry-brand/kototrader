package org.cryptolosers.trading.model

/** ticker=BR exchangeId="MOEX"  internalId=34 */
data class Ticker(val symbol: SymbolName, val exchange: Exchange? = null)

data class TickerInfo (
    val ticker: Ticker,
    val shortDescription: String,
    val type: InstrumentType,
//    val currency: Currency
    val lotSize: Long
)

typealias SymbolName = String
typealias Exchange = String

object Exchanges {
    val MOEX: Exchange = "ММВБ"
    val MOEX_FORTS: Exchange = "FORTS"
    val NASDAQ: Exchange = "NASDAQ"
    val NYSE: Exchange = "NYSE"
}

typealias InstrumentType = String