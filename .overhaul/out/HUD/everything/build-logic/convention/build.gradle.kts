/*
 * The convention plugins. Every module in the main build applies one or more of these instead of
 * configuring the Android and Kotlin extensions itself.
 */

plugins {
    `kotlin-dsl`
}

group = "com.example.hud.everything.buildlogic"

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
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.serialization.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.google.services.gradlePlugin)
    compileOnly(libs.firebase.crashlytics.gradlePlugin)
    compileOnly(libs.room3.gradlePlugin)

    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.example.hud.everything.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationFlavors") {
            id = "com.example.hud.everything.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.example.hud.everything.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "com.example.hud.everything.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "com.example.hud.everything.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "com.example.hud.everything.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidData") {
            id = "com.example.hud.everything.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("jvmLibrary") {
            id = "com.example.hud.everything.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidFirebase") {
            id = "com.example.hud.everything.android.firebase"
            implementationClass = "AndroidFirebaseConventionPlugin"
        }
        register("androidLicenses") {
            id = "com.example.hud.everything.android.licenses"
            implementationClass = "AndroidLicensesConventionPlugin"
        }
        register("androidRoom") {
            id = "com.example.hud.everything.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}
