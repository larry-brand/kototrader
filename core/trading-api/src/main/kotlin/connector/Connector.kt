package org.cryptolosers.trading.connector

import org.cryptolosers.trading.TradingApi

class Connector(
    private val terminalConnector: TerminalConnector,
    private val reconnectRunnable: ReconnectThread = ReconnectThread(terminalConnector)
) {

    private var reconnectThread: Thread? = null

    fun connect() {
        reconnectThread = Thread(reconnectRunnable)
        reconnectThread!!.start()
        while(reconnectRunnable.connector.isConnected() != ConnectionStatus.CONNECTED) {
            Thread.sleep(1000)
        }
    }

    fun changePassword(newPassword: String) {
        terminalConnector.changePassword(newPassword)
    }

    fun abort() {
        reconnectThread!!.interrupt()
    }

    fun tradingApi(): TradingApi {
        return terminalConnector.tradingApi()
    }

}