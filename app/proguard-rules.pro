# =============================================================================
# LifeModule ProGuard / R8 Rules
# =============================================================================

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepattributes SourceFile, LineNumberTable   # readable crash stack traces
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @javax.inject.Singleton class * { *; }
-dontwarn dagger.hilt.**

# ── Room ──────────────────────────────────────────────────────────────────────
# Keep all Room entities / DAOs so R8 doesn't strip @Entity or @Dao annotations
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# ── Jetpack Compose ───────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Navigation Compose ────────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }

# ── ML Kit Barcode ────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── CameraX ───────────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── Coil ──────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── SQLCipher (JNI native bindings - must not be renamed/stripped) ────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ── Vico (charts) ─────────────────────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**
# ── Health Connect ─────────────────────────────────────────────────────
-keep class androidx.health.connect.** { *; }
-dontwarn androidx.health.connect.**
# ── App data models (Room entities — must survive obfuscation) ─────────────────
-keep class de.lifemodule.app.data.** { *; }

# ── Kotlin Serialization (required for Navigation 2.8+ type-safe routes) ─────
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.lifemodule.app.**$$serializer { *; }
-keepclassmembers class de.lifemodule.app.** {
    *** Companion;
}
-keepclasseswithmembers class de.lifemodule.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Strip Log spam in release (keeps w/e crash info intact) ────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
