package org.cryptolosers.telegrambot

import mu.KotlinLogging
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.concurrent.ConcurrentHashMap

class TradingTelegramBot() : TelegramLongPollingBot() {
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

            if (!chatIds.contains(chatId)) {
                logger.info { "Новый пользователь написал боту, username: ${message.from.userName}, " +
                        "имя: ${message.from.firstName} ${message.from.lastName}, chatId: ${chatId}" }
            }
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