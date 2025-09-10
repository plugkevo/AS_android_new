plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services) // Ensure the version for this plugin in libs.versions.toml is up-to-date

}

android {
    namespace = "com.example.africanshipping25"
    compileSdk = 36 // Recommended to use latest stable API, e.g., 34 or 36

    defaultConfig {
        applicationId = "com.example.africanshipping25"
        minSdk = 24
        targetSdk = 36 // Recommended to use latest stable API, e.g., 34 or 36
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
        // Ensure your Java version matches your project setup and dependencies
        // Typically, Android Studio projects default to 1.8 or 11.
        // If your project was created with a newer template, it might be 1.8.
        sourceCompatibility = JavaVersion.VERSION_1_8 // Common for Android
        targetCompatibility = JavaVersion.VERSION_1_8 // Common for Android
    }
    kotlinOptions {
        jvmTarget = "1.8" // Must match sourceCompatibility and targetCompatibility
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // AndroidX Core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Material Design (use the libs.material alias if it's set up in versions.toml)
    implementation(libs.material) // Preferred over hardcoding "com.google.android.material:material:1.11.0"

    // Firebase BoM - THIS MUST BE platform() and declare FIRST among Firebase deps
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Always use the latest stable version

    // Firebase Authentication (no version needed, BOM provides it)
    implementation("com.google.firebase:firebase-auth-ktx") // Correct, from BOM

    // Firebase Firestore (no version needed, BOM provides it)
    implementation("com.google.firebase:firebase-firestore-ktx") // Correct, from BOM

    // Google Play Services Auth library (This is new for Google Sign-In)
    // Needs explicit version as it's not part of the Firebase BOM directly
    implementation("com.google.android.gms:play-services-auth:21.2.0") // Keep explicit version

    // Optional: For Credential Manager
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")

    // Lottie Animation
    implementation("com.airbnb.android:lottie:6.4.0") // Keep explicit version

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.2.0") // Keep explicit version

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // Keep explicit version

    // For JSON parsing (Gson)
    implementation("com.google.code.gson:gson:2.8.9") // Keep explicit version

    // Circle ImageView
    implementation("de.hdodenhof:circleimageview:3.1.0") // Keep explicit version

    implementation("com.google.firebase:firebase-messaging")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))

    // Add the dependencies for the In-App Messaging and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-inappmessaging-display")
    implementation("com.google.firebase:firebase-analytics")
    // Image loading library
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // Firebase Storage
    implementation ("com.google.firebase:firebase-storage:20.3.0")
    // Image loading library
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // Firebase Storage
    implementation ("com.google.firebase:firebase-storage:20.3.0")

    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.1")

}