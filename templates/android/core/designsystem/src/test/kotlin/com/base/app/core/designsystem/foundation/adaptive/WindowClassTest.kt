package com.base.app.core.designsystem.foundation.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.TEST_SDK
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class WindowClassTest {

    @get:Rule
    val rule = createComposeRule()

    private class ResizableWindow(width: Int, height: Int) : WindowInfo {
        override val isWindowFocused = true
        override var containerSize by mutableStateOf(IntSize(width, height))
    }

    private val window = ResizableWindow(900, 800)
    private var compositions = 0
    private var seen: SizeClass? = null
    private var gutter = 0.dp

    private fun setResizableContent(pane: (() -> Float)? = null) {
        rule.setContent {
            CompositionLocalProvider(LocalWindowInfo provides window, LocalDensity provides Density(1f)) {
                AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                    if (pane == null) {
                        Consumer()
                    } else {
                        ProvideContainerWidth(pane().dp) { Consumer() }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Consumer() {
        seen = AppTheme.containerClass
        gutter = AppTheme.spacing.gutter
        SideEffect { compositions++ }
        Box {}
    }

    @Test
    fun `resizing within a size class recomposes nothing that reads it`() {
        setResizableContent()
        rule.waitForIdle()
        val settled = compositions
        assertEquals(WindowWidth.Expanded, seen?.width)

        for (width in listOf(940, 1000, 1100, 1199)) {
            window.containerSize = IntSize(width, 800)
            rule.waitForIdle()
        }
        assertEquals(settled, compositions)

        window.containerSize = IntSize(1200, 800)
        rule.waitForIdle()
        assertEquals(settled + 1, compositions)
        assertEquals(WindowWidth.Large, seen?.width)
    }

    @Test
    fun `a pane gets its own class and gutter, and the window keeps its height class`() {
        setResizableContent(pane = { 360f })
        rule.waitForIdle()
        assertEquals(SizeClass(WindowWidth.Compact, WindowHeight.Medium), seen)
        assertEquals(16.dp, gutter)
    }
}
