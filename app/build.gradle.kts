plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "de.lifemodule.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.lifemodule.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Minification off in debug for faster iteration and readable stacktraces
            isMinifyEnabled = false
        }
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
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// Room schema export location (required when exportSchema = true)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // ── Module dependencies ───────────────────────────────────────────────
    implementation(project(":core"))
    implementation(project(":features:gym"))
    implementation(project(":features:nutrition"))
    implementation(project(":features:health"))
    implementation(project(":features:planner"))
    implementation(project(":features:shopping"))
    implementation(project(":features:analytics"))
    implementation(project(":features:logbook"))
    implementation(project(":features:scanner"))
    implementation(project(":features:recipes"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // CameraX + ML Kit Barcode
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode)

    // Accompanist (runtime permissions — Apache 2.0)
    implementation(libs.accompanist.permissions)

    // ── New libraries ──────────────────────────────────────────────────────
    // Vico: animated Compose-native charts (Apache 2.0)
    implementation(libs.vico.compose.m3)

    // Jetpack DataStore: modern async SharedPreferences replacement (Apache 2.0)
    implementation(libs.datastore.preferences)

    // Timber: structured logging for all modules (Apache 2.0)
    implementation(libs.timber)

    // Kotlin Serialization runtime (required for @Serializable routes in Navigation 2.8+)
    implementation(libs.kotlinx.serialization.json)

    // Health Connect: read steps, distance, sleep, HR from device sensors + wearables
    implementation(libs.health.connect.client)

    // Force consistent Guava version — Health Connect + CameraX both depend on
    // ListenableFuture; without this, one pulls in an incompatible artifact.
    implementation(libs.guava)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
