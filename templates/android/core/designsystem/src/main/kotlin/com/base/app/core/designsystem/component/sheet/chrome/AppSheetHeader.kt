package com.base.app.core.designsystem.component.sheet.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.base.app.core.designsystem.component.sheet.content.LocalSheetSlots
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * A sheet's title row: [title] centred between [leading] and [trailing] (Cancel, Done, close), never
 * under them: the wider side's width is reserved on both sides. At font scale 1.5 and above the title
 * takes up to two lines and the row grows. The title is a heading for screen readers. Outside a
 * sheet it draws in place, without the scroll-edge fade.
 */
@Composable
public fun AppSheetHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = { AppSheetCloseButton() },
) {
    Layout(
        contents = listOf(
            { leading?.let { Row(verticalAlignment = Alignment.CenterVertically, content = it) } },
            {
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) { heading() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    title()
                    subtitle?.invoke()
                }
            },
            { trailing?.let { Row(verticalAlignment = Alignment.CenterVertically, content = it) } },
        ),
        modifier = modifier.padding(horizontal = AppTheme.spacing.xs),
    ) { (leadingSlot, titleSlot, trailingSlot), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val start = leadingSlot.firstOrNull()?.measure(loose)
        val end = trailingSlot.firstOrNull()?.measure(loose)
        val reserve = maxOf(start?.width ?: 0, end?.width ?: 0)
        val titleWidth = (constraints.maxWidth - 2 * reserve).coerceAtLeast(0)
        val middle = titleSlot.first().measure(Constraints(maxWidth = titleWidth))
        val height = maxOf(
            DesignTokens.Sheet.Header.minHeight.roundToPx(),
            middle.height,
            start?.height ?: 0,
            end?.height ?: 0,
        ).coerceAtMost(constraints.maxHeight)
        layout(constraints.maxWidth, height) {
            start?.placeRelative(0, (height - start.height) / 2)
            middle.placeRelative((constraints.maxWidth - middle.width) / 2, (height - middle.height) / 2)
            end?.placeRelative(constraints.maxWidth - end.width, (height - end.height) / 2)
        }
    }
}

/** [title] and [subtitle] as text; [title] also names the sheet for screen readers. */
@Composable
public fun AppSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = { AppSheetCloseButton() },
) {
    val slots = LocalSheetSlots.current
    if (slots != null) {
        DisposableEffect(slots, title) {
            slots.headerTitle = title
            onDispose { if (slots.headerTitle == title) slots.headerTitle = null }
        }
    }
    val lines = if (LocalDensity.current.fontScale >= LargeTextScale) 2 else 1
    AppSheetHeader(
        title = {
            AppText(
                text = title,
                style = AppTheme.typography.headline,
                color = AppTheme.colors.contentPrimary,
                textAlign = TextAlign.Center,
                maxLines = lines,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        subtitle = subtitle?.let {
            {
                AppText(
                    text = it,
                    style = AppTheme.typography.footnote,
                    color = AppTheme.colors.contentSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = lines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leading = leading,
        trailing = trailing,
    )
}

/** From this font scale the title may wrap to a second line rather than lose its end. */
private const val LargeTextScale = 1.5f

@AppComponentPreviews
@Composable
private fun AppSheetHeaderPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.surfaceElevated),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppSheetHeader(title = "Edit profile")
        AppSheetHeader(title = "Filters", subtitle = "12 results", leading = { AppButton(text = "Reset", onClick = {}) })
        AppSheetHeader(title = { AppText(text = "A title written as its own composable, long enough to wrap", style = AppTheme.typography.headline) })
    }
}
