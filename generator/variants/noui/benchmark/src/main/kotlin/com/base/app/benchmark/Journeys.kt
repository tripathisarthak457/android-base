package com.base.app.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/*
 * The steps the startup benchmark and the profile generator are built from. The start screen is
 * found by its resource id (the app sets testTagsAsResourceId), never by text, so the steps work in
 * every language, and a step fails with a message when what it waits for never shows: a journey
 * that silently stops records a profile, or a timing, of something other than what it claims.
 */

/** The application id of the APK under test, passed by the build (see build.gradle.kts). */
val TARGET_APP_ID: String =
    InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: error("targetAppId is missing: run the benchmarks through Gradle, which passes it.")

private const val TIMEOUT_MS = 10_000L

/** Starts the app and waits for the screen it opens on. */
fun MacrobenchmarkScope.startToStart() {
    startActivityAndWait()
    check(device.wait(Until.hasObject(By.res("start")), TIMEOUT_MS)) {
        "The start screen never appeared."
    }
}
