package org.cryptolosers.simulator

import org.cryptolosers.trading.TradingApi

interface IRobot {

    suspend fun onNextMinute(tradingApi: TradingApi)

}