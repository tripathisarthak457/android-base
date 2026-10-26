package com.example.finalafter.crui3.core.designsystem.component.sheet.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui3.core.designsystem.R
import com.example.finalafter.crui3.core.designsystem.component.sheet.AppSheetDefaults
import com.example.finalafter.crui3.core.designsystem.component.sheet.AppSheetProperties
import com.example.finalafter.crui3.core.designsystem.component.sheet.AppSheetScope
import com.example.finalafter.crui3.core.designsystem.component.sheet.HostedSheet
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetContentInteraction
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetDetent
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetModality
import com.example.finalafter.crui3.core.designsystem.component.sheet.action.SheetAction
import com.example.finalafter.crui3.core.designsystem.component.sheet.content.SheetColumn
import com.example.finalafter.crui3.core.designsystem.component.sheet.content.sheetContentPadding
import com.example.finalafter.crui3.core.designsystem.component.sheet.rememberAppSheetState
import com.example.finalafter.crui3.core.designsystem.component.text.AppIcon
import com.example.finalafter.crui3.core.designsystem.component.text.AppText
import com.example.finalafter.crui3.core.designsystem.foundation.adaptive.WindowWidth
import com.example.finalafter.crui3.core.designsystem.foundation.content.AppSurface
import com.example.finalafter.crui3.core.designsystem.foundation.draw.HairlineEdge
import com.example.finalafter.crui3.core.designsystem.foundation.draw.disabledAlpha
import com.example.finalafter.crui3.core.designsystem.foundation.draw.hairline
import com.example.finalafter.crui3.core.designsystem.foundation.interaction.PressFeedback
import com.example.finalafter.crui3.core.designsystem.foundation.interaction.pressable
import com.example.finalafter.crui3.core.designsystem.icon.AppIcons
import com.example.finalafter.crui3.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crui3.core.designsystem.theme.tokens.DesignTokens

/**
 * "What would you like to do with this?": actions in a sheet that fits them, with Cancel apart. At
 * Medium width and up it is a card anchored to [anchor], without Cancel; with no anchor it is centred.
 * An action runs before the sheet asks to close, so a caller that clears its state on dismissal
 * still has it when the action runs.
 */
@Composable
fun AppActionSheet(
    visible: Boolean,
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String = stringResource(R.string.designsystem_cancel),
    anchor: SheetAnchor? = null,
) {
    if (AppTheme.windowClass.width >= WindowWidth.Medium) {
        ActionCard(visible, actions, onDismissRequest, title, message, anchor, modifier)
        return
    }
    val state = rememberAppSheetState(listOf(ActionFit))
    val colors = AppSheetDefaults.colors(container = Color.Transparent, rim = Color.Transparent)
    val style = AppSheetDefaults.style(colors = colors, shadow = null)
    HostedSheet(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        state = state,
        modality = SheetModality.Modal,
        dismissible = true,
        properties = ScrollingProperties,
        style = style,
        paneTitle = title ?: stringResource(R.string.designsystem_actions_pane),
        onDetentChange = null,
        onHidden = null,
        grabber = null,
        header = null,
        footer = null,
        content = { ActionSheetContent(actions, title, message, cancelLabel) },
        asDialog = true,
    )
}

@Composable
private fun AppSheetScope.ActionSheetContent(actions: List<SheetAction>, title: String?, message: String?, cancelLabel: String) {
    val sheet = sheetContentPadding()
    val inset = AppTheme.spacing.sm
    val padding = remember(sheet, inset) { GroupPadding(sheet, inset) }
    val cancel = remember(cancelLabel) { SheetAction(cancelLabel, onClick = {}) }
    SheetColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(inset)) {
        AppSurface(modifier = Modifier.fillMaxWidth(), shape = AppTheme.shapes.lg, color = AppTheme.colors.surfaceElevated, clipContent = true) {
            Column {
                if (title != null || message != null) ActionHeading(title, message)
                actions.forEachIndexed { index, action ->
                    ActionRow(
                        action = action,
                        divider = index > 0 || title != null || message != null,
                        onClick = {
                            action.onClick()
                            dismiss()
                        },
                    )
                }
            }
        }
        AppSurface(modifier = Modifier.fillMaxWidth(), shape = AppTheme.shapes.lg, color = AppTheme.colors.surfaceElevated, clipContent = true) {
            ActionRow(action = cancel, divider = false, bold = true, onClick = ::dismiss)
        }
    }
}

@Composable
internal fun ActionHeading(title: String?, message: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppActionSheetDefaults.HeadingSpacing),
    ) {
        val colors = AppTheme.colors
        title?.let {
            AppText(
                text = it,
                style = AppTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = colors.contentSecondary,
                textAlign = TextAlign.Center,
            )
        }
        message?.let { AppText(text = it, style = AppTheme.typography.footnote, color = colors.contentSecondary, textAlign = TextAlign.Center) }
    }
}

/** A row at least 56 dp tall, which grows when its label wraps at a large font scale. */
@Composable
internal fun ActionRow(
    action: SheetAction,
    divider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
) {
    val colors = AppTheme.colors
    val color = if (action.destructive) colors.danger.content else colors.accentContent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.hairline(HairlineEdge.Top) else Modifier)
            .pressable(onClick = onClick, feedback = PressFeedback.Highlight, enabled = action.enabled)
            .semantics { if (action.selected) selected = true }
            .disabledAlpha(action.enabled)
            .heightIn(min = DesignTokens.Sheet.ActionSheet.rowHeight)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        action.icon?.let { AppIcon(it, contentDescription = null, tint = color) }
        AppText(
            text = action.label,
            style = if (bold) AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold) else AppTheme.typography.body,
            color = color,
            textAlign = TextAlign.Center,
        )
        if (action.selected) {
            AppIcon(AppIcons.Check, contentDescription = null, tint = colors.accentContent)
        } else if (action.icon != null) {
            Spacer(Modifier.width(AppTheme.sizes.icon))
        }
    }
}

/** The groups keep a narrow margin, as iOS's do, and the sheet's bottom padding. */
private class GroupPadding(private val sheet: PaddingValues, private val inset: Dp) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = inset

    override fun calculateTopPadding(): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = inset

    override fun calculateBottomPadding(): Dp = sheet.calculateBottomPadding()
}

/** One stop: the actions' own height, at most 0.6 of Large's, scrolling beyond that. */
private val ActionFit = SheetDetent.Custom("action-fit") {
    minOf(contentHeight, maxHeight * DesignTokens.Sheet.ActionSheet.maxFitFraction)
}

private val ScrollingProperties = AppSheetProperties(contentInteraction = SheetContentInteraction.Scrolls)

/** The action sheet's heading. */
object AppActionSheetDefaults {
    val HeadingSpacing = 2.dp
}

@AppComponentPreviews
@Composable
private fun AppActionSheetPreview() {
    Box(modifier = Modifier.background(AppTheme.colors.background).fillMaxWidth().height(AppTheme.sizes.rowHeight * 8)) {
        AppActionSheet(
            visible = true,
            actions = listOf(
                SheetAction(label = "Take photo", onClick = {}, icon = AppIcons.Camera),
                SheetAction(label = "Choose from library", onClick = {}, icon = AppIcons.ImageIcon),
                SheetAction(label = "Remove photo", onClick = {}, destructive = true),
                SheetAction(label = "Use the photo from your account", onClick = {}, enabled = false),
            ),
            onDismissRequest = {},
            title = "Profile photo",
        )
    }
}
