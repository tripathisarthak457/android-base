package com.base.app.core.playstore

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.overlay.LocalOverlayHost
import com.base.app.core.designsystem.component.overlay.ToastMessage
import com.base.app.core.playstore.review.ReviewPrompt
import com.base.app.core.playstore.update.AppUpdates
import com.base.app.core.playstore.update.UpdateState
import kotlin.time.Duration.Companion.seconds

/**
 * Play's update and review prompts, for the app's root. A newer build is offered once per process
 * through Play's own sheet; once it has downloaded, a toast in the window's toast layer offers to
 * restart into it, once, and nothing restarts by itself. The review prompt counts this launch and
 * asks when its policy says so.
 */
@Composable
fun PlayStorePrompts(
    updates: AppUpdates,
    review: ReviewPrompt,
) {
    val activity: Activity? = LocalActivity.current
    val state by updates.state.collectAsStateWithLifecycle()

    var offeredVersion by rememberSaveable { mutableStateOf<Int?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val version = offeredVersion ?: return@rememberLauncherForActivityResult
        offeredVersion = null
        if (result.resultCode != Activity.RESULT_OK) updates.declined(version)
    }

    SideEffect(updates) { updates.check() }
    if (activity != null) {
        LaunchedEffect(review, activity) { review.onLaunch(activity) }
    }

    val offer = state as? UpdateState.Offer
    if (offer != null) {
        // Launched after the frame rather than run in it: Play's sheet is an activity started through the
        // system, a call no frame should wait for.
        LaunchedEffect(offer) {
            offeredVersion = offer.available.updateInfo.availableVersionCode()
            updates.offered()
            offer.available.startFlexibleUpdate(launcher)
        }
    }

    val ready = state as? UpdateState.Ready
    val toasts = LocalOverlayHost.current?.toasts
    val readyText = stringResource(R.string.playstore_update_ready)
    val restartLabel = stringResource(R.string.playstore_update_restart)
    // Saved, so a configuration change does not offer the restart a second time.
    var readyShown by rememberSaveable { mutableStateOf(false) }
    if (ready != null && toasts != null && !readyShown) {
        LaunchedEffect(ready, toasts) {
            readyShown = true
            toasts.show(
                ToastMessage(
                    text = readyText,
                    tone = AppTone.Info,
                    actionLabel = restartLabel,
                    onAction = { updates.restart(ready.downloaded) },
                    durationMillis = ReadyMessageDuration.inWholeMilliseconds,
                ),
            )
        }
    }
}

/** Long enough to read and reach for; after it, Play installs the update when the app is next closed. */
private val ReadyMessageDuration = 10.seconds
