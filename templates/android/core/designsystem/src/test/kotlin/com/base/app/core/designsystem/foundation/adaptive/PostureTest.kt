package com.base.app.core.designsystem.foundation.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TestSdk
import com.base.app.core.designsystem.component.collections.AppImageViewer
import com.base.app.core.designsystem.component.overlay.AppSheet
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A half-open foldable: ADAPTIVE-PLAN 7. At mdpi a pixel is a dp, so the hinge reads in either. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w800dp-h840dp-mdpi")
class PostureTest {

    @get:Rule
    val rule = createComposeRule()

    private fun setContent(posture: WindowPosture, content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.setContent { AppTheme(mode = ThemeMode.Light, reduceMotion = true, posture = posture, content = content) }
    }

    private fun twoPane(posture: WindowPosture) = setContent(posture) {
        AppTwoPane(
            first = { Box(Modifier.fillMaxSize().testTag("first")) },
            second = { Box(Modifier.fillMaxSize().testTag("second")) },
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Test
    fun `held like a book, the two panes meet the hinge on either side`() {
        twoPane(WindowPosture(WindowPosture.Kind.Book, hinge = Rect(390f, 0f, 410f, 840f)))
        val first = rule.onNodeWithTag("first").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithTag("second").getUnclippedBoundsInRoot()
        assertEquals(390.dp, first.right)
        assertEquals(410.dp, second.left)
    }

    @Test
    fun `in tabletop the first pane is above the fold and the second below it`() {
        twoPane(WindowPosture(WindowPosture.Kind.Tabletop, hinge = Rect(0f, 400f, 800f, 420f)))
        val first = rule.onNodeWithTag("first").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithTag("second").getUnclippedBoundsInRoot()
        assertEquals(400.dp, first.bottom)
        assertEquals(420.dp, second.top)
    }

    @Test
    fun `flat, the panes share the width as usual`() {
        twoPane(WindowPosture())
        val first = rule.onNodeWithTag("first").getUnclippedBoundsInRoot()
        assertEquals(0.dp, first.top)
    }

    @Test
    fun `in tabletop a sheet rises from the bottom and rests on the fold`() {
        setContent(WindowPosture(WindowPosture.Kind.Tabletop, hinge = Rect(0f, 400f, 800f, 420f))) {
            AppSheet(visible = true, onDismissRequest = {}, modifier = Modifier.testTag("sheet")) {
                Box(Modifier.fillMaxSize())
            }
        }
        rule.waitForIdle()
        assertEquals(420.dp, rule.onNodeWithTag("sheet").getUnclippedBoundsInRoot().top)
    }

    @Test
    fun `in tabletop the picture fills the half above the fold`() {
        setContent(WindowPosture(WindowPosture.Kind.Tabletop, hinge = Rect(0f, 400f, 800f, 420f))) {
            AppImageViewer(onDismiss = {}) { Box(Modifier.fillMaxSize().testTag("picture")) }
        }
        assertEquals(400.dp, rule.onNodeWithTag("picture").getUnclippedBoundsInRoot().bottom)
    }
}
