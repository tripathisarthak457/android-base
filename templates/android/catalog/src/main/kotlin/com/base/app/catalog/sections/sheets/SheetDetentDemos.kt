package com.base.app.catalog.sections.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.base.app.catalog.CatalogGroup
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppBottomActionBar
import com.base.app.core.designsystem.component.feedback.AppSkeletonLine
import com.base.app.core.designsystem.component.feedback.AppStateView
import com.base.app.core.designsystem.component.feedback.StateKind
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.list.AppSection
import com.base.app.core.designsystem.component.list.AppToggleRow
import com.base.app.core.designsystem.component.selection.AppSlider
import com.base.app.core.designsystem.component.sheet.AppSheet
import com.base.app.core.designsystem.component.sheet.AppSheetProperties
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.SheetContentInteraction
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.SheetKeyboard
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.base.app.core.designsystem.component.sheet.content.SheetColumn
import com.base.app.core.designsystem.component.sheet.content.sheetFooterItem
import com.base.app.core.designsystem.component.sheet.rememberAppSheetState
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Heights: the default, two detents with a pinned footer, a form that fits, every detent kind, content that changes height. */
@Composable
internal fun DetentDemos() {
    CatalogGroup("Detents", caption = "Drag between detents, flick to skip one, pull past the top for the rubber band.") {
        Triggers {
            BasicDemo()
            FiltersDemo()
            RenameDemo()
            StateDemo()
        }
    }
    CatalogGroup("Detent lab", caption = "Every kind of detent, moved from code. The readout is drawn, not composed: it changes every frame.") {
        DetentLab()
    }
}

/** iOS's default: Large only, a title with a close button, a long list that scrolls once the sheet is up. */
@Composable
private fun BasicDemo() {
    var shown by remember { mutableStateOf(false) }
    val state = rememberAppSheetState()
    DemoTrigger("basic", "Basic", onClick = { shown = true }, prepares = state)
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = state,
        header = { AppSheetHeader(title = "Inbox") },
    ) {
        LazyColumn(
            contentPadding = contentPadding,
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) { sampleRows(60, prefix = "Message") }
    }
}

/** Half and full height, a pinned Apply, and the choice of what a scroll does first. */
@Composable
private fun FiltersDemo() {
    var shown by remember { mutableStateOf(false) }
    var scrollsFirst by remember { mutableStateOf(false) }
    val state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large))
    DemoTrigger("filters", "Filters", onClick = { shown = true }, prepares = state)
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = state,
        properties = AppSheetProperties(
            contentInteraction = if (scrollsFirst) SheetContentInteraction.Scrolls else SheetContentInteraction.Resizes,
        ),
        header = { AppSheetHeader(title = "Filters") },
        footer = {
            AppBottomActionBar {
                AppButton(text = "Apply", onClick = { dismiss() }, modifier = Modifier.weight(1f))
            }
        },
    ) {
        LazyColumn(contentPadding = contentPadding, flingBehavior = AppScrollDefaults.flingBehavior()) {
            item(key = "mode", contentType = "mode") {
                AppToggleRow(title = "List scrolls before the sheet grows", checked = scrollsFirst, onCheckedChange = { scrollsFirst = it })
            }
            items(40, key = { it }, contentType = { "filter" }) { index ->
                var on by rememberSaveable { mutableStateOf(index % 3 == 0) }
                AppToggleRow(title = "Filter ${index + 1}", checked = on, onCheckedChange = { on = it })
            }
            sheetFooterItem()
        }
    }
}

/** A form sized to itself, its Save riding the keyboard; the keyboard either lifts it or expands it. */
@Composable
private fun RenameDemo() {
    var shown by remember { mutableStateOf(false) }
    var tall by remember { mutableStateOf(false) }
    var expands by remember { mutableStateOf(false) }
    val name = rememberTextFieldState("Weekend trip")
    DemoTrigger(
        "rename",
        "Rename",
        onClick = {
            tall = false
            shown = true
        },
    )
    DemoTrigger(
        "rename_tall",
        "Rename, tall form",
        onClick = {
            tall = true
            shown = true
        },
    )
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.FitContent)),
        properties = AppSheetProperties(keyboard = if (expands) SheetKeyboard.Expand else SheetKeyboard.Lift),
        header = { AppSheetHeader(title = "Rename list") },
        footer = {
            AppBottomActionBar {
                AppButton(text = "Save", onClick = { dismiss() }, modifier = Modifier.weight(1f))
            }
        },
    ) {
        SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppTextField(state = name, label = "Name")
            AppToggleRow(title = "The keyboard expands the sheet", checked = expands, onCheckedChange = { expands = it })
            if (tall) {
                AppText(
                    text = "A form taller than the room the keyboard leaves: the sheet stays where it is and the form scrolls.",
                    style = AppTheme.typography.footnote,
                    color = AppTheme.colors.contentSecondary,
                )
                // allow(adaptive-fixed-size): the demo needs a form taller than the keyboard's room
                AppSection(modifier = Modifier.height(600.dp)) {
                    repeat(6) { AppRow(title = "Field ${it + 1}", value = "Value") }
                }
            }
        }
    }
}

