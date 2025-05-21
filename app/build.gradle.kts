plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.africanshipping25"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.africanshipping25"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")


// Or your current AGP version
    testImplementation(libs.junit)

// For JSON parsing (Gson)
    implementation("com.google.code.gson:gson:2.8.9")
// If you are using Kotlin, ensure you have the Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")
// Use your project's Kotlin version
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("de.hdodenhof:circleimageview:3.1.0")
}