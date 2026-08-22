package com.example.data.serialization

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class MzdBackupSerializerIntegrityTest {
    @Test
    fun explicitInitialTypeIsPreservedWhenTransactionsSuggestAnotherType() {
        val root = JSONObject("""
            {"habayeb_debts":{"customers":[{"id":"c1","name":"Customer","phone":"","notes":"","created_at":100,"initial_type":"OWED_BY_THEM"}],"debt_transactions":[{"id":"t1","customer_id":"c1","type":"OWED_TO_THEM","amount":"10","timestamp":100,"description":"","is_foreign":false,"currency_code":"ر.ي","foreign_amount":"10","exchange_rate":"1","is_rate_calculated":false,"equivalent_amount":"10","base_currency_code":"ر.ي"}]}}
        """.trimIndent())

        val restored = MzdBackupSerializer.parseHabayebCustomers(root)
        assertEquals("OWED_BY_THEM", restored.single().customer.initialType)
    }
}
