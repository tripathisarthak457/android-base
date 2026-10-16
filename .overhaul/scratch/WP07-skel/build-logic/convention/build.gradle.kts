/*
 * The convention plugins. Every module in the main build applies one or more of these instead of
 * configuring the Kotlin and Android extensions itself.
 */

plugins {
    `kotlin-dsl`
}

group = "com.base.app.buildlogic"

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
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.compose.multiplatform.gradlePlugin)

    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.base.app.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("kmpPure") {
            id = "com.base.app.kmp.pure"
            implementationClass = "KmpPureConventionPlugin"
        }
        register("kmpLibrary") {
            id = "com.base.app.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "com.base.app.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "com.base.app.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("kmpData") {
            id = "com.base.app.kmp.data"
            implementationClass = "KmpDataConventionPlugin"
        }
    }
}
