package com.example.hud.everything.core.ui.appstatus

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.hud.everything.core.designsystem.component.feedback.AppEmptyState
import com.example.hud.everything.core.designsystem.icon.AppIcons
import com.example.hud.everything.core.designsystem.theme.AppTheme
import com.example.hud.everything.core.network.appstatus.AppStatus
import com.example.hud.everything.core.ui.R

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .pointerInput(Unit) {}
            .safeDrawingPadding(),
    ) {
        when (status) {
            AppStatus.UpdateRequired -> AppEmptyState(
                title = stringResource(R.string.appstatus_update_title),
                message = stringResource(R.string.appstatus_update_message),
                icon = AppIcons.Download,
                actionLabel = stringResource(R.string.appstatus_update_action),
                onAction = { context.openStoreListing() },
            )

            AppStatus.Maintenance -> AppEmptyState(
                title = stringResource(R.string.appstatus_maintenance_title),
                message = stringResource(R.string.appstatus_maintenance_message),
                icon = AppIcons.Clock,
                actionLabel = stringResource(R.string.appstatus_maintenance_action),
                onAction = onRetry,
            )

            AppStatus.Available -> Unit
        }
    }
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
