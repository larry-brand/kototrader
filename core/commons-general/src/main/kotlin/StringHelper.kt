package org.cryptolosers.commons

import java.math.BigDecimal

fun BigDecimal.toStringWithSign(): String {
    return if (this <= BigDecimal.ZERO ) {
        toString()
    }
    else {
        "+" + toString()
    }
}