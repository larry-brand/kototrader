package org.cryptolosers.transaq

import org.cryptolosers.trading.model.InstrumentType
import org.cryptolosers.trading.model.TickerInfo
import java.math.BigDecimal

val FinamStockInstrument: InstrumentType = "SHARE" // акции
val FinamBondInstrument: InstrumentType = "BOND" // облигации корпоративные
val FinamFutureInstrument: InstrumentType = "FUT" // фьючерсы FORTS
val FinamOptionInstrument: InstrumentType = "OPT" // опционы
val FinamGKOInstrument: InstrumentType = "GKO" // гос. бумаги
val FinamFOBInstrument: InstrumentType = "FOB" // фьючерсы ММВБ

val FinamIndexInstrument: InstrumentType = "IDX" // индексы
val FinamQuotesInstrument: InstrumentType = "QUOTES" // котировки (прочие)
val FinamCurrencyInstrument: InstrumentType = "CURRENCY" // валютные пары
val FinamADRInstrument: InstrumentType = "ADR" // АДР
val FinamNYSEInstrument: InstrumentType = "NYSE" // данные с NYSE
val FinamMetalInstrument: InstrumentType = "METAL" // металлы
val FinamOilInstrument: InstrumentType = "OIL" // нефтяной сектор

val FinamErrorInstrument: InstrumentType = "ERROR"

data class TransaqTickerInfo(
    val tickerInfo: TickerInfo,
//    val secCode: String,
//    val market: String,
//    val board: String,
    val decimals: Long,
    val minstep: BigDecimal,
    val lotSize: Long,
    val pointCost: BigDecimal
)
