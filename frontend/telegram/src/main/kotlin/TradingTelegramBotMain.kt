package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.Exchanges
import org.cryptolosers.trading.model.Timeframe
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

val immediateRun = true
val forceNotCheckLastCandle = true
val showNotFavoriteTickersSize = 5
val volumeXMedianFavoriteTickers = BigDecimal(2)
val volumeXMedianNotFavoriteTickers = BigDecimal(5)
val favoriteTickers = listOf("RIH5", "SiH5", "SBER", "SVCB", "BSPB", "BSPBP")

fun main() {
    val logger = KotlinLogging.logger {}

    val bot = TradingTelegramBot()
    thread {
        try {
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(bot)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    thread {
        val conn  = Connector(TransaqConnector())
        conn.connect()
        val tradingApi: ViewTradingApi = conn.tradingApi()

        val favoriteTickers = runBlocking {
            favoriteTickers.map { f ->
                tradingApi.getAllTickers().first { it.ticker.symbol == f && (it.ticker.exchange == Exchanges.MOEX || it.ticker.exchange == Exchanges.MOEX_FORTS) }.ticker
            }
        }

        val scheduler = Executors.newScheduledThreadPool(1)

        val task = CandleTelegramHandler(bot, tradingApi, favoriteTickers, Timeframe.MIN15)

        val currentTime = Calendar.getInstance()
        val minute = currentTime.get(Calendar.MINUTE)
        val second = currentTime.get(Calendar.SECOND)
        val delay = when {
            immediateRun -> 0
            minute < 15 -> (15 - minute) * 60 + (10 - second) // до 15 минут
            minute < 30 -> (30 - minute) * 60 + (10 - second) // до 30 минут
            minute < 45 -> (45 - minute) * 60 + (10 - second) // до 45 минут
            else -> (60 - minute) * 60 + (10 - second) // до следующего часа
        }
        scheduler.scheduleAtFixedRate(task, delay.toLong(), 15*60, TimeUnit.SECONDS)
    }

}