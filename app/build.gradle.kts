plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.services)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.konserve"
    compileSdk = 34

    viewBinding {
        enable = true
    }
    
    defaultConfig {
        applicationId = "com.example.konserve"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Calendar
    implementation("com.kizitonwose.calendar:view:2.6.0-beta04")
    implementation(libs.material.calendarview)

    // Mapbox location
    implementation("com.mapbox.maps:android:11.6.0")
    implementation("com.mapbox.plugin:maps-plugin-locationcomponent:10.13.0")
    implementation("com.mapbox.navigation:android:2.9.0")

    // Glide for image handling
    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.14.2")

    // Retrofit and Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    
    // Google Play Services
    implementation(libs.play.services.maps)

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}