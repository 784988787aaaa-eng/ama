package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.FixedCommitment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM fixed_commitments ORDER BY orderIndex ASC")
    fun getAllCommitmentsFlow(): Flow<List<FixedCommitment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: FixedCommitment)

    @Update
    suspend fun updateCommitments(commitments: List<FixedCommitment>)

    @Query("DELETE FROM fixed_commitments WHERE name = :name")
    suspend fun deleteCommitment(name: String)

    @Query("DELETE FROM fixed_commitments")
    suspend fun clearAllCommitments()
}
