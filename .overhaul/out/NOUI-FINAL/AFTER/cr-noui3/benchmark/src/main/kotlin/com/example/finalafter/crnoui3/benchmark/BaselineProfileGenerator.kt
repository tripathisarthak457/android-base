package com.example.finalafter.crnoui3.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Records the code a cold start runs, so ART compiles it at install instead of interpreting it
 * while the person waits:
 *
 * ```
 * ./gradlew :app:generateBaselineProfile
 * ```
 *
 * It goes into the startup profile too, which lays out the primary dex. When the project has
 * screens of its own, add a journey through the first minutes of a session here, collected
 * without `includeInStartupProfile`, and record the profile again.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = TARGET_APP_ID, includeInStartupProfile = true) {
        pressHome()
        startToStart()
    }
}
