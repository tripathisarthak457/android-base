package com.example.cmp.catalog.sections.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cmp.catalog.CatalogGroup
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.button.ButtonVariant
import com.example.cmp.core.designsystem.component.list.AppRow
import com.example.cmp.core.designsystem.component.list.AppSection
import com.example.cmp.core.designsystem.component.list.AppToggleRow
import com.example.cmp.core.designsystem.component.selection.AppSegmentedControl
import com.example.cmp.core.designsystem.component.selection.AppSlider
import com.example.cmp.core.designsystem.component.sheet.AppSheet
import com.example.cmp.core.designsystem.component.sheet.AppSheetDefaults
import com.example.cmp.core.designsystem.component.sheet.AppSheetProperties
import com.example.cmp.core.designsystem.component.sheet.AppSheetScope
import com.example.cmp.core.designsystem.component.sheet.SheetDetent
import com.example.cmp.core.designsystem.component.sheet.chrome.AppSheetCloseButton
import com.example.cmp.core.designsystem.component.sheet.chrome.AppSheetGrabber
import com.example.cmp.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.cmp.core.designsystem.component.sheet.content.SheetColumn
import com.example.cmp.core.designsystem.component.sheet.rememberAppSheetState
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.designsystem.theme.SheetMaterial

/** How a sheet looks: its material, inset and corners, artwork under the grabber, and its headers. */
@Composable
internal fun StyleDemos() {
    CatalogGroup(
        "Materials",
        caption = "Solid is opaque; Frosted is drawn translucency that samples nothing; Glass samples the scene where the " +
            "liquid glass feature is on, and draws Frosted elsewhere. Each becomes opaque on its way to full height.",
    ) {
        StylesDemo()
    }
    CatalogGroup("Artwork", caption = "Content that starts under the grabber, which turns to its contrast form over it.") {
        Triggers { MediaDemo() }
    }
    CatalogGroup("Headers", caption = "Slots, not flags: a title, Cancel and Done, a subtitle, or no header at all.") {
        Triggers { HeaderDemos() }
    }
}

@Composable
private fun StylesDemo() {
    var shown by remember { mutableStateOf(false) }
    var material by remember { mutableIntStateOf(0) }
    var inset by remember { mutableStateOf(true) }
    var grouped by remember { mutableStateOf(false) }
    var grabber by remember { mutableStateOf(true) }
    val styleCorner = AppTheme.style.sheetRadius.value
    var corner by remember { mutableFloatStateOf(styleCorner) }
    AppSegmentedControl(options = MaterialNames, selected = material, onSelect = { material = it })
    AppToggleRow(title = "Floats inset at partial heights", checked = inset, onCheckedChange = { inset = it })
    AppToggleRow(title = "Grouped colours", checked = grouped, onCheckedChange = { grouped = it })
    AppToggleRow(title = "Grabber", checked = grabber, onCheckedChange = { grabber = it })
    AppText(text = "Corner radius: ${corner.toInt()} dp", style = AppTheme.typography.footnote)
    AppSlider(value = corner, onValueChange = { corner = it }, valueRange = 0f..40f)
    DemoTrigger("styles", "Show", onClick = { shown = true })
    val base = AppSheetDefaults.style(
        material = Materials[material],
        colors = if (grouped) AppSheetDefaults.groupedColors() else AppSheetDefaults.colors(),
        cornerRadius = corner.dp,
    )
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        style = if (inset) base else base.copy(floatingInset = 0.dp),
        grabber = if (grabber) GrabberSlot else null,
        header = { AppSheetHeader(title = MaterialNames[material]) },
    ) {
        SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppSection {
                repeat(8) { AppRow(title = "Row ${it + 1}", value = "Value") }
            }
        }
    }
}

/** Now playing: artwork to the top corners, a gradient body, the grabber over the picture. */
@Composable
private fun MediaDemo() {
    var shown by remember { mutableStateOf(false) }
    DemoTrigger("media", "Now playing", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        properties = AppSheetProperties(contentUnderGrabber = true),
        style = AppSheetDefaults.style(containerBrush = Brush.verticalGradient(listOf(Color(0xFF3A1C71), Color(0xFF1B1B2F)))),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFFD76D77), Color(0xFFFFAF7B)))),
            )
            Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                AppText(text = "Evening Tide", style = AppTheme.typography.title2, color = Color.White)
                AppText(text = "The Harbour Lights", style = AppTheme.typography.body, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun HeaderDemos() {
    var shown by remember { mutableStateOf(false) }
    var kind by remember { mutableIntStateOf(0) }
    HeaderNames.forEachIndexed { index, name ->
        DemoTrigger(
            "headers_$index",
            name,
            onClick = {
                kind = index
                shown = true
            },
        )
    }
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        paneTitle = if (HeaderSamples[kind] == null) "Shared album" else null,
        header = HeaderSamples[kind],
    ) {
        LazyColumn(
            contentPadding = contentPadding,
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) { sampleRows(20) }
    }
}

private val MaterialNames = listOf("Solid", "Frosted", "Glass")

private val GrabberSlot: @Composable AppSheetScope.() -> Unit = { AppSheetGrabber() }

private val Materials = listOf(SheetMaterial.Solid, SheetMaterial.Frosted, SheetMaterial.Glass)

private val HeaderNames = listOf("Title only", "Cancel and Done", "Close only", "Subtitle", "Long title", "No header")

/** One header per name above; the last sheet has none and is named by its pane title. */
private val HeaderSamples: List<(@Composable AppSheetScope.() -> Unit)?> = listOf(
    { AppSheetHeader(title = "Title only", trailing = null) },
    {
        AppSheetHeader(
            title = "Edit event",
            leading = { AppButton(text = "Cancel", onClick = { requestDismiss() }, variant = ButtonVariant.Plain) },
            trailing = { AppButton(text = "Done", onClick = { dismiss() }, variant = ButtonVariant.Plain) },
        )
    },
    { AppSheetHeader(title = {}, trailing = { AppSheetCloseButton() }) },
    { AppSheetHeader(title = "Photos", subtitle = "128 items") },
    { AppSheetHeader(title = "A title long enough to need a second line at a large font size") },
    null,
)
