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

val Exchange_MOEX: ExchangeName = "MOEX"
val Exchange_MOEX_FORTS: ExchangeName = "FORTS"
val Exchange_NASDAQ: ExchangeName = "NASDAQ"
val Exchange_NYSE: ExchangeName = "NYSE"

typealias InstrumentType = String