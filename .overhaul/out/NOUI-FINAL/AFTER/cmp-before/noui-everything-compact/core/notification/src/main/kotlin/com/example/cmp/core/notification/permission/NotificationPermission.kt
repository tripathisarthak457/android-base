package com.example.cmp.core.notification.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Where the permission to post notifications stands. */
enum class NotificationAccess {
    Granted,

    /** Not granted, and the system will still ask. */
    NotGranted,

    /** Refused in a way the system will not ask about again; only the app's settings page can change it. */
    Blocked,
}

/** The notification permission for one screen: its current [access], and the way to ask. */
@Stable
class NotificationPermission internal constructor(
    private val context: Context,
    private val launch: () -> Unit,
) {
    var access: NotificationAccess by mutableStateOf(context.currentAccess())
        internal set

    /** Shows the system's question, or the app's notification settings once the question is blocked. */
    fun request() {
        when (access) {
            NotificationAccess.Granted -> Unit
            NotificationAccess.NotGranted -> launch()
            NotificationAccess.Blocked -> context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    internal fun refresh() {
        val now = context.currentAccess()
        // A refusal for good looks like "not asked yet" to a fresh check; keep it until it is granted.
        if (now == NotificationAccess.Granted || access != NotificationAccess.Blocked) access = now
    }
}

/**
 * The permission to post notifications, asked for in context. Nothing in this project calls this
 * yet, so on Android 13 and up no notification is shown until a screen of yours does and the
 * person grants it: ask where they can see why. Before Android 13 there is nothing to ask and
 * [NotificationPermission.access] is always granted. [onResult] hears the answer to a request made
 * from here.
 */
@Composable
fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit = {}): NotificationPermission {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    var permission: NotificationPermission? = null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission?.access = when {
            granted -> NotificationAccess.Granted
            context.findActivity()?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS) } == true ->
                NotificationAccess.NotGranted
            else -> NotificationAccess.Blocked
        }
        currentOnResult(granted)
    }
    val remembered = remember(context) {
        NotificationPermission(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    permission = remembered

    // Granted in the system's settings while the app was away: nothing calls back to say so.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { remembered.refresh() }
    return remembered
}

private fun Context.currentAccess(): NotificationAccess =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) {
        NotificationAccess.Granted
    } else {
        NotificationAccess.NotGranted
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
