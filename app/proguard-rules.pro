# ==============================================================================
# 🛡️ MASTER PROGUARD & R8 PRODUCTION HARDENING SHIELD
# Comprehensive Code Shrinking, Obfuscation & Reflection Protection Rules
# ==============================================================================

# --- 1. General R8 / ProGuard Optimization & Diagnostic Flags ---
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-repackageclasses ''
-verbose
-dontpreverify

# Keep fundamental metadata and reflection annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,Exceptions

# ==============================================================================
# 🏛️ LAYER 1: ROOM DATABASE, ENTITIES & TYPE CONVERTERS HARDENING
# ==============================================================================

# Prevent Room Database abstract and implementation classes from being stripped or renamed
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}

# Keep all Room DAOs, their SQL query methods, and DAO builders
-keep interface * {
    @androidx.room.Dao *;
}
-keep interface com.example.data.local.dao.** { *; }

# Keep all Room Database Entity models completely intact (tables, column fields, constructors)
-keep @androidx.room.Entity class * { *; }
-keep class com.example.data.local.entities.** {
    <fields>;
    <methods>;
    <init>(...);
}

# Protect Room Type Converters and their serialization methods
-keep class * {
    @androidx.room.TypeConverter <methods>;
}
-keep class com.example.data.local.converter.** { *; }
-keep class com.example.data.local.BigDecimalConverter { *; }
-keep class com.example.data.local.AppDatabase { *; }

# Suppress benign Room generation warnings
-dontwarn androidx.room.**

# ==============================================================================
# 📦 LAYER 2: BACKUP, JSON / MZD SERIALIZATION & DOMAIN PAYLOADS
# ==============================================================================

# Protect all Kotlinx Serialization & Java Serialization models
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Protect all Domain Models, Serialization Payloads, Enums and Business Profiles
-keep class com.example.domain.model.** {
    <fields>;
    <methods>;
    <init>(...);
}
-keep class com.example.data.serialization.** {
    <fields>;
    <methods>;
    <init>(...);
}
-keep class com.example.data.serialization.pdf.** { *; }
-keep class com.example.ui.state.** {
    <fields>;
    <methods>;
    <init>(...);
}

# Protect Enum classes against name ordinal stripping
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
}

# ==============================================================================
# ⚙️ LAYER 3: WORKMANAGER & BACKGROUND SYNC WORKERS
# ==============================================================================

# Prevent R8 from removing or renaming WorkManager worker classes and their constructors
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Specific Worker implementations
-keep class com.example.AutoBackupWorker { *; }
-keep class com.example.BackupReminderWorker { *; }
-keep class com.example.CloudUploadWorker { *; }
-keep class com.example.TrashCleanupWorker { *; }

# WorkManager internal runtime reflection
-keep class androidx.work.** { *; }
-keep class androidx.startup.** { *; }

# ==============================================================================
# 🔐 LAYER 4: SECURITY, CRYPTOGRAPHY, BIOMETRICS & CLOUD AUTHENTICATION
# ==============================================================================

# Protect custom cryptographic algorithms, hashing utilities and licensing guard
-keep class com.example.domain.DatabaseSecurityGuard { *; }
-keep class com.example.domain.HashUtils { *; }
-keep class com.example.domain.LicenseCrypto { *; }
-keep class com.example.domain.BiometricAuthHelper { *; }

# Keep security validation methods in ViewModels intact
-keepclassmembers class com.example.ui.viewmodel.SecurityAndLicenseViewModel {
    *** isTrialExpired(...);
    *** activateLicense(...);
    *** checkDeviceBinding(...);
}
-keepclassmembers class com.example.ui.viewmodel.FinanceViewModel {
    *** isTrialExpired(...);
    *** activateLicense(...);
}

# AndroidX BiometricPrompt Protection
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Google Identity, Credential Manager & Google Drive API
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.example.data.cloud.** { *; }

# Networking and Security library suppressions
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ==============================================================================
# 🎨 LAYER 5: JETPACK COMPOSE & IMMUTABLE UI STATE STABILITY
# ==============================================================================

# Preserve Compose compiler runtime stability annotations
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <fields>;
    @androidx.compose.runtime.Stable <fields>;
}

# Preserve navigation route and serializable arguments
-keep class com.example.ui.navigation.** { *; }

# Preserve Coroutines internal stack frames
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ==============================================================================
# 🛡️ LAYER 6: PRODUCTION LOGGING & SENSITIVE DATA LEAK PREVENTION
# ==============================================================================
# إزالة استدعاءات السجلات التفصيلية وسجلات التصحيح تلقائياً في نسخة الإنتاج لمنع أي تسريب محتمل
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
