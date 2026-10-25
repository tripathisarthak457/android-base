package com.base.app.core.designsystem.component.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A row that opens and closes the content beneath it, its chevron turning to point at what it
 * revealed. A screen reader hears whether it is expanded.
 */
@Composable
public fun AppDisclosure(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transitions = rememberAppTransitions()
    val turn = animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = AppTheme.springs.thumb.unit,
        label = "disclosureChevron",
    )
    val state = stringResource(if (expanded) R.string.designsystem_expanded else R.string.designsystem_collapsed)
    val action = stringResource(if (expanded) R.string.designsystem_collapse else R.string.designsystem_expand)
    // The chevron is mirrored in right-to-left layouts, so it turns the other way to point down.
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(
                    onClick = { onExpandedChange(!expanded) },
                    feedback = PressFeedback.Highlight,
                    enabled = enabled,
                    onClickLabel = action,
                )
                .semantics { stateDescription = state }
                .disabledAlpha(enabled)
                .heightIn(min = AppTheme.sizes.rowHeight)
                .padding(horizontal = AppTheme.spacing.lg, vertical = AppDisclosureDefaults.VerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDisclosureDefaults.TitleSpacing)) {
                AppText(text = title, style = AppTheme.typography.body, color = AppTheme.colors.contentPrimary)
                subtitle?.let {
                    AppText(text = it, style = AppTheme.typography.subheadline, color = AppTheme.colors.contentSecondary)
                }
            }
            AppIcon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = AppTheme.colors.contentTertiary,
                size = AppDisclosureDefaults.ChevronSize,
                modifier = Modifier.graphicsLayer { rotationZ = direction * turn.value },
            )
        }

        AnimatedVisibility(visible = expanded, enter = transitions.expandIn, exit = transitions.collapseOut) {
            Column(modifier = Modifier.padding(horizontal = AppTheme.spacing.lg).padding(bottom = AppTheme.spacing.md), content = content)
        }
    }
}

/** The disclosure row's metrics, a row's. */
public object AppDisclosureDefaults {
    public val VerticalPadding: Dp = 11.dp
    public val TitleSpacing: Dp = 2.dp
    public val ChevronSize: Dp = 16.dp
}

@AppComponentPreviews
@Composable
private fun AppDisclosurePreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppDisclosure(title = "Shipping", expanded = false, onExpandedChange = {}, subtitle = "2 to 4 working days") {
            AppText(text = "Free over 50.")
        }
        AppDisclosure(title = "Returns", expanded = true, onExpandedChange = {}) {
            AppText(text = "Return anything within 30 days, in its original packaging.")
        }
        AppDisclosure(title = "Warranty", expanded = false, onExpandedChange = {}, enabled = false) {
            AppText(text = "Two years.")
        }
    }
}
