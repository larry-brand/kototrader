package org.cryptolosers.trading.connector

import org.cryptolosers.trading.TradingApi

class Connector(val internalTerminalConnector: InternalTerminalConnector, val reconnectRunnable: ReconnectThread = ReconnectThread(internalTerminalConnector)) {

    var reconnectThread: Thread? = null

    fun connect() {
        reconnectThread = Thread(reconnectRunnable)
        reconnectThread!!.start()
        while(reconnectRunnable.connector.isConnected() != ConnectionStatus.CONNECTED) {
            Thread.sleep(1000)
        }
    }

    fun changePassword(newPassword: String) {
        internalTerminalConnector.changePassword(newPassword)
    }

    fun abort() {
        reconnectThread!!.interrupt()
    }

    fun tradingApi(): TradingApi {
        return internalTerminalConnector.tradingApi()
    }

}