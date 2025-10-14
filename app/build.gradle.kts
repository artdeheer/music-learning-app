plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yourname.musiclearning"
    // Use the straightforward syntax for compileSdk:
    compileSdk = 36 // Android 15 (adjust if your toolchain requires)

    defaultConfig {
        applicationId = "com.yourname.musiclearning"
        minSdk = 26          // 26+ is a good modern floor; keep 25 if you need it
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // set true when you’re ready to shrink/obfuscate
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Optional: this makes debug builds install alongside release
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // Modern JVM targets (AGP 8+ supports 17 and it’s recommended)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // If you use ViewBinding/BuildConfig, keep their toggles here as needed
    // composeOptions {} usually not needed if you use the Compose BOM in libs.versions.toml
}

dependencies {
    // ICONS
    implementation("androidx.compose.material:material-icons-extended") //define in toml file later

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
