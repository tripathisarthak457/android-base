package com.example.standard.buildlogic.android

import com.android.build.api.dsl.CommonExtension
import com.example.standard.buildlogic.config.AppConfig
import com.example.standard.buildlogic.library
import com.example.standard.buildlogic.libs
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
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
    }

    extension.compileOptions.apply {
        sourceCompatibility = AppConfig.JAVA_VERSION
        targetCompatibility = AppConfig.JAVA_VERSION
        isCoreLibraryDesugaringEnabled = AppConfig.NEEDS_CORE_LIBRARY_DESUGARING
    }

    // android.util.Log throws "not mocked" in unit tests; defaults make it a no-op instead.
    extension.testOptions.unitTests.isReturnDefaultValues = true

    // Robolectric runs unit tests against the merged manifest and resources, so a module whose
    // tests use it gets them; every other module's unit tests skip that merge.
    onRobolectricTests {
        extension.testOptions.unitTests.isIncludeAndroidResources = true
    }

    extension.packaging.resources.apply {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/*.kotlin_module"
        excludes += "DebugProbesKt.bin"
        excludes += "kotlin-tooling-metadata.json"
    }

    // Android runs on phones, foldables, tablets, desktop windows and cars. An activity locked to
    // one orientation, or refusing to resize, letterboxes on most of them, so either fails the build.
    extension.lint.error += setOf(
        "LockedOrientationActivity",
        "SourceLockedOrientationActivity",
        "NonResizeableActivity",
        // A resource without its module's prefix (set by the library convention) can silently
        // replace another module's resource of the same name when the app merges them.
        "ResourceName",
    )

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

/** Test libraries for the test source sets a module actually has. */
internal fun Project.configureTestDependencies() {
    dependencies {
        if (file("src/test").exists() && path != ":core:testing") {
            add("testImplementation", project(":core:testing"))
        }
        if (file("src/androidTest").exists()) {
            add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
            add("androidTestImplementation", libs.library("androidx-test-runner"))
        }
    }
}

/**
 * Runs [action] once this module's unit tests depend on Robolectric, directly or through
 * `:core:testing:compose`. Dependencies are declared after the conventions apply, so this waits
 * for them instead of looking now.
 */
internal fun Project.onRobolectricTests(action: () -> Unit) {
    var applied = false
    configurations.named("testImplementation").configure {
        dependencies.configureEach {
            val robolectric = group == "org.robolectric" || (this is ProjectDependency && path == COMPOSE_TESTING)
            if (robolectric && !applied) {
                applied = true
                action()
            }
        }
    }
}

private const val COMPOSE_TESTING = ":core:testing:compose"
