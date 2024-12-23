package org.cryptolosers.telegrambot

import mu.KotlinLogging
import org.cryptolosers.bybit.ByBitViewTradingApi
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.Timeframe
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

val DEBUG = true
val immediateRun = DEBUG
val forceNotCheckLastCandle = DEBUG
val showCountNotFavoriteTickers = 5
val volumeXMedianFavoriteTickers = BigDecimal(0.1)
val volumeXMedianNotFavoriteTickers = BigDecimal(0.1)
val favoriteTickers = listOf("RIH5", "SiH5", "SBER", "SVCB", "BSPB", "BSPBP", "BTCUSDT", "TONUSDT", "UNKNOWN")

fun main() {
    val logger = KotlinLogging.logger {}

    val bot = TradingTelegramBot()
    thread {
        try {
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(bot)
            logger.info { "Telegram бот зарегистрирован, инициализирован" }
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    thread {
        val conn  = Connector(TransaqConnector())
        conn.connect()
        val stockTradingApi: ViewTradingApi = conn.tradingApi()
        val cryptoTradingApi: ViewTradingApi = ByBitViewTradingApi()
        val appCfg = AppCfg(stockTradingApi, cryptoTradingApi)

        val scheduler = Executors.newScheduledThreadPool(1)
        val task = Runnable {
            val now = LocalDateTime.now()

            // 15 min
            val stockAlerts = StockAlertBuilder(stockTradingApi, Timeframe.MIN15, appCfg).build() +
                    CryptoAlertBuilder(cryptoTradingApi, Timeframe.MIN15, appCfg).build()

            val alertSender = AlertSender(bot, Timeframe.MIN15, appCfg)
            val myUserSettings = UserSettings(
                favoriteTickers, showCountNotFavoriteTickers, volumeXMedianFavoriteTickers, volumeXMedianNotFavoriteTickers
            )
            alertSender.send(stockAlerts, now, myUserSettings)

            // 1h
            val minute = Calendar.getInstance().get(Calendar.MINUTE)
            if (minute in 0..3) {
                val stockAlertsH1 = StockAlertBuilder(stockTradingApi, Timeframe.HOUR1, appCfg).build() +
                        CryptoAlertBuilder(cryptoTradingApi, Timeframe.HOUR1, appCfg).build()

                val alertSenderH1 = AlertSender(bot, Timeframe.HOUR1, appCfg)
                alertSenderH1.send(stockAlertsH1, now, myUserSettings)
            }
        }

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
        scheduler.scheduleAtFixedRate(task, delay.toLong(), 15 * 60, TimeUnit.SECONDS)
    }


}

data class UserSettings(
    val favoriteTickers: List<String>,
    val showCountNotFavoriteTickers: Int,
    val volumeXMedianFavoriteTickers: BigDecimal,
    val volumeXMedianNotFavoriteTickers: BigDecimal
)