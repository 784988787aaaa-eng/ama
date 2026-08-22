package com.example.domain

import java.security.MessageDigest

object HashUtils {
    // Salt/Pepper constraint to prevent standard offline lookups on SHA-256 digests
    private const val APP_PEPPER = "SmartMakhzanSecurityGuard_2026_#!"
    private val HEX_CHARS = "0123456789abcdef".toCharArray()
    private val sha256Digest = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }

    /**
     * Generates a protected cryptographic SHA-256 hash using static and dynamic salts.
     * Backwards-compatible fallback handles legacy inputs gracefully.
     */
    fun hashString(input: String, deviceSalt: String = ""): String {
        val dynamicSalt = if (deviceSalt.isNotEmpty()) deviceSalt.reversed() else "DefaultDeviceSalt2026#$"
        val saltedInput = input + APP_PEPPER + dynamicSalt
        val bytes = saltedInput.toByteArray(Charsets.UTF_8)
        val md = sha256Digest.get()
        md.reset()
        val digest = md.digest(bytes)
        
        val hexChars = CharArray(digest.size * 2)
        for (i in digest.indices) {
            val v = digest[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_CHARS[v ushr 4]
            hexChars[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Constant-time comparison to prevent side-channel timing attacks.
     */
    fun secureEquals(a: String?, b: String?): Boolean {
        if (a == null || b == null) return false
        if (a.length != b.length) return false
        var result = 0
        for (i in 0 until a.length) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Memory scrubber helper: zeroes out sensitive character arrays.
     */
    fun wipeCharArray(array: CharArray) {
        array.fill('\u0000')
    }

    /**
     * Memory scrubber helper: zeroes out sensitive byte arrays.
     */
    fun wipeByteArray(array: ByteArray) {
        array.fill(0)
    }
}

