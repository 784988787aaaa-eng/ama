package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول تصنيفات العملاء والحبايب المخصصة
 * ملاحظة معمارية: هذا الكيان مستخدم بالكامل في نظام تصنيفات وتصفية شاشة "ديون الحبايب" (Habayeb Module)
 */
@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "tabType") val tabType: String,
    @ColumnInfo(name = "iconEmoji") val iconEmoji: String,
    @ColumnInfo(name = "displayOrder") val displayOrder: Int = 0,
    @ColumnInfo(name = "isSystemClosed") val isSystemClosed: Boolean = false
)
