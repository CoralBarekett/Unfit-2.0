plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.app.unfit20"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.unfit20"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
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
    // Core + AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.com.google.android.material)

    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Firebase (BOM + sub-libraries)
    implementation(platform(libs.com.google.firebase.bom))
    implementation(libs.com.google.firebase.auth.ktx)
    implementation(libs.com.google.firebase.firestore.ktx)
    implementation(libs.com.google.firebase.storage.ktx)

    // Coroutines
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    implementation(libs.org.jetbrains.kotlinx.coroutines.play.services)

    // Image Loading
    implementation(libs.com.squareup.picasso)
    implementation(libs.com.github.bumptech.glide)
    kapt(libs.com.github.bumptech.glide.compiler)
    implementation(libs.de.hdodenhof.circleimageview)
}