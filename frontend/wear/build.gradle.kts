plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

android {
    namespace = "com.frontend.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.frontend"
        minSdk = 30   // Wear OS 3.0 (API 30) — Galaxy Watch 4+
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Wear OS Compose UI
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear)
    // Compose Foundation (HorizontalPager)
    implementation("androidx.compose.foundation:foundation:1.7.0")

    // Wearable Data Layer (폰과 통신)
    implementation(libs.play.services.wearable)
    // Coroutines support for Play Services (await() 확장)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Core
    implementation(libs.androidx.core.ktx)
}
