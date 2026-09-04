package com.base.app.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Records the classes and methods used on the startup path, so ART can compile them ahead of time
 * at install rather than interpreting them on first launch.
 *
 * On a mid-range device this is typically a 20-30% cut in cold start for no code change at all,
 * which makes it the highest-value performance work available and entirely a build concern.
 *
 * Run it against a rooted emulator or a physical device:
 *
 * ```
 * ./gradlew :benchmark:generateBaselineProfile
 * ```
 *
 * and commit the result at `app/src/main/baseline-prof.txt`. Re-record it when the startup path
 * changes materially — a new splash, a different start destination — not on every release; a
 * stale profile degrades gracefully, it does not break anything.
 *
 * `includeInStartupProfile = true` on the startup interaction also produces a *startup* profile,
 * which is the subset ART compiles most aggressively.
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

        // Scrolling is included on purpose: the first frames of the first list are part of what
        // the user perceives as "startup", and they exercise a large amount of Compose that a
        // bare launch never touches.
        device.waitForIdle()
    }

    private companion object {
        /**
         * The dev flavour's id. The profile is recorded against whichever variant is installed;
         * dev is the one a developer has on the device, and the profile is applicable to all of
         * them because the code path is identical.
         */
        const val PACKAGE_NAME = "com.base.app.dev"
    }
}
