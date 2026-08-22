package com.example.data.serialization

import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb
import com.example.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal

/**
 * اختبارات الحالات الطرفية لمنظومة النسخ الاحتياطي وحساب البصمة المشفرة (BackupPayloadSerializer)
 *
 * التوثيق المعماري:
 * يختبر هذا الملف ثبات البصمة المنطقية (Deterministic Hash)، وضمان عدم فقدان دقة الكسور العشرية،
 * وسلامة تصدير واستيراد البيانات التراكمية، ومقاومة التلف في السجلات الفارغة.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BackupPayloadSerializerTest {

    @Test
    fun testDeterministicIntegrityHash() {
        val settings = AppSettings(
            id = 1,
            currencySymbol = "ر.ي",
            schoolExpensesEnabled = true
        )
        val commitment = FixedCommitment(
            id = 1,
            name = "إيجار الشقة",
            targetAmount = BigDecimal("150000.00"),
            currentProgress = BigDecimal("50000.00"),
            orderIndex = 0
        )
        val customer = HabayebCustomer(
            id = "c1",
            name = "علي محمد",
            phone = "777123456",
            initialType = TransactionType.CREDIT.value
        )

        val payload1 = BackupPayloadData(
            settings = settings,
            commitments = listOf(commitment),
            transactions = emptyList(),
            habayebCustomers = listOf(customer),
            habayebTransactions = emptyList()
        )

        val payload2 = BackupPayloadData(
            settings = settings,
            commitments = listOf(commitment),
            transactions = emptyList(),
            habayebCustomers = listOf(customer),
            habayebTransactions = emptyList()
        )

        val hash1 = BackupPayloadSerializer.calculateIntegrityHash(payload1)
        val hash2 = BackupPayloadSerializer.calculateIntegrityHash(payload2)

        assertNotNull(hash1)
        assertEquals(hash1, hash2)
    }

    @Test
    fun testExportAndParsePayloadStream() {
        val settings = AppSettings(
            id = 1,
            currencySymbol = "ر.ي"
        )
        val tx = TransactionDb(
            id = "tx100",
            timestamp = 1600000000000L,
            type = "EXPENSE",
            category = "طعام",
            amount = BigDecimal("12345.67"),
            description = "وجبة غداء"
        )

        val payload = BackupPayloadData(
            settings = settings,
            commitments = emptyList(),
            transactions = listOf(tx),
            habayebCustomers = emptyList(),
            habayebTransactions = emptyList()
        )

        val writer = StringWriter()
        BackupPayloadSerializer.exportBackupToWriter(payload, writer)
        val exportedJson = writer.toString()

        assertTrue(exportedJson.contains("Mizan Al-Dar"))
        assertTrue(exportedJson.contains("12345.67"))
        assertTrue(exportedJson.contains("وجبة غداء"))

        // استيراد التدفق
        val reader = StringReader(exportedJson)
        val restored = BackupPayloadSerializer.parseBackupPayloadStream(reader)

        assertNotNull(restored)
        assertEquals(1, restored.transactions.size)
        assertEquals("tx100", restored.transactions[0].id)
        assertEquals(BigDecimal("12345.67"), restored.transactions[0].amount)
        assertEquals("وجبة غداء", restored.transactions[0].description)
    }

    @Test
    fun testEmptyPayloadIntegrity() {
        val emptyPayload = BackupPayloadData(
            settings = AppSettings(id = 1),
            commitments = emptyList(),
            transactions = emptyList()
        )

        val hash = BackupPayloadSerializer.calculateIntegrityHash(emptyPayload)
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())

        val writer = StringWriter()
        BackupPayloadSerializer.exportBackupToWriter(emptyPayload, writer)
        val exported = writer.toString()

        val restored = BackupPayloadSerializer.parseBackupPayloadStream(StringReader(exported))
        assertEquals(0, restored.transactions.size)
        assertEquals(0, restored.commitments.size)
        assertEquals(0, restored.habayebCustomers.size)
    }
}
