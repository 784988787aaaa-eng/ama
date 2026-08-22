# --- Advanced R8 & ProGuard Optimization Flags ---
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-verbose

# --- Defensive Engineering Keeps ---

# Prevent Room entities, DAOs, and Database classes from being renamed or stripped
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep interface * {
    @androidx.room.Dao *;
}
-keepclassmembers class * {
    @androidx.room.Database *;
}

# Keep local database entities intact for Room reflection & JSON Backup Serialization
-keep class com.example.data.local.entities.** { *; }
-keep interface com.example.data.local.dao.** { *; }

# Keep security & licensing logic methods intact
-keepclassmembers class com.example.ui.viewmodel.FinanceViewModel {
    *** isTrialExpired(...);
    *** activateLicense(...);
}

# --- Compose and UI State Optimizations ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <fields>;
    @androidx.compose.runtime.Stable <fields>;
}

# Keep WorkManager and App Startup components from being stripped by R8/Proguard
-keep class androidx.work.** { *; }
-keep class androidx.startup.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    <init>(***);
}

# OkHttp Platform rules
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn androidx.room.**



