package com.example.cmp.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.cmp.core.mvi.UiMessage
import kotlinx.coroutines.flow.Flow

/**
 * Hands the messages a ViewModel emits to the [MessagePresenter] in scope. Collected whenever the
 * screen is started, whether or not anything shows them yet: a ViewModel's message buffer that
 * nobody drains fills up, and from then on drops what it is given. A message taken while the app
 * is in the background would be shown to no one, so none is taken then.
 */
@Composable
fun MessageHost(messages: Flow<UiMessage>) {
    val presenter by rememberUpdatedState(LocalMessagePresenter.current)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(messages, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            messages.collect { message -> presenter.present(message.presented(context)) }
        }
    }
}

private fun UiMessage.presented(context: Context) = PresentedMessage(
    text = text.resolve(context),
    title = title?.resolve(context),
    kind = kind,
    durationMillis = durationMillis,
    actionLabel = action?.label?.resolve(context),
    onAction = action?.onClick,
)
