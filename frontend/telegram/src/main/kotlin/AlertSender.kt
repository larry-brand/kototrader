package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.commons.toStringWithSign
import org.cryptolosers.indicators.DraftAlert
import org.cryptolosers.indicators.isAlert
import org.cryptolosers.trading.model.Ticker
import org.cryptolosers.trading.model.Timeframe
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.lang.Exception
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AlertSender(
    private val bot: TradingTelegramBot,
    private val timeframe: Timeframe,
    private val appCfg: AppCfg
) {
    private val logger = KotlinLogging.logger {}

    fun send(allAlerts: List<DraftAlert>, now: LocalDateTime, userSettings: UserSettings) {
        val nowString = now.format(DateTimeFormatter.ofPattern("HH:mm"))

        val activeFavoriteTickers = getActiveFavoriteTickets(userSettings.favoriteTickers)
        val tickersAlerts = getTickersAlerts(allAlerts, userSettings)
        val favoriteAlerts = getFavoriteAlerts(allAlerts, activeFavoriteTickers, userSettings)
        val notFavoriteAlerts = getNotFavoriteAlerts(allAlerts, tickersAlerts + favoriteAlerts, userSettings)

        if (tickersAlerts.isNotEmpty() || favoriteAlerts.isNotEmpty() || notFavoriteAlerts.isNotEmpty()) {
            sendAlerts(tickersAlerts + favoriteAlerts, notFavoriteAlerts, nowString)
        } else {
            logger.info { "Не найдено повышенных объемов на ${timeframe.toText()} в $nowString" }
        }
    }

    private fun getActiveFavoriteTickets(favoriteTickers: List<String>): List<Ticker> {
        return runBlocking {
            favoriteTickers.mapNotNull { t ->
                val foundTicker = appCfg.getAllCfgTickers().firstOrNull { it.ticker.symbol == t }
                if (foundTicker != null) {
                    foundTicker.ticker
                } else {
                    logger.warn { "Favorite тикер не найден, тикер: $t " }
                    null
                }
            }
        }
    }

    private fun getTickersAlerts(allAlerts: List<DraftAlert>, userSettings: UserSettings): List<DraftAlert> {
        val alerts = userSettings.volumePriceAlerts.filter { it.target is TickerAlertTargetSettings && it.timeframe == timeframe }
        return alerts.mapNotNull { a ->
            allAlerts.firstOrNull { a.target is TickerAlertTargetSettings && a.target.ticker == it.ticker.ticker.symbol && it.alert.isAlert(a.volumeXMedian) }
        }
    }

    private fun getFavoriteAlerts(allAlerts: List<DraftAlert>, activeFavoriteTickers: List<Ticker>, userSettings: UserSettings): List<DraftAlert> {
        val favoriteTickersOrderIndex = activeFavoriteTickers.withIndex().associate { it.value to it.index }
        val alert = userSettings.volumePriceAlerts.firstOrNull { it.target is TickerGroupTargetSettings && it.target.group == TickerGroupSettings.FavoriteTickers && it.timeframe == timeframe }
        if (alert != null) {
            return allAlerts.filter { activeFavoriteTickers.contains(it.ticker.ticker) && it.alert.isAlert(alert.volumeXMedian) }.sortedBy { favoriteTickersOrderIndex[it.ticker.ticker] }
        } else {
            return emptyList()
        }
    }

    private fun getNotFavoriteAlerts(allAlerts: List<DraftAlert>, favoriteAlerts: List<DraftAlert>, userSettings: UserSettings): List<DraftAlert> {
        val alert = userSettings.volumePriceAlerts.firstOrNull { it.target is TickerGroupTargetSettings && it.target.group == TickerGroupSettings.AllTickers && it.timeframe == timeframe }
        if (alert != null) {
            val notFavoriteAlerts = allAlerts.filter { it.alert.isAlert(alert.volumeXMedian) }.toMutableList()
            notFavoriteAlerts.removeIf { favoriteAlerts.map { f -> f.ticker.ticker }.contains(it.ticker.ticker) }
            notFavoriteAlerts.sortedByDescending {
                if (it.alert.volumeX >= BigDecimal(7) && it.alert.pricePercentage.abs() >= BigDecimal(1)) {
                    it.alert.volumeX * it.alert.pricePercentage
                } else {
                    it.alert.volumeX
                }
            }
            return notFavoriteAlerts.take(userSettings.notFavoriteTickersAlertsLimit)
        } else {
            return emptyList()
        }
    }

    private fun sendAlerts(tickersAndFavoriteAlerts: List<DraftAlert>, notFavoriteAlerts: List<DraftAlert>, nowString: String) {
        val tickersAndFavoriteTickersText = tickersAndFavoriteAlerts.toText(star = true)
        val notFavoriteTickersText = notFavoriteAlerts.toText(star = false)
        val tickersAndFavoriteTickersLoggerText = tickersAndFavoriteAlerts.toText(star = true, debug = true)
        val notFavoriteTickersLoggerText = notFavoriteAlerts.toText(star = false, debug = true)

        val favoriteSeparator = if (tickersAndFavoriteTickersText.isNotEmpty()) "\n---\n" else ""
        val messageText = "⚠️ Повышенные объемы на ${timeframe.toText()} в $nowString:\n$tickersAndFavoriteTickersText${favoriteSeparator}$notFavoriteTickersText"
        logger.info { "Повышенные объемы на ${timeframe.toText()} в $nowString:\n$tickersAndFavoriteTickersLoggerText${favoriteSeparator}$notFavoriteTickersLoggerText" }
        bot.chatIds.forEach { id ->
            val message = SendMessage()
            message.chatId = id
            message.text = messageText
            message.parseMode = "Markdown"
            bot.execute(message)
        }
    }

    private fun List<DraftAlert>.toText(star: Boolean, debug: Boolean = false): String {
        return joinToString(separator = "\n") {
            val vBold = if (it.alert.volumeX > BigDecimal(7)) "*" else ""
            val pBold = if (it.alert.pricePercentage > BigDecimal(1)) "*" else ""
            val isImportant = if (star && (it.alert.volumeX > BigDecimal(7) && it.alert.pricePercentage > BigDecimal(1))) "❗️" else ""
            val star = if (star) "★" else ""
            val debugText = if (debug) { " " + it.alert.details } else ""
            ("$star${isImportant}#${it.ticker.ticker.symbol} ${it.ticker.shortDescription} = " +
                    "${vBold}x${it.alert.volumeX}${vBold} " +
                    "${pBold}${it.alert.pricePercentage.toStringWithSign()}%${pBold}${debugText}")
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
}