package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.TransactionDb
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * واجهة استعلامات قيود دفتر اليومية المالي العام (Main Ledger Transaction Dao)
 *
 * المسؤوليات المعمارية:
 * 1. استرجاع وتدفق قيود الدفتر اليومي مرتبة تنازلياً حسب الطابع الزمني للاستفادة من الفهارس المخصصة.
 * 2. توفير مجاميع السيولة النقدية والمصروفات الدورية بدقة عبر Room TypeConverter (BigDecimal) دون فقدان للكسور.
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionDb>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedTransactionsDirect(limit: Int, offset: Int): List<TransactionDb>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN CAST(amount AS REAL) ELSE 0.0 END), 0.0) - 
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN CAST(amount AS REAL) ELSE 0.0 END), 0.0) 
        FROM transactions
    """)
    fun getTotalCashFlow(): Flow<BigDecimal>

    @Query("SELECT COALESCE(SUM(CAST(amount AS REAL)), 0.0) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    suspend fun getExpensesSumForPeriod(startTimestamp: Long, endTimestamp: Long): BigDecimal

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionsCountDirect(): Int

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionDb?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionDb)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionDb)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}
