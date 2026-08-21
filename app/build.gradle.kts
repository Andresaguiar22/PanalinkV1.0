import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}

android {
    sourceSets {
        getByName("test") {
            assets {
                srcDir("schemas")
            }
        }
    }
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.panalink.app"
        minSdk = 24
        targetSdk = 35
        val baseVersionCode = 1
        val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()
        versionCode = (System.getenv("VERSION_CODE") ?: (baseVersionCode + runNumber).toString()).toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.$runNumber"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val secretsFile = file("../secrets.properties")
        val secrets = Properties()
        if (secretsFile.exists()) secrets.load(secretsFile.inputStream())

        val appUrl = System.getenv("APP_URL") ?: secrets.getProperty("BACKEND_URL") ?: "http://10.0.2.2:3000"
        buildConfigField("String", "BACKEND_URL", "\"$appUrl\"")

        // Panalink OTA: las actualizaciones se distribuyen manualmente desde
        // el repositorio público dedicado Andresaguiar22/panalink-ota.
        buildConfigField("String", "GITHUB_OWNER", "\"Andresaguiar22\"")
        buildConfigField("String", "GITHUB_REPOSITORY", "\"panalink-ota\"")
    }

    signingConfigs {
        create("release") {
            val secretsFile = file("../secrets.properties")
            val appSecretsFile = file("secrets.properties")
            val secrets = Properties()
            if (secretsFile.exists()) secrets.load(secretsFile.inputStream())
            if (appSecretsFile.exists()) secrets.load(appSecretsFile.inputStream())

            val keystoreFile = System.getenv("KEYSTORE_FILE") ?: secrets.getProperty("KEYSTORE_FILE")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: secrets.getProperty("KEYSTORE_PASSWORD")
            val keyAliasStr = System.getenv("KEY_ALIAS") ?: secrets.getProperty("KEY_ALIAS")
            val keyPasswordEnv = System.getenv("KEY_PASSWORD") ?: secrets.getProperty("KEY_PASSWORD")
            val keyPasswordStr = if (!keyPasswordEnv.isNullOrEmpty()) keyPasswordEnv else keystorePassword
            val isReleaseRequested = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
            if (!keystoreFile.isNullOrEmpty() && !keystorePassword.isNullOrEmpty() && !keyAliasStr.isNullOrEmpty()) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAliasStr
                keyPassword = keyPasswordStr
            } else if (isReleaseRequested) {
                throw GradleException("RELEASE BUILD BLOCKED: SIGNING CREDENTIALS (KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS) ARE MISSING")
            }
        }
    }

    buildTypes {
        release {
            // Fast Release: keep the official Release signing configuration,
            // but disable R8/minification so Termux builds finish quickly.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug { }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation("androidx.core:core-splashscreen:1.0.1")
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.compose.runtime.livedata)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.coil.gif)
  implementation(libs.coil.video)
  implementation(libs.converter.moshi)
  implementation(libs.zxing.android.embedded)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.datasource)
  implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")
  implementation(libs.webrtc)
  implementation(libs.socket.io)
  implementation("androidx.security:security-crypto:1.1.0-alpha06")

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

ksp {
    arg("room.schemaLocation", projectDir.absolutePath + "/schemas")
}