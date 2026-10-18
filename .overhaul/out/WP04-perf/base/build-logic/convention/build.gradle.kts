/*
 * The convention plugins. Every module in the main build applies one or more of these instead of
 * configuring the Android and Kotlin extensions itself.
 */

plugins {
    `kotlin-dsl`
}

group = "com.example.wp04.base.buildlogic"

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
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.example.wp04.base.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationFlavors") {
            id = "com.example.wp04.base.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.example.wp04.base.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "com.example.wp04.base.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "com.example.wp04.base.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "com.example.wp04.base.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidData") {
            id = "com.example.wp04.base.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("jvmLibrary") {
            id = "com.example.wp04.base.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidFirebase") {
            id = "com.example.wp04.base.android.firebase"
            implementationClass = "AndroidFirebaseConventionPlugin"
        }
        register("androidRoom") {
            id = "com.example.wp04.base.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}
