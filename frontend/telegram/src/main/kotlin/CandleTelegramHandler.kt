package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.commons.moexLiquidTickers
import org.cryptolosers.commons.toStringWithSign
import org.cryptolosers.indicators.TickerWithAlert
import org.cryptolosers.indicators.VolumeAlerts
import org.cryptolosers.indicators.getCandlesCount
import org.cryptolosers.indicators.isAlert
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.model.Exchanges
import org.cryptolosers.trading.model.Session
import org.cryptolosers.trading.model.Ticker
import org.cryptolosers.trading.model.Timeframe
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class CandleTelegramHandler(
    private val bot: TradingTelegramBot,
    private val tradingApi: ViewTradingApi,
    private val favoriteTickers: List<Ticker>,
    private val timeframe: Timeframe
): Runnable {
    private val volumeAlerts = VolumeAlerts()
    private val logger = KotlinLogging.logger {}

    override fun run() {
        val nowString = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        if (!checkWorkTime()) {
            return
        }

        logger.info { "\nЗапуск оповещения:" }
        val startTime = System.currentTimeMillis()
        val executorService = Executors.newFixedThreadPool(5)
        val allAlerts = Collections.synchronizedList(ArrayList<TickerWithAlert>())

        makeAlerts(moexLiquidTickers, executorService, allAlerts)

        if (!checkProsessingAlertsFinished(executorService)) {
            return
        }

        val favoriteAlerts = getFavoriteAlerts(allAlerts, volumeXMedianFavoriteTickers)
        val notFavoriteAlerts = getNotFavoriteAlerts(allAlerts, favoriteAlerts, volumeXMedianNotFavoriteTickers)

        logger.info { "Время работы загрузки свечей: " +
                ((System.currentTimeMillis().toDouble() - startTime) / 1000).toBigDecimal().setScale(2, RoundingMode.HALF_DOWN) + " сек" }

        if (favoriteAlerts.isNotEmpty() || notFavoriteAlerts.isNotEmpty()) {
            sendAlerts(favoriteAlerts, notFavoriteAlerts, nowString)
        }
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

    private fun makeAlerts(tickers: List<String>, executorService: ExecutorService, allAlerts: MutableList<TickerWithAlert>) {
        tickers.forEach { t ->
            executorService.submit {
                runBlocking {
                    val foundTickers = tradingApi.getAllTickers()
                        .filter { it.ticker.symbol == t && (it.ticker.exchange == Exchanges.MOEX || it.ticker.exchange == Exchanges.MOEX_FORTS) }
                    if (foundTickers.isEmpty()) {
                        logger.warn { "Не найден тикер: $t" }
                        return@runBlocking
                    }
                    if (foundTickers.size > 1) {
                        logger.warn { "Найдено больше, чем один тикер: $t, найдено: $foundTickers" }
                        return@runBlocking
                    }
                    val ticker = foundTickers.first()
                    var candles = tradingApi.getLastCandles(
                        ticker.ticker,
                        timeframe,
                        getCandlesCount(ticker.ticker, timeframe),
                        Session.CURRENT_AND_PREVIOUS
                    )

                    if (candles.isNotEmpty() && Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes() >= timeframe.toMin()/2 + timeframe.toMin() && !forceNotCheckLastCandle) {
                        logger.info { "Индикатор пропущен, последняя свечка слишком старая, разница во времени ${Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes()} минут, тикер $t" }
                        return@runBlocking
                    }
                    if (candles.isNotEmpty() && Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes() < timeframe.toMin() && !forceNotCheckLastCandle) {
                        candles = candles.dropLast(1)
                    }
                    if (candles.isNotEmpty() && Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes() >= timeframe.toMin()/2 + timeframe.toMin()*2 && !forceNotCheckLastCandle) {
                        logger.info { "Индикатор пропущен, предпоследняя свечка слишком старая, разница во времени ${Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes()} минут, тикер $t" }
                        return@runBlocking
                    }
                    val alert = volumeAlerts.isBigVolume(candles)
                    if (alert != null) {
                        allAlerts.add(TickerWithAlert(ticker, alert))
                    }
                }
            }
        }
    }

    private fun sendAlerts(favoriteAlerts: List<TickerWithAlert>, notFavoriteAlerts: List<TickerWithAlert>, nowString: String) {
        val favoriteTickersText = favoriteAlerts.toText(favorite = true)
        val notFavoriteTickersText = notFavoriteAlerts.toText(favorite = false)
        val favoriteTickersLoggerText = favoriteAlerts.toText(favorite = true, debug = true)
        val notFavoriteTickersLoggerText = notFavoriteAlerts.toText(favorite = false, debug = true)

        val favoriteSeparator = if (favoriteTickersText.isNotEmpty()) "\n---\n" else ""
        val messageText = "⚠️ Повышенные объемы на ${timeframe.toText()} в $nowString:\n$favoriteTickersText${favoriteSeparator}$notFavoriteTickersText"
        logger.info { "Повышенные объемы на ${timeframe.toText()} в $nowString:\n$favoriteTickersLoggerText${favoriteSeparator}$notFavoriteTickersLoggerText" }
        bot.chatIds.forEach { id ->
            val message = SendMessage()
            message.chatId = id
            message.text = messageText
            message.parseMode = "Markdown"
            bot.execute(message)
        }
    }

    private fun checkProsessingAlertsFinished(executorService: ExecutorService): Boolean {
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

    private fun getFavoriteAlerts(allAlerts: MutableList<TickerWithAlert>, volumeXMedian: BigDecimal): List<TickerWithAlert> {
        val favoriteTickersOrderIndex = favoriteTickers.withIndex().associate { it.value to it.index }
        return allAlerts.filter { favoriteTickers.contains(it.ticker.ticker) && it.alert.isAlert(volumeXMedian) }.sortedBy { favoriteTickersOrderIndex[it.ticker.ticker] }
    }

    private fun getNotFavoriteAlerts(allAlerts: MutableList<TickerWithAlert>, favoriteAlerts: List<TickerWithAlert>, volumeXMedian: BigDecimal): List<TickerWithAlert> {
        val notFavoriteAlerts = allAlerts.filter { it.alert.isAlert(volumeXMedian) }.toMutableList()
        notFavoriteAlerts.removeIf { favoriteAlerts.map { f -> f.ticker.ticker }.contains(it.ticker.ticker) }
        notFavoriteAlerts.sortBy {
            if (it.alert.volumeX!! >= BigDecimal(7) && it.alert.pricePercentage!!.abs() >= BigDecimal(1)) {
                it.alert.volumeX!! * it.alert.pricePercentage!!
            } else {
                it.alert.volumeX!!
            }
        }
        return notFavoriteAlerts.take(showNotFavoriteTickersSize)
    }

    private fun Timeframe.toMin(): Int {
        return when(this) {
            Timeframe.MIN1 -> 1
            Timeframe.MIN5 -> 5
            Timeframe.MIN15 -> 15
            Timeframe.HOUR1 -> 60
            else -> { throw Exception("Invalid timeframe: $timeframe") }
        }
    }

    private fun Timeframe.toText(): String {
        return when(this) {
            Timeframe.MIN1 -> "M1"
            Timeframe.MIN5 -> "M5"
            Timeframe.MIN15 -> "M15"
            Timeframe.HOUR1 -> "H1"
            Timeframe.DAY1 -> "D1"
            else -> { throw Exception("Invalid timeframe: $timeframe") }
        }
    }

    private fun List<TickerWithAlert>.toText(favorite: Boolean, debug: Boolean = false): String {
        return joinToString(separator = "\n") {
            val vBold = if (it.alert.volumeX!! > BigDecimal(7)) "*" else ""
            val pBold = if (it.alert.pricePercentage!! > BigDecimal(1)) "*" else ""
            val isImportant = if (favorite && (it.alert.volumeX!! > BigDecimal(7) && it.alert.pricePercentage!! > BigDecimal(1))) "❗️" else ""
            val star = if (favorite) "★" else ""
            val debugText = if (debug) { " " + it.alert.details } else ""
            ("$star${isImportant}#${it.ticker.ticker.symbol} ${it.ticker.shortDescription} = " +
                    "${vBold}x${it.alert.volumeX}${vBold} " +
                    "${pBold}${it.alert.pricePercentage?.toStringWithSign()}%${pBold}${debugText}")
        }
    }
}