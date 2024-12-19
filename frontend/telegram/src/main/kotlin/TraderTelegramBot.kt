import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.commons.toStringWithSign
import org.cryptolosers.indicators.TickerWithIndicator
import org.cryptolosers.indicators.VolumeIndicators
import org.cryptolosers.indicators.getCandlesCount
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.connector.Connector
import org.cryptolosers.trading.model.Exchanges
import org.cryptolosers.trading.model.Session
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
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
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

val logger = KotlinLogging.logger {}

fun main() {
    val forceRun = true
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
    val moexWatchList = listOf("ROSN", "SBER", "LKOH", "GAZP", "NVTK", "LNZL", "SVCB")
    val favorites = listOf("SBER")

    thread {
        val scheduler = Executors.newScheduledThreadPool(1)

        val task = Runnable {
            val nowString = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)

            // Проверяем, что текущее время находится в пределах с 10:00 до 24:00 и это будний день
            if (!(currentHour in 10..24 && currentDay >= Calendar.MONDAY && currentDay <= Calendar.FRIDAY) && !forceRun) {
                logger.info { "Задача пропущена: не будний день или не в рабочие часы." }
                return@Runnable
            }

            val volumeIndicators = VolumeIndicators(tradingApi)
            logger.info { "\nЗапуск индикаторов:" }
            val startTime = System.currentTimeMillis()
            val executorService = Executors.newFixedThreadPool(5)

            val printSignals = Collections.synchronizedList(ArrayList<TickerWithIndicator>())
            moexWatchList.forEach { t ->
                executorService.submit {
                    runBlocking {
                        val tickers = tradingApi.getAllTickers()
                            .filter { it.ticker.symbol == t && it.ticker.exchange == Exchanges.MOEX }
                        if (tickers.isEmpty()) {
                            logger.warn { "can not find ticker: $t" }
                            return@runBlocking
                        }
                        if (tickers.size > 1) {
                            logger.warn { "find more than one tickers: $t, found: $tickers" }
                            return@runBlocking
                        }
                        val ticker = tickers.first()
                        val candles = tradingApi.getLastCandles(
                            ticker.ticker,
                            Timeframe.MIN15,
                            getCandlesCount(Timeframe.MIN15),
                            Session.CURRENT_AND_PREVIOUS
                        )
                        if (candles.isNotEmpty() && Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes() > 17 && !forceRun ) {
                            logger.info { "Индикатор пропущен, последняя свечка слишком старая, разница во времени ${Duration.between(candles.last().timestamp, LocalDateTime.now()).toMinutes()} минут, тикер $t" }
                            return@runBlocking
                        }
                        val indicator = volumeIndicators.isBigVolume(candles)
                        if (indicator.isSignal) printSignals.add(TickerWithIndicator(ticker, indicator))
                    }
                }
            }

            executorService.shutdown()
            val finished = executorService.awaitTermination(3, TimeUnit.MINUTES)
            if (finished) {
                logger.info { "All tasks finished successfully." }
            } else {
                logger.error { "Timeout reached before all tasks completed." }
            }

            val printSignalsString = printSignals.joinToString(separator = "\n") {
                ("#${it.ticker.ticker.symbol} ${it.ticker.shortDescription} = " +
                        "Об. x${it.indicator.volumeChangeFromMedianXInCandle} " +
                        "Ц. ${it.indicator.priceChangePercentageInCandle?.toStringWithSign()}%")
            }
            logger.info {
                "Время работы загрузки свечей: " + ((System.currentTimeMillis()
                    .toDouble() - startTime) / 1000).toBigDecimal().setScale(3, RoundingMode.HALF_DOWN) + " сек"
            }

            if (printSignals.isNotEmpty()) {
                val messageText = "⚠️ Повышенные объемы на M15 в $nowString:\n$printSignalsString"
                logger.info { messageText }
                bot.chatIds.forEach { id ->
                    val message = SendMessage()
                    message.chatId = id
                    message.text = messageText
                    message.parseMode = "Markdown"
                    bot.execute(message)
                }
            } else {
                logger.info {"Нет повышенных объемов на M15 в $nowString" }
            }
        }

        val currentTime = Calendar.getInstance()
        val minute = currentTime.get(Calendar.MINUTE)
        val second = currentTime.get(Calendar.SECOND)
//        val delay = when {
//            minute < 15 -> 15 - minute
//            minute < 30 -> 30 - minute
//            minute < 45 -> 45 - minute
//            else -> 60 - minute
//        }

//        val delay = when {
//            minute < 15 -> (15 - minute) * 60 + (10 - second) // до 15 минут
//            minute < 30 -> (30 - minute) * 60 + (10 - second) // до 30 минут
//            minute < 45 -> (45 - minute) * 60 + (10 - second) // до 45 минут
//            else -> (60 - minute) * 60 + (10 - second) // до следующего часа
//        }

        val delay = 0
        scheduler.scheduleAtFixedRate(task, delay.toLong(), 15*60, TimeUnit.SECONDS)
    }

}