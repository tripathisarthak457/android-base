/*
 * The convention plugins. Every module in the main build applies one or more of these instead of
 * configuring the Android and Kotlin extensions itself.
 */

plugins {
    `kotlin-dsl`
}

group = "com.example.everything.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Only plugins whose classes the conventions name. The rest are applied by id, from the
    // classpath the root build's `apply false` entries give every module.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.firebase.crashlytics.gradlePlugin)
    compileOnly(libs.room3.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)

    testImplementation(libs.junit)
}

tasks.validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.example.everything.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationFlavors") {
            id = "com.example.everything.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.example.everything.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "com.example.everything.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "com.example.everything.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidScreenshots") {
            id = "com.example.everything.android.screenshots"
            implementationClass = "AndroidScreenshotsConventionPlugin"
        }
        register("androidFeature") {
            id = "com.example.everything.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidData") {
            id = "com.example.everything.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("jvmLibrary") {
            id = "com.example.everything.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidFirebase") {
            id = "com.example.everything.android.firebase"
            implementationClass = "AndroidFirebaseConventionPlugin"
        }
        register("androidLicenses") {
            id = "com.example.everything.android.licenses"
            implementationClass = "AndroidLicensesConventionPlugin"
        }
        register("androidRoom") {
            id = "com.example.everything.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}
