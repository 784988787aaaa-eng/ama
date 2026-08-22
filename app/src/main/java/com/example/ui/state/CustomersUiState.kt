package com.example.ui.state

import androidx.compose.runtime.Immutable
import com.example.data.local.entities.HabayebCustomer
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * نماذج حالة العرض والتجميع المحاسبي لعملاء الحبايب (Habayeb Presentation & Aggregate State Models)
 *
 * التوثيق المعماري وفصل المسؤوليات:
 * 1. `CustomerUiState`: نموذج عرض غير قابل للتغيير (@Immutable) يغذي بطاقات العملاء في واجهة المستخدم،
 *    ويفصل بين المبالغ المالية الدقيقة (BigDecimal) وبين القيم السريعة لتحديث الرسوم.
 * 2. `CustomerBalancesPojo` و `CustomerCurrencyBalancePojo`: كائنات وسيطة خفيفة الوزن لاستقبال نتائج استعلامات
 *    Room المجمعة (Aggregations) مباشرة من قاعدة البيانات دون تحميل جميع سجلات المعاملات في الذاكرة.
 */
@Immutable
data class CustomerUiState(
    val id: String,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = 0L,
    val totalTransactions: Int = 0,
    val netDebt: Double = 0.0,
    val displayNetDebt: Double = 0.0,
    val displayCurrencySymbol: String = "",
    val lastTransactionTimestamp: Long = 0L,
    val isStable: Boolean = true,
    val originalCustomer: HabayebCustomer,
    val foreignDebts: Map<String, BigDecimal> = emptyMap(),
    val defaultCurrencyTotal: BigDecimal = BigDecimal.ZERO,
    val normalizedName: String = "",
    val defaultCurrencyTotalAbs: BigDecimal = BigDecimal.ZERO
) {
    val isClosed: Boolean get() = defaultCurrencyTotal.setScale(4, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ZERO) == 0 && foreignDebts.isEmpty()
}

@Immutable
data class CustomersUiState(
    val customers: List<CustomerUiState> = emptyList(),
    val totalOwedByThem: BigDecimal = BigDecimal.ZERO,
    val totalOwedToThem: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false
)

@Immutable
data class CustomerBalancesPojo(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerNotes: String,
    val customerCreatedAt: Long,
    val customerInitialType: String,
    val totalTransactions: Int,
    val netDebt: Double,
    val lastTxTime: Long,
    val hasForeign: Int
) {
    fun toEntity() = HabayebCustomer(
        id = customerId,
        name = customerName,
        phone = customerPhone,
        notes = customerNotes,
        createdAt = customerCreatedAt,
        initialType = customerInitialType
    )
}

@Immutable
data class CustomerCurrencyBalancePojo(
    val customerId: String,
    val currencyCode: String,
    val netBalance: BigDecimal
)

