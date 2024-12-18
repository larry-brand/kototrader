import kotlinx.coroutines.runBlocking
import org.cryptolosers.indicators.VolumeIndicators
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.Exchanges
import org.cryptolosers.trading.model.Ticker
import org.cryptolosers.trading.model.Timeframe
import org.cryptolosers.transaq.connector.concurrent.TransaqConnector
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread


class TraderTelegramBot() : TelegramLongPollingBot() {

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

fun main() {
    val bot = TraderTelegramBot()
    thread {
        try {
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(bot)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    val conn  = Connector(TransaqConnector())
    conn.connect()
    val tradingApi: ViewTradingApi = conn.tradingApi()
    val moexWatchList = listOf("ROSN", "SBER", "LKOH", "GAZP", "NVTK", "LNZL")
    val favorites = listOf("SBER")
    thread {
        runBlocking {
            val volumeIndicators = VolumeIndicators(tradingApi)
            println("Run indicators:")
            val printSignals = moexWatchList.mapNotNull {
                val indicator = volumeIndicators.isBigVolumeOnStartSession(Ticker(it, Exchanges.MOEX), Timeframe.MIN15)
                if (indicator.isSignal) {
                    val tickerInfo = tradingApi.getAllTickers().firstOrNull { t -> t.ticker == Ticker(it, Exchanges.MOEX) }
                    "${tickerInfo?.shortDescription}($it)  v: ${indicator.volumeChangeFromMedianPercentageInCandle?.toStringWithSign()}% " +
                            "p: ${indicator.priceChangePercentageInCandle?.toStringWithSign()}%"
                } else {
                    null
                }
            }

            if (printSignals.isNotEmpty()) {
                val messageText = "Повышенные объемы на открытии на M15 в 10:15:" + "\n" + printSignals.joinToString(separator = "\n")
                println(messageText)
                bot.chatIds.forEach { id ->
                    val message = SendMessage()
                    message.chatId = id
                    message.text = messageText
                    bot.execute(message) // отправка сообщения
                }
            }
        }
    }


}

fun BigDecimal.toStringWithSign(): String {
    return if (this <= BigDecimal.ZERO ) {
        toString()
    }
    else {
        "+" + toString()
    }
}