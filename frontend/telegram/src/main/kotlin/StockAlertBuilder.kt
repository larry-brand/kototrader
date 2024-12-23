package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.indicators.TickerWithAlert
import org.cryptolosers.indicators.VolumeAlerts
import org.cryptolosers.indicators.getCandlesCount
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.model.Candle
import org.cryptolosers.trading.model.Ticker
import org.cryptolosers.trading.model.Timeframe
import java.lang.Exception
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class StockAlertBuilder(
    private val stockTradingApi: ViewTradingApi,
    private val timeframe: Timeframe,
    private val appCfg: AppCfg
) {
    private val volumeAlerts = VolumeAlerts()
    private val logger = KotlinLogging.logger {}

    fun build(): List<TickerWithAlert> {
        if (!checkWorkTime()) {
            return emptyList()
        }

        logger.info { "------------------ Запуск создания оповещений для $timeframe ------------------" }
        val startTime = System.currentTimeMillis()
        val executorService = Executors.newFixedThreadPool(5)
        val alerts = Collections.synchronizedList(ArrayList<TickerWithAlert>())

        makeAlerts(executorService, alerts)

        if (!checkMakingAlertsFinished(executorService)) {
            return emptyList()
        }

        logger.info { "Время работы загрузки свечей: " +
                ((System.currentTimeMillis().toDouble() - startTime) / 1000).toBigDecimal().setScale(2, RoundingMode.HALF_DOWN) + " сек" }

        return alerts
    }

    private fun checkWorkTime(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)

        // Проверяем, что текущее время находится в пределах с 10:00 до 24:00 и это будний день
        if (!(currentHour in 10..24 && currentDay >= Calendar.MONDAY && currentDay <= Calendar.FRIDAY) && !immediateRun) {
            logger.info { "Задача пропущена: не будний день или не в рабочие часы." }
            return false
        }
        return true
    }

    private fun checkMakingAlertsFinished(executorService: ExecutorService): Boolean {
        executorService.shutdown()
        val finished = executorService.awaitTermination(3, TimeUnit.MINUTES)
        if (finished) {
            logger.debug { "Все задачи по формированию оповещений закончились" }
            return true
        } else {
            logger.error { "Достигнут таймаут из-за долгого формирования оповещений" }
            return false
        }
    }

    private fun makeAlerts(executorService: ExecutorService, alerts: MutableList<TickerWithAlert>) {
        appCfg.stockCfgTickers.forEach { t ->
            executorService.submit {
                runBlocking {
                    val candles = stockTradingApi.getLastCandles(
                        t.ticker,
                        timeframe,
                        getCandlesCount(t.ticker, timeframe)
                    )
                    if (!checkCandles(candles, t.ticker)) {
                        return@runBlocking
                    }

                    val alert = volumeAlerts.isBigVolume(candles)
                    alerts.add(TickerWithAlert(t, alert))

                }
            }
        }
    }

    private fun checkCandles(candlesRequest: List<Candle>, ticker: Ticker): Boolean {
        var candles = candlesRequest
        if (candles.isNotEmpty() && Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes() >= timeframe.toMin()/2 + timeframe.toMin() && !forceNotCheckLastCandle) {
            logger.info { "Индикатор пропущен, последняя свечка слишком старая, разница во времени ${Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes()} минут, тикер $ticker" }
            return false
        }
        if (candles.isNotEmpty() && Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes() < timeframe.toMin() && !forceNotCheckLastCandle) {
            candles = candles.dropLast(1)
        }
        if (candles.isNotEmpty() && Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes() >= timeframe.toMin()/2 + timeframe.toMin()*2 && !forceNotCheckLastCandle) {
            logger.info { "Индикатор пропущен, предпоследняя свечка слишком старая, разница во времени ${Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes()} минут, тикер $ticker" }
            return false
        }
        if (candles.size <= 20) {
            logger.info { "Слишком мало свечек: ${candles.size}, тикер $ticker" }
            return false
        }
        return true
    }

}

private fun Timeframe.toMin(): Int {
    return when(this) {
        Timeframe.MIN1 -> 1
        Timeframe.MIN5 -> 5
        Timeframe.MIN15 -> 15
        Timeframe.HOUR1 -> 60
        else -> { throw Exception("Invalid timeframe: $this") }
    }
}