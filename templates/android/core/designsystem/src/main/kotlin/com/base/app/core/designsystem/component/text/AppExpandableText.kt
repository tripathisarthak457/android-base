package com.base.app.core.designsystem.component.text

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppLinkButton
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Text cut to [collapsedMaxLines] with a Show more link, which appears only when something was
 * actually cut. The height follows on the layout spring, and the choice survives rotation. Meant
 * for a description on a detail screen, not for rows in a list: a list row that resizes moves every
 * row below it.
 */
@Composable
fun AppExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 3,
    style: TextStyle = AppTheme.typography.body,
    color: Color = AppTheme.colors.contentPrimary,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    // Measured, not guessed from the length: whether text overflows depends on the width and the
    // font, and a control that expands nothing is worse than none.
    var cut by remember(text, collapsedMaxLines) { mutableStateOf(false) }
    val resize = if (AppTheme.reduceMotion) Modifier else Modifier.animateContentSize(AppTheme.springs.layout.size)

    Column(modifier = modifier.then(resize)) {
        AppText(
            text = text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) cut = layout.hasVisualOverflow },
        )
        if (cut || expanded) {
            AppLinkButton(
                text = stringResource(if (expanded) R.string.designsystem_show_less else R.string.designsystem_show_more),
                onClick = { expanded = !expanded },
                style = AppTheme.typography.subheadline,
            )
        }
    }
}

@AppComponentPreviews
@Composable
private fun AppExpandableTextPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppExpandableText(text = "A short note that fits.")
        AppExpandableText(
            text = "A longer description that runs well past three lines, so the reader sees the first part " +
                "and a way to read the rest: the trail starts at the north car park, climbs through the beech " +
                "wood and comes out on the ridge, where the path follows the old wall to the summit cairn.",
        )
    }
}
