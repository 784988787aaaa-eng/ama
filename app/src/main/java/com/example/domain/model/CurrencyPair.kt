package com.example.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Type-Safe Value Object representing a currency pair and its conversion exchange rate.
 * Encapsulates rate safety and rounding constraints.
 */
data class CurrencyPair(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: BigDecimal = BigDecimal.ONE
) {
    val isValid: Boolean
        get() = rate.compareTo(BigDecimal.ZERO) > 0

    val safeRate: BigDecimal
        get() = if (rate.compareTo(BigDecimal.ZERO) > 0) {
            rate.setScale(4, RoundingMode.HALF_EVEN)
        } else {
            BigDecimal.ONE.setScale(4, RoundingMode.HALF_EVEN)
        }

    val isSelfPair: Boolean
        get() = baseCurrency.trim().equals(targetCurrency.trim(), ignoreCase = true)
}
