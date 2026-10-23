package io.example.fitcnetdb.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.example.fitcnetdb.core.designsystem.TestSdk
import io.example.fitcnetdb.core.designsystem.component.button.AppButton
import io.example.fitcnetdb.core.designsystem.component.container.AppBottomActionBar
import io.example.fitcnetdb.core.designsystem.component.sheet.action.AppActionSheet
import io.example.fitcnetdb.core.designsystem.component.sheet.action.SheetAction
import io.example.fitcnetdb.core.designsystem.component.sheet.action.rememberSheetAnchor
import io.example.fitcnetdb.core.designsystem.component.sheet.action.sheetAnchor
import io.example.fitcnetdb.core.designsystem.component.sheet.chrome.AppSheetHeader
import io.example.fitcnetdb.core.designsystem.foundation.adaptive.WindowPosture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Sheets in windows other than a phone held upright: short, wide, tablet, desktop and half-open
 * foldables, in the same two looks as [SheetScreenshotTest]. At mdpi a pixel is a dp, so a hinge
 * reads in either.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SheetWindowScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shots = SheetShots(rule)

    @Composable
    private fun TwoDetents(visible: Boolean, footer: Boolean = false) {
        AppSheet(
            visible = visible,
            onDismissRequest = {},
            state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
            header = { AppSheetHeader(title = "Filters", modifier = Modifier.testTag(Probe)) },
            footer = if (footer) {
                { AppBottomActionBar { AppButton(text = "Apply", onClick = {}, modifier = Modifier.weight(1f)) } }
            } else {
                null
            },
        ) { ShotRows(30) }
    }

    @Test
    @Config(qualifiers = "w400dp-h500dp-mdpi")
    fun `a short window still offers medium`() {
        shots.shoot("short-500") { TwoDetents(it) }
    }

    @Test
    @Config(qualifiers = "w400dp-h400dp-mdpi")
    fun `in compact height a sheet has one detent`() {
        shots.shoot("short-400") { TwoDetents(it) }
    }

    @Test
    @Config(qualifiers = "w900dp-h400dp-mdpi")
    fun `in a wide short window the sheet is centred and capped, its footer scrolling with the list at 2x`() {
        shots.shoot("wide-short", looks = listOf(Plain, Largest)) { TwoDetents(it, footer = true) }
        val bounds = rule.onNodeWithTag("sheet_surface").getUnclippedBoundsInRoot()
        assertEquals(640.dp, bounds.right - bounds.left)
        assertEquals(450.dp, bounds.left + (bounds.right - bounds.left) / 2)
    }

    @Test
    @Config(qualifiers = "w610dp-h1000dp-mdpi")
    fun `at medium width a modal sheet is a centred form sheet`() {
        shots.shoot("form-610") { TwoDetents(it) }
    }

    @Test
    @Config(qualifiers = "w900dp-h1000dp-mdpi")
    fun `at expanded width the form sheet stays a form sheet`() {
        shots.shoot("form-900") { TwoDetents(it) }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun `on a desktop window a modal sheet is a centred form sheet`() {
        shots.shoot("desktop-form") { TwoDetents(it) }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun `on a desktop window a sheet can be a start panel`() {
        shots.shoot("desktop-start") { visible -> Panel(visible, SheetPlacement.Start, SheetModality.NonModal()) }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun `on a desktop window a modal sheet can be an end panel`() {
        shots.shoot("desktop-end") { visible -> Panel(visible, SheetPlacement.End, SheetModality.Modal) }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun `an action sheet is a card at its anchor and wraps at the largest text`() {
        val actions = listOf(
            SheetAction("Share with people nearby", onClick = {}),
            SheetAction("Delete from every device", onClick = {}, destructive = true),
        )
        shots.shoot(
            "desktop-action-card",
            looks = listOf(Plain, Largest),
            insideSurface = false,
            texts = listOf("Delete from every device"),
            screen = {},
        ) { visible ->
            val anchor = rememberSheetAnchor()
            Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.TopEnd) {
                AppButton(text = "More", onClick = {}, modifier = Modifier.sheetAnchor(anchor))
            }
            AppActionSheet(
                visible = visible,
                actions = actions,
                onDismissRequest = {},
                title = "Photo",
                anchor = anchor,
            )
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h840dp-mdpi")
    fun `half open like a laptop the partial detents rest below the fold`() {
        val fold = Rect(0f, 400f, 800f, 420f)
        shots.shoot("tabletop", posture = WindowPosture(WindowPosture.Kind.Tabletop, hinge = fold)) { TwoDetents(it) }
        val bounds = rule.onNodeWithTag("sheet_surface").getUnclippedBoundsInRoot()
        assertTrue("the sheet's top ${bounds.top} is above the fold", bounds.top >= 420.dp)
    }

    @Test
    @Config(qualifiers = "w800dp-h840dp-mdpi")
    fun `held like a book the sheet stays in one half`() {
        val fold = Rect(390f, 0f, 410f, 840f)
        shots.shoot("book", posture = WindowPosture(WindowPosture.Kind.Book, hinge = fold)) { TwoDetents(it) }
        val bounds = rule.onNodeWithTag("sheet_surface").getUnclippedBoundsInRoot()
        assertTrue("the sheet at $bounds crosses the hinge", bounds.right <= 390.dp || bounds.left >= 410.dp)
    }

    @Composable
    private fun Panel(visible: Boolean, placement: SheetPlacement, modality: SheetModality) {
        AppSheet(
            visible = visible,
            onDismissRequest = {},
            modality = modality,
            properties = AppSheetProperties(placement = placement),
            header = { AppSheetHeader(title = "Inspector", modifier = Modifier.testTag(Probe)) },
        ) { ShotRows(30) }
    }
}
