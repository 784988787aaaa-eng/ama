package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TransactionType

/**
 * كيان بيانات عملاء الحبايب في قاعدة بيانات Room (Habayeb Customer Entity)
 *
 * التوثيق والحدود المعمارية:
 * 1. يمثل هذا الكيان العقد المباشر مع جدول `habayeb_customers` في قاعدة البيانات المحلية.
 * 2. أسماء الأعمدة والأنواع والفهارس ثابتة تاريخياً لضمان التوافق التام مع ترقيات قاعدة البيانات (Room Migrations) ومنظومة النسخ الاحتياطي المشفر.
 * 3. حقل `initialType`: يحفظ نية المستخدم الصريحة في تصنيف العميل عند الإنشاء (له/عليه) ولا يتم تعديله تلقائياً مع تغير الأرصدة.
 */
@Entity(
    tableName = "habayeb_customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"]),
        Index(value = ["createdAt"]),
        Index(value = ["initialType"])
    ]
)
data class HabayebCustomer(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "initialType", defaultValue = "OWED_BY_THEM") val initialType: String = TransactionType.OWED_BY_THEM.value
)
