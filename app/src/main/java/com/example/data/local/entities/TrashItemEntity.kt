package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_items")
data class DeletedItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "sourceSystem") val sourceSystem: String,
    @ColumnInfo(name = "originalTableName") val originalTableName: String,
    @ColumnInfo(name = "jsonData") val jsonData: String,
    @ColumnInfo(name = "deletedAt") val deletedAt: Long = System.currentTimeMillis()
)
