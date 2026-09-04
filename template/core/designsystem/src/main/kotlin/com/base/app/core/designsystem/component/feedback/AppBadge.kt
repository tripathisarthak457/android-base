package com.base.app.core.designsystem.component.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.StatusColors

/**
 * A count badge, anchored to the top-right of whatever it decorates.
 *
 * `99+` rather than an unbounded number: a three-digit count is wider than the icon it sits on,
 * which pushes the badge out of the tab bar entirely. The cap is applied here so no caller has to
 * remember it.
 *
 * It animates in and out with a scale rather than appearing instantly, because a badge popping
 * into existence with no transition reads as a rendering glitch on a bar the user is looking at.
 */
@Composable
fun AppBadgedBox(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    color: Color = AppTheme.colors.danger.content,
    contentColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()

        AnimatedVisibility(
            visible = count > 0,
            enter = scaleIn(tween(AppTheme.motion.quick)) + fadeIn(),
            exit = scaleOut(tween(AppTheme.motion.instant)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-6).dp),
        ) {
            val label = if (count > maxCount) "$maxCount+" else count.toString()
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .background(color, AppTheme.shapes.pill)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = label,
                    style = AppTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A bare dot, for "there is something new here" with no number to show. */
@Composable
fun AppDotBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.danger.content,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(tween(AppTheme.motion.quick)) + fadeIn(),
            exit = scaleOut(tween(AppTheme.motion.instant)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, AppTheme.shapes.pill)
                    .clearAndSetSemantics {},
            )
        }
    }
}

/**
 * A status pill — "Delivered", "Pending", "Failed".
 *
 * Takes a [StatusColors] rather than a foreground and a background, so a caller picks a *meaning*
 * (`AppTheme.colors.danger`) and cannot pair a red label with an amber fill.
 */
@Composable
fun AppStatusPill(
    text: String,
    status: StatusColors,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .background(status.subtle, AppTheme.shapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { AppIcon(it, contentDescription = null, tint = status.content, size = 14.dp) }
        AppText(
            text = text,
            style = AppTheme.typography.labelSmall,
            color = status.content,
            maxLines = 1,
        )
    }
}
