plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cars"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cars"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.fragment:fragment:1.8.5")

    // JSON handling
    implementation("com.google.code.gson:gson:2.10.1")

    // Java SDK for Appwrite (Alternative from GitHub) - Temporarily removed
    // implementation("online.bingzi:sdk-for-java:0.8")
    
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation(libs.activity)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}