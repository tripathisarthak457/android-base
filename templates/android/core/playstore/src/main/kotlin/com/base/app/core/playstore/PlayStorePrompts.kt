package com.base.app.core.playstore

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.designsystem.component.feedback.AppSnackbar
import com.base.app.core.designsystem.component.feedback.AppSnackbarHost
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.playstore.review.ReviewPrompt
import com.base.app.core.playstore.update.AppUpdates
import com.base.app.core.playstore.update.UpdateState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Play's update and review prompts, for the app's root. A newer build is offered once per process
 * through Play's own sheet; once it has downloaded, a message offers to restart into it and nothing
 * restarts by itself. The review prompt counts this launch and asks when its policy says so.
 */
@Composable
fun BoxScope.PlayStorePrompts(
    updates: AppUpdates,
    review: ReviewPrompt,
    modifier: Modifier = Modifier,
) {
    val activity: Activity? = LocalActivity.current
    val state by updates.state.collectAsStateWithLifecycle()

    var offeredVersion by rememberSaveable { mutableStateOf<Int?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val version = offeredVersion ?: return@rememberLauncherForActivityResult
        offeredVersion = null
        if (result.resultCode != Activity.RESULT_OK) updates.declined(version)
    }

    LaunchedEffect(updates) { updates.check() }
    LaunchedEffect(review, activity) { activity?.let { review.onLaunch(it) } }

    val offer = state as? UpdateState.Offer
    LaunchedEffect(offer) {
        if (offer == null) return@LaunchedEffect
        offeredVersion = offer.available.updateInfo.availableVersionCode()
        updates.offered()
        offer.available.startFlexibleUpdate(launcher)
    }

    val ready = state as? UpdateState.Ready
    var showReady by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(ready) {
        if (ready != null) {
            delay(ReadyMessageDuration)
            showReady = false
        }
    }
    AppSnackbarHost(visible = ready != null && showReady, modifier = modifier) {
        AppSnackbar(
            text = stringResource(R.string.playstore_update_ready),
            tone = AppTone.Info,
            actionLabel = stringResource(R.string.playstore_update_restart),
            onAction = { ready?.let { updates.restart(it.downloaded) } },
        )
    }
}

/** Long enough to read and reach for; after it, Play installs the update when the app is next closed. */
private val ReadyMessageDuration = 10.seconds
