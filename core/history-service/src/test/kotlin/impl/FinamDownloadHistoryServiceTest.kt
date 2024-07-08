package org.cryptolosers.history.impl

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import org.cryptolosers.history.HistoryTickerId
import org.cryptolosers.history.LOCAL_DATE_PATTERN
import org.cryptolosers.history.LOCAL_DATE_TIME_PATTERN
import org.cryptolosers.history.Timeframe
import org.cryptolosers.history.download.api.DownloadHistoryApi
import org.cryptolosers.history.download.impl.FinamDownloadHistoryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File.separator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FinamDownloadHistoryServiceTest {

    @BeforeEach
    fun init() {
        System.setProperty("user.dir", ".$separator..$separator..$separator");
    }

    @Test
    fun `we can download raw stock data with corrected url`() {
        val service = spyk<FinamDownloadHistoryService>()

        every { service.inputStream(any()) } returns ByteArrayInputStream("data_for_another_url".toByteArray())
        every { service.inputStream("https://export.finam.ru/export9.out?market=14&em=22460&token=&code=SPFB.BR&apply=0&df=8&mf=0" +
                "&yf=2021&from=08.01.2021&dt=10&mt=0&yt=2021&to=10.01.2021&p=7&f=OUTPUT_FILE&e=.txt&cn=SPFB.BR&dtf=1&tmf=1&MSOR=1" +
                "&mstime=on&mstimever=1&sep=5&sep2=1&datf=1") } returns ByteArrayInputStream("data_for_my_url".toByteArray())

        val startDate = LocalDate.parse("20210108", DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
        val endDate = LocalDate.parse("20210110", DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN))
        val bytes = service.download(HistoryTickerId("BR"), Timeframe.HOUR1, startDate, endDate)

        bytes shouldBe "data_for_my_url".toByteArray()
    }


    val FINAM_DATA = """SPFB.BR 60 20211228 080000 78.7000000 78.7400000 78.6400000 78.7000000 5854
SPFB.BR 60 20211228 090000 78.6900000 78.9600000 78.6300000 78.8400000 11454
"""
    @Test
    fun `we can parse stock data to candles`() {
        val api: DownloadHistoryApi = FinamDownloadHistoryService()
        val b = FINAM_DATA.toByteArray()
        val parsed = api.parseBytesToCandles(b)
        val date = DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN).withZone(ZoneOffset.UTC).format(parsed.first().timestamp)

        date shouldBe "20211228 080000"
        parsed.first().openPrice shouldBe BigDecimal("78.7")
        parsed.first().highPrice shouldBe BigDecimal("78.74")
        parsed.first().lowPrice shouldBe BigDecimal("78.64")
        parsed.first().closePrice shouldBe BigDecimal("78.7")
        parsed.first().volume shouldBe 5854
        parsed.size shouldBe  2
    }

    @Test
    fun `we can parse stock data to tiks enties`() {
        // TODO
    }

}