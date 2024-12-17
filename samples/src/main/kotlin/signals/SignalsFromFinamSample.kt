package org.cryptolosers.samples.signals

import kotlinx.coroutines.runBlocking
import org.cryptolosers.indicators.VolumeIndicators
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.*
import org.cryptolosers.transaq.FinamFutureInstrument
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import java.math.BigDecimal
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
    val moexWatchList = listOf("ROSN", "SBER", "LKOH", "GAZP", "NVTK", "LNZL")

    thread {
        runBlocking {
            val volumeIndicators = VolumeIndicators(tradingApi)
            println("Run indicators:")
            val printSignals = moexWatchList.mapNotNull {
                val indicator = volumeIndicators.isBigVolumeOnStartSession(Ticker(it, Exchanges.MOEX), Timeframe.MIN15)
                if (indicator.isSignal) {
                    "Ticker: $it, volume change: ${indicator.volumeChangeFromMedianPercentageInCandle?.toStringWithSign()}%, " +
                            "price change: ${indicator.priceChangePercentageInCandle?.toStringWithSign()}%"
                } else {
                    null
                }
            }

            println("Volume signals at 10:15, 15min bar: ")
            printSignals.forEach {
                println(it)
            }

        }
    }

    conn.abort()
}

fun BigDecimal.toStringWithSign(): String {
    return if (this <= BigDecimal.ZERO ) {
        toString()
    }
    else {
        "+" + toString()
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
GAZT
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
GAZS
AGRO
GCHE
RASP
NKNC
NKNCP
GAZC
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
MRKZ
HIMCP
SLEN
STSB
STSBP
ZVEZ
VGSB
VGSBP
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