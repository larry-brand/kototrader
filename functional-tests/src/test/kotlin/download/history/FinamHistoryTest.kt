package org.cryptolosers.functionaltests.history

import io.kotest.matchers.shouldBe
import org.cryptolosers.history.HistoryTickerId
import org.cryptolosers.history.LOCAL_DATE_FRIENDLY_PATTERN
import org.cryptolosers.history.LOCAL_DATE_TIME_PATTERN
import org.cryptolosers.history.Timeframe
import org.cryptolosers.history.download.api.DownloadHistoryApi
import org.cryptolosers.history.download.impl.FinamDownloadHistoryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FinamHistoryTest {

    @BeforeEach
    fun init() {
        System.setProperty("user.dir", ".${File.separator}..${File.separator}")
    }

    @Test
    fun `we can download correct stock data`() {
        val api: DownloadHistoryApi = FinamDownloadHistoryService()
        val startDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern(LOCAL_DATE_FRIENDLY_PATTERN))
        val endDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern(LOCAL_DATE_FRIENDLY_PATTERN))

        val downloaded: ByteArray = api.download(HistoryTickerId("BR"), Timeframe.ONE_HOUR, startDate, endDate)

        val s = String(downloaded)
        for (i in 0 until s.lines().size - 1) {
            s.lines()[i] shouldBe FINAM_DATA.lines()[i]
        }
    }

    @Test
    fun `we can parse downloaded stock data`() {
        val api: DownloadHistoryApi = FinamDownloadHistoryService()
        val startDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern(LOCAL_DATE_FRIENDLY_PATTERN))
        val endDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern(LOCAL_DATE_FRIENDLY_PATTERN))
        val downloaded: ByteArray = api.download(HistoryTickerId("BR"), Timeframe.ONE_HOUR, startDate, endDate)

        val parsed = api.parseBytesToCandles(downloaded)

        val date = DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN).withZone(ZoneOffset.UTC).format(parsed.first().timestamp)
        date shouldBe "20211228 080000"
        parsed.first().openPrice shouldBe BigDecimal("78.7")
        parsed.first().highPrice shouldBe BigDecimal("78.74")
        parsed.first().lowPrice shouldBe BigDecimal("78.64")
        parsed.first().closePrice shouldBe BigDecimal("78.7")
        parsed.first().volume shouldBe 5854
        parsed.size shouldBe 17
    }

    companion object {
        const val FINAM_DATA = """SPFB.BR 60 20211228 080000 78.7000000 78.7400000 78.6400000 78.7000000 5854
SPFB.BR 60 20211228 090000 78.6900000 78.9600000 78.6300000 78.8400000 11454
SPFB.BR 60 20211228 100000 78.8500000 79.2000000 78.6900000 79.1600000 34783
SPFB.BR 60 20211228 110000 79.1600000 79.1600000 78.4700000 78.5500000 59737
SPFB.BR 60 20211228 120000 78.5300000 79.0800000 78.5200000 78.8700000 61524
SPFB.BR 60 20211228 130000 78.8600000 79.3600000 78.8300000 79.1200000 62817
SPFB.BR 60 20211228 140000 79.1100000 79.6900000 79.0000000 79.6900000 59514
SPFB.BR 60 20211228 150000 79.6900000 79.8800000 79.5200000 79.6600000 47237
SPFB.BR 60 20211228 160000 79.6500000 79.7900000 79.4900000 79.5700000 43742
SPFB.BR 60 20211228 170000 79.5700000 79.7700000 79.1300000 79.2100000 71159
SPFB.BR 60 20211228 180000 79.1800000 79.4100000 78.8500000 79.1800000 89934
SPFB.BR 60 20211228 190000 79.1700000 79.6600000 79.1300000 79.2100000 47841
SPFB.BR 60 20211228 200000 79.1000000 79.3400000 78.9100000 79.2100000 40201
SPFB.BR 60 20211228 210000 79.2100000 79.3400000 78.9000000 79.0400000 23393
SPFB.BR 60 20211228 220000 79.0300000 79.0500000 78.8500000 79.0000000 16428
SPFB.BR 60 20211228 230000 79.0100000 79.1900000 78.8200000 78.9500000 27737
SPFB.BR 60 20211229 000000 78.9400000 79.1400000 78.9300000 79.0800000 10035
"""
    }
}
