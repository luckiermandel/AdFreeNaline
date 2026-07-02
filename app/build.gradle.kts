import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
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
        versionCode = 3
        versionName = "1.2"

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
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("com.google.android.material:material:1.12.0")

    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.maplibre.gl:android-sdk-opengl:11.11.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")

    val coroutinesVersion = "1.8.1"
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}
