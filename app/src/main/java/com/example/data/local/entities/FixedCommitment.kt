package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * كيان الالتزامات والمصروفات المالية الثابتة (Fixed Financial Commitments Entity)
 *
 * التوثيق والحدود المعمارية:
 * 1. يمثل بنود الالتزامات الثابتة (مثل الإيجار، الفواتير، المستحقات الدورية) في جدول `fixed_commitments`.
 * 2. الحسابات المالية: `targetAmount` و `currentProgress` من نوع BigDecimal لضمان دقة نسب الإنجاز والمبالغ المتبقية.
 * 3. حقل `orderIndex`: يتحكم في الترتيب المخصص لعرض الالتزامات في واجهة المستخدم.
 */
@Entity(tableName = "fixed_commitments")
data class FixedCommitment(
    @PrimaryKey @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "targetAmount") val targetAmount: BigDecimal,
    @ColumnInfo(name = "currentProgress") val currentProgress: BigDecimal,
    @ColumnInfo(name = "orderIndex") val orderIndex: Int = 0
)
