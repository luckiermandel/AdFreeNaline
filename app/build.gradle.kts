import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val defaultKeystoreFile =
    File(System.getProperty("user.home"), ".config/adfreenaline/keys/adfreenaline-release.jks")

val userSigningPropertiesFile =
    File(System.getProperty("user.home"), ".config/adfreenaline/signing.properties")

val userSigningProperties = Properties()
if (userSigningPropertiesFile.isFile) {
    userSigningPropertiesFile.inputStream().use { userSigningProperties.load(it) }
}

fun signingCredential(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: userSigningProperties.getProperty(name)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.luckierdev.adfreenaline"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.luckierdev.adfreenaline"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingCredential("ADFREE_KEYSTORE_FILE")
            val keystoreFile = when {
                !keystorePath.isNullOrBlank() -> file(keystorePath)
                defaultKeystoreFile.isFile -> defaultKeystoreFile
                else -> null
            }
            if (keystoreFile != null) {
                storeFile = keystoreFile
                storePassword = signingCredential("ADFREE_KEYSTORE_PASSWORD")
                keyAlias = signingCredential("ADFREE_KEY_ALIAS")
                keyPassword = signingCredential("ADFREE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.google.material)

    implementation(libs.osmdroid)
    implementation(libs.maplibre)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
