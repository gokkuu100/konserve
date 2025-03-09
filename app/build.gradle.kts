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

        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"${project.findProperty("GOOGLE_CLOUD_API_KEY") ?: ""}\"")
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }
}

dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)

    // HTTP Client and JSON parsing
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Image processing
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Google Cloud Vision with careful exclusions
    implementation("com.google.cloud:google-cloud-vision:3.16.0") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.protobuf", module = "protobuf-java")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
        exclude(group = "io.grpc", module = "grpc-protobuf")
        exclude(group = "io.grpc", module = "grpc-stub")
        exclude(group = "io.grpc", module = "grpc-protobuf-lite")
        exclude(group = "org.threeten", module = "threetenbp")
        exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
    }
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0") {
        exclude(group = "com.google.guava", module = "guava")
    }
    
    // Use a compatible version of gRPC that works with both Firebase and Vision
    implementation("io.grpc:grpc-okhttp:1.44.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    
    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    // Lifecycle components
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    // CameraX
    implementation ("androidx.camera:camera-camera2:1.1.0")
    implementation ("androidx.camera:camera-lifecycle:1.1.0")
    implementation ("androidx.camera:camera-view:1.1.0")

    // Mapbox
    implementation("com.mapbox.maps:android:11.6.1")
    implementation ("com.mapbox.mapboxsdk:mapbox-sdk-services:5.7.0")

    // Calendar
    implementation ("com.applandeo:material-calendar-view:1.9.2")
    implementation("com.kizitonwose.calendar:view:2.5.4")

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

    // UI Components
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.airbnb.android:lottie:6.6.3")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.transition:transition:1.4.1")
    
}