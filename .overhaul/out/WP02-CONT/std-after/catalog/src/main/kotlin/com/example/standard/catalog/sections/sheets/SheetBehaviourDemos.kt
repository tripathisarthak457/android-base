package com.example.standard.catalog.sections.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.standard.catalog.CatalogGroup
import com.example.standard.core.designsystem.component.button.AppButton
import com.example.standard.core.designsystem.component.input.AppTextField
import com.example.standard.core.designsystem.component.overlay.AppAlert
import com.example.standard.core.designsystem.component.sheet.AppSheet
import com.example.standard.core.designsystem.component.sheet.AppSheetDefaults
import com.example.standard.core.designsystem.component.sheet.AppSheetProperties
import com.example.standard.core.designsystem.component.sheet.SheetDetent
import com.example.standard.core.designsystem.component.sheet.SheetModality
import com.example.standard.core.designsystem.component.sheet.SheetTransition
import com.example.standard.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.standard.core.designsystem.component.sheet.content.SheetColumn
import com.example.standard.core.designsystem.component.sheet.content.SheetDismissConfirmation
import com.example.standard.core.designsystem.component.sheet.content.SheetDismissGuard
import com.example.standard.core.designsystem.component.sheet.content.sheetDragExclusion
import com.example.standard.core.designsystem.component.sheet.rememberAppSheetState
import com.example.standard.core.designsystem.component.text.AppText
import com.example.standard.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.designsystem.theme.SheetMaterial

/** What a sheet does: guarded closing, stacking, holding still, cross-fading, Back and screen readers. */
@Composable
internal fun BehaviourDemos() {
    CatalogGroup(
        "Closing and Back",
        caption = "A dirty form asks before it goes, whichever way the person closes it. Back shrinks the sheet as it " +
            "follows the gesture; a guarded sheet resists and springs back.",
    ) {
        Triggers {
            ComposeDemo()
            GuardDemo()
        }
    }
    CatalogGroup("Stack", caption = "A sheet opened from a sheet stacks on it; the one beneath recedes and dims.") {
        StackDemo()
    }
    CatalogGroup("Holding still", caption = "No drag: the sheet stays where it is while a finger draws. Back and the close button still work.") {
        Triggers { HoldDemo() }
    }
    CatalogGroup(
        "Reduce motion",
        caption = "A cross-fade instead of the slide. With \"Remove animations\" on, every sheet changes in one frame.",
    ) {
        Triggers { CrossFadeDemo() }
    }
    CatalogGroup(
        "Screen readers and keyboards",
        caption = "The grabber reads as \"Resize sheet\" with expand, collapse and dismiss actions; the dim reads as \"Close " +
            "sheet\". Focus moves into a modal sheet, Tab stays inside it, Escape closes it, and focus returns on close.",
    ) {
        Triggers { AccessibilityDemo() }
    }
}

/** A message being written: closing it while it has text asks first. */
@Composable
private fun ComposeDemo() {
    var shown by remember { mutableStateOf(false) }
    val message = rememberTextFieldState()
    DemoTrigger("compose", "Compose message", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        header = { AppSheetHeader(title = "New message") },
    ) {
        SheetDismissConfirmation(
            enabled = message.text.isNotEmpty(),
            onDiscard = {
                message.clearText()
                dismiss()
            },
        )
        SheetColumn {
            AppTextField(
                state = message,
                placeholder = "Write something, then try to close",
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 6),
            )
        }
    }
}

/** The raw guard with a question of the app's own. */
@Composable
private fun GuardDemo() {
    var shown by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    DemoTrigger("compose_guard", "Custom question", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        header = { AppSheetHeader(title = "Upload in progress") },
    ) {
        SheetDismissGuard { asking = true }
        AppAlert(
            visible = asking,
            title = "Stop the upload?",
            message = "The 3 photos sent so far stay in the album.",
            confirmLabel = "Stop",
            dismissLabel = "Keep uploading",
            destructive = true,
            onConfirm = {
                asking = false
                dismiss()
            },
            onDismissRequest = { asking = false },
        )
        SheetColumn {
            AppText(text = "Drag down, tap the dim, press Back or Escape: each asks the question once.", style = AppTheme.typography.body)
        }
    }
}

/** Three deep, then a frosted sheet stacked on a solid one. */
@Composable
private fun StackDemo() {
    val shown = remember { mutableStateListOf(false, false, false) }
    DemoTrigger("stack", "Open the first", onClick = { shown[0] = true })
    for (level in 0 until 3) {
        // A sheet's saved state is keyed by its call site, and these share one.
        key(level) {
            AppSheet(
                visible = shown[level],
                onDismissRequest = { shown[level] = false },
                state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
                style = AppSheetDefaults.style(material = if (level == 2) SheetMaterial.Frosted else SheetMaterial.Solid),
                header = { AppSheetHeader(title = "Sheet ${level + 1}") },
            ) {
                SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                    if (level < 2) {
                        AppButton(
                            text = "Open sheet ${level + 2}",
                            onClick = { shown[level + 1] = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        AppText(
                            text = "Frosted, over a solid sheet: it draws without sampling what is beneath.",
                            style = AppTheme.typography.body,
                        )
                    }
                }
            }
        }
    }
}

/** A signature pad: the sheet must not move while the finger draws on it. */
@Composable
private fun HoldDemo() {
    var shown by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(true) }
    DemoTrigger("hold", "Signature", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium)),
        properties = AppSheetProperties(dragEnabled = !held),
        header = { AppSheetHeader(title = "Sign here") },
    ) {
        SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppButton(
                text = if (held) "Let the sheet move, draw in the pad only" else "Hold the whole sheet still",
                onClick = { held = !held },
            )
            SignaturePad(Modifier.sheetDragExclusion())
        }
    }
}

@Composable
private fun SignaturePad(modifier: Modifier = Modifier) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf(emptyList<Offset>()) }
    val ink = AppTheme.colors.contentPrimary
    Canvas(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(AppTheme.shapes.md)
            .background(AppTheme.colors.fill)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { current = listOf(it) },
                    onDragEnd = {
                        strokes += current
                        current = emptyList()
                    },
                    onDrag = { change, _ -> current = current + change.position },
                )
            },
    ) {
        (strokes + listOf(current)).forEach { points ->
            if (points.size < 2) return@forEach
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, ink, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun CrossFadeDemo() {
    var shown by remember { mutableStateOf(false) }
    DemoTrigger("motion", "Cross-fade", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        properties = AppSheetProperties(transition = SheetTransition.CrossFade),
        header = { AppSheetHeader(title = "Fades in and out") },
    ) {
        LazyColumn(
            contentPadding = contentPadding,
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) { sampleRows(20) }
    }
}

/** A form to walk with a keyboard or a screen reader. */
@Composable
private fun AccessibilityDemo() {
    var shown by remember { mutableStateOf(false) }
    val first = rememberTextFieldState()
    val second = rememberTextFieldState()
    DemoTrigger("a11y", "Keyboard and TalkBack", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        modality = SheetModality.Modal,
        paneTitle = "Shipping address",
        header = { AppSheetHeader(title = "Address") },
    ) {
        SheetColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppTextField(state = first, label = "Street")
            AppTextField(state = second, label = "City")
            AppButton(text = "Save", onClick = { dismiss() }, modifier = Modifier.fillMaxWidth())
        }
    }
}
