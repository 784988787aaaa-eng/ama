package com.example.data.local

import androidx.room.Dao

/**
 * واجهة استعلامات دفتر الميزان اليومي والتصنيفات المخصصة
 * تعتمد التفتيت المعماري عبر توريث واجهات الاستعلام المستقلة (TransactionDao و CustomCategoryDao)
 */
@Dao
interface LedgerDao : TransactionDao, CustomCategoryDao


