package com.base.app.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.PlatformFonts

/**
 * The theme's slots as they are filled now: the colour roles as swatches and the type scale, in
 * both themes. It is where a change to the tokens shows first, and the screenshot tests render it,
 * so they always have a case to run before the project has components of its own.
 */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    uiMode =
        Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun ThemeSlotsPreview() {
    AppTheme(fonts = PlatformFonts) { ThemeSlotsPreviewContent() }
}

@Composable
private fun ThemeSlotsPreviewContent() {
    val colors = AppTheme.colors
    val type = AppTheme.typography
    val swatches = listOf(
        "background" to colors.background,
        "surface" to colors.surface,
        "fill" to colors.fill,
        "separator" to colors.separator,
        "outline" to colors.outline,
        "contentPrimary" to colors.contentPrimary,
        "contentSecondary" to colors.contentSecondary,
        "accent" to colors.accent,
        "accentSubtle" to colors.accentSubtle,
        "accentContent" to colors.accentContent,
    )
    val roles = listOf(
        "largeTitle" to type.largeTitle,
        "title1" to type.title1,
        "headline" to type.headline,
        "body" to type.body,
        "footnote" to type.footnote,
        "caption1" to type.caption1,
        "mono" to type.mono,
    )
    Column(
        modifier = Modifier.background(colors.background).padding(AppTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        swatches.forEach { (name, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Box(Modifier.size(AppTheme.sizes.icon).clip(AppTheme.shapes.sm).background(color))
                BasicText(text = name, style = type.footnote.copy(color = colors.contentSecondary))
            }
        }
        roles.forEach { (name, style) ->
            BasicText(text = name, style = style.copy(color = colors.contentPrimary))
        }
    }
}
