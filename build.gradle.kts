// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.2.1" apply false
    id("com.android.library") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("org.jetbrains.compose") version "1.5.11" apply false
    id("org.jmailen.kotlinter") version "5.0.1" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.15" apply false
}

// Temporarily disable kotlinter for testing
// subprojects {
//     apply(plugin = "org.jmailen.kotlinter") // Version should be inherited from parent
// }