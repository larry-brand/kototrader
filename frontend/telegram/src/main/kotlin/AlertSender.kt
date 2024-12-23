package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.commons.toStringWithSign
import org.cryptolosers.indicators.TickerWithAlert
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

    fun send(allAlerts: List<TickerWithAlert>, now: LocalDateTime, userSettings: UserSettings) {
        val nowString = now.format(DateTimeFormatter.ofPattern("HH:mm"))

        val activeFavoriteTickers = getActiveFavoriteTickets(userSettings.favoriteTickers)
        val favoriteAlerts = getFavoriteAlerts(allAlerts, userSettings.volumeXMedianFavoriteTickers, activeFavoriteTickers)
        val notFavoriteAlerts = getNotFavoriteAlerts(allAlerts, favoriteAlerts, userSettings.volumeXMedianNotFavoriteTickers)

        if (favoriteAlerts.isNotEmpty() || notFavoriteAlerts.isNotEmpty()) {
            sendAlerts(favoriteAlerts, notFavoriteAlerts, nowString)
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

    private fun getFavoriteAlerts(allAlerts: List<TickerWithAlert>, volumeXMedian: BigDecimal, activeFavoriteTickers: List<Ticker>): List<TickerWithAlert> {
        val favoriteTickersOrderIndex = activeFavoriteTickers.withIndex().associate { it.value to it.index }
        return allAlerts.filter { activeFavoriteTickers.contains(it.ticker.ticker) && it.alert.isAlert(volumeXMedian) }.sortedBy { favoriteTickersOrderIndex[it.ticker.ticker] }
    }

    private fun getNotFavoriteAlerts(allAlerts: List<TickerWithAlert>, favoriteAlerts: List<TickerWithAlert>, volumeXMedian: BigDecimal): List<TickerWithAlert> {
        val notFavoriteAlerts = allAlerts.filter { it.alert.isAlert(volumeXMedian) }.toMutableList()
        notFavoriteAlerts.removeIf { favoriteAlerts.map { f -> f.ticker.ticker }.contains(it.ticker.ticker) }
        notFavoriteAlerts.sortedByDescending {
            if (it.alert.volumeX >= BigDecimal(7) && it.alert.pricePercentage.abs() >= BigDecimal(1)) {
                it.alert.volumeX * it.alert.pricePercentage
            } else {
                it.alert.volumeX
            }
        }
        return notFavoriteAlerts.take(showCountNotFavoriteTickers)
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

    private fun List<TickerWithAlert>.toText(favorite: Boolean, debug: Boolean = false): String {
        return joinToString(separator = "\n") {
            val vBold = if (it.alert.volumeX > BigDecimal(7)) "*" else ""
            val pBold = if (it.alert.pricePercentage > BigDecimal(1)) "*" else ""
            val isImportant = if (favorite && (it.alert.volumeX > BigDecimal(7) && it.alert.pricePercentage > BigDecimal(1))) "❗️" else ""
            val star = if (favorite) "★" else ""
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