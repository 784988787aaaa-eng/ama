package com.example.ui.screens.habayeb.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class CurrencyConfigDirectedConversionTest {
    @Test
    fun conversionUsesTheDirectedRateByMultiplication() {
        val usdToYer = BigDecimal("250")
        assertEquals(
            BigDecimal("2500.0000"),
            CurrencyConfig.convertAmountBigDecimal(BigDecimal("10"), "ر.ي", "$", usdToYer)
        )
    }

    @Test
    fun invalidRateIsRejectedInsteadOfBecomingOne() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyConfig.convertAmountBigDecimal(BigDecimal("10"), "ر.ي", "$", BigDecimal.ZERO)
        }
    }
}
