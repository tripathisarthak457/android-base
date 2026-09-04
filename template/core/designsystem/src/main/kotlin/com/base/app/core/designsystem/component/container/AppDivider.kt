package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A hairline.
 *
 * The thickness is a fixed 1dp rather than `Dp.Hairline`. A hairline is a *pixel*, so on a 3.5x
 * density screen it renders a third as thick as it does on a 1x one, and a rule that reads
 * correctly on the test device disappears on a flagship.
 *
 * [startIndent] exists because a divider in a list of rows with leading icons should start where
 * the text does, not at the window edge — a full-bleed rule cuts the icon column off from its own
 * row.
 */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.divider,
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(thickness)
            .background(color),
    )
}

@Composable
fun AppVerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.divider,
    thickness: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness)
            .background(color),
    )
}
