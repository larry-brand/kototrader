package org.cryptolosers.telegrambot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.cryptolosers.indicators.TickerWithAlert
import org.cryptolosers.indicators.VolumeAlerts
import org.cryptolosers.indicators.getCandlesCount
import org.cryptolosers.trading.ViewTradingApi
import org.cryptolosers.trading.model.Timeframe

class CryptoAlertBuilder(
    private val cryptoTradingApi: ViewTradingApi,
    private val timeframe: Timeframe,
    private val appCfg: AppCfg
) {
    private val volumeAlerts = VolumeAlerts()
    private val logger = KotlinLogging.logger {}

    fun build(): List<TickerWithAlert> {
        val allCryptoAlerts = runBlocking {
            makeCryptoAlerts()
        }
        return allCryptoAlerts
    }

    private suspend fun makeCryptoAlerts(): MutableList<TickerWithAlert> {
        val allAlerts = mutableListOf<TickerWithAlert>()
        appCfg.cryptoCfgTickers.forEach { t ->
            val candles = cryptoTradingApi.getLastCandles(
                t.ticker,
                timeframe,
                getCandlesCount(t.ticker, timeframe),
            )
            val alert = volumeAlerts.isBigVolume(candles)
            allAlerts.add(TickerWithAlert(t, alert))
        }
        return allAlerts
    }
}