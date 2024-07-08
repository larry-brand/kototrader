package org.cryptolosers.frontend.desktop

import org.cryptolosers.history.HistoryService
import org.cryptolosers.history.HistoryTickerId
import org.cryptolosers.history.Timeframe
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.StandardChartTheme
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.axis.ValueAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.CandlestickRenderer
import org.jfree.chart.renderer.xy.XYShapeRenderer
import org.jfree.data.time.Minute
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.data.xy.*
import java.awt.*
import java.awt.event.ItemEvent
import java.math.BigDecimal
import java.text.ParseException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.swing.*


class HighLowChartExample(title: String?) : JFrame(title) {
    private val historyService = HistoryService()
    private val topPanel: JPanel
    private val chartPanel: ChartPanel
    private var tickerName: String
    private var timeframe = Timeframe.DAY1
    private var startDate = LocalDateTime.now().minusYears(1)

    init {
        tickerName = historyService.tickers()[0].name
        chartPanel = ChartPanel(updateChart(tickerName))
        contentPane.add(chartPanel, BorderLayout.CENTER)

        topPanel = JPanel()
        topPanel.background = Color.WHITE
        topPanel.layout = FlowLayout(FlowLayout.LEFT)
        contentPane.add(topPanel, BorderLayout.NORTH)
        addSearchComponent(topPanel)
        addTimeframe(topPanel)
        addPeriod(topPanel)
    }

