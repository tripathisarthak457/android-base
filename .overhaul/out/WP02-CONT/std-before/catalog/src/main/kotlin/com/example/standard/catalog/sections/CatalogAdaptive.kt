package com.example.standard.catalog.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.standard.catalog.CatalogGroup
import com.example.standard.catalog.LocalCatalogEnvironment
import com.example.standard.catalog.ProvideEnvironment
import com.example.standard.core.designsystem.component.button.AppButton
import com.example.standard.core.designsystem.component.button.ButtonSize
import com.example.standard.core.designsystem.component.button.ButtonVariant
import com.example.standard.core.designsystem.component.container.AppCard
import com.example.standard.core.designsystem.component.input.AppTextField
import com.example.standard.core.designsystem.component.list.AppRow
import com.example.standard.core.designsystem.component.list.AppSection
import com.example.standard.core.designsystem.component.list.AppToggleRow
import com.example.standard.core.designsystem.component.overlay.LocalOverlayHost
import com.example.standard.core.designsystem.component.overlay.ToastMessage
import com.example.standard.core.designsystem.component.selection.AppSegmentedControl
import com.example.standard.core.designsystem.component.selection.AppSlider
import com.example.standard.core.designsystem.component.sheet.AppSheet
import com.example.standard.core.designsystem.component.sheet.SheetDetent
import com.example.standard.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.standard.core.designsystem.component.sheet.content.SheetColumn
import com.example.standard.core.designsystem.component.sheet.rememberAppSheetState
import com.example.standard.core.designsystem.component.text.AppText
import com.example.standard.core.designsystem.foundation.adaptive.AppTwoPane
import com.example.standard.core.designsystem.foundation.adaptive.InputTraits
import com.example.standard.core.designsystem.foundation.adaptive.ProvideContainerWidth
import com.example.standard.core.designsystem.foundation.adaptive.SizeClass
import com.example.standard.core.designsystem.foundation.adaptive.WindowPosture
import com.example.standard.core.designsystem.foundation.adaptive.WindowWidth
import com.example.standard.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun AdaptiveSection() {
    CatalogGroup(
        "A pane of any width",
        caption = "The pane is laid out at the slider's width and shrunk to fit. What is in it reads the pane's size class, " +
            "so the gutter, the rows' readable width and the two halves change at Android's breakpoints, not at the window's.",
    ) {
        ResizablePane()
    }
    CatalogGroup(
        "Foldable postures",
        caption = "A half-open foldable, simulated. Book puts the halves of AppTwoPane either side of the hinge; " +
            "tabletop rests a sheet on the fold and keeps a picture above it.",
    ) {
        PostureDemo()
    }
    CatalogGroup(
        "Keyboard",
        caption = "With a hardware keyboard, Enter sends what is typed and Shift+Enter starts a new line. " +
            "On a touch keyboard the action key keeps its own meaning.",
    ) {
        KeyboardDemo()
    }
}

