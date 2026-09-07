package com.base.app.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * Every component in the design system, rendered to a PNG in both themes and compared against the
 * one committed beside it.
 *
 * This is the only check that can fail on a change nobody can describe in words: an accent that
 * lost its contrast in dark mode, a text style that grew two pixels, a shape radius applied to one
 * component and not its neighbour. `./gradlew build` stays green through every one of them, and a
 * reviewer reading a diff of hex values cannot evaluate the result either.
 *
 *   ./gradlew :catalog:verifyRoborazziDebug   fail on any difference, and write a diff image
 *   ./gradlew :catalog:recordRoborazziDebug   accept the current rendering as the new truth
 *
 * `record` rewrites src/test/screenshots. Run it when the change to a component was the point,
 * look at the images, and commit them with it — the pull request then shows what actually moved.
 *
 * The pages come from CatalogSection, which is the same list the installed catalog app renders.
 * There is no second inventory of components to keep in step: a page added to that enum is
 * screenshot-tested from the moment it exists.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = SCREENSHOT_DEVICE)
class CatalogScreenshotTest(
    private val section: CatalogSection,
    private val mode: ThemeMode,
) {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun renders() {
        // Stopping the clock is what makes this terminate. A page carrying a spinner, a blinking
        // cursor or a shimmer never goes idle, and every Compose test API waits for idle before it
        // does anything — so the capture hangs rather than fails, which is worse. With the clock
        // held at zero the frame that gets captured is the first one, identically, every time.
        compose.mainClock.autoAdvance = false

        compose.setContent {
            AppTheme(mode = mode) {
                Box(
                    modifier = Modifier
                        .background(AppTheme.colors.background)
                        .padding(AppTheme.spacing.lg),
                ) {
                    section.Content()
                }
            }
        }

        val root = compose.onRoot()
        // The capture is clipped to the window, silently. A page that outgrows it would lose
        // whatever fell off the bottom and the image would still look plausible, so the height
        // is asserted rather than trusted. At mdpi one dp is one pixel, which is what lets
        // these two be compared directly.
        val rendered = root.fetchSemanticsNode().size.height
        check(rendered < SCREENSHOT_HEIGHT_DP) {
            "The ${section.name} page is ${rendered}px tall and the window is " +
                "${SCREENSHOT_HEIGHT_DP}px, so the bottom of it would not be captured. " +
                "Raise SCREENSHOT_HEIGHT_DP and the height in SCREENSHOT_DEVICE together."
        }
        root.captureRoboImage("src/test/screenshots/${section.name}-${mode.name}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun pages(): List<Array<Any>> =
            CatalogSection.entries
                .filterNot { it in SKIPPED }
                .flatMap { section ->
                    listOf(ThemeMode.Light, ThemeMode.Dark).map { arrayOf<Any>(section, it) }
                }

        // Both pages exist to be watched moving. A still first frame of either asserts nothing the
        // pages below do not already cover.
        private val SKIPPED = setOf(CatalogSection.Motion, CatalogSection.Feel)
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds
// with. The level only decides which platform renders the page; nothing here reads it.
private const val SCREENSHOT_SDK = 35

// A phone's width, and a window tall enough that no page reaches the bottom of it — the test
// checks that rather than assuming it. mdpi keeps one dp to one pixel, so the images stay small
// and the height assertion needs no density conversion. The two must agree.
private const val SCREENSHOT_HEIGHT_DP = 1600
private const val SCREENSHOT_DEVICE = "w412dp-h1600dp-mdpi"
