import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.google.services) apply false
}

// Conditionally apply Google Services plugin only if google-services.json exists
// This allows builds to succeed without Firebase configuration for development
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("google-services.json not found. Firebase features will be disabled.")
    logger.warn("Copy app/google-services.json.example to app/google-services.json and configure it.")
}

// Load local.properties
val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }

fun getLocalProperty(
    key: String,
    defaultValue: String = "",
): String = localProperties.getProperty(key) ?: project.findProperty(key)?.toString() ?: defaultValue

// Read version from VERSION file (single source of truth)
val versionFile = rootProject.file("VERSION")
val appVersion =
    if (versionFile.exists()) {
        versionFile.readText().trim()
    } else {
        "0.9.0" // Fallback version
    }
val versionParts = appVersion.split(".")
val calculatedVersionCode =
    versionParts[0].toInt() * 10000 +
        versionParts[1].toInt() * 100 +
        versionParts[2].toInt()

android {
    namespace = "three.two.bit.phonemanager"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release-keystore.jks")
            storePassword = getLocalProperty("RELEASE_STORE_PASSWORD", "phonemanager123")
            keyAlias = getLocalProperty("RELEASE_KEY_ALIAS", "phonemanager")
            keyPassword = getLocalProperty("RELEASE_KEY_PASSWORD", "phonemanager123")
        }
    }

    defaultConfig {
        applicationId = "three.two.bit.phonemanager"
        minSdk = 26
        targetSdk = 36
        versionCode = calculatedVersionCode
        versionName = appVersion

        testInstrumentationRunner = "three.two.bit.phonemanager.base.HiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables {
            useSupportLibrary = true
        }

        // API configuration - override in local.properties or CI environment
        buildConfigField("String", "API_BASE_URL", "\"${getLocalProperty("API_BASE_URL")}\"")
        buildConfigField("String", "API_KEY", "\"${getLocalProperty("API_KEY")}\"")

        // Story E9.11: Google OAuth client ID (web client for Android)
        buildConfigField("String", "GOOGLE_OAUTH_CLIENT_ID", "\"${getLocalProperty("GOOGLE_OAUTH_CLIENT_ID")}\"")

        // Story E9.11: Apple OAuth client ID (Services ID from Apple Developer Portal)
        buildConfigField("String", "APPLE_OAUTH_CLIENT_ID", "\"${getLocalProperty("APPLE_OAUTH_CLIENT_ID")}\"")

        // Story E3.1: Google Maps API key
        manifestPlaceholders["MAPS_API_KEY"] = getLocalProperty("MAPS_API_KEY")

        // Story E11.9: Deep link scheme for invite codes
        buildConfigField("String", "DEEP_LINK_SCHEME", "\"phonemanager\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"

            // Story E9.11: Enable mock auth in debug builds (set to false to test real OAuth)
            buildConfigField("boolean", "USE_MOCK_AUTH", getLocalProperty("USE_MOCK_AUTH_DEBUG", "true"))

            // Debug builds can use test endpoints
            val debugBaseUrl =
                getLocalProperty("API_BASE_URL_DEBUG").ifBlank {
                    getLocalProperty("API_BASE_URL").ifBlank { "https://api-dev.phonemanager.example.com" }
                }
            val debugApiKey =
                getLocalProperty("API_KEY_DEBUG").ifBlank {
                    getLocalProperty("API_KEY")
                }
            buildConfigField("String", "API_BASE_URL", "\"$debugBaseUrl\"")
            buildConfigField("String", "API_KEY", "\"$debugApiKey\"")

            // Debug Maps API key (with .dev package name)
            val debugMapsKey =
                getLocalProperty("MAPS_API_KEY_DEBUG").ifBlank {
                    getLocalProperty("MAPS_API_KEY")
                }
            manifestPlaceholders["MAPS_API_KEY"] = debugMapsKey
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Story E9.11: Always use real auth in release builds
            buildConfigField("boolean", "USE_MOCK_AUTH", "false")

            // Release builds require proper configuration
            buildConfigField("String", "API_BASE_URL", "\"${getLocalProperty("API_BASE_URL")}\"")
            buildConfigField("String", "API_KEY", "\"${getLocalProperty("API_KEY")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Timber for logging
    implementation(libs.timber)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Google Play Services Location
    implementation(libs.play.services.location)

    // Google Maps (Story E3.1)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Google Play Services Auth (Story E9.11: OAuth Sign-In)
    implementation(libs.play.services.auth)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.browser)

    // Ktor for networking
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)

    // Encrypted SharedPreferences for API keys
    implementation(libs.androidx.security.crypto)

    // Lottie Animations
    implementation(libs.lottie.compose)

    // Story E11.9: CameraX for QR Code Scanning
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Story E11.9: ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)

    // Story E11.9: ZXing for QR Code Generation
    implementation(libs.zxing.core)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Firebase (Story E12.6: FCM Push Notifications, Story 1.2: Analytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // Testing - Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlin.test)

    // Testing - Instrumented/E2E Tests
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestUtil(libs.androidx.test.orchestrator)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
