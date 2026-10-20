plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.smartgallery.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartgallery.app"
        minSdk = 24
        targetSdk = 34
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // WorkManager Dependency for Background Scanning
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Glide for Fast Gallery Images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Background Coroutines Engine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google ML Kit Official Image Labeling (AI Scanner)
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // Photo Zoom Library
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
}
