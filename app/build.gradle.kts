val composeVersion = "1.5.11" // Same version as the compose plugin
val lifecycleVersion = "2.7.0" // Same version as other lifecycle dependencies

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
}

android {
    buildToolsVersion = "34.0.0"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dessalines.thumbkey"
        minSdk = 21
        targetSdk = 34
        versionCode = 127
        versionName = "4.0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    // Necessary for izzyondroid releases
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    if (project.hasProperty("RELEASE_STORE_FILE")) {
        signingConfigs {
            create("release") {
                storeFile = file(project.property("RELEASE_STORE_FILE")!!)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String?
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String?

                // Optional, specify signing versions used
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }
    buildTypes {
        release {
            if (project.hasProperty("RELEASE_STORE_FILE")) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = " (DEBUG)"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjvm-default=all-compatibility", "-opt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }
    namespace = "com.dessalines.thumbkey"
}

dependencies {
    // Exporting / importing DB helper
    implementation("com.github.dessalines:room-db-export-import:0.1.0")

    // Compose
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(compose.uiTooling)

    // Activities
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // LiveData and Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Emoji Picker
    implementation("androidx.emoji2:emoji2-emojipicker:1.4.0")

    // Markdown
    implementation("com.github.jeziellago:compose-markdown:0.5.4")

    // Preferences
    implementation("me.zhanghai.compose.preference:library:1.1.1")

    // Input switcher
    implementation("com.louiscad.splitties:splitties-systemservices:3.0.0")
    implementation("com.louiscad.splitties:splitties-views:3.0.0")

    // Room
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // App compat
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.compose.runtime:runtime-livedata:${composeVersion}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${lifecycleVersion}")
    implementation("androidx.compose.foundation:foundation:${composeVersion}")
}