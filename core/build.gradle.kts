plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "de.lifemodule.app.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

}

// Room schema export location
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // ── Core AndroidX ─────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    // ── Hilt ──────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── Room ──────────────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // ── SQLCipher (database encryption) ───────────────────────────────────
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)

    // ── Coil (image loading) ──────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── CameraX + ML Kit Barcode ──────────────────────────────────────────
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode)

    // ── Accompanist (runtime permissions) ─────────────────────────────────
    implementation(libs.accompanist.permissions)

    // ── Vico (animated charts) ────────────────────────────────────────────
    implementation(libs.vico.compose.m3)

    // ── Jetpack DataStore ─────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── Kotlin Serialization ──────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ── Timber (structured logging) ───────────────────────────────────────
    api(libs.timber)

    // ── Health Connect ────────────────────────────────────────────────────
    implementation(libs.health.connect.client)

    // ── Guava (consistent version) ────────────────────────────────────────
    implementation(libs.guava)

    // ── Unit tests ─────────────────────────────────────────────────────────
    testImplementation(libs.junit)
}
