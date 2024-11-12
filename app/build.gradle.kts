plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)  // Google Services plugin for Firebase
}


android {
    namespace = "com.example.leisurepace"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.leisurepace"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Jetpack Compose dependencies
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.ui)  // Compose UI components
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.firestore)  // Material Design 3
    debugImplementation(libs.ui.tooling)  // For Compose debugging
    androidTestImplementation(libs.ui.test.junit4)  // For Compose UI testing in instrumentation tests

    // Android UI libraries
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)

    // Firebase and Google Play Services
    implementation(libs.firebase.auth)  // Firebase Authentication
    implementation(libs.play.services.auth)  // Google Sign-In and Play Services
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth.v2211)


    // Credential Manager for Google Sign-In
    implementation(libs.androidx.credentials)  // Credential Manager API

    // AndroidX lifecycle components
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)  // Compose UI testing
    //load in gif
    implementation(libs.glide.v4132)
    annotationProcessor(libs.compiler.v4132)
    // Enforcing specific versions to avoid conflicts
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v350)

    // Optional: Exclude higher versions causing conflicts
    configurations.all {
        resolutionStrategy {
            force("androidx.test.ext:junit:1.1.5")
            force("androidx.test.espresso:espresso-core:3.5.0")
        }
    }
    implementation(kotlin("script-runtime"))
}


