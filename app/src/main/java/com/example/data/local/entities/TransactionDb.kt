package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * كيان قيود معاملات دفتر اليومية المالي العام (Main Ledger Transaction Entity)
 *
 * التوثيق والحدود المعمارية:
 * 1. يمثل هذا الكيان سجلات المصروفات والإيرادات اليومية في جدول `transactions`.
 * 2. الحساب المالي الدقيق: حقل `amount` يستخدم BigDecimal حصراً لضمان سلامة المجاميع المحاسبية.
 * 3. الفهارس المركبة (type + timestamp, category + timestamp): مصممة لتسريع استعلامات التقارير والرسوم البيانية وتصفية الفئات.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        Index(value = ["type", "timestamp"]),
        Index(value = ["category", "timestamp"])
    ]
)
data class TransactionDb(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "amount") val amount: BigDecimal,
    @ColumnInfo(name = "description") val description: String
)
