package com.example.lean.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.lean.core.designsystem.TestSdk
import com.example.lean.core.designsystem.component.button.AppButton
import com.example.lean.core.designsystem.component.button.ButtonSize
import com.example.lean.core.designsystem.component.container.AppCard
import com.example.lean.core.designsystem.component.text.AppText
import com.example.lean.core.designsystem.foundation.draw.TextBones
import com.example.lean.core.designsystem.theme.AppTheme
import com.example.lean.core.designsystem.theme.AppTypography
import com.example.lean.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A redacted row is the row it stands for: the same size at every font scale and in both themes,
 * with bones where its letters will be and none of the letters themselves. So nothing moves or is
 * cut when the data replaces the placeholder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RedactedGeometryTest {

    @get:Rule
    val rule = createComposeRule()

    private var fontScale by mutableFloatStateOf(1f)
    private var dark by mutableStateOf(false)

    @Test
    fun `a redacted row takes exactly the room of the loaded row`() {
        var bone = Color.Unspecified
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                // Motion off: the band would be a lighter colour on the bones, not a darker one,
                // but held still the bones are exactly their colour.
                AppTheme(mode = if (dark) ThemeMode.Dark else ThemeMode.Light, reduceMotion = true) {
                    bone = AppTheme.colors.skeleton
                    Column(Modifier.width(RowWidth)) {
                        Box(Modifier.testTag("redacted")) { AppRedacted(active = true) { PersonRow() } }
                        Box(Modifier.testTag("loaded")) { AppRedacted(active = false) { PersonRow() } }
                    }
                }
            }
        }
        val heights = mutableMapOf<Float, Dp>()
        for (scale in listOf(1f, 1.5f)) {
            for (theme in listOf(false, true)) {
                fontScale = scale
                dark = theme
                // With the clock advanced by hand, nothing else applies a change made between frames.
                rule.waitForIdle()
                rule.mainClock.advanceTimeByFrame()
                rule.mainClock.advanceTimeByFrame()
                val redacted = rule.onNodeWithTag("redacted").getBoundsInRoot()
                val loaded = rule.onNodeWithTag("loaded").getBoundsInRoot()
                val what = "at font scale $scale, ${if (theme) "dark" else "light"}"
                assertEquals(what, loaded.right - loaded.left, redacted.right - redacted.left)
                assertEquals(what, loaded.bottom - loaded.top, redacted.bottom - redacted.top)
                if (!theme) assertNoGlyphs(bone, what)
                heights[scale] = redacted.bottom - redacted.top
            }
        }
        assertTrue("the font scale never reached the row: $heights", heights.getValue(1.5f) > heights.getValue(1f))
    }

    @Test
    fun `a placeholder line is as tall as the line of text it stands for`() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(mode = ThemeMode.Light, reduceMotion = true) {
                Column {
                    AppText(
                        "Mg",
                        modifier = Modifier.testTag("text"),
                        style = AppTheme.typography.headline,
                        maxLines = 1,
                    )
                    Box(Modifier.testTag("line")) { AppSkeletonLine(style = AppTheme.typography.headline) }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val text = rule.onNodeWithTag("text").getBoundsInRoot()
        val line = rule.onNodeWithTag("line").getBoundsInRoot()
        assertEquals(text.bottom - text.top, line.bottom - line.top)
    }

    @Test
    fun `each line's bone lies inside the line, in every role of the type scale`() {
        val density = Density(2.75f, fontScale = 1.5f)
        val fonts = createFontFamilyResolver(RuntimeEnvironment.getApplication())
        val measurer = TextMeasurer(fonts, density, LayoutDirection.Ltr)
        lateinit var typography: AppTypography
        rule.setContent { AppTheme(mode = ThemeMode.Light, reduceMotion = true) { typography = AppTheme.typography } }
        rule.waitForIdle()
        for (style in typography.sansRoles + typography.mono) {
            val layout = measurer.measure(Paragraph, style, constraints = Constraints(maxWidth = 700))
            val bones = TextBones().apply { update(layout) }
            for (line in 0 until layout.lineCount) {
                val top = bones.top(line)
                val bottom = bones.bottom(line)
                val box = "${layout.getLineTop(line)}..${layout.getLineBottom(line)}"
                val what = "line $line of ${style.fontSize}: bone $top..$bottom in $box"
                assertTrue(what, top >= layout.getLineTop(line) - 0.5f && bottom <= layout.getLineBottom(line) + 0.5f)
                assertTrue(what, bones.lines[line * 4] < bones.lines[line * 4 + 2])
            }
        }
    }

    /** No pixel inside the card is darker than a bone: the text's letters were not drawn. */
    private fun assertNoGlyphs(bone: Color, what: String) {
        val pixels = rule.onNodeWithTag("redacted").captureToImage().toPixelMap()
        val inset = with(rule.density) { 6.dp.roundToPx() }
        val floor = bone.luminance() - 0.01f
        for (y in inset until pixels.height - inset) {
            for (x in inset until pixels.width - inset) {
                assertTrue("$what: a darker pixel at ($x, $y), a letter drawn", pixels[x, y].luminance() >= floor)
            }
        }
    }

    @Composable
    private fun PersonRow() {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppAvatar(name = "Ada Lovelace")
                Column(modifier = Modifier.weight(1f)) {
                    AppText(text = "Ada Lovelace", style = AppTheme.typography.headline)
                    AppText(
                        text = "Wrote the first program for a machine that did not exist yet, a century early.",
                        style = AppTheme.typography.subheadline,
                        maxLines = 2,
                    )
                }
            }
            AppButton(text = "Follow", onClick = {}, size = ButtonSize.Small, modifier = Modifier.align(Alignment.End))
        }
    }

    private companion object {
        val RowWidth = 360.dp
        const val Paragraph = "Placeholder text long enough to wrap onto a second line, and a third at this width."
    }
}
