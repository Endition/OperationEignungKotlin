plugins {
    id("kotlin-kapt")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.hilt)
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.example.operationeignung"
    compileSdk = 36

    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "com.example.operationeignung"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        // optional, aber sinnvoll:
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.material3)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation.layout)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Standard-Abhängigkeiten...
    implementation(libs.androidx.core.ktx.v1131)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation für Screens
    implementation(libs.androidx.navigation.compose)

    //Icons
    implementation(libs.androidx.material.icons.extended.android)

    // ViewModel für MVVM
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material.v1110)

    // Room für SQLite Datenbank
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // JSON-Verarbeitung für den Import
    implementation(libs.kotlinx.serialization.json)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
hilt {
    enableAggregatingTask = false
}
kapt {
    correctErrorTypes = true
}
