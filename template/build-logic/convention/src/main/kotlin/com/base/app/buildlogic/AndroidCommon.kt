package com.base.app.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * The Android and Kotlin configuration every module shares, applied once by
 * [AndroidApplicationConventionPlugin] and [AndroidLibraryConventionPlugin].
 *
 * Nothing environment-specific belongs here — see [configureFlavors] for that. This is only the
 * settings that would otherwise be copy-pasted identically into every build file and then drift
 * the first time one of them is edited alone.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.compileSdk = AppConfig.COMPILE_SDK

    extension.defaultConfig.apply {
        minSdk = AppConfig.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    extension.compileOptions.apply {
        sourceCompatibility = AppConfig.JAVA_VERSION
        targetCompatibility = AppConfig.JAVA_VERSION
        isCoreLibraryDesugaringEnabled = AppConfig.NEEDS_CORE_LIBRARY_DESUGARING
    }

    extension.testOptions.unitTests.apply {
        // android.util.Log is an unimplemented stub off-device and throws "not mocked" on every
        // call, so any class that logs fails its test for a reason unrelated to the logic under
        // test. Returning defaults makes logging a silent no-op in unit tests instead.
        isReturnDefaultValues = true
        isIncludeAndroidResources = true
    }

    extension.packaging.resources.apply {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/*.version"
        excludes += "/META-INF/*.kotlin_module"
        excludes += "DebugProbesKt.bin"
        excludes += "kotlin-tooling-metadata.json"
    }

    // Gradle 9 fails a test task that finds no tests, on the assumption it is misconfigured. For
    // a module that legitimately has none yet — a design system, a DI-only module — that turns
    // `./gradlew build` into a failure with nothing wrong. The genuine misconfiguration this
    // guards against (a broken runner) surfaces as a compile or class-loading error anyway.
    tasks.withType(Test::class.java).configureEach {
        failOnNoDiscoveredTests.set(false)
    }

    configureKotlinCompiler()

    if (AppConfig.NEEDS_CORE_LIBRARY_DESUGARING) {
        dependencies {
            add("coreLibraryDesugaring", libs.library("desugar-jdk-libs"))
        }
    }
}

private fun Project.configureKotlinCompiler() {
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(AppConfig.JVM_TARGET)
            // Kotlin 2.2 changed where an annotation on a constructor parameter lands by default
            // and warns on every one until told explicitly. `param-property` is the behaviour the
            // annotations in this project (@Inject, @SerialName, @Json*) already assume.
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
}
