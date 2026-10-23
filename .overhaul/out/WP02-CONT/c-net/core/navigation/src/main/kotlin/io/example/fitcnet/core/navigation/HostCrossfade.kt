package io.example.fitcnet.core.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.example.fitcnet.core.designsystem.theme.AppTheme

/**
 * Swaps whole navigation hosts, such as onboarding, the signed-out screens and the signed-in
 * shell, with the app's one whole-screen crossfade, at most once per session. While it runs both
 * hosts are composed, and only the arriving one may act: [content] is told whether its host takes
 * the navigator's commands (pass it to [AppShell] or [AppNavigationHost]), and a Back press is
 * swallowed rather than popping the host that is leaving.
 *
 * @param target which host to show; a change starts the crossfade.
 * @param content the host for a value of [target]. `receivesCommands` is false for the one leaving.
 */
@Composable
fun <T> HostCrossfade(
    target: T,
    modifier: Modifier = Modifier,
    content: @Composable (host: T, receivesCommands: Boolean) -> Unit,
) {
    val swap = updateTransition(targetState = target, label = "host")
    val background = AppTheme.colors.background
    swap.AnimatedContent(
        // The window has no background of its own once the app has drawn, and two hosts half faded
        // in and out would show it through each other.
        modifier = modifier.drawBehind { if (swap.isRunning) drawRect(background) },
        transitionSpec = { fadeIn(tween(HOST_FADE_MS)) togetherWith fadeOut(tween(HOST_FADE_MS)) },
    ) { shown ->
        content(shown, shown == target)
    }
    // From the composition that changes the target, not only once the fade has started a frame or two
    // later: a press in between would pop the host that is leaving.
    val crossfading = target != swap.currentState || swap.isRunning
    val idle = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(state = idle, isBackEnabled = crossfading, onBackCompleted = {})
}

private const val HOST_FADE_MS = 200