@Composable
private fun ResizablePane() {
    var width by remember { mutableFloatStateOf(411f) }
    // Only the class reaches composition: the pane resizes in layout, and its content recomposes
    // when a breakpoint is crossed, as it would in a real window.
    val widthClass by remember { derivedStateOf { SizeClass.of(width, PaneHeight.value).width } }
    AppText(text = "${widthClass.name}, ${widthClass.range()}", style = AppTheme.typography.headline)
    AppSlider(value = width, onValueChange = { width = it }, valueRange = MinPaneWidth..MaxPaneWidth)
    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(AppTheme.colors.backgroundGrouped)
            .layout { measurable, constraints ->
                val paneWidth = width.dp.roundToPx()
                val paneHeight = PaneHeight.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(paneWidth, paneHeight))
                val scale = minOf(1f, constraints.maxWidth / paneWidth.toFloat())
                layout(constraints.maxWidth, (paneHeight * scale).roundToInt()) {
                    placeable.placeWithLayer(0, 0) {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                    }
                }
            },
    ) {
        ProvideContainerWidth(widthClass.representativeWidth()) {
            Column(Modifier.fillMaxSize()) {
                AppTwoPane(
                    first = {
                        AppSection(header = "Settings") {
                            AppRow(title = "Account", onClick = {})
                            AppRow(title = "Notifications", value = "On", onClick = {})
                        }
                    },
                    second = {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            AppText(text = "Detail", style = AppTheme.typography.headline)
                            AppText(
                                text = "Beside the list from Expanded, below it on anything narrower.",
                                style = AppTheme.typography.subheadline,
                                color = AppTheme.colors.contentSecondary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PostureDemo() {
    var posture by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf(false) }
    val window = LocalWindowInfo.current.containerSize
    val hingeWidth = 24f
    val simulated = when (posture) {
        1 -> WindowPosture(WindowPosture.Kind.Book, Rect(window.width / 2f - hingeWidth / 2, 0f, window.width / 2f + hingeWidth / 2, window.height.toFloat()))
        2 -> WindowPosture(WindowPosture.Kind.Tabletop, Rect(0f, window.height / 2f - hingeWidth / 2, window.width.toFloat(), window.height / 2f + hingeWidth / 2))
        else -> WindowPosture()
    }
    AppSegmentedControl(options = listOf("Flat", "Book", "Tabletop"), selected = posture, onSelect = { posture = it })
    ProvideEnvironment(LocalCatalogEnvironment.current, posture = simulated) {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            AppTwoPane(
                first = { DemoHalf("First half") },
                second = { DemoHalf("Second half") },
                modifier = Modifier.fillMaxSize(),
                sideBySide = true,
            )
        }
        AppButton(text = "Show a sheet", onClick = { sheet = true }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
        AppSheet(
            visible = sheet,
            onDismissRequest = { sheet = false },
            state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
            header = { AppSheetHeader(title = "Filters") },
        ) {
            SheetColumn {
                AppSection {
                    AppRow(title = "Newest first", onClick = {})
                    AppRow(title = "Nearest first", onClick = {})
                }
            }
        }
    }
}

@Composable
private fun DemoHalf(label: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surfaceGrouped, AppTheme.shapes.md),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = label, style = AppTheme.typography.subheadline, color = AppTheme.colors.contentSecondary)
    }
}

@Composable
private fun KeyboardDemo() {
    var keyboard by remember { mutableStateOf(true) }
    val message = rememberTextFieldState()
    val toasts = LocalOverlayHost.current?.toasts
    AppToggleRow(title = "Hardware keyboard attached", checked = keyboard, onCheckedChange = { keyboard = it })
    ProvideEnvironment(LocalCatalogEnvironment.current, input = InputTraits(hardwareKeyboard = keyboard)) {
        AppTextField(
            state = message,
            label = "Message",
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 4),
            onSubmit = {
                toasts?.show(ToastMessage(text = "Sent: ${message.text}"))
                message.clearText()
            },
        )
    }
}

private fun WindowWidth.range(): String = when (this) {
    WindowWidth.Compact -> "under 600 dp"
    WindowWidth.Medium -> "600 to 839 dp"
    WindowWidth.Expanded -> "840 to 1199 dp"
    WindowWidth.Large -> "1200 to 1599 dp"
    WindowWidth.ExtraLarge -> "1600 dp and wider"
}

/** A width inside the class: everything the pane's content reads follows from the class alone. */
private fun WindowWidth.representativeWidth() = when (this) {
    WindowWidth.Compact -> 411.dp
    WindowWidth.Medium -> 700.dp
    WindowWidth.Expanded -> 1000.dp
    WindowWidth.Large -> 1300.dp
    WindowWidth.ExtraLarge -> 1700.dp
}

private const val MinPaneWidth = 320f
private const val MaxPaneWidth = 1800f
private val PaneHeight = 300.dp
