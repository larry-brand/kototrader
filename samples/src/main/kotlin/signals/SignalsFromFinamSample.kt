package org.cryptolosers.samples.signals

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.cryptolosers.commons.toStringWithSign
import org.cryptolosers.indicators.TickerWithIndicator
import org.cryptolosers.indicators.VolumeIndicators
import org.cryptolosers.indicators.getCandlesCount
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * Sample
 * Connect to Finam via Transaq connector and get signals for buy or sell.
 * Before run:
 * - copy config/terminalConfig-example.json to config/terminalConfig.json
 * - insert your login and password (of Finam Transaq connector) to config/terminalConfig.json
 */
suspend fun main() {
    val conn  = Connector(TransaqConnector())
    conn.connect()
    val tradingApi: ViewTradingApi = conn.tradingApi()
    val logger = KotlinLogging.logger {}
    val moexWatchList = listOf("ROSN", "SBER", "LKOH", "GAZP", "NVTK", "LNZL", "SVCB")

    thread {
        runBlocking {
            while (true) {
                val volumeIndicators = VolumeIndicators(tradingApi)
                logger.info { "\nЗапуск индикаторов:" }
                val startTime = System.currentTimeMillis()
                val executorService = Executors.newFixedThreadPool(5)

                val printSignals = Collections.synchronizedList(ArrayList<TickerWithIndicator>())
                moexWatchList.forEach { t ->
                    executorService.submit {
                        runBlocking {
                            val tickers = tradingApi.getAllTickers()
                                .filter { it.ticker.symbol == t && it.ticker.exchange == Exchanges.MOEX }
                            if (tickers.isEmpty()) {
                                logger.warn { "can not find ticker: $t" }
                                return@runBlocking
                            }
                            if (tickers.size > 1) {
                                logger.warn {"find more than one tickers: $t, found: $tickers" }
                                return@runBlocking
                            }
                            val ticker = tickers.first()
                            val candles = tradingApi.getLastCandles(
                                ticker.ticker,
                                Timeframe.MIN15,
                                getCandlesCount(Timeframe.MIN15),
                                Session.CURRENT_AND_PREVIOUS
                            )
                            val indicator = volumeIndicators.isBigVolumeOnStartSession(candles)
                            if (indicator.isSignal) printSignals.add(TickerWithIndicator(ticker, indicator))
                        }
                    }
                }

                executorService.shutdown()
                val finished = executorService.awaitTermination(3, TimeUnit.MINUTES)
                if (finished) {
                    logger.info { "All tasks finished successfully." }
                } else {
                    logger.error { "Timeout reached before all tasks completed." }
                }

                logger.info { "Volume signals at 10:15, 15min bar: " }
                printSignals.forEach {
                    println("${it.ticker.shortDescription}(${it.ticker.ticker.symbol}) " +
                            "volume: ${it.indicator.volumeChangeFromMedianXInCandle?.toStringWithSign()}%, " +
                             "price: ${it.indicator.priceChangePercentageInCandle?.toStringWithSign()}%")
                }
                logger.info { "Время работы загрузки свечей: " + ((System.currentTimeMillis().toDouble() - startTime) / 1000).toBigDecimal().setScale(3, RoundingMode.HALF_DOWN) + " сек" }

                delay(3000)
            }
        }
    }
}

val moexWatchList = """SBER
SBERP
ROSN
LKOH
SIBN
GAZP
NVTK
PLZL
GMKN
TATN
TATNP
YDEX
SNGS
SNGSP
VTBR
CHMF
PHOR
NLMK
AKRN
UNAC
OZON
MGNT
T
RUAL
MOEX
IRAO
ALRS
MAGN
MTSS
BANE
BANEP
IRKT
CBOM
SVCB
VSMO
PIKK
HYDR
ROSB
AFLT
FLOT
ENPG
RTKM
RTKMP
AGRO
GCHE
RASP
NKNC
NKNCP
NMTP
UGLD
HEAD
TRNFP
BSPB
BSPBP
FEES
FIXP
UWGN
KZOS
KZOSP
POSI
LENT
FESH
AFKS
LSNG
LSNGP
UPRO
KAZT
KAZTP
UTAR
MGTS
MGTSP
RGSS
TRMK
ASTR
MSNG
INGR
APTK
SFIN
LSRG
KMAZ
MDMG
LEAS
BELU
GEMC
PRMD
ELMT
VEON-RX
VKCO
RENI
SMLT
USBN
MSRS
AVAN
AQUA
VJGZ
VJGZP
MTLR
MTLRP
MFGS
MFGSP
SOFL
YAKG
OGKB
DSKY
DIAS
SELG
MBNK
UKUZ
MRKS
VSEH
NKHP
MRKP
MSTT
OZPH
AMEZ
DVEC
CIAN
MRKK
TNSE
DELI
RKKE
DATA
KCHE
KCHEP
RNFT
MRKU
OMZZP
TGKA
LNZL
LNZLP
HNFG
VRSB
VRSBP
SGZH
VSYD
VSYDP
JNOS
JNOSP
IVAT
SVAV
MRKC
RTSB
RTSBP
ETLN
ABRD
ELFV
WTCM
WTCMP
CHMK
SPBE
BLNG
ROLO
WUSH
EUTR
MVID
NNSB
NNSBP
ZAYM
URKZ
KROT
KROTP
TTLK
BRZL
PMSB
PMSBP
PAZA
GAZA
GAZAP
MRKV
SAGO
SAGOP
APRI
YRSB
YRSBP
KRSB
KRSBP
QIWI
GTRK
CHKZ
LPSB
LMBZ
KOGK
ZILL
TGKN
KRKN
KRKNP
TGKB
TGKBP
KBSB
ABIO
PRMB
MRKY
EELT
RUSI
KGKC
KGKCP
RZSB
IGST
IGSTP
OKEY
RTGZ
MISB
MISBP
YKEN
YKENP
NAUK
NSVZ
KMEZ
HIMCP
SLEN
STSB
STSBP
ZVEZ
VGSB
KLVZ
RBCM
PRFN
MGNZ
CARM
UNKL
RDRB
NKSH
MGKL
TORS
TORSP
GECO
CNTL
CNTLP
CHGZ
NFAZ
ACKO
TASB
TASBP
KLSB
SARE
SAREP
ROST
GEMA
MAGE
MAGEP
DZRD
DZRDP
TUZA
LVHK
LIFE
ARSA
ASSB
VLHZ
MRSB
KRKOP
DIOD
KUZB
SVET
SVETP
BISVP""".split("\n")