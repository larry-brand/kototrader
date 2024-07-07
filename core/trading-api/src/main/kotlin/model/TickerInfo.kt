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

val MOEX_Exchange: ExchangeName = "MOEX"
val NASDAQ_Exchange: ExchangeName = "NASDAQ"
val NYSE_Exchange: ExchangeName = "NYSE"

typealias InstrumentType = String