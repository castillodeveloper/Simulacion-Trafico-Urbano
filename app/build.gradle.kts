plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.simulaciontraficourbano"
    compileSdk = 36 // VOLVEMOS A 36 PORQUE TUS LIBRERÍAS LO EXIGEN

    defaultConfig {
        applicationId = "com.example.simulaciontraficourbano"
        minSdk = 26
        targetSdk = 36 // VOLVEMOS A 36
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
        compose = true
    }
}

dependencies {
    // --- LIBRERÍAS NECESARIAS PARA TU CÓDIGO ---

    // 1. Navegación (Corrige los errores en MainActivity y NavHost)
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // 2. Iconos Extendidos (Corrige los errores en ConfigScreen y StatsScreen)
    implementation("androidx.compose.material:material-icons-extended")

    // -------------------------------------------------------

    // Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose y Core
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}