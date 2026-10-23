package io.example.fitcnetdb.core.designsystem.component.sheet.surface

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.example.fitcnetdb.core.designsystem.TestSdk
import io.example.fitcnetdb.core.designsystem.component.button.AppButton
import io.example.fitcnetdb.core.designsystem.component.container.AppBottomActionBar
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheet
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheetDefaults
import io.example.fitcnetdb.core.designsystem.component.sheet.LocalSheetSurface
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetDetent
import io.example.fitcnetdb.core.designsystem.component.sheet.chrome.AppSheetHeader
import io.example.fitcnetdb.core.designsystem.component.sheet.content.SheetColumn
import io.example.fitcnetdb.core.designsystem.component.sheet.rememberAppSheetState
import io.example.fitcnetdb.core.designsystem.theme.AppTheme
import io.example.fitcnetdb.core.designsystem.theme.SheetMaterial
import io.example.fitcnetdb.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The strip a sheet keeps clear for the navigation bar lies under its pinned footer, and the
 * content runs on beneath both: what shows there is the footer's colour, never the rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w400dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SheetFooterBackingTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `rows running under the footer never show between it and the bottom edge`() {
        var view: View? = null
        var container = Color.Unspecified
        rule.setContent {
            view = LocalView.current
            AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
                AppSheet(
                    visible = true,
                    onDismissRequest = {},
                    state = rememberAppSheetState(listOf(SheetDetent.Large)),
                    style = AppSheetDefaults.style(material = SheetMaterial.Solid),
                    header = { AppSheetHeader(title = "Filters") },
                    footer = {
                        container = checkNotNull(LocalSheetSurface.current).container
                        AppBottomActionBar(modifier = Modifier.testTag(Footer)) {
                            AppButton(text = "Apply", onClick = {}, modifier = Modifier.weight(1f))
                        }
                    },
                ) {
                    SheetColumn { Box(Modifier.fillMaxWidth().height(3_000.dp).background(Rows)) }
                }
            }
        }
        val navigationBar = with(rule.density) { NavigationBar.roundToPx() }
        rule.runOnIdle {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navigationBar))
                .build()
            ViewCompat.dispatchApplyWindowInsets(checkNotNull(view), insets)
        }
        rule.mainClock.advanceTimeBy(SettleMillis)
        rule.waitForIdle()

        val window = rule.onRoot().getBoundsInRoot()
        assertEquals(
            "the footer rests on the navigation bar",
            (window.bottom - NavigationBar).value,
            rule.onNodeWithTag(Footer).getBoundsInRoot().bottom.value,
            0.5f,
        )
        val pixels = rule.onRoot().captureToImage().toPixelMap()
        val x = pixels.width / 2
        for (y in listOf(pixels.height - navigationBar + 1, pixels.height - navigationBar / 2, pixels.height - 1)) {
            assertEquals("the strip at y $y of ${pixels.height}", container.toArgb(), pixels[x, y].toArgb())
        }
    }

    private companion object {
        const val Footer = "footer"
        const val SettleMillis = 2_000L
        val Rows = Color(0xFFD81B60)
        val NavigationBar = 48.dp
    }
}
