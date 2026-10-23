package io.example.fitcnetdb.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.example.fitcnetdb.core.designsystem.component.feedback.AppTone
import io.example.fitcnetdb.core.designsystem.component.overlay.LocalOverlayHost
import io.example.fitcnetdb.core.designsystem.component.overlay.ToastMessage
import io.example.fitcnetdb.core.mvi.MessageKind
import io.example.fitcnetdb.core.mvi.UiMessage
import kotlinx.coroutines.flow.Flow

/**
 * Shows the messages a ViewModel emits as toasts in the window's toast layer, which rests above the
 * tab bar and any sheet. Collected only while the screen is started: a message taken from the
 * channel while the app is in the background would be shown to no one.
 */
@Composable
fun MessageHost(messages: Flow<UiMessage>) {
    val toasts = LocalOverlayHost.current?.toasts ?: return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(messages, lifecycleOwner, toasts) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            messages.collect { message ->
                toasts.show(
                    ToastMessage(
                        text = message.text.resolve(context),
                        title = message.title?.resolve(context),
                        tone = message.kind.toTone(),
                        actionLabel = message.action?.label?.resolve(context),
                        onAction = message.action?.onClick,
                        durationMillis = message.durationMillis,
                    ),
                )
            }
        }
    }
}

private fun MessageKind.toTone(): AppTone = when (this) {
    MessageKind.Success -> AppTone.Success
    MessageKind.Error -> AppTone.Error
    MessageKind.Warning -> AppTone.Warning
    MessageKind.Info -> AppTone.Info
}
