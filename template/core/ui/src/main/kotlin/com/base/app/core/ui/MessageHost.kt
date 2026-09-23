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

/** Renders the messages a ViewModel emits, and times them out. */
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

/** A screen that hosts its own messages. */
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

/** The application's message severity, mapped to the design system's visual tone. */
private fun MessageKind.toTone(): AppTone = when (this) {
    MessageKind.Success -> AppTone.Success
    MessageKind.Error -> AppTone.Error
    MessageKind.Warning -> AppTone.Warning
    MessageKind.Info -> AppTone.Info
}
