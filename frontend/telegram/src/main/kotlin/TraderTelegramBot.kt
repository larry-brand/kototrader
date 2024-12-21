package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.Exchanges
import org.cryptolosers.trading.model.Timeframe
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TraderTelegramBot() : TelegramLongPollingBot() {
    private val logger = KotlinLogging.logger {}

    val chatIds = ConcurrentHashMap.newKeySet<String>()
    init {
     chatIds.add(System.getProperty("botMyChatId"))
    }

    // Возвращает имя бота
    override fun getBotUsername(): String {
        return System.getProperty("botUsername")
    }

    // Возвращает токен бота
    override fun getBotToken(): String {
        return System.getProperty("botToken")
    }

    // Обработка входящих сообщений
    override fun onUpdateReceived(update: Update?) {
        if (update != null && update.hasMessage()) {
            val message: Message = update.message
            val chatId = message.chatId.toString()
            val text = message.text

            chatIds.add(chatId)
            // Ответ на сообщение
            val responseMessage = when {
                text.equals("/start", ignoreCase = true) -> "Привет! Я ваш телеграм-бот."
                else -> "Вы написали: $text"
            }

            sendMessage(chatId, responseMessage)
        }
    }

    // Отправка сообщения
    private fun sendMessage(chatId: String, text: String) {
        try {
            val message = SendMessage()
            message.chatId = chatId
            message.text = text
            execute(message) // отправка сообщения
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}

val immediateRun = true
val forceNotCheckLastCandle = true
val showNotFavoriteTickersSize = 5

fun main() {
    val logger = KotlinLogging.logger {}

    val bot = TraderTelegramBot()
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
            listOf("RIH5", "SiH5", "SBER", "SVCB", "BSPB", "BSPBP", "ETLN").map { f ->
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