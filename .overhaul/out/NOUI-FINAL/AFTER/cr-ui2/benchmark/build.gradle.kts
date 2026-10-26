import com.example.finalafter.crui2.buildlogic.config.AppConfig

/*
 * Macrobenchmarks (startup, tab switch, push and pop, predictive back, rapid Back, scroll, and the
 * recompositions of the shell's journeys), one package per journey, and the baseline profile
 * generator. They run against the dev flavour's benchmarkRelease build: R8 on,
 * profileable, signed with the debug key, installed beside a real copy of the app.
 *
 *   ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest     every benchmark, on a connected phone
 *   ./gradlew :app:generateBaselineProfile                         the profile, merged into app/src/main
 */

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
    // Not applied: it only puts build-logic on this script's classpath, for AppConfig.
    id("com.example.finalafter.crui2.android.library") apply false
}

android {
    namespace = "com.example.finalafter.crui2.benchmark"
    compileSdk = AppConfig.COMPILE_SDK

    defaultConfig {
        // Macrobenchmark and baseline profile collection need API 28.
        minSdk = maxOf(28, AppConfig.MIN_SDK)
        targetSdk = AppConfig.TARGET_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Only the dev flavour is measured: it installs beside a real copy of the app, and without a
        // choice the app's four flavours are ambiguous to Gradle.
        missingDimensionStrategy("environment", "dev")
    }

    compileOptions {
        sourceCompatibility = AppConfig.JAVA_VERSION
        targetCompatibility = AppConfig.JAVA_VERSION
    }

    targetProjectPath = ":app"
}

kotlin {
    compilerOptions {
        jvmTarget.set(AppConfig.JVM_TARGET)
    }
}

baselineProfile {
    useConnectedDevices = true
}

// The tested APK's own id, so a renamed app or a changed flavour suffix needs no edit here.
androidComponents {
    onVariants { variant ->
        val loader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { apks ->
                checkNotNull(loader.load(apks)?.applicationId) { "The tested APK has no application id." }
            },
        )
    }
}

dependencies {
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
