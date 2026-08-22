package com.example.ui.viewmodel.backup

import android.content.Context
import com.example.data.local.entities.AppSettings
import com.example.data.repository.FinanceRepository
import com.example.data.serialization.BackupPayloadSerializer
import com.example.data.serialization.MzdBackupSerializer
import kotlinx.coroutines.flow.first

object BackupPayloadBuilder {
    suspend fun buildBackupJson(
        repository: FinanceRepository,
        isMzd: Boolean,
        context: Context
    ): String {
        val currentSettings = repository.settingsFlow.first() ?: AppSettings()
        val commitments = repository.commitmentsFlow.first()
        val transactions = repository.transactionsFlow.first()
        val habayebCusts = repository.getAllCustomersDirect()
        val habayebTxs = repository.getAllTransactionsDirect()
        val deletedItems = repository.deletedItemsFlow.first()
        return if (isMzd) {
            MzdBackupSerializer.exportBackupToJson(currentSettings, commitments, transactions, habayebCusts, habayebTxs, deletedItems, context)
        } else {
            BackupPayloadSerializer.exportBackupToJson(currentSettings, commitments, transactions, habayebCusts, habayebTxs, deletedItems, context)
        }
    }
}
