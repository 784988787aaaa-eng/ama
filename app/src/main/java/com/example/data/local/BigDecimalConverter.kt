package com.example.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal

class BigDecimalConverter {
    @TypeConverter
    fun fromString(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return null
        return try {
            val cleaned = cleanNumberString(value)
            if (cleaned.isEmpty()) BigDecimal.ZERO else BigDecimal(cleaned)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    @TypeConverter
    fun toString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun fromDouble(value: Double?): BigDecimal? = value?.let {
        runCatching { BigDecimal.valueOf(it) }.getOrDefault(BigDecimal.ZERO)
    }

    @TypeConverter
    fun toDouble(value: BigDecimal?): Double? = value?.let {
        runCatching { it.toDouble() }.getOrDefault(0.0)
    }

    private fun cleanNumberString(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        val len = trimmed.length
        val sb = StringBuilder(len)
        var seenDot = false

        for (i in 0 until len) {
            val ch = trimmed[i]
            when {
                ch in '0'..'9' -> sb.append(ch)
                ch in '٠'..'٩' -> sb.append((ch - '٠' + '0'.code).toChar())
                ch in '۰'..'۹' -> sb.append((ch - '۰' + '0'.code).toChar())
                ch == '.' || ch == ',' || ch == '٫' -> {
                    if (!seenDot) {
                        sb.append('.')
                        seenDot = true
                    }
                }
                ch == '-' && sb.isEmpty() -> sb.append('-')
            }
        }
        val result = sb.toString()
        if (result == "-" || result == "." || result == "-.") return ""
        return result
    }
}
