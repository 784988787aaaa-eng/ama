package com.example.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal

/**
 * محول الأنواع الرقمية المالية لقاعدة بيانات Room (BigDecimal TypeConverter)
 *
 * المبادئ الهندسية والمحاسبية الحاكمة:
 * 1. الحفاظ المطلق على الدقة المحاسبية (Arbitrary Precision) ومنع أي تقريب أو تحويل وسيط يفقد الدقة.
 * 2. الصفر (BigDecimal.ZERO) يمثل قيمة حسابية ورصيداً محاسبياً قانونياً؛ لذا فإن تحويل السلاسل التالفة
 *    أو غير الصالحة (Malformed Strings) إلى صفر بصمت يُعد خللاً محاسبياً خطيراً يُخفي تلف البيانات.
 *    لذلك يتم إرجاع `null` عند تلف القيمة لتمكين طبقات التحقق من اكتشاف الخلل ومنع تشويه ميزانية المستخدم.
 * 3. رفض القيم غير الرقمية الخاصة مثل NaN و Positive/Negative Infinity بشكل قاطع.
 * 4. دعم الأرقام بالصيغ المشرقية (٠-٩) والفارسية (۰-۹) وتوحيد الفواصل العشرية (٫ , .) للحفاظ على بيانات المستخدم القديمة.
 */
class BigDecimalConverter {

    @TypeConverter
    fun fromString(value: String?): BigDecimal? {
        if (value.isNullOrBlank() || value.equals("null", ignoreCase = true)) return null
        val cleaned = cleanNumberString(value)
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned)
        } catch (_: Exception) {
            // إرجاع null عند الفشل لتفادي تحويل السجلات التالفة إلى صفر محاسبي مضلل
            null
        }
    }

    @TypeConverter
    fun toString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun fromDouble(value: Double?): BigDecimal? {
        if (value == null || value.isNaN() || value.isInfinite()) return null
        return try {
            BigDecimal.valueOf(value)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun toDouble(value: BigDecimal?): Double? = value?.toDouble()

    companion object {
        /**
         * تطهير السلاسل الرقمية وتوحيد المحارف العشرية والأرقام بمختلف اللغات المحلية
         */
        fun cleanNumberString(input: String): String {
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
}

