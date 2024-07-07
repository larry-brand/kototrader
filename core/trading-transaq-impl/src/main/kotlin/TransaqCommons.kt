package org.cryptolosers.transaq

import mu.KotlinLogging
import org.cryptolosers.trading.model.InstrumentType
import org.cryptolosers.trading.model.PriceInfo
import org.cryptolosers.trading.model.Ticker
import org.cryptolosers.trading.model.TickerInfo
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

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
    val secCode: String,
    val market: String,
    val board: String,
    val decimals: Long,
    val minstep: BigDecimal,
    val lotSize: Long,
    val pointCost: BigDecimal
)

data class SecCodeMarket(
    val secCode: String,
    val market: Long
)

data class SecCodeBoard(
    val secCode: String,
    val board: String
)

data class TransaqPriceInfo(
    var priceInfo: PriceInfo? = null,
    @Volatile
    var subscribed: Boolean = false,
    private val lock: Lock = ReentrantLock(),
    private val condition: Condition = lock.newCondition()

) {
    fun await(): PriceInfo {
        lock.lock()
        try {
            condition.await(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.error(e) { "PriceInfo was interrupted" }
        }
        return priceInfo ?: throw IllegalStateException("Can not get price info, it is not filled in subscription")
    }

    fun getFilledPriceInfo(): PriceInfo {
        return priceInfo ?: throw IllegalStateException("Can not get price info, it is not filled in subscription")
    }

    companion object {
        fun signalAll(ticker: Ticker, memory: TransaqMemory) {
            val priceInfoMemory = memory.priceMap[ticker]
            if (priceInfoMemory != null) {
                priceInfoMemory.lock.lock()
                priceInfoMemory.condition.signalAll()
                priceInfoMemory.lock.unlock()
            }
        }
    }
}
private val logger = KotlinLogging.logger {}