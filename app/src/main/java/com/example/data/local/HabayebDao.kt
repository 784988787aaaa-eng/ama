package com.example.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import kotlinx.coroutines.flow.Flow

/**
 * واجهة استعلامات ووصول بيانات عملاء ومعاملات الحبايب (Habayeb Dao)
 *
 * المسؤوليات المعمارية الحاكمة:
 * 1. حصر المسؤولية في عمليات قراءة وكتابة البيانات فقط دون إجراء حسابات مالية أو تقريب أرقام.
 * 2. تأمين العمليات المركبة (مثل حذف العميل ومعاملاته أو إدراج عميل برصيد افتتاحي) داخل @Transaction لضمان الذرية (Atomicity) ومنع الحالات الجزئية للبيانات.
 * 3. دعم التدفقات الحية (Flow) لواجهات العرض، مع دوال معلقة مباشرة (Direct/Suspend) لعمليات المعالجة الخلفية وعمال المزامنة (Workers).
 */
@Dao
interface HabayebDao {

    // ==========================================
    // Customer Queries
    // ==========================================

    @Query("SELECT * FROM habayeb_customers ORDER BY createdAt DESC")
    fun getAllCustomersFlow(): Flow<List<HabayebCustomer>>

    @Query("SELECT * FROM habayeb_customers")
    suspend fun getAllCustomersDirect(): List<HabayebCustomer>

    @Query("SELECT * FROM habayeb_customers WHERE id = :id")
    suspend fun getCustomerByIdDirect(id: String): HabayebCustomer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: HabayebCustomer)

    @Update
    suspend fun updateCustomer(customer: HabayebCustomer)

    @Query("UPDATE habayeb_customers SET name = :newName WHERE id = :id")
    suspend fun updateCustomerName(id: String, newName: String)

    @Delete
    suspend fun deleteCustomer(customer: HabayebCustomer)

    @Query("DELETE FROM habayeb_customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("DELETE FROM habayeb_customers")
    suspend fun clearAllCustomers()


    // ==========================================
    // Transaction Queries
    // ==========================================

    @Query("SELECT * FROM habayeb_transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<HabayebTransaction>>

    @Query("SELECT * FROM habayeb_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<HabayebTransaction>>

    @Query("SELECT * FROM habayeb_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getTransactionsForCustomerDirect(customerId: String): List<HabayebTransaction>

    @Query("SELECT * FROM habayeb_transactions")
    suspend fun getAllTransactionsDirect(): List<HabayebTransaction>

    @Query("SELECT * FROM habayeb_transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): HabayebTransaction?

    @Query("SELECT * FROM habayeb_transactions WHERE is_foreign = 1")
    fun getForeignTransactionsFlow(): Flow<List<HabayebTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: HabayebTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: HabayebTransaction)

    @Query("DELETE FROM habayeb_transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsByCustomer(customerId: String)

    @Query("DELETE FROM habayeb_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM habayeb_transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT COUNT(*) FROM habayeb_transactions")
    fun getHabayebTransactionsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habayeb_transactions")
    suspend fun getHabayebTransactionsCountDirect(): Int


    // ==========================================
    // Pagination & Filtered Queries
    // ==========================================

    @Query("SELECT * FROM habayeb_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsPagingSourceForCustomer(customerId: String): PagingSource<Int, HabayebTransaction>

    @Query("SELECT * FROM habayeb_transactions WHERE customerId = :customerId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsForCustomerPaged(customerId: String, limit: Int, offset: Int): List<HabayebTransaction>

    @Query("SELECT * FROM habayeb_transactions WHERE customerId = :customerId ORDER BY timestamp DESC LIMIT :limit")
    fun getTransactionsForCustomerWithLimitFlow(customerId: String, limit: Int): Flow<List<HabayebTransaction>>


    // ==========================================
    // Adaptation & Combined Operations
    // ==========================================

    @Query("""
        UPDATE habayeb_transactions 
        SET type = CASE type 
            WHEN 'OWED_TO_THEM' THEN 'OWED_BY_THEM' 
            WHEN 'PAYMENT_TO_THEM' THEN 'PAYMENT_BY_THEM' 
            ELSE type 
        END 
        WHERE customerId = :customerId AND type IN ('OWED_TO_THEM', 'PAYMENT_TO_THEM')
    """)
    suspend fun adaptTransactionsToOwedByThem(customerId: String)

    @Query("""
        UPDATE habayeb_transactions 
        SET type = CASE type 
            WHEN 'OWED_BY_THEM' THEN 'OWED_TO_THEM' 
            WHEN 'PAYMENT_BY_THEM' THEN 'PAYMENT_TO_THEM' 
            ELSE type 
        END 
        WHERE customerId = :customerId AND type IN ('OWED_BY_THEM', 'PAYMENT_BY_THEM')
    """)
    suspend fun adaptTransactionsToOwedToThem(customerId: String)

    @Transaction
    suspend fun insertCustomerWithOpeningTransaction(customer: HabayebCustomer, transaction: HabayebTransaction?) {
        insertCustomer(customer)
        if (transaction != null) {
            insertTransaction(transaction)
        }
    }

    @Transaction
    suspend fun insertTransactionsBatch(transactions: List<HabayebTransaction>) {
        for (tx in transactions) {
            insertTransaction(tx)
        }
    }

    @Transaction
    suspend fun deleteCustomerAndTransactions(customerId: String) {
        deleteTransactionsByCustomer(customerId)
        deleteCustomerById(customerId)
    }

    @Transaction
    suspend fun deleteCustomersAndTransactionsBatch(customerIds: List<String>) {
        for (id in customerIds) {
            deleteTransactionsByCustomer(id)
            deleteCustomerById(id)
        }
    }
}
