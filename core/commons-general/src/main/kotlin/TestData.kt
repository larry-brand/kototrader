package org.cryptolosers.commons

import java.time.LocalDate

val fortsMap = linkedMapOf(
    "RIH5" to LocalDate.parse("2024-12-19"),
    "SiH5" to LocalDate.parse("2024-12-19"),
)

val lowLiquidFavorite = """
MTLRP
""".trim().split("\n")

val allCryptoCfgTickers = """
BTCUSDT
TONUSDT
""".trim().split("\n")

val allStockCfgTickers = (fortsMap.keys.toList() + """
T
SBER
GAZP
LKOH
OZON
SMLT
YDEX
GMKN
MTLR
PLZL
AFKS
AFLT
NVTK
TATN
VTBR
MOEX
SNGSP
TRNFP
MGNT
NLMK
SBERP
VKCO
CHMF
HEAD
ALRS
SNGS
MAGN
PIKK
MTSS
POSI
RUAL
SIBN
RNFT
PHOR
SVCB
UWGN
SGZH
UGLD
IRAO
FLOT
TATNP
RTKM
MTLRP
BSPB
EUTR
SFIN
ASTR
FEES
TRMK
IRKT
GTRK
SPBE
QIWI
MVID
UPRO
CBOM
BANEP
FESH
LSRG
RASP
LEAS
WUSH
SELG
HYDR
BANE
SOFL
BELU
RTKMP
MDMG
ENPG
AGRO
NMTP
SVAV
RENI
UNAC
MBNK
ABIO
DATA
AQUA
FIXP
DIAS
LENT
MSNG
TGKN
OZPH
LSNGP
AMEZ
VSEH
OGKB
MRKC
CIAN
ETLN
MRKP
KMAZ
HNFG
RBCM
DELI
ROLO
AKRN
""".trim().split("\n") + lowLiquidFavorite).distinct()

val moexAllTickers = fortsMap.values + """
SBER
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
BISVP
""".trim().split("\n")