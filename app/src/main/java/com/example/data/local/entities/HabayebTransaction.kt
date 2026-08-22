package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ui.viewmodel.FinanceConstants
import java.math.BigDecimal

/**
 * كيان قيود معاملات عملاء الحبايب في قاعدة بيانات Room (Habayeb Transaction Entity)
 *
 * التوثيق والحدود المعمارية:
 * 1. الدقة المالية الإلزامية: جميع الحقول المالية (amount, foreignAmount, exchangeRate, equivalentAmount)
 *    تستخدم نوع BigDecimal حصراً لتفادي أي فقدان في دقة الكسور أو تراكم أخطاء الفاصلة العائمة.
 * 2. دعم العملات المتعددة والصرف: تخزن المعاملة قيمة العملة الأجنبية، وسعر الصرف، والقيمة المكافئة بالعملة الأساسية.
 * 3. مفتاح الربط الرئيسي (linkedMainTxId): يربط قيد الحبايب بالمعاملة النظيرة في دفتر اليومية العام لضمان التزامن المالي.
 * 4. التكامل المرجعي (ForeignKey): مرتبط بحذف وتحديث متتالي (CASCADE) مع كيان العميل HabayebCustomer.
 */
@Entity(
    tableName = "habayeb_transactions",
    foreignKeys = [
        ForeignKey(
            entity = HabayebCustomer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["currency_code"]),
        Index(value = ["customerId", "timestamp"]),
        Index(value = ["customerId", "type"]),
        Index(value = ["linkedMainTxId"])
    ]
)
data class HabayebTransaction(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "customerId") val customerId: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "amount") val amount: BigDecimal,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "linkedMainTxId") val linkedMainTxId: String? = null,
    @ColumnInfo(name = "is_foreign") val isForeign: Boolean = false,
    @ColumnInfo(name = "currency_code") val currencyCode: String = FinanceConstants.DEFAULT_CURRENCY_CODE,
    @ColumnInfo(name = "foreign_amount") val foreignAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "exchange_rate") val exchangeRate: BigDecimal = BigDecimal.ONE,
    @ColumnInfo(name = "is_rate_calculated") val isRateCalculated: Boolean = false,
    @ColumnInfo(name = "equivalent_amount") val equivalentAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "base_currency_code") val baseCurrencyCode: String = FinanceConstants.DEFAULT_CURRENCY_CODE
) {
    // Clean Architectural Mapping Helpers (ignored by Room)
    val originalAmount: BigDecimal get() = foreignAmount
    val currencySymbol: String get() = currencyCode
    val isExchanged: Boolean get() = isRateCalculated
    val targetCurrencySymbol: String get() = baseCurrencyCode
    val exchangedAmount: BigDecimal get() = equivalentAmount
}
