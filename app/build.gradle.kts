import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val signingProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String): String? =
    signingProperties.getProperty(key) ?: System.getenv(key)

val storeFilePath = signingValue("RELEASE_STORE_FILE")
val hasReleaseSigning = storeFilePath != null &&
    rootProject.file(storeFilePath).exists() &&
    signingValue("RELEASE_STORE_PASSWORD") != null &&
    signingValue("RELEASE_KEY_ALIAS") != null &&
    signingValue("RELEASE_KEY_PASSWORD") != null

/** Artifact base name, e.g. `MyJoystick-1.0-release.apk`. */
val appArtifactName = "MyJoystick"
val appVersionName = "1.0"

android {
    namespace = "ru.tynkovski.myjoystick"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.tynkovski.myjoystick"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(storeFilePath!!)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Human readable artifact names: MyJoystick-1.0-release.apk instead of app-release.apk.
// AGP 9 removed the legacy `applicationVariants` API, so the modern variant API is used here.
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("$appArtifactName-$appVersionName-${variant.buildType}.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
