plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // 🖼️ Glide for Fast Gallery Images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // ⚙️ Coroutines for Background AI Tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
