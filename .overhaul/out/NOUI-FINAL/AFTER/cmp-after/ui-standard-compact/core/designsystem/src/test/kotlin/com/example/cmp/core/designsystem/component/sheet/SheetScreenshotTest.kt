package com.example.cmp.core.designsystem.component.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.TestSdk
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.container.AppBottomActionBar
import com.example.cmp.core.designsystem.component.sheet.action.AppActionSheet
import com.example.cmp.core.designsystem.component.sheet.action.SheetAction
import com.example.cmp.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.cmp.core.designsystem.component.sheet.content.SheetColumn
import com.example.cmp.core.designsystem.component.sheet.content.SheetDismissConfirmation
import com.example.cmp.core.designsystem.component.sheet.stack.LocalSheetStack
import com.example.cmp.core.designsystem.component.sheet.stack.cardStackPresenter
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.layout.BarFrame
import com.example.cmp.core.designsystem.foundation.layout.LocalBarFrame
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.designsystem.theme.SheetMaterial
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Sheets on a 400 x 1000 dp phone, light and left to right at the default size, then dark, right
 * to left at 1.5x (2x where large text is the point). Larger windows and folds are in
 * [SheetWindowScreenshotTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w400dp-h1000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SheetScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shots = SheetShots(rule)

    /** The style's floating inset, read where a case composes. */
    private var inset = 0.dp

    @Test
    fun `a medium sheet floats inset, solid and frosted`() {
        for (material in listOf(SheetMaterial.Solid, SheetMaterial.Frosted)) {
            shots.shoot("medium-${material.name.lowercase()}") { visible ->
                AppSheet(
                    visible = visible,
                    onDismissRequest = {},
                    state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
                    style = AppSheetDefaults.style(material = material),
                    header = { AppSheetHeader(title = "Filters", modifier = Modifier.testTag(Probe)) },
                ) { ShotRows(40) }
            }
        }
    }

    @Test
    fun `at large the presenter recedes into a card, halfway and all the way`() {
        val halfway = SheetDetent.Fraction(0.75f)
        for ((name, detent) in listOf("halfway" to halfway, "large" to SheetDetent.Large)) {
            shots.shoot(
                "card-stack-$name",
                screen = { Presenter { ShotScreen() } },
            ) { visible ->
                AppSheet(
                    visible = visible,
                    onDismissRequest = {},
                    state = rememberAppSheetState(listOf(SheetDetent.Medium, halfway, SheetDetent.Large), detent),
                    header = { AppSheetHeader(title = "Compose", modifier = Modifier.testTag(Probe)) },
                ) { ShotRows(40) }
            }
        }
    }

    @Test
    fun `a fit-content sheet is as tall as what it holds`() {
        shots.shoot("fit-content") { visible ->
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.FitContent)),
                header = { AppSheetHeader(title = "Delivery") },
            ) {
                SheetColumn {
                    AppText(
                        text = "Tomorrow, between 9 and 11, by bike. The courier calls when they are outside.",
                        modifier = Modifier.testTag(Probe),
                        style = AppTheme.typography.body,
                    )
                }
            }
        }
    }

    @Test
    fun `header variants keep their title between the sides`() {
        shots.shoot("header-variants", looks = listOf(Plain, Largest)) { visible ->
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.Medium)),
                header = {
                    AppSheetHeader(
                        title = "Edit the reminder for tomorrow morning",
                        subtitle = "Repeats every weekday",
                        modifier = Modifier.testTag(Probe),
                        leading = { AppButton(text = "Cancel", onClick = {}) },
                    )
                },
            ) { ShotRows(10) }
        }
    }

    @Test
    fun `a footer stays above the bottom edge and the list ends above it`() {
        shots.shoot("footer") { visible ->
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
                header = { AppSheetHeader(title = "Filters") },
                footer = {
                    AppBottomActionBar {
                        AppButton(text = "Show 128 results", onClick = {}, modifier = Modifier.weight(1f).testTag(Probe))
                    }
                },
            ) { ShotRows(40) }
        }
    }

    @Test
    fun `a sheet over a sheet pushes the first one back`() {
        shots.shoot("stacked") { visible ->
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.Large)),
                header = { AppSheetHeader(title = "Order") },
            ) {
                ShotRows(20)
                AppSheet(
                    visible = visible,
                    onDismissRequest = {},
                    state = rememberAppSheetState(listOf(SheetDetent.Medium)),
                    header = { AppSheetHeader(title = "Pick a size", modifier = Modifier.testTag(Probe)) },
                ) { ShotRows(5) }
            }
        }
    }

    @Test
    fun `closing a dirty form asks in an action sheet over it`() {
        var request: (() -> Unit)? = null
        shots.shoot("guard-asks", whenShown = { rule.runOnIdle { request?.invoke() } }) { visible ->
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.Large)),
                header = { AppSheetHeader(title = "New event", modifier = Modifier.testTag(Probe)) },
            ) {
                request = this::requestDismiss
                SheetDismissConfirmation(enabled = true, onDiscard = {}, discardLabel = "Discard changes")
                ShotRows(10)
            }
        }
    }

    @Test
    fun `a non-modal peek rests above the tab bar`() {
        shots.shoot(
            "nonmodal-peek",
            screen = {
                ShotScreen()
                FakeTabBar()
            },
        ) { visible ->
            inset = AppTheme.style.sheetInset
            val density = LocalDensity.current
            val window = LocalWindowInfo.current.containerSize.height
            val barTop = window - with(density) { BarHeight.toPx() }
            val frame = remember(barTop) { BarFrame(settled = { barTop }, live = { barTop }) }
            CompositionLocalProvider(LocalBarFrame provides frame) {
                AppSheet(
                    visible = visible,
                    onDismissRequest = {},
                    state = rememberAppSheetState(listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large)),
                    modality = SheetModality.NonModal(upThrough = SheetDetent.Medium),
                    dismissible = false,
                    header = { AppSheetHeader(title = "Places nearby", modifier = Modifier.testTag(Probe), trailing = null) },
                ) { ShotRows(20, readFirst = false) }
            }
        }
        // The surface node is the whole layer, as tall as Large, so what has to clear the bar is the
        // header, with the floating inset between them.
        val header = rule.onNodeWithTag(Probe, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val bar = rule.onNodeWithTag("tab_bar").getUnclippedBoundsInRoot()
        assertTrue("the peek's header ends at ${header.bottom}, the bar starts at ${bar.top}", header.bottom <= bar.top - inset + 0.5.dp)
    }

    @Test
    fun `a long list rests the same whichever way fit-content and large are listed`() {
        val orders = listOf(
            listOf(SheetDetent.FitContent),
            listOf(SheetDetent.FitContent, SheetDetent.Large),
            listOf(SheetDetent.Large, SheetDetent.FitContent),
        )
        val images = orders.mapIndexed { index, detents ->
            shots.shoot("fit-long-$index", looks = listOf(Plain)) { visible ->
                AppSheet(
                    visible = visible,
                    onDismissRequest = {},
                    state = rememberAppSheetState(detents),
                    grabber = null,
                    header = { AppSheetHeader(title = "Contacts", modifier = Modifier.testTag(Probe)) },
                ) { ShotRows(60) }
            }.single()
        }
        assertTrue("[FitContent, Large] rests differently from [FitContent]", images[0].sameAs(images[1]))
        assertTrue("[Large, FitContent] rests differently from [FitContent]", images[0].sameAs(images[2]))
    }

    @Test
    fun `bare text keeps its gutter while the sheet floats`() {
        shots.shoot("bare-text") { visible ->
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
            ) {
                SheetColumn {
                    AppText(
                        text = "Text set straight in the sheet, with no row or card around it, starts at the gutter.",
                        modifier = Modifier.testTag(Probe),
                        style = AppTheme.typography.body,
                    )
                }
            }
        }
        val surface = rule.onNodeWithTag("sheet_surface").getUnclippedBoundsInRoot()
        val text = rule.onNodeWithTag(Probe, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("the text starts ${text.left - surface.left} from the sheet's edge", text.left - surface.left >= 16.dp)
    }

    @Test
    fun `a peek grows with the font size`() {
        shots.shoot("peek", looks = listOf(Plain, Largest)) { visible ->
            inset = AppTheme.style.sheetInset
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.Peek, SheetDetent.Large)),
                modality = SheetModality.NonModal(),
                dismissible = false,
                header = { AppSheetHeader(title = "Now playing", subtitle = "Side B", modifier = Modifier.testTag(Probe), trailing = null) },
            ) { ShotRows(10, readFirst = false) }
        }
        val header = rule.onNodeWithTag(Probe, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val window = rule.onRoot().getUnclippedBoundsInRoot()
        assertTrue("at 2x the header ends at ${header.bottom}, past the floating edge", header.bottom <= window.bottom - inset + 0.5.dp)
    }

    @Test
    fun `artwork can run under the grabber`() {
        shots.shoot("under-grabber") { visible ->
            AppSheet(
                visible = visible,
                onDismissRequest = {},
                state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
                properties = AppSheetProperties(contentUnderGrabber = true),
            ) {
                SheetColumn {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFF0077FF), Color(0xFF00C2A8)))),
                    )
                    AppText("Lighthouse at dusk", modifier = Modifier.testTag(Probe), style = AppTheme.typography.title2)
                }
            }
        }
    }

    @Test
    fun `action sheet rows wrap at the largest text`() {
        val actions = listOf(
            SheetAction("Share with people nearby", onClick = {}),
            SheetAction("Move to another album", onClick = {}),
            SheetAction("Delete from every device", onClick = {}, destructive = true),
        )
        val texts = listOf("Delete from every device")
        shots.shoot("action-sheet", looks = listOf(Plain, Largest), insideSurface = false, texts = texts) { visible ->
            AppActionSheet(visible = visible, actions = actions, onDismissRequest = {}, title = "Photo")
        }
    }

    /** The screen as the shell would give it to a sheet: receding into a card while one is up. */
    @Suppress("ktlint:compose:content-slot-reused") // The stack is there throughout or never.
    @Composable
    private fun Presenter(content: @Composable () -> Unit) {
        val stack = LocalSheetStack.current ?: return content()
        Box(Modifier.fillMaxSize().cardStackPresenter(stack)) { content() }
    }

    @Composable
    private fun FakeTabBar() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .background(AppTheme.colors.surface)
                    .testTag("tab_bar"),
            )
        }
    }

    private companion object {
        val BarHeight = 83.dp
    }
}
