/*
 * Baseline profile generation, plus a place for startup benchmarks.
 *
 * The generated profile is what lets ART pre-compile the startup path at install time instead of
 * interpreting it on first launch. On a mid-range device that is typically a 20-30% cut in cold
 * start, for no code change — it is the highest-value performance work available, and it is
 * entirely a build concern.
 *
 * Run `./gradlew :benchmark:generateBaselineProfile` against a rooted emulator or a physical
 * device, and commit the result under `app/src/main/baseline-prof.txt`.
 */

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.base.app.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
