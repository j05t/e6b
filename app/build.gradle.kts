plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.jstappdev.e6bflightcomputer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jstappdev.e6bflightcomputer"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildToolsVersion = "34.0.0"
}

dependencies {
    implementation(libs.androidx.appcompat)
}