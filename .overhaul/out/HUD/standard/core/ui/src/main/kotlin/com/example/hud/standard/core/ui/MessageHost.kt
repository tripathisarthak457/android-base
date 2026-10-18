package com.example.hud.standard.core.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.hud.standard.core.designsystem.component.feedback.AppSnackbar
import com.example.hud.standard.core.designsystem.component.feedback.AppSnackbarHost
import com.example.hud.standard.core.designsystem.component.feedback.AppTone
import com.example.hud.standard.core.mvi.MessageKind
import com.example.hud.standard.core.mvi.UiMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Renders the messages a ViewModel emits, and times them out. Collected only while the screen is
 * started: a message taken from the channel while the app is in the background would time out
 * with nothing on screen, and the person would never see it.
 */
@Composable
fun BoxScope.MessageHost(messages: Flow<UiMessage>) {
    var current by remember { mutableStateOf<UiMessage?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(messages, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            messages.collect { current = it }
        }
    }

    // Keyed on the id, so the same words emitted twice restart the timer instead of being ignored.
    LaunchedEffect(current?.id) {
        val message = current ?: return@LaunchedEffect
        delay(message.durationMillis)
        if (current?.id == message.id) current = null
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

/** The application's message severity, mapped to the design system's visual tone. */
private fun MessageKind.toTone(): AppTone = when (this) {
    MessageKind.Success -> AppTone.Success
    MessageKind.Error -> AppTone.Error
    MessageKind.Warning -> AppTone.Warning
    MessageKind.Info -> AppTone.Info
}
