# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Room entities and DAOs
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }

# Keep ViewModels and State classes
-keep class com.example.ui.** { *; }

# Keep engine classes
-keep class com.example.data.engine.** { *; }

# Keep kotlinx.coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep DADB library
-keep class dev.mobile.dadb.** { *; }
-dontwarn dev.mobile.dadb.**

# Keep Firebase AI
-keep class com.google.firebase.ai.** { *; }

# Keep OkHttp and Moshi
-keep class okhttp3.** { *; }
-keep class com.squareup.moshi.** { *; }
-dontwarn okhttp3.**
-dontwarn com.squareup.moshi.**

# Keep Kotlin standard library
-keep class kotlin.** { *; }

# Keep AndroidX annotations
-keep class androidx.annotation.** { *; }

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Activity Result APIs
-keep class androidx.activity.result.** { *; }

# Keep navigation
-keep class androidx.navigation.** { *; }

# Keep lifecycle
-keep class androidx.lifecycle.** { *; }

# Keep datastore
-keep class androidx.datastore.** { *; }

# Keep coil
-keep class coil.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
