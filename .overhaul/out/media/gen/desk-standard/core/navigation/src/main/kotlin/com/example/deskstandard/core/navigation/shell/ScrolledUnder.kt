package com.example.deskstandard.core.navigation.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.example.deskstandard.core.designsystem.theme.AppTheme
import com.example.deskstandard.core.navigation.ShellState
import kotlinx.coroutines.flow.collectLatest

/**
 * How much of the bottom chrome's scroll edge look shows, from 0 while the screen in front rests on
 * the end of its content to 1 while content runs under the bar. It eases between the two when the
 * list reaches or leaves its end, or another screen comes to the front, as UIKit's tab bar changes
 * appearance. The docked bar's surface and the bottom edge fade read it in draw, so the change
 * draws two layers and composes nothing.
 */
@Composable
internal fun rememberScrolledUnder(state: ShellState): () -> Float {
    val reduceMotion = AppTheme.reduceMotion
    val motion = AppTheme.springs
    val shown = remember { Animatable(1f) }
    LaunchedEffect(state, shown, reduceMotion, motion) {
        val spec = tween<Float>(motion.fadeQuick, easing = motion.enterEasing)
        snapshotFlow { state.tabFront?.contentBelow ?: true }.collectLatest { below ->
            val target = if (below) 1f else 0f
            if (reduceMotion) shown.snapTo(target) else shown.animateTo(target, spec)
        }
    }
    return remember(shown) { { shown.value } }
}
