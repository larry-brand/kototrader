package org.cryptolosers.transaq.connector.concurrent

import mu.KotlinLogging
import org.cryptolosers.commons.printFail
import org.cryptolosers.commons.printNeutral
import org.cryptolosers.commons.printSuccess
import org.cryptolosers.commons.utils.JAXBUtils
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.connector.ConnectionStatus
import org.cryptolosers.trading.connector.InternalTerminalConnector
import org.cryptolosers.transaq.ConfigTransaqFile
import org.cryptolosers.transaq.TransaqMemory
import org.cryptolosers.transaq.TransaqTradingApi
import org.cryptolosers.transaq.TransaqTradingApiTCallback
import org.cryptolosers.transaq.connector.jna.TXmlConnector
import org.cryptolosers.transaq.xml.callback.ServerStatus
import org.cryptolosers.transaq.xml.command.ChangePass
import org.cryptolosers.transaq.xml.command.Connect
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class InternalTransaqConnector: InternalTerminalConnector {

    private val logger = KotlinLogging.logger {}
    private val memory = TransaqMemory()
    private val transaqTradingApiTCallback: TransaqTradingApiTCallback = TransaqTradingApiTCallback(memory)
    private val tradingApi = TransaqTradingApi(memory)

    @Volatile
    var connectedStatus = AtomicInteger(0)

    private val connectionListener: Runnable? = null

    constructor() {
        // Initialize library and init path for transaq log files: _dsp.log, _ts.log, _xdf.log
        TXmlConnector.initialize(".\\build\\\u0000", 2)
        TXmlConnector.setCallback(transaqTradingApiTCallback)
    }

    override fun connect() {
        synchronized(connectedStatus) {
            try {
                connectedStatus.set(-1)
                val file = ConfigTransaqFile()
                file.init()
                memory.reset()
                val connect = Connect()
                connect.login = file.login
                connect.password = file.password
                connect.host = file.host
                connect.port = file.port
                connect.language = "ru"
                connect.milliseconds = true
                connect.push_u_limits = 3
                connect.push_pos_equity = 3
                val connectXml = JAXBUtils.marshall(connect)
                logger.printNeutral { "CONNECTING to $file , please wait..." }
                val resultXml: String = TXmlConnector.sendCommand(connectXml)
                val result =
                    JAXBUtils.unmarshall(
                        resultXml,
                        org.cryptolosers.transaq.xml.misc.Result::class.java
                    )
                if (result.success == null || result.success == false) {
                    logger.printFail { "CONNECTION FAILED , Transaq.Result is not successful" }
                    throw IllegalStateException()
                }

                // get xml serverStatus from callback
                val serverStatus: ServerStatus =
                    memory.responseServerStatuses.poll(60, TimeUnit.SECONDS)
                if (java.lang.Boolean.TRUE.toString() != serverStatus.connected) {
                    logger.printFail {  "CONNECTION FAILED , Transaq.ServerStatus.connected = " + serverStatus.connected }
                    connectionListener?.run()
                    throw IllegalStateException()
                }

                logger.printSuccess { "CONNECTED SUCCESSFUL to $file" }
                connectedStatus.set(1)

            } catch (e: RuntimeException) {
                logger.printFail { "CONNECTION FAILED , reason = ${e.message}" }
                connectedStatus.set(0)
                connectionListener?.run()
                throw IllegalStateException(e)
            } finally {
                (connectedStatus as java.lang.Object).notifyAll()
            }
        }
    }

    override fun softReconnect() {
        TODO("Not yet implemented")
    }

    override fun hardReconnect() {
        TODO("Not yet implemented")
    }

    override fun isConnected(): ConnectionStatus {
        val connectedStatus = connectedStatus.get()
        if (connectedStatus == 0) {
            return ConnectionStatus.NOT_CONNECTED
        } else if (connectedStatus == 1) {
            //TODO: check if connected - we need to call any command
            return ConnectionStatus.CONNECTED
        } else if (connectedStatus == -1) {
            return ConnectionStatus.CONNECTING
        }
        return ConnectionStatus.NOT_CONNECTED
    }

    override fun setConnectionListener(runnable: Runnable?) {
        TODO("Not yet implemented")
    }

    override fun changePassword(newPassword: String) {
        val file = ConfigTransaqFile()
        file.init()
        val changePass = ChangePass()
        changePass.oldpass = file.password
        changePass.newpass = newPassword
        val changePassXml = JAXBUtils.marshall(changePass)
        val changePassResultXml = TXmlConnector.sendCommand(changePassXml)

        val result =
            JAXBUtils.unmarshall(
                changePassResultXml,
                org.cryptolosers.transaq.xml.misc.Result::class.java
            )
        if (result.success == null || result.success == false) {
            logger.printFail { "CHANGE PASSWORD FAILED , Transaq.Result is not successful" }
            throw IllegalStateException()
        }
    }

    override fun tradingApi(): TradingApi {
        return tradingApi
    }
}