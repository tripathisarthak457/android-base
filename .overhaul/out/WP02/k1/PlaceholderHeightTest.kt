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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import io.example.wp02fmt.core.designsystem.component.container.AppCard
import io.example.wp02fmt.core.designsystem.component.feedback.AppSkeleton
import io.example.wp02fmt.core.designsystem.component.text.AppText
import io.example.wp02fmt.core.designsystem.theme.AppTheme
import io.example.wp02fmt.core.testing.compose.TEST_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class PlaceholderHeightTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun placeholdersAreAsTallAsTheTextTheyStandFor() {
        compose.setContent {
            AppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    for (scale in SCALES) {
                        val base = LocalDensity.current
                        CompositionLocalProvider(LocalDensity provides Density(base.density, scale)) {
                            val title = AppTheme.typography.titleMedium
                            val display = AppTheme.typography.displaySmall
                            Box(Modifier.testTag("row$scale")) {
                                AppCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                                    AppText(text = "Subscriptions 1", style = title)
                                }
                            }
                            Box(Modifier.testTag("placeholder$scale")) {
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    Box(Modifier.fillMaxWidth(0.6f).clearAndSetSemantics {}) {
                                        AppText(text = " ", style = title, color = Color.Transparent)
                                        AppSkeleton(modifier = Modifier.matchParentSize())
                                    }
                                }
                            }
                            Box(Modifier.testTag("title$scale")) {
                                AppText(text = "Order 1", style = display)
                            }
                            Box(Modifier.testTag("bone$scale")) {
                                Box(Modifier.fillMaxWidth(0.7f).clearAndSetSemantics {}) {
                                    AppText(text = " ", style = display, color = Color.Transparent)
                                    AppSkeleton(modifier = Modifier.matchParentSize())
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()

        for (scale in SCALES) {
            val row = heightPx("row$scale")
            val placeholder = heightPx("placeholder$scale")
            val title = heightPx("title$scale")
            val bone = heightPx("bone$scale")
            println("K1 scale=$scale px: row=$row placeholder=$placeholder title=$title bone=$bone")
            assertEquals("list row at font scale $scale", row, placeholder)
            assertEquals("detail title at font scale $scale", title, bone)
        }
    }

    private fun heightPx(tag: String): Int = compose.onNodeWithTag(tag).fetchSemanticsNode().size.height

    private companion object {
        val SCALES = listOf(1f, 1.3f, 1.5f, 2f)
    }
}
