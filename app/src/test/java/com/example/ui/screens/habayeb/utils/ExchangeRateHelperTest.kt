package com.example.ui.screens.habayeb.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ExchangeRateHelperTest {
    @Test
    fun setRateStoresDirectedPairAndMathematicalInverse() {
        val json = ExchangeRateHelper.setRate("{}", "ر.ي", "$", BigDecimal("250"))

        assertEquals(BigDecimal("250.00000000"), ExchangeRateHelper.getRateBigDecimal(json, "ر.ي", "$"))
        assertEquals(BigDecimal("0.00400000"), ExchangeRateHelper.getRateBigDecimal(json, "$", "ر.ي"))
        assertTrue(json.contains("__schemaVersion"))
    }

    @Test
    fun directedConversionRatesAreIndependentOfCurrencyRank() {
        val json = ExchangeRateHelper.setRate("{}", "ر.ي", "$", BigDecimal("250"))
        val usdToYer = ExchangeRateHelper.getRateBigDecimal(json, "ر.ي", "$")
        val yerToUsd = ExchangeRateHelper.getRateBigDecimal(json, "$", "ر.ي")

        assertEquals(BigDecimal("250.00000000"), usdToYer)
        assertEquals(BigDecimal("0.00400000"), yerToUsd)
        assertEquals(BigDecimal("2500.0000"), CurrencyConfig.convertAmountBigDecimal(BigDecimal("10"), "ر.ي", "$", usdToYer))
        assertEquals(BigDecimal("10.0000"), CurrencyConfig.convertAmountBigDecimal(BigDecimal("2500"), "$", "ر.ي", yerToUsd))
    }

    @Test
    fun legacySymmetricJsonMigratesToDirectedSchema() {
        val legacy = """
            {"ر.ي":{"$":"250"},"$":{"ر.ي":"250"}}
        """.trimIndent()

        val migrated = ExchangeRateHelper.migrateRates(legacy, "ر.ي", "ر.ي")

        assertEquals(BigDecimal("250.00000000"), ExchangeRateHelper.getRateBigDecimal(migrated, "ر.ي", "$"))
        assertEquals(BigDecimal("0.00400000"), ExchangeRateHelper.getRateBigDecimal(migrated, "$", "ر.ي"))
        assertTrue(migrated.contains("__schemaVersion"))
    }

    @Test
    fun missingOrInvalidRatesDoNotBecomeOne() {
        assertEquals(BigDecimal("0.00000000"), ExchangeRateHelper.getRateBigDecimal("{}", "ر.ي", "$"))
        assertEquals(BigDecimal("1.00000000"), ExchangeRateHelper.getRateBigDecimal("{}", "ر.ي", "ر.ي"))
    }

    @Test
    fun migrateRatesIsIdempotentForDirectedData() {
        val once = ExchangeRateHelper.migrateRates("{\"ر.ي\":{\"$\":\"250\"},\"$\":{\"ر.ي\":\"0.004\"}}", "ر.ي", "ر.ي")
        val twice = ExchangeRateHelper.migrateRates(once, "ر.ي", "ر.ي")

        assertEquals(ExchangeRateHelper.getRateBigDecimal(once, "ر.ي", "$"), ExchangeRateHelper.getRateBigDecimal(twice, "ر.ي", "$"))
        assertEquals(ExchangeRateHelper.getRateBigDecimal(once, "$", "ر.ي"), ExchangeRateHelper.getRateBigDecimal(twice, "$", "ر.ي"))
    }
}
