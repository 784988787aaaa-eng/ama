package com.example.ui.screens.habayeb.utils

import com.example.data.local.entities.HabayebTransaction
import com.example.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * اختبارات الحالات الطرفية لحاسبة تاريخ وأرصدة عملاء الحبايب (CustomerHistoryCalculator)
 *
 * التوثيق المعماري:
 * يختبر هذا الملف السلوك الحسابي للأرصدة المتعددة العملات، وحساب التسلسل الزمني،
 * والتعامل مع القوائم الفارغة، والقيم الصفرية دون أي انهيار أو أخطاء تقريب.
 */
class CustomerHistoryCalculatorTest {

    @Test
    fun testEmptyTransactionList() {
        val result = CustomerHistoryCalculator.calculate(
            allCustomerTxs = emptyList(),
            currencySymbol = "ر.ي",
            exchangeRatesJson = null
        )

        assertEquals(0.0, result.netDebt, 0.0001)
        assertTrue(result.runningBalances.isEmpty())
        assertTrue(result.txSequenceNumbers.isEmpty())
        assertEquals(1, result.currencyKeys.size)
        assertEquals("ر.ي", result.currencyKeys[0])
    }

    @Test
    fun testSingleCreditTransaction() {
        val tx = HabayebTransaction(
            id = "tx1",
            customerId = "cust1",
            type = TransactionType.CREDIT.value, // له
            amount = BigDecimal("500.00"),
            currency = "ر.ي",
            timestamp = 1000L
        )

        val result = CustomerHistoryCalculator.calculate(
            allCustomerTxs = listOf(tx),
            currencySymbol = "ر.ي",
            exchangeRatesJson = null
        )

        // له = رصيد موجب للمستخدم (عليه دين لنا)
        assertEquals(BigDecimal("500.00"), result.owedByThemBDMap["ر.ي"])
        assertEquals(BigDecimal("500.00"), result.runningBalances["tx1"])
        assertEquals(1, result.txSequenceNumbers["tx1"])
    }

    @Test
    fun testDebitAndCreditSequence() {
        val tx1 = HabayebTransaction(
            id = "tx1",
            customerId = "cust1",
            type = TransactionType.CREDIT.value, // له 1000
            amount = BigDecimal("1000.00"),
            currency = "ر.ي",
            timestamp = 1000L
        )
        val tx2 = HabayebTransaction(
            id = "tx2",
            customerId = "cust1",
            type = TransactionType.DEBIT.value, // عليه 400
            amount = BigDecimal("400.00"),
            currency = "ر.ي",
            timestamp = 2000L
        )

        val result = CustomerHistoryCalculator.calculate(
            allCustomerTxs = listOf(tx2, tx1), // غير مرتبة زمنياً للتحقق من الترتيب
            currencySymbol = "ر.ي",
            exchangeRatesJson = null
        )

        // التحقق من أن tx1 أخذت التسلسل 1، و tx2 أخذت التسلسل 2
        assertEquals(1, result.txSequenceNumbers["tx1"])
        assertEquals(2, result.txSequenceNumbers["tx2"])

        // الرصيد التراكمي: بعد الأولى 1000، بعد الثانية 600
        assertEquals(BigDecimal("1000.00"), result.runningBalances["tx1"])
        assertEquals(BigDecimal("600.00"), result.runningBalances["tx2"])
    }
}
