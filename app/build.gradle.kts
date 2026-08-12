import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing is configured out of band so that no key material or password is ever
// committed. Provide either a keystore.properties in the repository root (gitignored):
//
//     storeFile=C:/path/to/release.jks
//     storePassword=...
//     keyAlias=...
//     keyPassword=...
//
// or the equivalent environment variables for CI. With nothing configured the release
// build still assembles, just unsigned, so a fresh clone is never broken by a missing key.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun signingSetting(property: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(environmentVariable)

val releaseStorePath = signingSetting("storeFile", "ANDROID_KEYSTORE_FILE")

android {
    namespace = "com.devicespooflab.hooks"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spoofmydevice"
        minSdk = 26
        targetSdk = 37
        versionCode = 11
        versionName = "2.0.2"
    }

    signingConfigs {
        create("release") {
            if (releaseStorePath != null) {
                storeFile = file(releaseStorePath)
                storePassword = signingSetting("storePassword", "ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingSetting("keyAlias", "ANDROID_KEY_ALIAS")
                keyPassword = signingSetting("keyPassword", "ANDROID_KEY_PASSWORD")
            }
            // v2 covers every supported device on its own at minSdk 26, so AGP drops the
            // old JAR signature. v3 is added for one reason: it carries a signing
            // lineage, which is what makes it possible to rotate to a new key later
            // without users having to uninstall. Cheap insurance for exactly the
            // situation this key was created to resolve.
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Left unsigned when no keystore is configured rather than failing the build,
            // so contributors without the key can still produce a release build.
            signingConfig = if (releaseStorePath != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }

        // Release-equivalent (R8, no debuggable flag) but signed with the debug key so
        // it can actually be installed. Use this for performance measurement: debug
        // builds run un-optimized composables and are not representative.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Skippability / stability reports for the composables in this module. Off by default;
// enable with `-PcomposeMetrics` and read build/compose-reports/*-composables.txt to see
// which composables still restart on every recomposition and why.
composeCompiler {
    if (project.hasProperty("composeMetrics")) {
        metricsDestination = layout.buildDirectory.dir("compose-metrics")
        reportsDestination = layout.buildDirectory.dir("compose-reports")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    // Installs the baseline profiles shipped inside the Compose artifacts. Without it
    // those profiles are never applied and Compose code runs interpreted until the JIT
    // warms up, which shows up as jank on the first scroll after launch.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha24")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("com.materialkolor:material-kolor:4.1.1")
    implementation("com.google.android.material:material:1.14.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    compileOnly("de.robv.android.xposed:api:82")

    testImplementation("junit:junit:4.13.2")
}
