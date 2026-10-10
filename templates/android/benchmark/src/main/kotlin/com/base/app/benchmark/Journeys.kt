package com.base.app.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/*
 * The steps every benchmark and the profile generator are built from. UI is found by resource id
 * (the app sets testTagsAsResourceId), never by text, so the journeys work in every language. Every
 * step fails with a message when what it needs is not on screen: a journey that silently stops
 * records a profile, or a frame count, of something other than what it claims.
 */

/** The application id of the APK under test, passed by the build (see build.gradle.kts). */
val TARGET_APP_ID: String =
    InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: error("targetAppId is missing: run the benchmarks through Gradle, which passes it.")

private const val TIMEOUT_MS = 10_000L

/**
 * The dev launch options: a synthetic signed-in session and onboarding marked done, so every journey
 * starts in the tab shell without a backend. Read only by the dev flavour's code.
 */
private const val FAKE_SESSION = "perf.fakeSession"

fun MacrobenchmarkScope.startToShell() {
    startActivityAndWait { intent -> intent.putExtra(FAKE_SESSION, "true") }
    waitFor(By.res("tab_0"), "the tab bar")
}

fun MacrobenchmarkScope.tabCount(): Int {
    waitFor(By.res("tab_0"), "the tab bar")
    return generateSequence(0) { it + 1 }.takeWhile { device.hasObject(By.res("tab_$it")) }.count()
}

fun MacrobenchmarkScope.tapTab(index: Int) {
    waitFor(By.res("tab_$index"), "tab $index").click()
    check(device.wait(Until.hasObject(By.res("tab_$index").selected(true)), TIMEOUT_MS)) {
        "Tab $index was tapped but is not selected."
    }
    device.waitForIdle()
}

fun MacrobenchmarkScope.openFirstRow() {
    val list = waitFor(By.res("primary_list"), "the root list (primary_list)")
    val row = checkNotNull(list.findObject(By.clickable(true))) { "primary_list has no row to open." }
    row.click()
    check(device.wait(Until.gone(By.res("primary_list")), TIMEOUT_MS)) { "The first row did not open a screen." }
    device.waitForIdle()
}

fun MacrobenchmarkScope.openRelated() {
    val row = findScrolling(By.res("related_row")) ?: error("The detail screen has no related_row to open.")
    row.click()
    device.waitForIdle()
}

fun MacrobenchmarkScope.backToRoot() {
    device.pressBack()
    waitFor(By.res("primary_list"), "the root list after Back")
    device.waitForIdle()
}

/** n Back presses in one input call, so all but the first arrive while the first pop is running. */
fun MacrobenchmarkScope.pressBackBurst(presses: Int) {
    device.executeShellCommand("input keyevent " + "4 ".repeat(presses))
}

/** A back swipe from the leading edge, which Android turns into a predictive back gesture. */
fun MacrobenchmarkScope.edgeSwipeBack() {
    val y = device.displayHeight / 2
    device.swipe(2, y, device.displayWidth * 7 / 10, y, SWIPE_STEPS)
}

/**
 * A back swipe made of single touch events, slow enough for the back card to follow the finger
 * through many frames before it lets go, or comes back to the edge and cancels ([cancel]).
 */
fun MacrobenchmarkScope.slowEdgeSwipeBack(cancel: Boolean = false) {
    val y = device.displayHeight / 2
    val reach = device.displayWidth * 45 / 100
    val moves = (1..SLOW_SWIPE_STEPS).map { step -> 2 + reach * step / SLOW_SWIPE_STEPS }
    val path = if (cancel) moves + moves.reversed().drop(1) + 2 else moves
    val events = buildList {
        add("input motionevent DOWN 2 $y")
        path.forEach { x -> add("input motionevent MOVE $x $y") }
        add("input motionevent UP ${path.last()} $y")
    }
    device.executeShellCommand(events.joinToString("; "))
    device.waitForIdle()
}

/** Opens the first row and starts a back swipe while the new screen is still sliding in. */
fun MacrobenchmarkScope.backSwipeDuringPush() {
    val list = waitFor(By.res("primary_list"), "the root list (primary_list)")
    val row = checkNotNull(list.findObject(By.clickable(true))) { "primary_list has no row to open." }
    val y = device.displayHeight / 2
    val (x, rowY) = row.visibleCenter.let { it.x to it.y }
    device.executeShellCommand("input tap $x $rowY; input swipe 2 $y ${device.displayWidth * 6 / 10} $y 160")
    assertAtRoot()
}

/** Pulls the primary list down from its top until it refreshes. */
fun MacrobenchmarkScope.pullToRefresh() {
    val list = waitFor(By.res("primary_list"), "the root list (primary_list)")
    val bounds = list.visibleBounds
    val x = bounds.centerX()
    device.swipe(x, bounds.top + bounds.height() / 10, x, bounds.top + bounds.height() * 6 / 10, PULL_STEPS)
    device.waitForIdle()
}

fun MacrobenchmarkScope.flingPrimaryList() {
    val list = waitFor(By.res("primary_list"), "the root list (primary_list)")
    // Clear of the screen edges, so the fling is not taken for a back gesture.
    list.setGestureMargin(device.displayWidth / 5)
    list.fling(Direction.DOWN)
    device.waitForIdle()
    list.fling(Direction.UP)
    device.waitForIdle()
}

fun MacrobenchmarkScope.assertAtRoot() {
    waitFor(By.res("primary_list"), "the root list")
    check(device.currentPackageName == TARGET_APP_ID) {
        "The app is no longer in front: Back presses past the root left it."
    }
}

private fun MacrobenchmarkScope.waitFor(selector: BySelector, what: String): UiObject2 =
    device.wait(Until.findObject(selector), TIMEOUT_MS)
        ?: error("$what is not on screen; the journey cannot continue.")

private fun MacrobenchmarkScope.findScrolling(selector: BySelector): UiObject2? {
    repeat(SCROLL_ATTEMPTS) {
        device.wait(Until.findObject(selector), SHORT_WAIT_MS)?.let { return it }
        val scrollable = device.findObject(By.scrollable(true)) ?: return null
        scrollable.setGestureMargin(device.displayWidth / 5)
        scrollable.scroll(Direction.DOWN, 0.8f)
    }
    return device.findObject(selector)
}

private const val SWIPE_STEPS = 20
private const val SLOW_SWIPE_STEPS = 24
private const val PULL_STEPS = 60
private const val SCROLL_ATTEMPTS = 3
private const val SHORT_WAIT_MS = 1_000L
