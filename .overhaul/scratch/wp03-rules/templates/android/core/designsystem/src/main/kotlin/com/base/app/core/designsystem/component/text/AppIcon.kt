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

/** An icon, tinted to the surrounding content colour unless told otherwise. */
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
