package com.example.finalafter.crui3.catalog.sections.sheets

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import com.example.finalafter.crui3.core.designsystem.component.sheet.AppSheet
import com.example.finalafter.crui3.core.designsystem.component.sheet.AppSheetDefaults
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetDetent
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetModality
import com.example.finalafter.crui3.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.finalafter.crui3.core.designsystem.component.sheet.content.SheetColumn
import com.example.finalafter.crui3.core.designsystem.component.sheet.rememberAppSheetState
import com.example.finalafter.crui3.core.designsystem.component.text.AppText
import com.example.finalafter.crui3.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui3.core.designsystem.preview.AppScreenPreviews
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crui3.core.designsystem.theme.SheetMaterial

// A preview shows a sheet resting at its first detent, so each of these renders the demo's sheet as
// it settles: at every window size, at twice the font size and in dark theme.

@AppScreenPreviews
@Composable
private fun TwoDetentSheetPreview() {
    AppSheet(
        visible = true,
        onDismissRequest = {},
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        header = { AppSheetHeader(title = "Filters") },
    ) {
        LazyColumn(
            contentPadding = contentPadding,
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) { sampleRows(40) }
    }
}

@AppScreenPreviews
@Composable
private fun FitContentSheetPreview() {
    AppSheet(
        visible = true,
        onDismissRequest = {},
        state = rememberAppSheetState(listOf(SheetDetent.FitContent)),
        header = { AppSheetHeader(title = "Delivery") },
    ) {
        SheetColumn {
            AppText(text = "Tomorrow, between 9 and 11, by bike.", style = AppTheme.typography.body)
        }
    }
}

@AppScreenPreviews
@Composable
private fun PersistentSheetPreview() {
    AppSheet(
        visible = true,
        onDismissRequest = {},
        state = rememberAppSheetState(listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large), SheetDetent.Medium),
        modality = SheetModality.NonModal(upThrough = SheetDetent.Medium),
        dismissible = false,
        style = AppSheetDefaults.style(material = SheetMaterial.Frosted),
        header = { AppSheetHeader(title = "Places", trailing = null) },
    ) {
        LazyColumn(
            contentPadding = contentPadding,
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) { sampleRows(20, prefix = "Place") }
    }
}
