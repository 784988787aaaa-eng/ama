package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * الالتزامات المالية الثابتة والمحددة
 */
@Entity(tableName = "fixed_commitments")
data class FixedCommitment(
    @PrimaryKey @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "targetAmount") val targetAmount: BigDecimal,
    @ColumnInfo(name = "currentProgress") val currentProgress: BigDecimal,
    @ColumnInfo(name = "orderIndex") val orderIndex: Int = 0
)
