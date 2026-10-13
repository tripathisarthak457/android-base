package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.chromeContentPadding
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/** Why a screen is showing a state instead of its content. */
enum class StateKind { Empty, Error, Offline }

/**
 * What a screen shows instead of its content: nothing here yet, it did not load, or there is no
 * connection. An error or offline state has a title and a "Try again" of its own unless told
 * otherwise. [illustration] replaces the glyph with art.
 *
 * It centres in the room the screen's chrome leaves (under the top bar, above the tab bar), and
 * scrolls when that room is too short for it, so neither its top nor its action is ever covered.
 */
@Composable
fun AppStateView(
    modifier: Modifier = Modifier,
    kind: StateKind = StateKind.Empty,
    title: String? = null,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    illustration: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val heading = title ?: when (kind) {
        StateKind.Empty -> null
        StateKind.Error -> stringResource(R.string.designsystem_error_title)
        StateKind.Offline -> stringResource(R.string.designsystem_offline_title)
    }
    val action = actionLabel ?: if (kind != StateKind.Empty) stringResource(R.string.designsystem_try_again) else null
    val glyph = icon ?: when (kind) {
        StateKind.Empty -> null
        StateKind.Error -> AppIcons.AlertTriangle
        StateKind.Offline -> AppIcons.WifiOff
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState(), flingBehavior = AppScrollDefaults.flingBehavior())
                .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.xl))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterVertically),
        ) {
            when {
                illustration != null -> illustration()
                glyph != null -> AppSurface(
                    modifier = Modifier.size(64.dp),
                    shape = AppTheme.shapes.pill,
                    color = if (kind == StateKind.Empty) colors.accentSubtle else colors.fill,
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppIcon(glyph, contentDescription = null, tint = if (kind == StateKind.Empty) colors.accentContent else colors.contentSecondary, size = 28.dp)
                    }
                }
            }
            heading?.let {
                AppText(text = it, style = AppTheme.typography.title3, color = colors.contentPrimary, textAlign = TextAlign.Center)
            }
            message?.let {
                AppText(
                    text = it,
                    modifier = Modifier.widthIn(max = 320.dp),
                    style = AppTheme.typography.callout,
                    color = colors.contentSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            if (action != null && onAction != null) {
                AppButton(
                    text = action,
                    onClick = onAction,
                    variant = if (kind == StateKind.Empty) ButtonVariant.Tinted else ButtonVariant.Filled,
                    modifier = Modifier.padding(top = AppTheme.spacing.sm),
                )
            }
        }
    }
}
