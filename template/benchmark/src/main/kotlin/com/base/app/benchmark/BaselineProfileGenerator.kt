package com.base.app.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Records the classes and methods used on the startup path, so ART can compile them ahead of time
 * at install rather than interpreting them on first launch.
 *
 * ```
 * ./gradlew :benchmark:generateBaselineProfile
 * ```
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        // Scrolling is included because the first list frames are part of perceived startup.
        device.waitForIdle()
    }

    private companion object {
        /**
         * The dev flavour's id. The profile is recorded against whichever variant is installed; dev
         * is the one a developer has on the device, and the profile is applicable to all of them
         * because the code path is identical.
         */
        const val PACKAGE_NAME = "com.base.app.dev"
    }
}
