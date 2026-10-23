package com.base.app.catalog.sections.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.base.app.catalog.CatalogGroup
import com.base.app.catalog.LocalCatalogEnvironment
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.sheetTrigger
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Every sheet the design system can draw, each opened from a button tagged `catalog_sheet_<id>` so
 * the device scripts can open it. A band of colour sits at the top of the page, so the dim, the card
 * stack and the translucent materials have something to read against.
 */
@Composable
fun SheetsSection() {
    CatalogGroup("Backdrop", caption = "Colour behind the sheets: the dim, the receding page and the frosted materials read against it.") {
        Backdrop()
    }
    DetentDemos()
    BehaviourDemos()
    PlacementDemos()
    StyleDemos()
    PresetDemos()
}

/**
 * A button that opens the demo [id]: its test tag is what `record.sh` and `perf.sh` tap. Given the
 * state of a sheet with a list, it prepares that sheet while a finger is down on it.
 */
@Composable
internal fun DemoTrigger(
    id: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prepares: AppSheetState? = null,
) {
    val trigger = prepares?.takeIf { LocalCatalogEnvironment.current.prepareSheets }
    AppButton(
        text = label,
        onClick = onClick,
        modifier = modifier
            .testTag("catalog_sheet_$id")
            .then(if (trigger != null) Modifier.sheetTrigger(trigger) else Modifier),
        variant = ButtonVariant.Tinted,
        size = ButtonSize.Small,
    )
}

/** Triggers laid out as a wrapping row. */
@Composable
internal fun Triggers(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) { content() }
}

/** [count] rows keyed by index, the body most demos scroll. */
internal fun LazyListScope.sampleRows(count: Int, prefix: String = "Row") {
    items(count, key = { it }, contentType = { "row" }) { index ->
        AppRow(title = "$prefix ${index + 1}", subtitle = "Supporting text", onClick = {})
    }
}

@Composable
private fun Backdrop() {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        BackdropColours.forEach { (from, to) ->
            Box(
                Modifier
                    .size(56.dp)
                    .background(Brush.linearGradient(listOf(from, to)), AppTheme.shapes.md),
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Brush.horizontalGradient(BackdropColours.map { it.first }), AppTheme.shapes.lg),
    )
}

private val BackdropColours = listOf(
    Color(0xFFFF6B6B) to Color(0xFFFFD93D),
    Color(0xFF6BCB77) to Color(0xFF4D96FF),
    Color(0xFF9D4EDD) to Color(0xFFFF6FB5),
    Color(0xFF00C2A8) to Color(0xFF0077FF),
)
