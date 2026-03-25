import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    id("com.google.gms.google-services")
}

fun loadProperties(path: String): Properties = Properties().apply {
    val file = rootProject.file(path)
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val envProperties = loadProperties(".env")
val localProperties = loadProperties("local.properties")

fun resolveConfig(key: String, default: String = ""): String {
    return providers.gradleProperty(key).orNull
        ?: providers.environmentVariable(key).orNull
        ?: envProperties.getProperty(key)
        ?: localProperties.getProperty(key)
        ?: default
}

fun usesCleartext(url: String): String =
    url.startsWith("http://", ignoreCase = true).toString()

val kakaoMapApiKey = resolveConfig("KAKAO_MAP_API_KEY")
val localBaseUrl = resolveConfig("BASE_URL_LOCAL", resolveConfig("BASE_URL", "http://10.0.2.2:8080/api/v1/"))
val devBaseUrl = resolveConfig("BASE_URL_DEV", "https://j14e108.p.ssafy.io/dev/api/v1/")
val prodBaseUrl = resolveConfig("BASE_URL_PROD", "https://j14e108.p.ssafy.io/api/v1/")

android {
    namespace = "com.frontend"
    compileSdk = 36

    flavorDimensions += "environment"

    defaultConfig {
        applicationId = "com.frontend"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["kakaoMapApiKey"] = kakaoMapApiKey
        buildConfigField("String", "KAKAO_MAP_API_KEY", "\"$kakaoMapApiKey\"")
    }

    productFlavors {
        create("local") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"$localBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = usesCleartext(localBaseUrl)
        }

        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = usesCleartext(devBaseUrl)
        }

        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = usesCleartext(prodBaseUrl)
        }
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
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.datastore.preferences)

    implementation(libs.coil.compose)

    implementation(libs.play.services.location)

    // Wearable Data Layer API
    implementation(libs.play.services.wearable)
    // Kakao Maps

    implementation(libs.kakao.maps)

    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
}
