/*
 * The convention plugins.
 *
 * Every module in the main build applies one or more of these instead of configuring the Android
 * and Kotlin extensions itself. That is the difference between changing the JVM target in one
 * place and changing it in twenty-two build files, one of which you will miss.
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
    compileOnly(libs.kotlin.serialization.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    // <opt:firebase>
    compileOnly(libs.google.services.gradlePlugin)
    // </opt:firebase>
    // <opt:crashlytics>
    compileOnly(libs.firebase.crashlytics.gradlePlugin)
    // </opt:crashlytics>
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.base.app.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationFlavors") {
            id = "com.base.app.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.base.app.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "com.base.app.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "com.base.app.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "com.base.app.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidData") {
            id = "com.base.app.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("jvmLibrary") {
            id = "com.base.app.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        // <opt:firebase>
        register("androidFirebase") {
            id = "com.base.app.android.firebase"
            implementationClass = "AndroidFirebaseConventionPlugin"
        }
        // </opt:firebase>
        // <opt:room>
        register("androidRoom") {
            id = "com.base.app.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        // </opt:room>
    }
}
