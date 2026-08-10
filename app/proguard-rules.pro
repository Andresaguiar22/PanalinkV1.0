# Project specific ProGuard rules for Panalink
# These rules harden the app against reverse engineering and modification.

# General obfuscation
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-repackageclasses ''

# Keep line numbers for debugging but rename source files
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute Panalink

# Moshi and JSON serialization protection
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** {
    @com.squareup.moshi.Json <fields>;
}

# Room database protection
-keep class com.example.data.database.** { *; }

# Retrofit protection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

# OkHttp protection
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep our security and crypto managers but obfuscate internal methods if possible
# We keep the object itself to ensure its singleton nature isn't broken by some tools
-keep class com.example.util.CryptoManager
-keep class com.example.util.SecurityManager
-keep class com.example.util.Resilience

# Prevent tampering with constants in SupabaseClient
-keepclassmembers class com.example.data.supabase.SupabaseClient {
    public static final java.lang.String supabaseUrl;
    public static final java.lang.String supabaseAnonKey;
}

# AndroidX and Material protection
-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
