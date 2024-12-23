package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.commons.allCryptoCfgTickers
import org.cryptolosers.commons.allStockCfgTickers
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.model.Exchanges
import org.cryptolosers.trading.model.TickerInfo

class AppCfg(
    private val tradingApi: ViewTradingApi,
    private val cryptoTradingApi: ViewTradingApi,
) {
    val stockCfgTickers: List<TickerInfo>
    val cryptoCfgTickers: List<TickerInfo>

    init {
        stockCfgTickers = runBlocking {
            allStockCfgTickers.mapNotNull { t ->
                val similarTickers = tradingApi.getAllTickers()
                    .filter { it.ticker.symbol == t && (it.ticker.exchange == Exchanges.MOEX || it.ticker.exchange == Exchanges.MOEX_FORTS) }
                if (similarTickers.isEmpty()) {
                    logger.error { "Не найден тикер: $t" }
                    return@mapNotNull null
                }
                if (similarTickers.size > 1) {
                    logger.error { "Найдено больше, чем один тикер: $t, найдено: $similarTickers" }
                    return@mapNotNull null
                }
                similarTickers.first()
            }
        }
        cryptoCfgTickers = runBlocking {
            allCryptoCfgTickers.mapNotNull { t ->
                val similarTickers = cryptoTradingApi.getAllTickers().filter { it.ticker.symbol == t }
                if (similarTickers.isEmpty()) {
                    logger.error { "Не найден тикер: $t" }
                    return@mapNotNull null
                }
                if (similarTickers.size > 1) {
                    logger.error { "Найдено больше, чем один тикер: $t, найдено: $similarTickers" }
                    return@mapNotNull null
                }
                similarTickers.first()
            }
        }
    }

    fun getAllCfgTickers(): List<TickerInfo> {
        return stockCfgTickers + cryptoCfgTickers
    }

    private val logger = KotlinLogging.logger {}

}