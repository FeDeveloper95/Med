plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.built.in1.kotlin)
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fedeveloper95.med"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fedeveloper95.med"
        minSdk = 30
        targetSdk = 37
        versionCode = 12
        versionName = "1.2"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.ui.v1110)
    implementation(libs.androidx.compose.ui.tooling.preview.v1110)
    implementation(libs.androidx.compose.material.icons.extended.v161)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}