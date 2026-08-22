package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BigDecimalConverterTest {
    private val converter = BigDecimalConverter()

    @Test
    fun validValuesRoundTripWithoutFloatingPoint() {
        val value = BigDecimal("12345678901234567890.123456789")
        val stored = converter.toString(value)
        assertEquals(value, converter.fromString(stored))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedStoredValueIsNotSilentlyConvertedToZero() {
        converter.fromString("not-a-number")
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonFiniteDoubleIsRejected() {
        converter.fromDouble(Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun hugeBigDecimalIsNotSilentlyConvertedToInfinity() {
        converter.toDouble(BigDecimal("1E+10000"))
    }
}
