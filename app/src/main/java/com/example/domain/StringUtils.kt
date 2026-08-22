package com.example.domain

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.data.local.entities.DatabaseDefaults
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Shared utility functions for text normalization and Arabic character mapping.
 */
object StringUtils {
    private val PHONE_CLEANUP_REGEX = Regex("[^0-9+]")

    @JvmStatic
    fun normalizeArabic(text: String): String {
        if (text.isEmpty()) return text
        val trimmed = text.trim()
        val len = trimmed.length
        val sb = StringBuilder(len)
        for (i in 0 until len) {
            when (val char = trimmed[i]) {
                '\u0622', '\u0623', '\u0625', '\u0671' -> sb.append('ا')
                '\u0629' -> sb.append('ه')
                '\u0649' -> sb.append('ي')
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652', '\u0653', '\u0654', '\u0655', '\u0670' -> {}
                else -> sb.append(char)
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun normalizeArabic(text: String, context: Context?): String = normalizeArabic(text)

    @JvmStatic
    fun getContactDetails(context: Context, contactUri: Uri): Pair<String, String>? {
        var name = ""
        var phone = ""
        try {
            val cr = context.contentResolver
            cr.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                    
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    if (idIndex >= 0) {
                        val contactId = cursor.getString(idIndex)
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val hasPhone = if (hasPhoneIndex >= 0) cursor.getString(hasPhoneIndex) else null
                        
                        if (hasPhone == "1") {
                            cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numberIndex >= 0) {
                                        phone = phoneCursor.getString(numberIndex) ?: ""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StringUtils", "Error fetching contact: ${e.message}")
        }

        val cleanedPhone = runCatching { phone.replace(PHONE_CLEANUP_REGEX, "") }.getOrDefault("")
        if (name.isNotEmpty()) {
            return Pair(name, cleanedPhone)
        }
        return null
    }

    @JvmStatic
    fun String.toEnglishDigits(): String = toWesternDigits()

    @JvmStatic
    fun String.toWesternDigits(): String {
        val len = length
        var hasArabicDigit = false
        for (i in 0 until len) {
            val c = this[i]
            if (c in '٠'..'٩' || c in '۰'..'۹') {
                hasArabicDigit = true
                break
            }
        }
        if (!hasArabicDigit) return this

        val chars = CharArray(len)
        for (i in 0 until len) {
            val c = this[i]
            chars[i] = when (c) {
                in '٠'..'٩' -> (c - '٠' + '0'.code).toChar()
                in '۰'..'۹' -> (c - '۰' + '0'.code).toChar()
                else -> c
            }
        }
        return String(chars)
    }

    /**
     * Normalizes Arabic and Farsi digits to Western digits, and replaces commas with dots.
     */
    @JvmStatic
    fun normalizeDigits(input: String): String {
        if (input.isEmpty()) return input
        val len = input.length
        val sb = java.lang.StringBuilder(len)
        for (i in 0 until len) {
            val ch = input[i]
            val replacement = when (ch) {
                ',' -> '.'
                in '٠'..'٩' -> (ch - '٠' + '0'.code).toChar()
                in '۰'..'۹' -> (ch - '۰' + '0'.code).toChar()
                else -> ch
            }
            sb.append(replacement)
        }
        return sb.toString()
    }
}

/**
 * Shared formatting utilities for displaying monetary values/currency.
 */
object FormatUtils {
    private val DECIMAL_SYMBOLS = DecimalFormatSymbols(Locale.ENGLISH)

    private val formatterInteger = ThreadLocal.withInitial { DecimalFormat("#,##0", DECIMAL_SYMBOLS) }
    private val formatterDecimal = ThreadLocal.withInitial { DecimalFormat("#,##0.##", DECIMAL_SYMBOLS) }

    private fun formatNumberInternal(value: BigDecimal): String {
        return runCatching {
            val rounded = value.setScale(2, RoundingMode.HALF_UP)
            val hasFraction = rounded.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0
            val formatter = if (hasFraction) formatterDecimal.get() else formatterInteger.get()
            formatter.format(rounded)
        }.getOrDefault(value.toPlainString())
    }

    @JvmStatic
    fun formatCurrency(amount: BigDecimal, symbol: String = "", context: Context? = null): String {
        val finalSymbol = symbol.ifEmpty { context?.getString(com.example.R.string.currency_yer) ?: DatabaseDefaults.DEFAULT_CURRENCY_SYMBOL }
        return formatBigDecimal(amount, finalSymbol)
    }

    @JvmStatic
    fun formatDoubleCurrency(amount: Double, symbol: String = "", context: Context? = null): String {
        val finalSymbol = symbol.ifEmpty { context?.getString(com.example.R.string.currency_yer) ?: DatabaseDefaults.DEFAULT_CURRENCY_SYMBOL }
        return formatDouble(amount, finalSymbol)
    }

    @JvmStatic
    fun formatDouble(value: Double, symbol: String = ""): String {
        return runCatching {
            formatBigDecimal(BigDecimal.valueOf(value), symbol)
        }.getOrElse {
            val formatted = value.toString()
            if (symbol.isNotEmpty()) "$formatted $symbol" else formatted
        }
    }

    @JvmStatic
    fun formatBigDecimal(value: BigDecimal, symbol: String = ""): String {
        val formatted = formatNumberInternal(value)
        return if (symbol.isNotEmpty()) "$formatted $symbol" else formatted
    }
}