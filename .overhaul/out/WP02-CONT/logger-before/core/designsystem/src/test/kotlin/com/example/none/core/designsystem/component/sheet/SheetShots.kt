package com.example.none.core.designsystem.component.sheet

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.none.core.designsystem.component.list.AppRow
import com.example.none.core.designsystem.component.text.AppText
import com.example.none.core.designsystem.foundation.adaptive.WindowPosture
import com.example.none.core.designsystem.theme.AppTheme
import com.example.none.core.designsystem.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertTrue

/** One way a person may have their phone set up. */
internal data class ShotLook(val dark: Boolean, val rtl: Boolean, val fontScale: Float) {
    val name: String
        get() = "${if (dark) "dark" else "light"}-${if (rtl) "rtl" else "ltr"}-font${(fontScale * 100).toInt()}"
}

internal val Plain = ShotLook(dark = false, rtl = false, fontScale = 1f)
internal val Stressed = ShotLook(dark = true, rtl = true, fontScale = 1.5f)
internal val Largest = ShotLook(dark = true, rtl = true, fontScale = 2f)

/** Light, left to right at the default size; then dark, right to left at 1.5x. */
internal val DefaultLooks = listOf(Plain, Stressed)

/**
 * Screenshots of a sheet over a screen, each look taken twice: with the sheet hidden and with it
 * settled. The sheet's image must differ from the screen's, and every node tagged [Probe] or
 * showing one of the probe texts (what the person has to be able to read) must lie inside the window
 * and, where the case asks, inside a sheet surface. The images go to `build/outputs/sheet/` for a person to look at: clipping by a
 * rounded corner or a neighbour shows there, not in bounds.
 */
internal class SheetShots(private val rule: ComposeContentTestRule) {
    private var look by mutableStateOf(Plain)
    private var visible by mutableStateOf(false)
    private var case: ShotCase? by mutableStateOf(null)
    private var contentSet = false

    /**
     * A test may shoot several cases to compare them; the rule takes content once, so each case is
     * composed afresh under its own key, with no state carried over from the one before.
     */
    fun shoot(
        name: String,
        looks: List<ShotLook> = DefaultLooks,
        posture: WindowPosture = WindowPosture(),
        insideSurface: Boolean = true,
        texts: List<String> = emptyList(),
        screen: @Composable () -> Unit = { ShotScreen() },
        whenShown: () -> Unit = {},
        sheet: @Composable (visible: Boolean) -> Unit,
    ): List<Bitmap> {
        visible = false
        case = ShotCase(id = (case?.id ?: 0) + 1, posture = posture, screen = screen, sheet = sheet)
        if (!contentSet) {
            contentSet = true
            rule.setContent {
                val shot = case ?: return@setContent
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, look.fontScale),
                    LocalLayoutDirection provides if (look.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    AppTheme(mode = if (look.dark) ThemeMode.Dark else ThemeMode.Light, reduceMotion = false, posture = shot.posture) {
                        key(shot.id) {
                            Box(Modifier.fillMaxSize()) {
                                shot.screen()
                                shot.sheet(visible)
                            }
                        }
                    }
                }
            }
        }
        return looks.map { each ->
            look = each
            visible = false
            settle()
            val without = capture()
            visible = true
            settle()
            whenShown()
            settle()
            val shown = capture()
            val label = "$name-${each.name}"
            save(label, shown)
            assertTrue("$label: the image does not differ from the screen without the sheet", differs(without, shown))
            assertProbesVisible(label, insideSurface, texts)
            shown
        }
    }

    fun settle() {
        rule.mainClock.advanceTimeBy(SettleMillis)
        rule.waitForIdle()
    }

    private fun capture(): Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

    private fun save(name: String, image: Bitmap) {
        val file = File("build/outputs/sheet/$name.png").apply { parentFile?.mkdirs() }
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun assertProbesVisible(label: String, insideSurface: Boolean, texts: List<String>) {
        val window = rule.onRoot().getUnclippedBoundsInRoot()
        val surfaces = rule.onAllNodesWithTag(SurfaceTag, useUnmergedTree = true).bounds()
        val probes = rule.onAllNodesWithTag(Probe, useUnmergedTree = true).bounds() +
            texts.flatMap { rule.onAllNodesWithText(it, useUnmergedTree = true).bounds() }
        assertTrue("$label: the case names nothing to read", probes.isNotEmpty())
        probes.forEachIndexed { index, probe ->
            assertTrue("$label: probe $index at $probe leaves the window $window", probe.within(window))
            if (insideSurface) {
                assertTrue(
                    "$label: probe $index at $probe is outside every sheet surface $surfaces",
                    surfaces.any { probe.within(it) },
                )
            }
        }
    }

    private fun SemanticsNodeInteractionCollection.bounds(): List<DpRect> =
        fetchSemanticsNodes().indices.map { this[it].getUnclippedBoundsInRoot() }

    private companion object {
        const val SettleMillis = 2_000L
        const val SurfaceTag = "sheet_surface"

        /** At least this share of the pixels changes when a sheet comes up, whatever its size. */
        const val MinChangedShare = 0.005f

        fun DpRect.within(outer: DpRect): Boolean {
            val slack = 0.5.dp
            return left >= outer.left - slack && top >= outer.top - slack &&
                right <= outer.right + slack && bottom <= outer.bottom + slack
        }

        fun differs(a: Bitmap, b: Bitmap): Boolean {
            if (a.width != b.width || a.height != b.height) return true
            val first = IntArray(a.width * a.height).also { a.getPixels(it, 0, a.width, 0, 0, a.width, a.height) }
            val second = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
            val changed = first.indices.count { first[it] != second[it] }
            return changed >= first.size * MinChangedShare
        }
    }
}

private class ShotCase(
    val id: Int,
    val posture: WindowPosture,
    val screen: @Composable () -> Unit,
    val sheet: @Composable (visible: Boolean) -> Unit,
)

/** The test tag of what a case asks the person to read. */
internal const val Probe = "probe"

/** A screen with colour and text behind the sheet, so the dim, the recede and the materials read against it. */
@Composable
internal fun ShotScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(AppTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF6B6B), Color(0xFF4D96FF), Color(0xFF9D4EDD)))),
        )
        repeat(12) { AppText("Behind the sheet, line ${it + 1}", style = AppTheme.typography.body) }
    }
}

/**
 * [count] rows, the first tagged as something to read unless [readFirst] is false: at a peek the
 * rows rest below the window's edge by design, and only the header has to be read.
 */
@Composable
internal fun AppSheetScope.ShotRows(count: Int, modifier: Modifier = Modifier, readFirst: Boolean = true) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        items(count, key = { it }, contentType = { "row" }) { index ->
            AppRow(
                title = "Row ${index + 1}",
                subtitle = "Supporting text that runs long enough to need a second line at large sizes",
                onClick = {},
                modifier = if (index == 0 && readFirst) Modifier.testTag(Probe) else Modifier,
            )
        }
    }
}
