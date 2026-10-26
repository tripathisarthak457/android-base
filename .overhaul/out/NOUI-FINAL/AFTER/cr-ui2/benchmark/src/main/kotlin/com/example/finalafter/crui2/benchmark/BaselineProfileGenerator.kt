package com.example.finalafter.crui2.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Records the code a person's first minutes run, so ART compiles it at install instead of
 * interpreting it while they watch:
 *
 * ```
 * ./gradlew :app:generateBaselineProfile
 * python3 tools/perf/split_profile.py
 * ```
 *
 * Startup goes into the startup profile too, which lays out the primary dex; the journeys do not, so
 * that file stays the startup path alone. The journeys cover what a session does in its first
 * minutes, the back card's track, cancel and commit included, so none of it runs interpreted.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = TARGET_APP_ID, includeInStartupProfile = true) {
        pressHome()
        startToShell()
    }

    @Test
    fun journeys() = rule.collect(packageName = TARGET_APP_ID) {
        startToShell()
        (1 until tabCount()).forEach(::tapTab)
        tapTab(0)
        openFirstRow()
        openRelated()
        backToRelatedThenRoot()
        openFirstRow()
        slowEdgeSwipeBack(cancel = true)
        slowEdgeSwipeBack()
        assertAtRoot()
        openFirstRow()
        edgeSwipeBack()
        assertAtRoot()
        backSwipeDuringPush()
        openFirstRow()
        openRelated()
        pressBackBurst(3)
        assertAtRoot()
        pullToRefresh()
        flingPrimaryList()
        // A tap on the selected tab brings its root list back to the top.
        tapTab(0)
    }

    private fun MacrobenchmarkScope.backToRelatedThenRoot() {
        device.pressBack()
        device.waitForIdle()
        backToRoot()
    }
}