/** A sheet sized to its content while the content loads, arrives, and fails. */
@Composable
private fun StateDemo() {
    var shown by remember { mutableStateOf(false) }
    var phase by remember { mutableIntStateOf(0) }
    DemoTrigger(
        "state",
        "Loading",
        onClick = {
            phase = 0
            shown = true
        },
    )
    LaunchedEffect(shown, phase) {
        if (shown && phase == 0) {
            delay(1_500)
            phase = 1
        }
    }
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.FitContent)),
        header = { AppSheetHeader(title = "Delivery") },
    ) {
        SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            when (phase) {
                0 -> repeat(3) { AppSkeletonLine(widthFraction = 1f - it * 0.2f) }
                1 -> {
                    AppSection {
                        AppRow(title = "Arrives", value = "Tomorrow, 9-11")
                        AppRow(title = "Courier", value = "Bike")
                        AppRow(title = "Fee", value = "Free")
                    }
                    AppButton(text = "Show the failure", onClick = { phase = 2 }, modifier = Modifier.fillMaxWidth())
                }
                else -> AppStateView(
                    kind = StateKind.Error,
                    title = "Couldn't load",
                    message = "Check the connection and try again.",
                    actionLabel = "Retry",
                    onAction = { phase = 0 },
                    contentPadding = PaddingValues(AppTheme.spacing.md),
                )
            }
        }
    }
}

/** Peek, a fixed height, three fractions, a custom resolver fed by a slider, and Large. */
@Composable
private fun DetentLab() {
    var shown by remember { mutableStateOf(false) }
    var sliver by remember { mutableFloatStateOf(0.1f) }
    // A resolver runs in measure and reads no state: it captures the slider's value as it is now.
    val custom = remember(sliver) {
        val fraction = sliver
        SheetDetent.Custom("sliver") { maxOf(44.dp, maxHeight * fraction) }
    }
    val detents = remember(custom) {
        listOf(
            SheetDetent.Peek,
            SheetDetent.Height(96.dp),
            custom,
            SheetDetent.Fraction(0.25f),
            SheetDetent.Fraction(0.5f),
            SheetDetent.Fraction(0.75f),
            SheetDetent.Large,
        )
    }
    val state = rememberAppSheetState(detents, initialDetent = SheetDetent.Fraction(0.5f))
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        DemoTrigger("lab", "Open the lab", onClick = { shown = true })
        AppText(text = "Custom detent: ${(sliver * 100).toInt()}% of Large", style = AppTheme.typography.footnote)
        AppSlider(value = sliver, onValueChange = { sliver = it }, valueRange = 0.05f..0.4f)
    }
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = state,
        header = { AppSheetHeader(title = "Detent lab") },
    ) {
        SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            Triggers {
                LabButton("Expand") { scope.launch { state.expand() } }
                LabButton("Collapse") { scope.launch { state.collapse() } }
                LabButton("Sliver") { scope.launch { state.animateTo(custom) } }
                LabButton("Large") { scope.launch { state.animateTo(SheetDetent.Large) } }
                LabButton("Only halves") { state.updateDetents(listOf(SheetDetent.Fraction(0.5f), SheetDetent.Large)) }
                LabButton("Every detent") { state.updateDetents(detents) }
            }
            DetentReadout(state)
        }
    }
}

@Composable
private fun LabButton(label: String, onClick: () -> Unit) {
    AppButton(text = label, onClick = onClick, variant = ButtonVariant.Plain, size = ButtonSize.Small)
}

/** Where the sheet is, redrawn every frame it moves without composing anything. */
@Composable
private fun DetentReadout(state: AppSheetState) {
    val measurer = rememberTextMeasurer()
    val style = AppTheme.typography.footnote.copy(color = AppTheme.colors.contentSecondary)
    val height = with(LocalDensity.current) { (style.lineHeight * 3).toDp() }
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val top = state.visibleTop
                val text = "current ${state.currentDetent}\ntarget ${state.targetDetent}\n" +
                    if (top.isNaN()) "hidden" else "top ${top.roundToInt()} px"
                drawText(measurer, text, style = style)
            },
    )
}
