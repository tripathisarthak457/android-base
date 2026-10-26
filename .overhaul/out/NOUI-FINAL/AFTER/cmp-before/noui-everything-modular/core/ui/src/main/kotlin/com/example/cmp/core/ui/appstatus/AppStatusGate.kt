package com.example.cmp.core.ui.appstatus

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.cmp.core.network.appstatus.AppStatus
import com.example.cmp.core.ui.PlaceholderScreen
import com.example.cmp.core.ui.R

/**
 * Covers the app when the server says this version is no longer supported, or that it is down for
 * maintenance, so no screen has to handle either by itself. Draws nothing while [status] is
 * [AppStatus.Available]. Place it in the root over the navigation host and under the developer
 * inspector, which stays reachable to see what the server said.
 *
 * @param status `ApiSignals.status`, the service's state as the last answers showed it.
 * @param onRetry called when the person asks to try again during maintenance; `ApiSignals.retry`.
 */
@Composable
fun AppStatusGate(status: AppStatus, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (status == AppStatus.Available) return

    val context = LocalContext.current
    val activity = LocalActivity.current
    // Back leaves the app as it is: nothing underneath can be used until this is resolved.
    val back = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = back, onBackCompleted = { activity?.moveTaskToBack(true) })

    // The cover takes every tap, so none reaches the app underneath, and a tap does what the
    // screen's button will: maintenance asks again, which is the only way it clears, and an
    // update opens the store. Without it the cover could be left only by ending the process.
    val update = status == AppStatus.UpdateRequired
    val title = if (update) {
        R.string.ui_appstatus_update_title
    } else {
        R.string.ui_appstatus_maintenance_title
    }
    val action = if (update) {
        R.string.ui_appstatus_update_action
    } else {
        R.string.ui_appstatus_maintenance_action
    }
    PlaceholderScreen(
        title = title,
        // allow(bare-clickable): the cover is the whole screen, which no press may scale or dim
        modifier = modifier.clickable(onClickLabel = stringResource(action)) {
            if (update) context.openStoreListing() else onRetry()
        },
    )
}

/** The Play Store app when it is installed, its website otherwise. */
private fun Context.openStoreListing() {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
    try {
        startActivity(app)
    } catch (noStore: ActivityNotFoundException) {
        startActivity(web)
    }
}
