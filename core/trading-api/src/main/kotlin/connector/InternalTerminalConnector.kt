package org.cryptolosers.trading.connector

import org.cryptolosers.trading.TradingApi

interface InternalTerminalConnector {
    fun connect()
    fun softReconnect()
    fun hardReconnect()
    fun isConnected(): ConnectionStatus
    fun setConnectionListener(runnable: Runnable?)

    fun changePassword(newPassword: String)

    fun tradingApi(): TradingApi
}

enum class ConnectionStatus {
    CONNECTED, NOT_CONNECTED, CONNECTING
}