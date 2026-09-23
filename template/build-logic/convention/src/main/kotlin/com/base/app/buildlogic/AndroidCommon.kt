package com.base.app.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * The Android and Kotlin configuration every module shares, applied once by
 * [AndroidApplicationConventionPlugin] and [AndroidLibraryConventionPlugin].
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
        // android.util.Log throws "not mocked" in unit tests; defaults make it a no-op instead.
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

    // Gradle 9 fails a test task with no tests, which would fail modules that have none yet.
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
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        // `runTest`, `TestScope` and the test dispatchers are all still marked experimental, and
        // every test that touches a coroutine warns until it opts in.
        if (name.endsWith("UnitTestKotlin") || name.endsWith("AndroidTestKotlin")) {
            compilerOptions.optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}
