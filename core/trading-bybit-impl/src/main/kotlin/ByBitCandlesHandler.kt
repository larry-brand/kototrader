package org.cryptolosers.bybit

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.cryptolosers.trading.model.Candle
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ByBitCandlesHandler {

    suspend fun getCandles(symbol: String, interval: String, limit: Int): List<Candle> {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })  // Настройка сериализации JSON
            }
        }

        try {
            // also can use https://api-testnet.bybit.com/
            val httpResponse = client.get("https://api.bybit.com/v5/market/kline") {
                parameter("symbol", symbol)
                parameter("interval", interval)
                parameter("limit", limit)
            }
            println(httpResponse.bodyAsText())
            val response: KlineResponse = httpResponse.body()

            if (response.retCode == 0) {
                return response.toCandles()
            } else {
                throw Exception("Error: ${response.retMsg}")
            }
        } catch (e: Exception) {
            println("Ошибка при запросе данных: ${e.message}")
            return emptyList()
        } finally {
            client.close() // Закрываем клиент
        }
    }

}

@Serializable
data class KlineResult(
    val symbol: String,
    val list: List<List<String>>
)

@Serializable
data class KlineResponse(
    val retCode: Int,
    val retMsg: String,
    val result: KlineResult
)

fun KlineResponse.toCandles(): List<Candle> {
    return result.list.map {
        Candle(
            timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(it[0].toLong()), ZoneId.of("Europe/Moscow")),//TODO: таймзона?
            openPrice = it[1].toBigDecimal(),
            highPrice = it[2].toBigDecimal(),
            lowPrice = it[3].toBigDecimal(),
            closePrice = it[4].toBigDecimal(),
            volume = it[5].toDouble().toLong()
        )
    }.reversed()
}