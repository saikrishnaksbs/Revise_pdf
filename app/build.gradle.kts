plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.revisepdf.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.revisepdf.app"
        // llama-android publishes arm64-v8a/x86_64 only and requires API 28.
        minSdk = 28
        targetSdk = 34
        versionCode = 3
        versionName = "0.2.1"

        ndk {
            // Phones are all arm64; dropping x86_64 halves the native payload.
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // The llama.cpp engine is ~70MB of native library and bindings, and it is useless without a
    // multi-GB GGUF model the user supplies separately. The lite flavour leaves it out entirely so
    // the PDF revision app stays a small, easy-to-sideload download.
    flavorDimensions += "ai"
    productFlavors {
        create("lite") {
            dimension = "ai"
            versionNameSuffix = "-lite"
            buildConfigField("boolean", "AI_ENABLED", "false")
        }
        create("full") {
            dimension = "ai"
            buildConfigField("boolean", "AI_ENABLED", "true")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.pdfbox.android)

    // The llama-kotlin coroutines facade is deliberately not used: it ships Kotlin 2.4 metadata,
    // which this project's Kotlin 2.0 compiler cannot read. The Java API returns the whole reply
    // anyway, which is all this app needs.
    "fullImplementation"(libs.llama.android)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
