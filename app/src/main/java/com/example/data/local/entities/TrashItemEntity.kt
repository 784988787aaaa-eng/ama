package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان سلة المحذوفات والتخزين الآمن للعناصر المسترجعة (Trash & Deleted Items Entity)
 *
 * التوثيق والحدود المعمارية:
 * 1. يحفظ هذا الجدول (`deleted_items`) حزم العناصر المحذوفة بتنسيق JSON المعياري للحفاظ على كل التفاصيل والعلاقات.
 * 2. `sourceSystem` و `originalTableName`: يحددان نوع ومصدر الكيان الأصلي (مثل HABAYEB, MAIN_LEDGER, HABAYEB_BUNDLE).
 * 3. `deletedAt`: طابع زمني دقيق للتحكم في فترة الاحتفاظ وسياسات التنظيف التلقائي للمحذوفات (Trash Auto-cleanup).
 */
@Entity(tableName = "deleted_items")
data class DeletedItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "sourceSystem") val sourceSystem: String,
    @ColumnInfo(name = "originalTableName") val originalTableName: String,
    @ColumnInfo(name = "jsonData") val jsonData: String,
    @ColumnInfo(name = "deletedAt") val deletedAt: Long = System.currentTimeMillis()
)
