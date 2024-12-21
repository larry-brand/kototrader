package org.cryptolosers.telegrambot
//
//import mu.KotlinLogging
//import org.cryptolosers.trading.ViewTradingApi
//import org.cryptolosers.trading.model.Timeframe
//import java.time.LocalDate
//import java.time.LocalTime
//import java.time.format.DateTimeFormatter
//
//class CandleTelegramPublisher(
//    private val bot: TradingTelegramBot,
//    private val tradingApi: ViewTradingApi,
//    private val timeframe: Timeframe,
//    private val now: LocalDate
//): Runnable {
//    private val logger = KotlinLogging.logger {}
//
//    override fun run() {
//        val nowString = now.format(DateTimeFormatter.ofPattern("HH:mm"))
//
//        if (favoriteAlerts.isNotEmpty() || notFavoriteAlerts.isNotEmpty()) {
//            sendAlerts(favoriteAlerts, notFavoriteAlerts, nowString)
//        } else {
//            logger.info { "Не найдено повышенных объемов на ${timeframe.toText()} в $nowString" }
//        }
//    }
//}