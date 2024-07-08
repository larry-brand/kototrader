package org.cryptolosers.transaq.connector.concurrent

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils.upperCase
import org.cryptolosers.commons.printFail
import org.cryptolosers.commons.printNeutral
import org.cryptolosers.commons.printSuccess
import org.cryptolosers.commons.utils.JAXBUtils
import org.cryptolosers.trading.TradingApi
import org.cryptolosers.trading.connector.ConnectionStatus
import org.cryptolosers.trading.connector.TerminalConnector
import org.cryptolosers.transaq.ConfigTransaqFile
import org.cryptolosers.transaq.TransaqMemory
import org.cryptolosers.transaq.TransaqTradingService
import org.cryptolosers.transaq.TransaqTradingTCallback
import org.cryptolosers.transaq.connector.jna.TXmlConnector
import org.cryptolosers.transaq.xml.callback.ServerStatus
import org.cryptolosers.transaq.xml.command.ChangePass
import org.cryptolosers.transaq.xml.command.Connect
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TransaqConnector: TerminalConnector {

    private val logger = KotlinLogging.logger {}
    private val memory = TransaqMemory()
    private val transaqTradingTCallback: TransaqTradingTCallback = TransaqTradingTCallback(memory)
    private val tradingApi = TransaqTradingService(memory)

    @Volatile
    var connectedStatus = AtomicInteger(0)

    private val connectionListener: Runnable? = null

    constructor() {
        // Initialize library and init path for transaq log files: _dsp.log, _ts.log, _xdf.log
        TXmlConnector.initialize(".\\build\\\u0000", 2)
        TXmlConnector.setCallback(transaqTradingTCallback)
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

                checkResult(resultXml, "CONNECTION")

                // get xml serverStatus from callback
                val serverStatus = memory.responseServerStatuses.poll(60, TimeUnit.SECONDS)
                if (serverStatus == null) {
                    logger.printFail {  "CONNECTION FAILED , Transaq.ServerStatus null" }
                    throw IllegalStateException("Transaq.ServerStatus null")
                }
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
                (connectedStatus as Object).notifyAll()
            }
        }
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

//    override fun setConnectionListener(runnable: Runnable?) {
//        TODO("Not yet implemented")
//    }

    override fun changePassword(newPassword: String) {
        val file = ConfigTransaqFile()
        file.init()
        val changePass = ChangePass()
        changePass.oldpass = file.password
        changePass.newpass = newPassword
        val changePassXml = JAXBUtils.marshall(changePass)
        val changePassResultXml = TXmlConnector.sendCommand(changePassXml)
        checkResult(changePassResultXml, "CHANGE PASSWORD")
    }

    override fun tradingApi(): TradingApi {
        return tradingApi
    }
}

fun sendCommandAndCheckResult(command: Any): org.cryptolosers.transaq.xml.misc.Result {
    val commandXml = JAXBUtils.marshall(command)
    val commandResultXml = TXmlConnector.sendCommand(commandXml)
    return checkResult(commandResultXml, upperCase(command::class.java.simpleName))
}

fun checkResult(resultXml: String, commandName: String): org.cryptolosers.transaq.xml.misc.Result {
    val result =
        JAXBUtils.unmarshall(
            resultXml,
            org.cryptolosers.transaq.xml.misc.Result::class.java
        )
    if (result.success == null || result.success == false) {
        logger.printFail { "$commandName FAILED , Transaq.Result is not successful" }
        throw IllegalStateException()
    }
    return result
}
private val logger = KotlinLogging.logger {}