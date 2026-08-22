package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * اختبارات التحقق من دقة وسلوك محول BigDecimalConverter
 */
class BigDecimalConverterTest {

    private val converter = BigDecimalConverter()

    @Test
    fun testFromString_integerNumber() {
        val result = converter.fromString("1500")
        assertEquals(BigDecimal("1500"), result)

        val arabicResult = converter.fromString("١٥٠٠")
        assertEquals(BigDecimal("1500"), result)
    }

    @Test
    fun testFromString_decimalNumber() {
        val result = converter.fromString("123.45")
        assertEquals(BigDecimal("123.45"), result)

        val arabicResult = converter.fromString("١٢٣٫٤٥")
        assertEquals(BigDecimal("123.45"), arabicResult)
    }

    @Test
    fun testFromString_emptyAndBlank() {
        assertNull(converter.fromString(""))
        assertNull(converter.fromString("   "))
    }

    @Test
    fun testFromString_null() {
        assertNull(converter.fromString(null))
        assertNull(converter.fromString("null"))
        assertNull(converter.fromString("NULL"))
    }

    @Test
    fun testFromString_malformedValue() {
        // السلاسل التالفة يجب ألا تتحول بصمت إلى صفر بل ترجع null
        assertNull(converter.fromString("invalid_text"))
        assertNull(converter.fromString("!@#$%^"))
        assertNull(converter.fromString("-"))
        assertNull(converter.fromString("."))
    }

    @Test
    fun testFromDouble_validAndInvalidDoubles() {
        val valid = converter.fromDouble(45.5)
        assertEquals(BigDecimal.valueOf(45.5), valid)

        // رفض NaN و Infinity
        assertNull(converter.fromDouble(Double.NaN))
        assertNull(converter.fromDouble(Double.POSITIVE_INFINITY))
        assertNull(converter.fromDouble(Double.NEGATIVE_INFINITY))
        assertNull(converter.fromDouble(null))
    }

    @Test
    fun testToString_conversion() {
        assertEquals("1500.75", converter.toString(BigDecimal("1500.75")))
        assertNull(converter.toString(null))
    }
}
