package com.base.app.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.base.app.core.common.mvi.MessageKind
import com.base.app.core.common.mvi.UiMessage
import com.base.app.core.designsystem.component.feedback.AppSnackbar
import com.base.app.core.designsystem.component.feedback.AppSnackbarHost
import com.base.app.core.designsystem.component.feedback.AppTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Renders the messages a ViewModel emits, and times them out.
 *
 * ## One at a time, newest wins
 *
 * A new message replaces whatever is showing rather than queueing behind it. Queueing sounds
 * fairer and is worse in practice: three failed requests produce three identical toasts the user
 * has to sit through, and the message that matters — the one about what they just did — is the
 * last to appear.
 *
 * ## The timer is keyed on the message
 *
 * `LaunchedEffect(current)` restarts the countdown whenever the message changes, so a replacement
 * gets its own full duration instead of inheriting the remainder of its predecessor's.
 */
@Composable
fun BoxScope.MessageHost(messages: Flow<UiMessage>) {
    var current by remember { mutableStateOf<UiMessage?>(null) }

    LaunchedEffect(messages) {
        messages.collect { current = it }
    }

    LaunchedEffect(current) {
        val message = current ?: return@LaunchedEffect
        delay(message.durationMillis)
        // Only clear if it is still the same message: a replacement arriving mid-delay starts its
        // own effect, and this one must not dismiss it on the old timer.
        if (current === message) current = null
    }

    AppSnackbarHost(visible = current != null) {
        current?.let { message ->
            AppSnackbar(
                text = message.text.asString(),
                title = message.title?.asString(),
                tone = message.kind.toTone(),
                actionLabel = message.action?.label?.asString(),
                onAction = message.action?.onClick,
                onDismiss = { current = null },
            )
        }
    }
}

/**
 * A screen that hosts its own messages.
 *
 * The wrapper exists so a feature writes `MessagingScaffold(viewModel.messages) { … }` instead of
 * remembering to put a `Box` and a host around every screen — and forgetting on the one screen
 * where a failure most needs to be visible.
 */
@Composable
fun MessagingSurface(
    messages: Flow<UiMessage>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        MessageHost(messages)
    }
}

/**
 * The application's message severity, mapped to the design system's visual tone.
 *
 * Five lines, and they are the reason `:core:designsystem` does not depend on `:core:common` —
 * see [AppTone].
 */
private fun MessageKind.toTone(): AppTone = when (this) {
    MessageKind.Success -> AppTone.Success
    MessageKind.Error -> AppTone.Error
    MessageKind.Warning -> AppTone.Warning
    MessageKind.Info -> AppTone.Info
}
