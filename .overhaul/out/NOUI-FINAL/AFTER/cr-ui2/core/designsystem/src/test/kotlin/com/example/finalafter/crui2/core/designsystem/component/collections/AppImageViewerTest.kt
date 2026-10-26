package com.example.finalafter.crui2.core.designsystem.component.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.TestSdk
import com.example.finalafter.crui2.core.designsystem.foundation.adaptive.WindowPosture
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A half-open foldable: ADAPTIVE-PLAN 7. At mdpi a pixel is a dp, so the hinge reads in either. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w800dp-h840dp-mdpi")
class AppImageViewerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `in tabletop the picture fills the half above the fold`() {
        val tabletop = WindowPosture(WindowPosture.Kind.Tabletop, hinge = Rect(0f, 400f, 800f, 420f))
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = true, posture = tabletop) {
                AppImageViewer(onDismiss = {}) { Box(Modifier.fillMaxSize().testTag("picture")) }
            }
        }
        assertEquals(400.dp, rule.onNodeWithTag("picture").getUnclippedBoundsInRoot().bottom)
    }
}
