package org.cryptolosers.simulator

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.cryptolosers.history.HistoryTickerId
import org.cryptolosers.history.LOCAL_DATE_TIME_FRIENDLY_PATTERN
import org.cryptolosers.history.Timeframe
import org.cryptolosers.history.file.HistoryFile
import org.cryptolosers.trading.model.MarketOrder
import org.cryptolosers.trading.model.OrderDirection
import org.cryptolosers.trading.model.Ticker
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class SimulatorTradingApiTest {

    val startDate = LocalDateTime.parse("2021-01-03 00:00:00", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FRIENDLY_PATTERN))
    val endDate = LocalDateTime.parse("2021-12-28 23:00:00", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FRIENDLY_PATTERN))
    val BIN_DATA = """20210108 110000 54.09 54.92 54.09 54.77 102342
20210108 120000 54.76 54.94 54.55 54.82 124817
"""
    @Test
    fun `we can run simulator`() {
        val simulator = HistorySimulator()
        mockkConstructor(HistoryFile::class)
        every { anyConstructed<HistoryFile>().bufferedReader() } returns BufferedReader(StringReader(BIN_DATA))
        var sended = false

        simulator.runOnCandles(HistoryTickerId("BR1"), Timeframe.ONE_HOUR, startDate, endDate) {
            runBlocking {
                if (!sended)
                    simulator.tradingApi.sendOrder(
                        MarketOrder(
                            Ticker("BR"),
                            abs(1L),
                            OrderDirection.SELL
                        )
                    )
                sended = true
            }
        }

        runBlocking {
            val wallet = simulator.tradingApi.getWallet()
            wallet.balance shouldBeLessThan BigDecimal(100_000)
            wallet.equity shouldBeGreaterThan BigDecimal(100_000)
            println(simulator.tradingApi.getWallet().toString() + "  | Dials " + simulator.tradingApi.dials)
        }
    }

}