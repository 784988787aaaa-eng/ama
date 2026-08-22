package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CustomCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY displayOrder ASC")
    fun getAllCustomCategoriesFlow(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories ORDER BY displayOrder ASC")
    suspend fun getAllCustomCategoriesDirect(): List<CustomCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategory)

    @Update
    suspend fun updateCategories(categories: List<CustomCategory>)

    @Delete
    suspend fun deleteCategory(category: CustomCategory)

    @Query("DELETE FROM custom_categories")
    suspend fun clearAllCustomCategories()
}
