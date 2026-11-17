import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// Release signing: the key and its passwords live in keystore.properties, which is never
// committed (see keystore.properties.example). Without it, the release APK is just unsigned.
val keystoreFile = rootProject.file("keystore.properties")
val keystore =
    Properties().apply {
        if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
    }

android {
    namespace = "com.codka.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.codka.app"
        minSdk = 24
        targetSdk = 36
        // Raise both for every APK given to sellers. 0.2.0 is the Borama pilot.
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // No code shrinking yet: Room and ExoPlayer would need keep rules tested first.
            isMinifyEnabled = false
            if (keystoreFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
        // Robolectric (database tests) needs the app's resources and manifest.
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    // Each database version's schema is saved here, so future migrations can be checked.
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    filter { exclude { it.file.path.contains("build") } }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Plays the recorded Somali clips back to back (task 18)
    implementation(libs.androidx.media3.exoplayer)
    // Payment history and settings (task 14)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    // Real org.json for unit tests (Android's built-in one is only a stub on the computer)
    testImplementation(libs.org.json)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Database tests run on the computer with Robolectric, which uses JUnit 4.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.vintage.engine)
}
