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

        val downloaded: ByteArray = api.download(HistoryTickerId("BR"), Timeframe.HOUR1, startDate, endDate)

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
        val downloaded: ByteArray = api.download(HistoryTickerId("BR"), Timeframe.HOUR1, startDate, endDate)

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
        const val FINAM_DATA = """SPFB.BR 60 20211228 080000 78.7 78.74 78.64 78.7 5854
SPFB.BR 60 20211228 090000 78.69 78.96 78.63 78.84 11454
SPFB.BR 60 20211228 100000 78.85 79.2 78.69 79.16 34783
SPFB.BR 60 20211228 110000 79.16 79.16 78.47 78.55 59737
SPFB.BR 60 20211228 120000 78.53 79.08 78.52 78.87 61524
SPFB.BR 60 20211228 130000 78.86 79.36 78.83 79.12 62817
SPFB.BR 60 20211228 140000 79.11 79.69 79 79.69 59514
SPFB.BR 60 20211228 150000 79.69 79.88 79.52 79.66 47237
SPFB.BR 60 20211228 160000 79.65 79.79 79.49 79.57 43742
SPFB.BR 60 20211228 170000 79.57 79.77 79.13 79.21 71159
SPFB.BR 60 20211228 180000 79.18 79.41 78.85 79.18 89934
SPFB.BR 60 20211228 190000 79.17 79.66 79.13 79.21 47841
SPFB.BR 60 20211228 200000 79.1 79.34 78.91 79.21 40201
SPFB.BR 60 20211228 210000 79.21 79.34 78.9 79.04 23393
SPFB.BR 60 20211228 220000 79.03 79.05 78.85 79 16428
SPFB.BR 60 20211228 230000 79.01 79.19 78.82 78.95 27737
SPFB.BR 60 20211229 000000 78.94 79.14 78.93 79.08 10035
"""
    }
}
