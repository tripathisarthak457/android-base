package com.base.app.buildlogic.kmp

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.base.app.buildlogic.config.AppConfig
import com.base.app.buildlogic.library
import com.base.app.buildlogic.libs
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A library for Android and iOS. The Android half uses AGP's multiplatform library plugin, which
 * has one variant and no flavours or BuildConfig, so everything that differs per environment
 * reaches shared code through the app's `AppEnvironment` instead.
 *
 * There is no `iosX64`: the Intel simulator is gone from current Xcode, and Kyant's backdrop, which
 * the glass feature uses, publishes no artefact for it.
 */
internal fun Project.configureKmpLibrary() {
    extensions.configure<KotlinMultiplatformExtension> {
        androidLibrary.apply {
            namespace = moduleNamespace()
            compileSdk = AppConfig.COMPILE_SDK
            minSdk = AppConfig.MIN_SDK
            compilerOptions { jvmTarget.set(AppConfig.JVM_TARGET) }
            withHostTest {
                isIncludeAndroidResources = true
            }
        }
        iosArm64()
        iosSimulatorArm64()
        configureCommonCompilation()
    }
}

/**
 * Pure Kotlin for every platform: `:core:common`. Android consumes its JVM artefact, so the module
 * carries no Android plugin at all and cannot reach an Android API by accident.
 */
internal fun Project.configureKmpPure() {
    extensions.configure<KotlinMultiplatformExtension> {
        jvm {
            compilerOptions { jvmTarget.set(AppConfig.JVM_TARGET) }
        }
        iosArm64()
        iosSimulatorArm64()
        configureCommonCompilation()
    }
}

/** `kotlin.test` and the coroutine test library in every module's common tests. */
internal fun Project.addCoroutinesTest() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.library("kotlinx-coroutines-test"))
        }
        // A compilation takes its language settings from its own source set, so an opt-in on
        // commonTest alone does not reach the Android host test that compiles the same files.
        sourceSets.matching { it.name.endsWith("Test") }.configureEach {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    // Gradle 9 fails a test task that finds no tests, which would fail modules that have none yet.
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }
}

/** The `kotlin { android { } }` block of AGP's multiplatform library plugin. */
internal val KotlinMultiplatformExtension.androidLibrary: KotlinMultiplatformAndroidLibraryTarget
    get() = (this as ExtensionAware).extensions.getByType()

private fun KotlinMultiplatformExtension.configureCommonCompilation() {
    compilerOptions {
        // Room's generated database constructor is an expect object; the language feature is
        // still marked Beta, and every module would warn about it otherwise.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
