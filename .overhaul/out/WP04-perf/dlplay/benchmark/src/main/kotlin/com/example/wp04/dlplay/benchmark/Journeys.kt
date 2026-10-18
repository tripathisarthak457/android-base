package com.example.wp04.dlplay.benchmark

import android.graphics.Point
import android.graphics.Rect
import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
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
    awaitTransition()
}

/** Opens the first related row of the detail screen on top. */
fun MacrobenchmarkScope.openRelated() {
    val row = findScrolling(By.res("related_row")) ?: error("The detail screen has no related_row to open.")
    row.click()
    // The screen the row was on is now covered by the one it opened, which takes it out of the
    // accessibility tree.
    check(row.awaitGone()) { "The related row did not open a screen." }
    awaitTransition()
}

fun MacrobenchmarkScope.backToRoot() {
    device.pressBack()
    waitFor(By.res("primary_list"), "the root list after Back")
    awaitTransition()
}

/** n Back presses in one input call, so all but the first arrive while the first pop is running. */
fun MacrobenchmarkScope.pressBackBurst(presses: Int) {
    device.executeShellCommand("input keyevent " + "4 ".repeat(presses))
}

/**
 * A back swipe from the leading edge of a screen opened from the root list, which Android turns into
 * a predictive back gesture.
 */
fun MacrobenchmarkScope.edgeSwipeBack() {
    val y = device.displayHeight / 2
    device.swipe(2, y, device.displayWidth * 7 / 10, y, SWIPE_STEPS)
    assertSwipedBack()
    awaitTransition()
}

/**
 * A back swipe on a screen opened from the root list, slow enough for the back card to follow the
 * finger through many frames before it lets go, or comes back to the edge and cancels ([cancel]).
 */
fun MacrobenchmarkScope.slowEdgeSwipeBack(cancel: Boolean = false) {
    val y = device.displayHeight / 2
    val reach = device.displayWidth * 45 / 100
    val out = (0..SLOW_SWIPE_POINTS).map { step -> Point(2 + reach * step / SLOW_SWIPE_POINTS, y) }
    val path = if (cancel) out + out.reversed().drop(1) else out
    check(device.swipe(path.toTypedArray(), SLOW_SWIPE_SEGMENT_STEPS)) { "The back swipe was not delivered." }
    if (!cancel) assertSwipedBack()
    awaitTransition()
}

/**
 * Opens the first row and starts a back swipe while the new screen is still sliding in, as a person
 * who changes their mind does: the swipe begins [SWIPE_AFTER_TAP_MS] after the tap.
 */
fun MacrobenchmarkScope.backSwipeDuringPush() {
    val list = waitFor(By.res("primary_list"), "the root list (primary_list)")
    val row = checkNotNull(list.findObject(By.clickable(true))) { "primary_list has no row to open." }
    val y = device.displayHeight / 2
    row.click()
    SystemClock.sleep(SWIPE_AFTER_TAP_MS)
    device.swipe(2, y, device.displayWidth * 6 / 10, y, DURING_PUSH_SWIPE_STEPS)
    assertSwipedBack()
    awaitTransition()
}

/** Pulls the primary list down until it refreshes; the list must be at its top. */
fun MacrobenchmarkScope.pullToRefresh() {
    val area = waitFor(By.res("primary_list"), "the root list (primary_list)").gestureArea()
    device.swipe(area.centerX(), area.top, area.centerX(), area.top + area.height() / 2, PULL_STEPS)
    device.waitForIdle()
}

/** Flings the primary list toward its end, then back toward its start. */
fun MacrobenchmarkScope.flingPrimaryList() {
    val area = waitFor(By.res("primary_list"), "the root list (primary_list)").gestureArea()
    device.swipe(area.centerX(), area.bottom, area.centerX(), area.top, FLING_STEPS)
    device.waitForIdle()
    device.swipe(area.centerX(), area.top, area.centerX(), area.bottom, FLING_STEPS)
    device.waitForIdle()
}

fun MacrobenchmarkScope.assertAtRoot() {
    waitFor(By.res("primary_list"), "the root list")
    check(device.currentPackageName == TARGET_APP_ID) {
        "The app is no longer in front: Back presses past the root left it."
    }
    awaitTransition()
}

/*
 * Under 3-button or 2-button navigation an edge swipe is an ordinary drag on the screen, so a swipe
 * that did not go back names the likely cause instead of leaving the next step to fail.
 */
private fun MacrobenchmarkScope.assertSwipedBack() {
    check(device.wait(Until.hasObject(By.res("primary_list")), TIMEOUT_MS)) {
        "The back swipe did not return to the root list (primary_list). Back swipes need gesture " +
            "navigation: Settings > System > Gestures > System navigation."
    }
}

private fun MacrobenchmarkScope.waitFor(selector: BySelector, what: String): UiObject2 =
    device.wait(Until.findObject(selector), TIMEOUT_MS)
        ?: error("$what is not on screen; the journey cannot continue.")

private fun MacrobenchmarkScope.findScrolling(selector: BySelector): UiObject2? {
    repeat(SCROLL_ATTEMPTS) {
        device.wait(Until.findObject(selector), SHORT_WAIT_MS)?.let { return it }
        val area = device.findObject(By.scrollable(true))?.gestureArea() ?: return null
        device.swipe(area.centerX(), area.bottom, area.centerX(), area.top, SCROLL_STEPS)
        device.waitForIdle()
    }
    return device.findObject(selector)
}

/*
 * Scrolling content runs under the floating tab bar, and back swipes start at the screen's side
 * edges, so a gesture on it keeps to its middle: clear of the bar below and of both edges. A
 * gesture that starts on the bar scrolls nothing.
 */
private fun UiObject2.gestureArea(): Rect {
    val bounds = visibleBounds
    val side = bounds.width() / 5
    return Rect(
        bounds.left + side,
        bounds.top + bounds.height() / 10,
        bounds.right - side,
        bounds.bottom - bounds.height() / 4,
    )
}

/*
 * Every move of the shell rides the screen spring (response 0.344 s, no bounce), which comes to rest
 * within about half a second of the frame that composes it. A step that starts one waits that long,
 * so the next step acts on a screen at rest: a push that arrives while a push still moves is
 * dropped, as two rows tapped within one transition open only the first.
 */
private fun awaitTransition() = SystemClock.sleep(TRANSITION_MS)

/** Whether this node leaves the accessibility tree within [TIMEOUT_MS]. */
private fun UiObject2.awaitGone(): Boolean {
    val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
    while (SystemClock.uptimeMillis() < deadline) {
        try {
            visibleBounds
        } catch (_: StaleObjectException) {
            return true
        }
        SystemClock.sleep(POLL_MS)
    }
    return false
}

// UiDevice injects one touch event per step, about 5 ms apart.
private const val SWIPE_STEPS = 20
private const val SLOW_SWIPE_POINTS = 24
private const val SLOW_SWIPE_SEGMENT_STEPS = 5
private const val DURING_PUSH_SWIPE_STEPS = 32
private const val SWIPE_AFTER_TAP_MS = 80L
private const val PULL_STEPS = 60
private const val FLING_STEPS = 8
private const val SCROLL_STEPS = 40
private const val SCROLL_ATTEMPTS = 3
private const val SHORT_WAIT_MS = 1_000L
private const val POLL_MS = 50L
private const val TRANSITION_MS = 600L