    private fun createDataset(tickerId: HistoryTickerId): OHLCDataset {
        val candles = historyService.readCandles(tickerId, timeframe,  startDate, LocalDateTime.now())

        val dataItems = mutableListOf<OHLCDataItem>()
        try {
            candles.forEach {
                dataItems.add(OHLCDataItem(
                    Date.from(it.timestamp.atZone(ZoneId.systemDefault()).toInstant()),
                    it.openPrice.toDouble(), it.highPrice.toDouble(), it.lowPrice.toDouble(), it.closePrice.toDouble(), it.volume.toDouble()
                ))
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        println("Candles: ${candles.size}")
        return DefaultOHLCDataset(tickerId.name, dataItems.toTypedArray())
    }

    fun createMyCandlestickChart(
        title: String?,
        timeAxisLabel: String?, valueAxisLabel: String?, dataset: OHLCDataset?,
        legend: Boolean
    ): JFreeChart {
        val timeAxis: ValueAxis = DateAxis(timeAxisLabel)
        val valueAxis = NumberAxis(valueAxisLabel)
        val plot = XYPlot(dataset, timeAxis, valueAxis, null)
        val renderer = CandlestickRenderer()
        renderer.autoWidthMethod = CandlestickRenderer.WIDTHMETHOD_SMALLEST
        renderer.drawVolume = false
        plot.setRenderer(0, renderer)
        val chart = JFreeChart(
            title, JFreeChart.DEFAULT_TITLE_FONT,
            plot, legend
        )
        plot.setDataset(0, dataset)


        val dealsRenderer = XYShapeRenderer()
        dealsRenderer.setSeriesPaint(0, Color.blue)
        plot.setRenderer(1, dealsRenderer)
        val candles = historyService.readCandles(HistoryTickerId(tickerName), timeframe,  startDate, LocalDateTime.now())
        val dealsDataset = TimeSeriesCollection()
        val dealsSeries = TimeSeries("deals")
        val dealsSeries2 = TimeSeries("deals2")
        try {
            candles.takeLast(30).forEach {
                dealsSeries.add(Minute(Date.from(it.timestamp.atZone(ZoneId.systemDefault()).toInstant())), it.highPrice+ BigDecimal(1000))
                dealsSeries2.add(Minute(Date.from(it.timestamp.atZone(ZoneId.systemDefault()).toInstant())), it.lowPrice- BigDecimal(2000))
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        dealsDataset.addSeries(dealsSeries)
        dealsDataset.addSeries(dealsSeries2)
        plot.setDataset(1, dealsDataset)

        StandardChartTheme("JFree").apply(chart)
        return chart
    }

    fun addSearchComponent(panel: Container) {
        val tickers = historyService.tickers().map { it.name }
        val combobox = JComboBox(tickers.toTypedArray())

        combobox.addItemListener {
            if (it.getStateChange() === ItemEvent.SELECTED) {
                val tickerNameSearch: String = it.getItem() as String
                tickerName = tickerNameSearch
                updateChart(tickerNameSearch)
            }
        }

        AutoCompleteDecorator.decorate(combobox)
        panel.add(JLabel("Ticker"))
        panel.add(combobox)
    }


    private fun addTimeframe(topPanel: JPanel) {
        val fifteenMin = JButton("15m")
        val oneHour = JButton("1h")
        val oneDay = JButton("1D")
        val buttonBackground: (JButton) -> Unit = {
            fifteenMin.background = Color.WHITE
            oneHour.background = Color.WHITE
            oneDay.background = Color.WHITE
            it.background = Color.LIGHT_GRAY
        }
        buttonBackground(oneDay)

        fifteenMin.addActionListener { timeframe = Timeframe.MIN15; updateChart(tickerName); buttonBackground(fifteenMin) }
        oneHour.addActionListener { timeframe = Timeframe.HOUR1; updateChart(tickerName); buttonBackground(oneHour) }
        oneDay.addActionListener { timeframe = Timeframe.DAY1; updateChart(tickerName); buttonBackground(oneDay) }

        topPanel.add(JLabel("      Timeframe "))
        topPanel.add(fifteenMin)
        topPanel.add(oneHour)
        topPanel.add(oneDay)
    }

    private fun addPeriod(topPanel: JPanel) {
        val all = JButton("All")
        val fiveYear = JButton("5Y")
        val oneYear = JButton("1Y")
        val oneMonth = JButton("1M")
        val buttonBackground: (JButton) -> Unit = {
            all.background = Color.WHITE
            fiveYear.background = Color.WHITE
            oneYear.background = Color.WHITE
            oneMonth.background = Color.WHITE
            it.background = Color.LIGHT_GRAY
        }
        buttonBackground(oneYear)

        all.addActionListener { startDate = LocalDateTime.now().minusYears(20); updateChart(tickerName); buttonBackground(all) } //TODO: fix exception
        fiveYear.addActionListener { startDate = LocalDateTime.now().minusYears(5); updateChart(tickerName); buttonBackground(fiveYear) }
        oneYear.addActionListener { startDate = LocalDateTime.now().minusYears(1); updateChart(tickerName); buttonBackground(oneYear) }
        oneMonth.addActionListener { startDate = LocalDateTime.now().minusMonths(1); updateChart(tickerName); buttonBackground(oneMonth) }

        topPanel.add(JLabel("      Period "))
        topPanel.add(all)
        topPanel.add(fiveYear)
        topPanel.add(oneYear)
        topPanel.add(oneMonth)
    }

    fun updateChart(tickerName: String): JFreeChart {
        historyService.downloadHistory(HistoryTickerId(tickerName), timeframe, startDate.toLocalDate(), LocalDate.now())
        // do something with object
        val dataset = createDataset(HistoryTickerId(tickerName))

        // Create chart
        val chart = createMyCandlestickChart(
            tickerName,
            "Date", "Price", dataset, false
        )
        val plot = chart.plot as XYPlot
        val rangeAxis = plot.rangeAxis as NumberAxis
        rangeAxis.autoRangeIncludesZero = false
        if (chartPanel != null) chartPanel.chart = chart
        return chart
    }

}

fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        val example = HighLowChartExample(
            "KotoTrader"
        )
        example.setSize(1600, 800)
        example.setLocationRelativeTo(null)
        example.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        example.isVisible = true
    }
}