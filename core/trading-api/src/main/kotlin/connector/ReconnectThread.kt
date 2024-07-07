package org.cryptolosers.trading.connector

import mu.KotlinLogging

class ReconnectThread(val connector: InternalTerminalConnector) : Runnable {

    private val logger = KotlinLogging.logger {}

    override fun run() {
        try {
            connector.connect()
        } catch (e: RuntimeException) {
            logger.error("", e)
        }

        // reconnecting
        while (true) {
            val status: ConnectionStatus = connector.isConnected()
            if (status === ConnectionStatus.NOT_CONNECTED) {
                try {
                    connector.connect()
                } catch (e: RuntimeException) {
                    logger.error("", e)
                }
            } else if (status === ConnectionStatus.CONNECTED) {
                // ok
                // TODO check connection, call any command
            } else if (status === ConnectionStatus.CONNECTING) {
                // ok
            } else {
                // not ok
            }
            try {
                // wait 5 sec and check connection status again and again
                Thread.sleep(5000)
                //logger.debug { "try reconnect again, 5 sec" }
            } catch (e: InterruptedException) {
                //logger.debug { "Connection thread interrupted, exit " }
                //return
            }
        }
    }

}