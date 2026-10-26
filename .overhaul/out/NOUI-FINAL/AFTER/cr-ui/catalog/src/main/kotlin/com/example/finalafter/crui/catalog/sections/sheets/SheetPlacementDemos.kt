package com.example.finalafter.crui.catalog.sections.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui.catalog.CatalogGroup
import com.example.finalafter.crui.core.designsystem.component.input.AppSearchField
import com.example.finalafter.crui.core.designsystem.component.list.AppRow
import com.example.finalafter.crui.core.designsystem.component.list.AppToggleRow
import com.example.finalafter.crui.core.designsystem.component.selection.AppSegmentedControl
import com.example.finalafter.crui.core.designsystem.component.sheet.AppSheet
import com.example.finalafter.crui.core.designsystem.component.sheet.AppSheetProperties
import com.example.finalafter.crui.core.designsystem.component.sheet.SheetDetent
import com.example.finalafter.crui.core.designsystem.component.sheet.SheetModality
import com.example.finalafter.crui.core.designsystem.component.sheet.SheetPlacement
import com.example.finalafter.crui.core.designsystem.component.sheet.action.AppActionSheet
import com.example.finalafter.crui.core.designsystem.component.sheet.action.SheetAction
import com.example.finalafter.crui.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.finalafter.crui.core.designsystem.component.sheet.content.SheetColumn
import com.example.finalafter.crui.core.designsystem.component.sheet.rememberAppSheetState
import com.example.finalafter.crui.core.designsystem.component.text.AppText
import com.example.finalafter.crui.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui.core.designsystem.icon.AppIcons
import com.example.finalafter.crui.core.designsystem.theme.AppTheme

/** Sheets that leave what is behind them usable, and where a sheet sits as the window grows. */
@Composable
internal fun PlacementDemos() {
    CatalogGroup(
        "Beside the content",
        caption = "Non-modal sheets: the page stays usable while they rest low. Scroll this page with one up; it dims " +
            "and blocks only once the sheet is pulled above half height.",
    ) {
        MapGrid()
        Triggers {
            MapsDemo()
            RouteDemo()
        }
    }
    CatalogGroup(
        "Placement",
        caption = "Auto is a bottom sheet on a phone, a centred card on a larger window and a panel at the start " +
            "edge of a wide one. Resize the window, or force one here.",
    ) {
        PlacementDemo()
    }
}

/** A persistent sheet: a peek with a search field, half and full height, never closed by the person. */
@Composable
private fun MapsDemo() {
    var shown by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf<String?>(null) }
    val query = rememberTextFieldState()
    DemoTrigger("maps", if (shown) "Close the places sheet" else "Places", onClick = { shown = !shown })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large)),
        modality = SheetModality.NonModal(upThrough = SheetDetent.Medium),
        dismissible = false,
        header = {
            AppSearchField(
                state = query,
                placeholder = "Search places",
                modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter, vertical = AppTheme.spacing.sm),
            )
        },
    ) {
        LazyColumn(contentPadding = contentPadding, flingBehavior = AppScrollDefaults.flingBehavior()) {
            items(Places, key = { it }, contentType = { "place" }) { place ->
                AppRow(title = place, subtitle = "Open now", onClick = { pin = place })
            }
        }
    }
    AppActionSheet(
        visible = pin != null,
        title = pin,
        actions = listOf(
            SheetAction(label = "Directions", icon = AppIcons.ChevronRight, onClick = {}),
            SheetAction(label = "Share", onClick = {}),
        ),
        onDismissRequest = { pin = null },
    )
}

/** A route's steps beside the page: taps beside the sheet reach the page, Back closes it. */
@Composable
private fun RouteDemo() {
    var shown by remember { mutableStateOf(false) }
    DemoTrigger("route", "Route details", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.FitContent, SheetDetent.Large)),
        modality = SheetModality.NonModal(),
        header = { AppSheetHeader(title = "To the station", subtitle = "18 min, 1.4 km") },
    ) {
        SheetColumn {
            RouteSteps.forEach { AppRow(title = it) }
        }
    }
}

@Composable
private fun PlacementDemo() {
    var placement by remember { mutableIntStateOf(0) }
    var modal by remember { mutableStateOf(true) }
    var shown by remember { mutableStateOf(false) }
    AppSegmentedControl(options = PlacementNames, selected = placement, onSelect = { placement = it })
    AppToggleRow(title = "Modal", checked = modal, onCheckedChange = { modal = it })
    DemoTrigger("placement", "Show", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        modality = if (modal) SheetModality.Modal else SheetModality.NonModal(),
        properties = AppSheetProperties(placement = Placements[placement]),
        header = { AppSheetHeader(title = PlacementNames[placement]) },
    ) {
        LazyColumn(
            contentPadding = contentPadding,
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) { sampleRows(30) }
    }
}

/** A stand-in for a map: a grid with a route drawn across it, so a non-modal sheet has something to sit beside. */
@Composable
private fun MapGrid() {
    val line = AppTheme.colors.separator
    val route = AppTheme.colors.accent
    val ground = AppTheme.colors.fill
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(AppTheme.shapes.lg),
    ) {
        drawRect(ground)
        val step = 24.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height))
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y))
            y += step
        }
        drawLine(
            route,
            Offset(size.width * 0.1f, size.height * 0.8f),
            Offset(size.width * 0.9f, size.height * 0.2f),
            strokeWidth = 4.dp.toPx(),
        )
    }
    AppText(text = "Tap the map's rows and buttons while a non-modal sheet rests low: they still work.", style = AppTheme.typography.footnote)
}

private val Places = listOf(
    "Corner Bakery", "Riverside Park", "City Library", "Night Market", "Harbour Café", "Old Town Square",
    "Botanic Garden", "Central Station", "Museum of Light", "Hill Viewpoint", "Tram Depot", "Fish Market",
)

private val RouteSteps = listOf(
    "Head north on Mill Lane", "Turn right at the bakery", "Cross the bridge", "Keep left at the fork",
    "The station is on your right",
)

private val PlacementNames = listOf("Auto", "Bottom", "Center", "Start", "End")

private val Placements = listOf(
    SheetPlacement.Automatic, SheetPlacement.Bottom, SheetPlacement.Center, SheetPlacement.Start, SheetPlacement.End,
)
