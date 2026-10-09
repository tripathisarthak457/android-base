package com.base.app.core.designsystem.foundation.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Fills the width up to [maxWidth], then stops and centres. Content that stretches across a tablet
 * or a desktop window is content nobody can read: lines too long to follow, buttons a room apart.
 *
 * ```
 * Column(Modifier.centeredMaxWidth(AppTheme.layout.formMaxWidth)) { … }
 * ```
 */
fun Modifier.centeredMaxWidth(maxWidth: Dp): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = maxWidth)

/**
 * Whether a screen with two halves — a picture and its words, a list and its detail — should put
 * them side by side. True from Expanded width, and on anything Medium or wider that is short: a
 * phone held sideways, where stacking the halves would push the second one off screen.
 */
val shouldUseTwoPanes: Boolean
    @Composable get() {
        val container = AppTheme.containerClass
        return container.hasRoomForTwoPanes || (container.isShort && container.width >= WindowWidth.Medium)
    }

/**
 * Two halves of one screen, side by side when [shouldUseTwoPanes] says there is room, stacked
 * otherwise. [firstWeight] is the first half's share of the width when they are side by side.
 */
@Composable
fun AppTwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstWeight: Float = 0.5f,
    sideBySide: Boolean = shouldUseTwoPanes,
    spacing: Dp = AppTheme.spacing.xl,
) {
    if (sideBySide) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(firstWeight)) { first() }
            Box(modifier = Modifier.weight(1f - firstWeight)) { second() }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
            first()
            second()
        }
    }
}
