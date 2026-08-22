package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.DeletedItemEntity
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb
import com.example.ui.screens.trash.utils.TrashItemParser
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Dao
abstract class TrashDao {

    companion object {
        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_HABAYEB_TRANSACTIONS = "habayeb_transactions"
        const val TABLE_FIXED_COMMITMENTS = "fixed_commitments"
        const val TABLE_HABAYEB_CUSTOMERS = "habayeb_customers"
        const val BUNDLE_HABAYEB = "habayeb_bundle"
        const val BUNDLE_DAR = "dar_bundle"
    }

    @Query("SELECT * FROM deleted_items ORDER BY deletedAt DESC")
    abstract fun getAllDeletedItemsFlow(): Flow<List<DeletedItemEntity>>

    @Query("SELECT * FROM deleted_items ORDER BY deletedAt DESC")
    abstract suspend fun getAllDeletedItemsDirect(): List<DeletedItemEntity>

    @Query("SELECT * FROM deleted_items WHERE id = :id LIMIT 1")
    abstract suspend fun getDeletedItemByIdDirect(id: String): DeletedItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDeletedItem(item: DeletedItemEntity)

    @Delete
    abstract suspend fun deleteItem(item: DeletedItemEntity)

    @Query("DELETE FROM deleted_items WHERE id = :id")
    abstract suspend fun deleteItemById(id: String)

    @Query("DELETE FROM deleted_items")
    abstract suspend fun clearAllDeletedItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransaction(tx: TransactionDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertHabayebTransaction(tx: HabayebTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFixedCommitment(commitment: FixedCommitment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertHabayebCustomer(customer: HabayebCustomer)

    @Query("SELECT COUNT(*) FROM habayeb_customers WHERE id = :customerId")
    abstract suspend fun checkCustomerExists(customerId: String): Int

    @Transaction
    open suspend fun restoreSingleTransactionFromBundle(itemId: String, txId: String) {
        val item = getDeletedItemByIdDirect(itemId) ?: return
        if (item.originalTableName != BUNDLE_HABAYEB) return
        
        val root = JSONObject(item.jsonData)
        val custData = root.getJSONObject("customer")
        val customerId = custData.getString("id")
        
        val customerExists = checkCustomerExists(customerId) > 0
        if (!customerExists) {
            val customer = TrashItemParser.parseHabayebCustomer(custData)
            insertHabayebCustomer(customer)
        }
        
        val txsArray = root.getJSONArray("transactions")
        var targetTxObj: JSONObject? = null
        val remainingTxs = JSONArray()
        
        for (i in 0 until txsArray.length()) {
            val txObj = txsArray.getJSONObject(i)
            if (txObj.getString("id") == txId) {
                targetTxObj = txObj
            } else {
                remainingTxs.put(txObj)
            }
        }
        
        if (targetTxObj != null) {
            val tx = TrashItemParser.parseHabayebTransaction(targetTxObj)
            insertHabayebTransaction(tx)
            
            if (remainingTxs.length() == 0) {
                deleteItem(item)
            } else {
                root.put("transactions", remainingTxs)
                val updatedItem = item.copy(jsonData = root.toString())
                insertDeletedItem(updatedItem)
            }
        }
    }

    @Transaction
    open suspend fun restoreDeletedItem(item: DeletedItemEntity) {
        val root = JSONObject(item.jsonData)
        when (item.originalTableName) {
            TABLE_TRANSACTIONS -> {
                val tx = TrashItemParser.parseTransactionDb(root)
                insertTransaction(tx)
            }
            TABLE_HABAYEB_TRANSACTIONS -> {
                val tx = TrashItemParser.parseHabayebTransaction(root)
                insertHabayebTransaction(tx)
            }
            TABLE_FIXED_COMMITMENTS -> {
                val fc = TrashItemParser.parseFixedCommitment(root)
                insertFixedCommitment(fc)
            }
            TABLE_HABAYEB_CUSTOMERS -> {
                val customer = TrashItemParser.parseHabayebCustomer(root)
                insertHabayebCustomer(customer)
            }
            BUNDLE_HABAYEB -> {
                val custData = root.getJSONObject("customer")
                val customer = TrashItemParser.parseHabayebCustomer(custData)
                insertHabayebCustomer(customer)

                val txsArray = root.getJSONArray("transactions")
                for (i in 0 until txsArray.length()) {
                    val txObj = txsArray.getJSONObject(i)
                    val tx = TrashItemParser.parseHabayebTransaction(txObj)
                    insertHabayebTransaction(tx)
                }
            }
            BUNDLE_DAR -> {
                if (root.has("commitments")) {
                    val fcsArray = root.getJSONArray("commitments")
                    for (i in 0 until fcsArray.length()) {
                        val fcObj = fcsArray.getJSONObject(i)
                        val fc = TrashItemParser.parseFixedCommitment(fcObj)
                        insertFixedCommitment(fc)
                    }
                }
                
                if (root.has("transactions")) {
                    val txsArray = root.getJSONArray("transactions")
                    for (i in 0 until txsArray.length()) {
                        val txObj = txsArray.getJSONObject(i)
                        val tx = TrashItemParser.parseTransactionDb(txObj)
                        insertTransaction(tx)
                    }
                }
            }
        }
        deleteItem(item)
    }
}

