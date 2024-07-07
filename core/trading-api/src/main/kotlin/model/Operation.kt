package org.cryptolosers.trading.model

import java.math.BigDecimal
import java.time.Instant

data class Position(val ticker: Ticker, var size: Long, var positionType: PositionType, var openPrice: BigDecimal? = null) {
}

enum class PositionType {
    LONG, SHORT
}

data class Wallet(var balance: BigDecimal, var equity: BigDecimal = balance, val margin: BigDecimal = BigDecimal.ZERO, val freeMargin: BigDecimal = BigDecimal.ZERO)

data class Operation(val id: String, val ticker: Ticker, val price: BigDecimal, val size: Long, val timestamp: Instant, val status: OperationStatus)

enum class OperationStatus {
    IN_PROGRESS, COMPLETED, CANCELLED
}