package com.example.ui.helper

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * مساعد العمليات الحسابية والتقريب المالي لحبايب (Habayeb Mathematical & Formatting Helper)
 *
 * المسؤوليات المعمارية:
 * 1. الدقة الحسابية الإلزامية: استخدام BigDecimal حصراً مع التقريب المصرفي (HALF_EVEN) لمنع تراكم أخطاء الفاصلة العائمة.
 * 2. أمان الخيوط: استخدام ThreadLocal لـ NumberFormat لتفادي تسريب الذاكرة ومشاكل التزامن عند تعدد خيوط العرض.
 * 3. التنسيق الذكي: إزالة الأصفار الزائدة على يمين الفاصلة مع الحفاظ على وضوح وقراءة الأرقام الكبيرة.
 */
object HabayebMathHelper {
    private val numberFormatThreadLocal = ThreadLocal.withInitial {
        NumberFormat.getNumberInstance(Locale.US)
    }

    fun toBigDecimal(value: Double): BigDecimal {
        return if (value.isNaN() || value.isInfinite()) {
            BigDecimal.ZERO
        } else {
            try {
                BigDecimal.valueOf(value)
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
        }
    }

    fun toBigDecimal(value: String): BigDecimal {
        return try {
            val clean = value.trim()
            if (clean.isBlank() || clean.equals("null", ignoreCase = true)) BigDecimal.ZERO else BigDecimal(clean)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun add(a: BigDecimal, b: BigDecimal): BigDecimal = a.add(b)
    fun subtract(a: BigDecimal, b: BigDecimal): BigDecimal = a.subtract(b)
    fun multiply(a: BigDecimal, b: BigDecimal): BigDecimal = a.multiply(b)
    fun divide(a: BigDecimal, b: BigDecimal): BigDecimal {
        if (b.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return a.divide(b, 10, RoundingMode.HALF_EVEN)
    }

    // دالة التنسيق الذكي لجمالية الواجهات والدقة المطلقة
    fun formatSmart(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val rounded = value.setScale(2, RoundingMode.HALF_EVEN)
        val stripped = rounded.stripTrailingZeros()
        val formatter = numberFormatThreadLocal.get()
        if (stripped.scale() <= 0) {
            formatter.minimumFractionDigits = 0
            formatter.maximumFractionDigits = 0
        } else {
            formatter.minimumFractionDigits = 0
            formatter.maximumFractionDigits = 2
        }
        return formatter.format(stripped)
    }

    fun formatRate(value: Double): String {
        if (value <= 0.0) return "0"
        return try {
            val bd = BigDecimal(value.toString())
                .setScale(2, RoundingMode.HALF_EVEN)
                .stripTrailingZeros()
            bd.toPlainString()
        } catch (e: Exception) {
            value.toString()
        }
    }

    fun formatRate(value: BigDecimal): String {
        return try {
            value.setScale(2, RoundingMode.HALF_EVEN)
                .stripTrailingZeros()
                .toPlainString()
        } catch (e: Exception) {
            value.toString()
        }
    }
}


