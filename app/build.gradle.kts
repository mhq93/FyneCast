import java.util.Properties

val localProperties = Properties()
localProperties.load(File(rootDir, "local.properties").inputStream())

plugins {
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.mhq.fynecast"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mhq.fynecast"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "WEATHER_API_KEY",
            "\"${localProperties["WEATHER_API_KEY"]}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_12
        targetCompatibility = JavaVersion.VERSION_12
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    // 1. Android Core & Lifecycle Stability Layers
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation("androidx.core:core-splashscreen:1.2.0")

    // 2. Main Compose UI Ecosystem (Driven exclusively by single version library catalog tracking)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)

    // 3. Unified Material 3 Design Tokens (All duplicates eliminated)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // 4. Asynchronous Architecture, Serialization & Navigation
    val nav_version = "2.9.8"
    implementation("androidx.navigation:navigation-compose:${nav_version}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Fixed Serialization version duplication
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // 5. Consolidated Data Tier Network Engines (Retrofit + Coil 3 Framework)
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    // 6. Native Android Room & Paging System Configuration
    val room_version = "2.8.4"
    val paging_version = "3.4.2"
    ksp("androidx.room:room-compiler:${room_version}")
    implementation("androidx.room:room-ktx:${room_version}")
    implementation("androidx.room:room-runtime:${room_version}")
    implementation("androidx.room:room-paging:${room_version}")
    implementation("androidx.paging:paging-runtime:$paging_version")
    implementation("androidx.paging:paging-compose:3.5.0-rc01")

    // 7. Secure Global Firebase Ecosystem (Cleaned up KTX duplicates)
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage") // Added for permanent user avatar handling
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // 8. User Location Mapping & Permissions Extensions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.maplibre.gl:android-sdk:12.0.0")

    // 9. To resolve language change...
    implementation("com.google.android.material:material:1.12.0")

    // 10. Automated Local Testing Harness Layers
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}