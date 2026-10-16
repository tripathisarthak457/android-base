package com.base.app.buildlogic.kmp

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.base.app.buildlogic.config.AppConfig
import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * The targets every library module compiles for: Android, and the two iOS device kinds a Mac can
 * run (no Intel simulator: the glass and image libraries this template can add have dropped it).
 */
internal fun Project.configureKmpLibrary() {
    extensions.configure<KotlinMultiplatformExtension> {
        // AGP 9 registers the Android target as the `android` extension of `kotlin`; the older
        // `androidLibrary` name is deprecated.
        (this as ExtensionAware).extensions.getByType<KotlinMultiplatformAndroidLibraryTarget>()
            .apply {
                namespace = moduleNamespace()
                compileSdk = AppConfig.COMPILE_SDK
                minSdk = AppConfig.MIN_SDK
                compilerOptions { jvmTarget.set(AppConfig.JVM_TARGET) }
                withHostTest {
                    // Compose resources are read through the Android asset manager, which a host
                    // test only has when the merged resources are on its classpath.
                    isIncludeAndroidResources = true
                }
            }
        iosArm64()
        iosSimulatorArm64()

        configureCommon()
    }
}

/**
 * A module with no Android or Compose code at all: `:core:common`. It has a JVM target rather than
 * an Android one, which Android modules consume like any Kotlin library, so the module graph can
 * refuse every Android plugin on it.
 */
internal fun Project.configureKmpPure() {
    extensions.configure<KotlinMultiplatformExtension> {
        jvm {
            compilerOptions { jvmTarget.set(AppConfig.JVM_TARGET) }
        }
        iosArm64()
        iosSimulatorArm64()

        configureCommon()
    }
}

private fun KotlinMultiplatformExtension.configureCommon() {
    compilerOptions {
        // `expect class` is still flagged as beta; the template's seams (the network monitor, the
        // database builder) are classes because each platform's has state.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation(kotlin("test"))
    }
}

/** Adds the coroutine test library to `commonTest`. */
internal fun Project.addCoroutinesTest() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonTest").dependencies {
            implementation(libs.library("kotlinx-coroutines-test"))
        }
    }
}
