package com.example.data.serialization

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import com.example.data.local.entities.AppSettings

class BackupPayloadBusinessProfileTest {
    private fun payload(
        businessName: String? = "محل الاختبار",
        logo: String? = "aGVsbG8="
    ) = BackupPayloadData(
        settings = AppSettings(),
        commitments = emptyList(),
        transactions = emptyList(),
        businessName = businessName,
        businessDescription = "وصف",
        businessPhones = listOf("777", "711"),
        businessLogoBase64 = logo
    )

    @Test
    fun exportedBackupContainsBusinessProfileAndVersion3IntegrityMetadata() {
        val writer = java.io.StringWriter()
        BackupPayloadSerializer.exportBackupToWriter(payload(), writer)
        val root = JSONObject(writer.toString())
        val metadata = root.getJSONObject("metadata")
        val profile = root.getJSONObject("business_profile")

        assertEquals(BackupPayloadSerializer.CURRENT_SECURITY_HASH_VERSION,
            metadata.getInt("security_hash_version"))
        assertEquals("محل الاختبار", profile.getString("name"))
        assertEquals("وصف", profile.getString("description"))
        assertEquals("aGVsbG8=", profile.getString("logo_base64"))
        assertTrue(BackupPayloadSerializer.verifyIntegrityHash(root, payload()))
    }

    @Test
    fun changingBusinessProfileInvalidatesVersion3IntegrityHash() {
        val original = payload()
        val writer = java.io.StringWriter()
        BackupPayloadSerializer.exportBackupToWriter(original, writer)
        val root = JSONObject(writer.toString())
        root.getJSONObject("business_profile").put("name", "اسم آخر")

        assertFalse(BackupPayloadSerializer.verifyIntegrityHash(root, original))
    }

    @Test
    fun parseBusinessProfilePreservesPhonesAndLogo() {
        val root = JSONObject("""
            {"business_profile":{"name":"محل","description":"وصف","phones":["777","  ","711"],"logo_base64":"aGVsbG8="}}
        """.trimIndent())

        val parsed = BackupPayloadSerializer.parseBusinessProfile(root)
        assertEquals("محل", parsed.name)
        assertEquals("وصف", parsed.description)
        assertEquals(listOf("777", "711"), parsed.phones)
        assertEquals("aGVsbG8=", parsed.logoBase64)
    }
}
