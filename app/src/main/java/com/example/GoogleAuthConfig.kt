package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.security.MessageDigest

/**
 * مركز إعدادات المصادقة مع جوجل (Google Auth)
 * يُستخدم لحل مشكلة الخطأ 10 (Developer Error) وضمان تطابق الإعدادات.
 */
object GoogleAuthConfig {
    private const val TAG = "GoogleAuthConfig"

    // ==========================================
    // ⚠️ أدخل معرف العميل هنا (WEB_CLIENT_ID) يدوياً
    // مثال: "1234567890-abcdefg.apps.googleusercontent.com"
    // ==========================================
    val WEB_CLIENT_ID = BuildConfig.GOOGLE_CLIENT_ID

    /**
     * وظيفة تحقق صارمة تتأكد من أن معرف العميل تم إدخاله بالتنسيق الصحيح والكامل الخاص بجوجل.
     */
    fun validateClientId(): Boolean {
        if (WEB_CLIENT_ID.isEmpty()) {
            Log.e(TAG, "❌ [GOOGLE_AUTH_ERROR] WEB_CLIENT_ID is EMPTY! Please insert your Web Client ID in GoogleAuthConfig.kt")
            return false
        }
        
        val trimmed = WEB_CLIENT_ID.trim()
        val suffix = ".apps.googleusercontent.com"
        
        if (!trimmed.endsWith(suffix)) {
            Log.e(TAG, "❌ [GOOGLE_AUTH_ERROR] Invalid Web Client ID: It must end with '$suffix'. Got: '$trimmed'")
            return false
        }
        
        val prefix = trimmed.substring(0, trimmed.length - suffix.length)
        if (prefix.isEmpty() || !prefix.any { it.isDigit() }) {
            Log.e(TAG, "❌ [GOOGLE_AUTH_ERROR] Invalid Web Client ID prefix. Prefix must contain numeric digits. Got: '$trimmed'")
            return false
        }
        
        Log.d(TAG, "✅ [GOOGLE_AUTH_SUCCESS] Google WEB_CLIENT_ID is valid and configured: '$trimmed'")
        return true
    }

    /**
     * وظيفة برمجية مساعدة لطباعة اسم الحزمة وبصمة SHA-1 الخاصة بالتطبيق عند التشغيل
     * لمقارنتها بدقة وتطابقها في Google Cloud Console.
     */
    fun logAppSignatureAndPackage(context: Context) {
        val packageName = context.packageName
        Log.d(TAG, "===============================================")
        Log.d(TAG, "📱 [GOOGLE_AUTH_COMPATIBILITY_CHECK] Checking Configuration:")
        Log.d(TAG, "📱 App Package Name: $packageName")
        
        try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = info.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                info.signatures
            }

            if (signatures != null) {
                for ((index, sig) in signatures.withIndex()) {
                    val rawCert = sig.toByteArray()
                    val md = MessageDigest.getInstance("SHA-1")
                    val publicKey = md.digest(rawCert)
                    val hexString = publicKey.joinToString(":") { 
                        String.format("%02X", it) 
                    }
                    Log.d(TAG, "🔑 SHA-1 Fingerprint [#$index]: $hexString")
                }
            } else {
                Log.e(TAG, "⚠️ No signing signatures found for this app!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching signatures", e)
        }
        
        Log.d(TAG, "===============================================")
    }
}
