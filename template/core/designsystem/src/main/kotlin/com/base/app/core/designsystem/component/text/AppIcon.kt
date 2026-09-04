package com.base.app.core.designsystem.component.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.foundation.LocalContentColor
import com.base.app.core.designsystem.theme.AppTheme

/**
 * An icon, tinted to the surrounding content colour unless told otherwise.
 *
 * A null [contentDescription] means the icon is decorative — the label beside it already says
 * what it does — and the semantics are cleared so a screen reader does not announce the same
 * thing twice. That is the common case inside a button; it is the *wrong* case for an icon-only
 * control, where the description is the only thing a screen reader has to go on.
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = AppTheme.sizes.icon,
) {
    val painter = rememberVectorPainter(imageVector)
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .then(if (contentDescription == null) Modifier.clearAndSetSemantics {} else Modifier),
        colorFilter = ColorFilter.tint(tint),
    )
}
