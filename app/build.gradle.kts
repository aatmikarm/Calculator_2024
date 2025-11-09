plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")

    id("com.google.gms.google-services")

}

android {
    namespace = "com.aatmik.calculator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aatmik.calculator"
        minSdk = 25
        targetSdk = 35
        versionCode = 9
        versionName = "9.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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
        // compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // compose (app size will increase)
    //    implementation("androidx.activity:activity-compose:1.9.2")
//    implementation(platform("androidx.compose:compose-bom:2024.09.01"))
//    implementation("androidx.compose.ui:ui")
//    implementation("androidx.compose.ui:ui-graphics")
//    implementation("androidx.compose.ui:ui-tooling-preview")
//    implementation("androidx.compose.material3:material3")
//    implementation("androidx.wear.compose:compose-material:1.4.0")
//    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha24")
//    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.01"))
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
//    debugImplementation("androidx.compose.ui:ui-tooling")
//    debugImplementation("androidx.compose.ui:ui-test-manifest")
//    implementation("androidx.compose.foundation:foundation-layout:1.7.1")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.cardview:cardview:1.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("net.objecthunter:exp4j:0.4.8")


    // new dependancies

    implementation("org.mariuszgromada.math:MathParser.org-mXparser:6.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    // admob
    implementation("com.google.android.gms:play-services-ads:24.6.0")
    // gson
    implementation("com.google.code.gson:gson:2.13.2")
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")

    // equations
    implementation("org.apache.commons:commons-math3:3.6.1")

    //retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // web scraper
    implementation("org.jsoup:jsoup:1.21.2")

    // CameraX dependencies (update these)
    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")

    // Add this for ListenableFuture support
    implementation("com.google.guava:guava:33.5.0-android")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")


}