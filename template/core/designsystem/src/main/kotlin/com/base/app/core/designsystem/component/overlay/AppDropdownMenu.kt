package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A menu anchored to whatever composed it.
 *
 * Built on `Popup`, which is `compose.ui` rather than Material — the anchoring, the outside-tap
 * dismissal and the back-press handling all come from the platform primitive, so what is left is
 * the surface, the entrance, and the rows.
 *
 * It scales out of its top-start corner rather than fading in place, which is what connects it
 * visually to the control that opened it. A menu that fades in centred appears to come from
 * nowhere.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!expanded) return

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0.9f,
            animationSpec = AppTheme.motion.sheet(),
            label = "menuScale",
        )
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(AppTheme.motion.quick),
            label = "menuAlpha",
        )

        AppSurface(
            modifier = modifier
                .padding(top = AppTheme.spacing.xs)
                .widthIn(min = MIN_WIDTH, max = MAX_WIDTH)
                .scale(scale)
                .alpha(alpha),
            shape = AppTheme.shapes.sm,
            color = AppTheme.colors.surface,
            elevation = AppTheme.elevation.overlay,
        ) {
            Column(modifier = Modifier.padding(vertical = AppTheme.spacing.xs)) { content() }
        }
    }
}

/**
 * A row in a menu.
 *
 * [destructive] colours the whole row, icon included, rather than only the label. A red word next
 * to a neutral icon reads as an accident; the two agreeing reads as a decision.
 */
@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val tint = when {
        !enabled -> AppTheme.colors.contentDisabled
        destructive -> AppTheme.colors.danger.content
        else -> AppTheme.colors.contentPrimary
    }

    Row(
        modifier = modifier
            .appClickable(onClick = onClick, enabled = enabled, minTouchTarget = 0.dp)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { AppIcon(it, contentDescription = null, tint = tint) }
        AppText(
            text = text,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.bodyMedium,
            color = tint,
            maxLines = 1,
        )
        trailing?.invoke()
    }
}

@Composable
fun AppMenuDivider(modifier: Modifier = Modifier) {
    AppDivider(
        modifier = modifier.padding(vertical = AppTheme.spacing.xs),
        color = AppTheme.colors.divider,
    )
}

private val MIN_WIDTH = 180.dp
private val MAX_WIDTH = 320.dp
