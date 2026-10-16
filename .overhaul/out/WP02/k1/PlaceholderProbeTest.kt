package io.example.wp02fmt.feature.subscriptions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import io.example.wp02fmt.core.designsystem.component.text.AppText
import io.example.wp02fmt.core.designsystem.theme.AppTheme
import io.example.wp02fmt.core.testing.compose.TEST_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class PlaceholderProbeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun probe() {
        compose.setContent {
            AppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    for (scale in SCALES) {
                        val base = LocalDensity.current
                        CompositionLocalProvider(LocalDensity provides Density(base.density, scale)) {
                            val styles = mapOf(
                                "title" to AppTheme.typography.titleMedium,
                                "display" to AppTheme.typography.displaySmall,
                            )
                            for ((styleName, style) in styles) {
                                for ((candidate, text) in CANDIDATES) {
                                    Probe("$styleName-$candidate-$scale", text, style)
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        for (scale in SCALES) {
            for (styleName in listOf("title", "display")) {
                val line = CANDIDATES.keys.joinToString(" ") { candidate ->
                    "$candidate=${heightPx("$styleName-$candidate-$scale")}"
                }
                println("K1PROBE scale=$scale style=$styleName px: $line")
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Probe(tag: String, text: String, style: TextStyle) {
        val color = if (tag.contains("-real-")) Color.Unspecified else Color.Transparent
        Box(Modifier.testTag(tag).fillMaxWidth(0.7f)) {
            AppText(text = text, style = style, color = color)
        }
    }

    private fun heightPx(tag: String): Int = compose.onNodeWithTag(tag).fetchSemanticsNode().size.height

    private companion object {
        val SCALES = listOf(1f, 1.15f, 1.3f, 1.5f, 1.8f, 2f)
        val CANDIDATES = linkedMapOf(
            "real" to "Subscriptions 1",
            "space" to " ",
            "nbsp" to " ",
            "empty" to "",
            "M" to "M",
            "Hg" to "Hg",
        )
    }
}
