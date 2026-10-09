package com.base.app.catalog.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.base.app.catalog.CatalogGroup
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.list.AppSection
import com.base.app.core.designsystem.component.overlay.AppSheet
import com.base.app.core.designsystem.component.overlay.LocalOverlayHost
import com.base.app.core.designsystem.component.overlay.ToastMessage
import com.base.app.core.designsystem.component.selection.AppSegmentedControl
import com.base.app.core.designsystem.component.selection.AppSwitch
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppHaptic
import com.base.app.core.designsystem.foundation.NoIndication
import com.base.app.core.designsystem.foundation.PressFeedback
import com.base.app.core.designsystem.foundation.RowHighlight
import com.base.app.core.designsystem.foundation.minimumTouchTarget
import com.base.app.core.designsystem.foundation.pressable
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppMotionStyle
import com.base.app.core.designsystem.theme.AppTheme

/** Press feedback, selection and motion side by side; the page checkpoint recordings are made from. */
@Composable
fun FeelSection() {
    CatalogGroup("Press feedback", caption = "Filled, tinted and destructive buttons shrink; plain ones dim; rows highlight. Nothing flashes grey.") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppButton(text = "Filled", onClick = {}, modifier = Modifier.testTag("cp1_filled"))
            AppButton(text = "Tinted", onClick = {}, variant = ButtonVariant.Tinted, modifier = Modifier.testTag("cp1_tinted"))
            AppButton(text = "Plain", onClick = {}, variant = ButtonVariant.Plain, modifier = Modifier.testTag("cp1_plain"))
            AppButton(text = "Delete", onClick = {}, variant = ButtonVariant.Destructive)
            AppIconButton(AppIcons.Heart, contentDescription = "Like", onClick = {})
        }
        AppCard(onClick = {}, modifier = Modifier.fillMaxWidth().testTag("cp1_card")) {
            AppText(text = "A card that opens something", style = AppTheme.typography.headline)
            AppText(text = "It shrinks by less than a button, and lays no film over what it shows.", style = AppTheme.typography.subheadline, color = AppTheme.colors.contentSecondary)
        }
        AppSection {
            AppRow(title = "A row", subtitle = "Highlights at once, fades after the finger lifts", onClick = {}, modifier = Modifier.testTag("cp1_row"))
            AppRow(title = "Another row", value = "Detail", onClick = {})
        }
    }
    CatalogGroup("The modifier itself", caption = "Modifier.pressable with each PressFeedback, on a plain box. minimumTouchTarget reserves 48dp around a small one.") {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            PressFeedback.entries.forEach { feedback ->
                Box(
                    Modifier
                        .pressable(onClick = {}, feedback = feedback)
                        .size(64.dp)
                        .background(AppTheme.colors.accentSubtle, AppTheme.shapes.md),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(text = feedback.name, style = AppTheme.typography.caption2, color = AppTheme.colors.accentContent)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.minimumTouchTarget().size(16.dp).background(AppTheme.colors.accent, AppTheme.shapes.pill))
            // Foundation's own clickables take an indication explicitly; these are the two the design
            // system offers them.
            Box(
                Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = RowHighlight) {}
                    .size(width = 120.dp, height = 48.dp)
                    .background(AppTheme.colors.fill, AppTheme.shapes.sm),
                contentAlignment = Alignment.Center,
            ) { AppText(text = "RowHighlight", style = AppTheme.typography.caption1) }
            Box(
                Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = NoIndication) {}
                    .size(width = 120.dp, height = 48.dp)
                    .background(AppTheme.colors.fill, AppTheme.shapes.sm),
                contentAlignment = Alignment.Center,
            ) { AppText(text = "NoIndication", style = AppTheme.typography.caption1) }
        }
    }
    CatalogGroup("Selection", caption = "The selection moving is the feedback: no press overlay, a tick on change. Drag the thumb across either control.") {
        var on by remember { mutableStateOf(true) }
        var segment by remember { mutableIntStateOf(0) }
        AppSwitch(checked = on, onCheckedChange = { on = it }, modifier = Modifier.testTag("cp1_switch"))
        AppSegmentedControl(options = listOf("Day", "Week", "Month", "Year"), selected = segment, onSelect = { segment = it }, modifier = Modifier.testTag("cp1_segmented"))
    }
    CatalogGroup("Sheet and toast", caption = "The sheet rests at medium, drags to large and down to close. The toast rises above whatever covers the bottom edge.") {
        var sheet by remember { mutableStateOf(false) }
        val overlays = LocalOverlayHost.current
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppButton(text = "Open sheet", onClick = { sheet = true }, variant = ButtonVariant.Tinted, modifier = Modifier.testTag("cp1_sheet_open"))
            AppButton(
                text = "Show toast",
                onClick = { overlays?.toasts?.show(ToastMessage(text = "Saved to your library", actionLabel = "Undo", onAction = {})) },
                variant = ButtonVariant.Tinted,
                modifier = Modifier.testTag("cp1_toast"),
            )
        }
        AppSheet(visible = sheet, onDismissRequest = { sheet = false }, title = "A sheet", showCloseButton = true) {
            AppSection {
                repeat(12) { AppRow(title = "Row ${it + 1}", onClick = {}) }
            }
        }
    }
    CatalogGroup("Motion styles", caption = "Pick one at the top of the index; every page then moves this way.") {
        AppMotionStyle.entries.forEach { style ->
            AppText(text = "${style.name}: ${style.summary}", style = AppTheme.typography.subheadline)
        }
    }
    CatalogGroup("Haptics", caption = "Silent on ordinary taps, as iOS is. Each of these is the one moment it plays.") {
        val haptics = rememberAppHaptics()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppHaptic.entries.forEach { haptic ->
                AppButton(text = haptic.name, onClick = { haptics.perform(haptic) }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
            }
        }
    }
}
