package org.cryptolosers.history

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkConstructor
import org.cryptolosers.history.file.HistoryFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.File.separator
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


open class HistoryServiceTest {

    @BeforeEach
    fun init() {
        System.setProperty("user.dir", ".$separator..$separator..$separator")
    }

    @Test
    fun `we can download history 1 hour`() {
        // TODO
    }

    @Test
    fun `we can download history tiks`() {
        // TODO
    }

    @Test
    fun `we already downloaded history 1 hour`() {
        // TODO
    }

    @Test
    fun `we already downloaded history tiks`() {
        // TODO
    }

    val startDate = LocalDateTime.parse("20210108 100000", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN))
    val endDate = LocalDateTime.parse("20210108 120000", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN))
    val BIN_DATA = """20210108 110000 54.09 54.92 54.09 54.77 102342
20210108 120000 54.76 54.94 54.55 54.82 124817
"""
    @Test
    fun `we can call onNextCandle`() {
        val service = HistoryService()
        mockkConstructor(HistoryFile::class)
        every { anyConstructed<HistoryFile>().bufferedReader() } returns BufferedReader(StringReader(BIN_DATA))

        var count = 0
        val robot: (HistoryCandle) -> Unit = {
            if (count == 0) {
                it.timestamp shouldBe LocalDateTime.parse("20210108 110000", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN))
                it.openPrice shouldBe BigDecimal("54.09")
                it.highPrice shouldBe BigDecimal("54.92")
                it.lowPrice shouldBe BigDecimal("54.09")
                it.closePrice shouldBe BigDecimal("54.77")
                it.volume shouldBe 102342
            }
            count++
        }

        service.runOnCandles(HistoryTickerId("BR"), Timeframe.HOUR1, startDate, endDate, robot)
        count shouldBe 2
    }

    @Test
    fun `we can read candles`() {
        val service = HistoryService()
        mockkConstructor(HistoryFile::class)
        every { anyConstructed<HistoryFile>().bufferedReader() } returns BufferedReader(StringReader(BIN_DATA))

        val candles = service.readCandles(HistoryTickerId("BR"), Timeframe.HOUR1, startDate, endDate)

        candles.size shouldBe 2
        candles[0].timestamp shouldBe LocalDateTime.parse("20210108 110000", DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN))
        candles[0].openPrice shouldBe BigDecimal("54.09")
        candles[0].highPrice shouldBe BigDecimal("54.92")
        candles[0].lowPrice shouldBe BigDecimal("54.09")
        candles[0].closePrice shouldBe BigDecimal("54.77")
        candles[0].volume shouldBe 102342
    }

}