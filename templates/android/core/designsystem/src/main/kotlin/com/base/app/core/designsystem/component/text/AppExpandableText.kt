package com.base.app.core.designsystem.component.text

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion

/**
 * Text cut to [collapsedMaxLines] with a Show more control, which appears only when something was
 * actually cut. The height animates both ways, and the choice survives rotation.
 */
@Composable
fun AppExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 3,
    style: TextStyle = AppTheme.typography.bodyMedium,
    color: Color = AppTheme.colors.contentPrimary,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    // Measured, not guessed from the length: whether text overflows depends on the width and the
    // font, and a control that expands nothing is worse than none.
    var cut by remember(text, collapsedMaxLines) { mutableStateOf(false) }
    val motion = AppTheme.motion
    val resize: FiniteAnimationSpec<IntSize> = if (rememberReduceMotion()) {
        snap()
    } else {
        tween(motion.medium, easing = motion.standard)
    }

    Column(modifier = modifier.animateContentSize(resize)) {
        AppText(
            text = text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) cut = layout.hasVisualOverflow },
        )
        if (cut || expanded) {
            AppButton(
                text = stringResource(
                    if (expanded) R.string.designsystem_show_less else R.string.designsystem_show_more,
                ),
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.Start),
                variant = ButtonVariant.Tertiary,
                size = ButtonSize.Small,
            )
        }
    }
}
