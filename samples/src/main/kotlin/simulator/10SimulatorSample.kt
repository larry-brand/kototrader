package org.cryptolosers.samples.simulator

import kotlinx.coroutines.runBlocking
import org.cryptolosers.commons.printSuccess
import org.cryptolosers.history.HistoryCandle
import org.cryptolosers.history.HistoryTickerId
import org.cryptolosers.history.LOCAL_DATE_TIME_FRIENDLY_PATTERN
import org.cryptolosers.history.Timeframe
import org.cryptolosers.simulator.HistorySimulator
import org.cryptolosers.simulator.IRobot
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.model.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

fun main() {
    val simulator = HistorySimulator()
    val robot: IRobot = MyAnchorRobot()
    val startDate = LocalDateTime.parse("2024-06-01 00:00:00", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FRIENDLY_PATTERN))
    val endDate = LocalDateTime.parse("2024-06-03 23:00:00", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FRIENDLY_PATTERN))

    simulator.runOnCandles(HistoryTickerId("Si"), Timeframe.MIN1, startDate, endDate) {
        runBlocking {
            robot.onNextMinute(simulator.tradingApi)
        }
    }
    runBlocking {
        printSuccess(simulator.tradingApi.getWallet().toString() + "  | Dials " + simulator.tradingApi.dials)
    }

}

class MyAnchorRobot : IRobot {

    private var positionPrice: BigDecimal? = null

    override suspend fun onNextMinute(tradingApi: TradingApi) {
        val historyCandle = tradingApi.getLastCandles(Ticker("Si", ""), org.cryptolosers.trading.model.Timeframe.MIN1, 1, Session.CURRENT).firstOrNull()
        if (historyCandle == null) {
            return
        }
        val ldt = historyCandle.timestamp.atZone(ZoneOffset.UTC).toLocalDateTime()
        val t = Ticker("Si")
        val nowPrice = historyCandle.closePrice
        val position: Position? = tradingApi.getOpenPosition(t)
        val closePosEndDay = true
        if (closePosEndDay) {
            if (ldt.hour >= 23 && position != null) {
                if (position.size > 0) {
                    tradingApi.sendOrder(
                        MarketOrder(
                            t,
                            abs(position.size),
                            OrderDirection.SELL
                        )
                    )
                } else {
                    tradingApi.sendOrder(
                        MarketOrder(
                            t,
                            abs(position.size),
                            OrderDirection.BUY
                        )
                    )
                }
                println("Close position, end of day, money: ${tradingApi.getWallet()}")
                return
            }
            if (ldt.hour >= 23) {
                return
            }
        }

        // 1. handle open new position
        positionPrice = position?.openPrice

        if (historyCandle.closePrice > historyCandle.openPrice) {
            println("BUY!" + historyCandle.timestamp)
            tradingApi.sendOrder(MarketOrder(t, 5, OrderDirection.BUY))
        } else {
            println("SELL!" + historyCandle.timestamp)
            tradingApi.sendOrder(MarketOrder(t, 5, OrderDirection.SELL))
        }

        // 2. handle stop, takeprofit
        if (position != null) {
            val stop = 100
            val take = 200
            if (position.size > 0) {
                if (nowPrice < positionPrice!! - BigDecimal(stop)) { // stop
                    tradingApi.sendOrder(
                        MarketOrder(
                            t,
                            abs(position.size),
                            OrderDirection.SELL
                        )
                    )
                    println("${historyCandle.timestamp}, send stop, money: ${tradingApi.getWallet()}")
                } else if (nowPrice > positionPrice!! + BigDecimal(take)) { // takeprofit
                    tradingApi.sendOrder(
                        MarketOrder(
                            t,
                            abs(position.size),
                            OrderDirection.SELL
                        )
                    )
                    println("${historyCandle.timestamp}, send takeprofit, money: ${tradingApi.getWallet()}")
                }
            } else if (position.size < 0) {
                if (nowPrice > positionPrice!! + BigDecimal(stop)) { // stop
                    tradingApi.sendOrder(
                        MarketOrder(
                            t,
                            abs(position.size),
                            OrderDirection.BUY
                        )
                    )
                    println("${historyCandle.timestamp}, send stop, money: ${tradingApi.getWallet()}")
                } else if (nowPrice < positionPrice!! - BigDecimal(take)) { // takeprofit
                    tradingApi.sendOrder(
                        MarketOrder(
                            t,
                            abs(position.size),
                            OrderDirection.BUY
                        )
                    )
                    println("${historyCandle.timestamp}, send takeprofit, money: ${tradingApi.getWallet()}")
                }
            }
        }
    }

    val sizeCandleInDollars = BigDecimal(0.50)
    val relativeSizeTelo = BigDecimal(0.2) //0.08
    val relativeSizeShpil = BigDecimal(0.5)

    fun isMolot(historyCandle: Candle): Boolean {
        val size = historyCandle.highPrice - historyCandle.lowPrice
        val oc = (historyCandle.closePrice - historyCandle.openPrice).abs()
        val isSizeCandleOk = size > sizeCandleInDollars && size.signum() != 0
        val isSmallTelo = oc / size < relativeSizeTelo
        val isShpilVnizy = historyCandle.lowPrice + size * relativeSizeShpil < historyCandle.openPrice &&
                historyCandle.lowPrice + size * relativeSizeShpil < historyCandle.closePrice

        if (isSizeCandleOk && isSmallTelo && isShpilVnizy) {
            return true
        }
        return false
    }

    fun isPadaushayaZvezda(historyCandle: Candle): Boolean {
        val size = historyCandle.highPrice - historyCandle.lowPrice
        val oc = (historyCandle.closePrice - historyCandle.openPrice).abs()
        val isSizeCandleOk = size > sizeCandleInDollars && size.signum() != 0
        val isSmallTelo = oc / size < relativeSizeTelo
        val isShpilVverhu = historyCandle.highPrice - size * relativeSizeShpil > historyCandle.openPrice &&
                historyCandle.highPrice - size * relativeSizeShpil > historyCandle.closePrice
        if (isSizeCandleOk && isSmallTelo && isShpilVverhu) {
            return true
        }
        return false
    }

}