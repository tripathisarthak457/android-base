package com.base.app.core.devtools.badge

import android.view.View
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.base.app.core.common.environment.AppEnvironment
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.devtools.DevTools
import com.base.app.core.devtools.log.DevToolsLog
import com.base.app.core.devtools.page.DevToolsPage
import com.base.app.core.devtools.screen.CurrentScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs
// <opt:flags>
import com.base.app.core.flags.FlagOverrides
import com.base.app.core.flags.LocalFeatureFlags
// </opt:flags>

@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class DevToolsOverlayTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a drag moves the badge with the finger and it settles on the side it was thrown to`() {
        compose.setContent { AppTheme { DevToolsOverlay(devTools()) } }
        val badge = compose.onNodeWithTag("devtools_badge")
        val before = badge.getUnclippedBoundsInRoot()
        assertTrue("The badge starts docked at the end edge.", before.left > 100.dp)

        badge.performTouchInput {
            swipe(start = center, end = center + Offset(-200.dp.toPx(), -100.dp.toPx()), durationMillis = 600)
        }
        compose.waitForIdle()

        val after = badge.getUnclippedBoundsInRoot()
        assertEquals("It docks at the start edge.", 0f, after.left.value, 1f)
        // The touch slop is not part of the drag, so it moves a little less than the finger.
        assertNear(before.top - after.top, expected = 100.dp, tolerance = 12.dp)
    }

    @Test
    fun `the system back gesture leaves the badge alone, wherever it is`() {
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            AppTheme { DevToolsOverlay(devTools()) }
        }
        val badge = compose.onNodeWithTag("devtools_badge")
        assertExcluded(view, badge)

        badge.performTouchInput {
            swipe(start = center, end = center + Offset(-200.dp.toPx(), -100.dp.toPx()), durationMillis = 600)
        }
        compose.waitForIdle()

        assertExcluded(view, badge)
    }

    @Test
    fun `a page's line under the badge opens the panel on that page`() {
        val page = DevToolsPage(
            key = "probe",
            title = android.R.string.ok,
            content = { BasicText("probe page content") },
            badgeContent = { openPage ->
                BasicText("probe line", Modifier.testTag("probe_line").pressable(onClick = openPage, feedback = PressFeedback.Scale))
            },
        )
        compose.setContent { AppTheme { DevToolsOverlay(devTools(pages = setOf(page))) } }

        compose.onNodeWithTag("probe_line").performClick()

        compose.onNodeWithText("probe page content").assertIsDisplayed()
    }

    private fun devTools(pages: Set<DevToolsPage> = emptySet()) = DevTools(
        environment = AppEnvironment(
            name = "dev",
            versionName = "1.0",
            applicationId = "com.example",
            apiBaseUrl = "https://api.example.com",
            isDebugBuild = true,
        ),
        log = DevToolsLog(),
        currentScreen = CurrentScreen(),
        pages = pages,
        // <opt:flags>
        flags = LocalFeatureFlags(FlagOverrides()),
        flagOverrides = FlagOverrides(),
        // </opt:flags>
    )

    private fun assertExcluded(view: View, badge: SemanticsNodeInteraction) {
        val bounds = badge.fetchSemanticsNode().boundsInRoot
        val excluded = view.systemGestureExclusionRects.map { it.toComposeRect() }
        assertTrue("$bounds is not within the excluded $excluded.", excluded.any { it.covers(bounds) })
    }

    // The exclusion is in whole pixels.
    private fun Rect.covers(other: Rect) =
        left <= other.left + 1 && top <= other.top + 1 && right >= other.right - 1 && bottom >= other.bottom - 1

    private fun assertNear(actual: Dp, expected: Dp, tolerance: Dp) =
        assertTrue("Moved $actual, expected about $expected.", abs((actual - expected).value) <= tolerance.value)
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
