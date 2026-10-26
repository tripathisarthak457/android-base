package com.example.cmp.core.designsystem.component.container

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme

/**
 * A separator one physical pixel thick, as iOS draws them. Between rows of one list, prefer
 * `Modifier.hairline` on the rows, which adds no layout node per divider.
 */
@Composable
public fun AppDivider(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.separator,
    startIndent: Dp = 0.dp,
) {
    val pixel = with(LocalDensity.current) { 1f.toDp() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(pixel),
    ) {
        drawRect(color)
    }
}

@Composable
public fun AppVerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.separator,
) {
    val pixel = with(LocalDensity.current) { 1f.toDp() }
    Canvas(modifier = modifier.fillMaxHeight().width(pixel)) { drawRect(color) }
}

@AppComponentPreviews
@Composable
private fun AppDividerPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppText(text = "Above")
        AppDivider()
        AppText(text = "Between")
        AppDivider(startIndent = AppTheme.spacing.lg)
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppText(text = "Left")
            AppVerticalDivider()
            AppText(text = "Right")
        }
    }
}
