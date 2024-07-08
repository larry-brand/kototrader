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
import org.cryptolosers.trading.model.MarketOrder
import org.cryptolosers.trading.model.OrderDirection
import org.cryptolosers.trading.model.Position
import org.cryptolosers.trading.model.Ticker
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

fun main() {
    val simulator = HistorySimulator()
    val robot: IRobot = MyAnchorRobot()
    val startDate = LocalDateTime.parse("2019-01-03 00:00:00", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FRIENDLY_PATTERN))
    val endDate = LocalDateTime.parse("2021-12-28 23:00:00", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FRIENDLY_PATTERN))

    simulator.runOnCandles(HistoryTickerId("BR"), Timeframe.HOUR1, startDate, endDate) {
        runBlocking {
            robot.onNextMinuteCandle(it, simulator.tradingApi)
        }
    }
    runBlocking {
        printSuccess(simulator.tradingApi.getWallet().toString() + "  | Dials " + simulator.tradingApi.dials)
    }

}

class MyAnchorRobot : IRobot {

    private val list: MutableList<HistoryCandle> = ArrayList()
    private var positionPrice: BigDecimal? = null

    override suspend fun onNextMinuteCandle(historyCandle: HistoryCandle, tradingApi: TradingApi) {
        list.add(historyCandle)
        if (list.size > 2) {
            list.removeAt(0)
        }
        if (list.size < 2) {
            return
        }
        val ldt = historyCandle.timestamp.atZone(ZoneOffset.UTC).toLocalDateTime()
        val t = Ticker("BR")
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

        if (position == null
            && list[0].openPrice < list[1].closePrice
            && (list[1].volume > 350_000) && isPadaushayaZvezda(historyCandle)
        ) {
            println("SELL!" + historyCandle.timestamp)
            tradingApi.sendOrder(MarketOrder(t, 5, OrderDirection.SELL))
        } else if (position == null
            && list[0].openPrice > list[1].closePrice
            && (list[1].volume > 350_000) && isMolot(historyCandle)
        ) {
            println("BUY!" + historyCandle.timestamp)
            tradingApi.sendOrder(MarketOrder(t, 5, OrderDirection.BUY))
        }

        // 2. handle stop, takeprofit
        if (position != null) {
            val stop = 0.2
            val take = 0.6
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

    fun isMolot(historyCandle: HistoryCandle): Boolean {
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

    fun isPadaushayaZvezda(historyCandle: HistoryCandle): Boolean {
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