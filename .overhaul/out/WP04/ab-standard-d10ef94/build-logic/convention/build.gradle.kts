/*
 * The convention plugins. Every module in the main build applies one or more of these instead of
 * configuring the Android and Kotlin extensions itself.
 */

plugins {
    `kotlin-dsl`
}

group = "com.example.wp04.abstd.buildlogic"

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

    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.example.wp04.abstd.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationFlavors") {
            id = "com.example.wp04.abstd.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.example.wp04.abstd.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "com.example.wp04.abstd.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "com.example.wp04.abstd.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "com.example.wp04.abstd.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidData") {
            id = "com.example.wp04.abstd.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("jvmLibrary") {
            id = "com.example.wp04.abstd.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidRoom") {
            id = "com.example.wp04.abstd.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}
