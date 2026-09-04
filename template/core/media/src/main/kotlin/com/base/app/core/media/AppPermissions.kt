package com.base.app.core.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Where a permission request ended up.
 *
 * [PermanentlyDenied] is the state that matters and the one most implementations omit. After two
 * refusals — or one on newer releases — the system stops showing the dialog altogether and the
 * request returns "denied" instantly. An app that keeps calling `launch()` then does nothing at
 * all from the user's point of view, forever. The only way out is Settings, and the app has to
 * say so.
 */
sealed interface PermissionState {
    data object Granted : PermissionState
    data object Denied : PermissionState
    data object PermanentlyDenied : PermissionState
    data object Unknown : PermissionState

    val isGranted: Boolean get() = this is Granted
}

/**
 * A permission, its current state, and the two things you can do about it.
 */
@Stable
class PermissionController internal constructor(
    private val permission: String,
    private val context: Context,
    private val requestLauncher: () -> Unit,
) {
    var state by mutableStateOf(context.permissionState(permission))
        internal set

    fun request() {
        if (state is PermissionState.PermanentlyDenied) {
            // Launching the system dialog here would return instantly and silently, which the
            // user reads as the button being broken.
            openSettings()
            return
        }
        requestLauncher()
    }

    /** Opens this app's settings page, which is the only route back from a permanent denial. */
    fun openSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    internal fun refresh() {
        // Re-read on resume: the user may have granted it in Settings while the app was
        // backgrounded, and nothing calls back to tell us.
        if (state !is PermissionState.PermanentlyDenied || context.permissionState(permission).isGranted) {
            state = context.permissionState(permission)
        }
    }
}

/**
 * A permission the screen can ask for.
 *
 * ## Distinguishing "denied" from "denied forever"
 *
 * The platform gives no direct signal. What it gives is
 * `shouldShowRequestPermissionRationale`, and the trick is *when* to read it: false **after** a
 * denial means the system will not ask again. Reading it before the first request is meaningless,
 * which is why the check is inside the result callback and not anywhere else.
 *
 * ```
 * val camera = rememberPermission(Manifest.permission.CAMERA)
 * when (val state = camera.state) {
 *     is PermissionState.PermanentlyDenied -> // explain, then camera.openSettings()
 *     is PermissionState.Granted -> // proceed
 *     else -> AppButton("Allow camera", camera::request)
 * }
 * ```
 */
@Composable
fun rememberPermission(permission: String): PermissionController {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<PermissionController?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        controller?.state = when {
            granted -> PermissionState.Granted
            context.shouldShowRationale(permission) -> PermissionState.Denied
            else -> PermissionState.PermanentlyDenied
        }
    }

    val created = remember(permission, context) {
        PermissionController(permission, context) { launcher.launch(permission) }
    }
    controller = created

    androidx.lifecycle.compose.LifecycleResumeEffect(permission) {
        created.refresh()
        onPauseOrDispose { }
    }

    return created
}

/**
 * The permission needed to read images the user picks, for this API level.
 *
 * On 33+ the coarse `READ_EXTERNAL_STORAGE` was split by media type. On 34+ nothing is needed at
 * all when the photo picker is used, which is why [MediaPicker.pickImage] does not ask — an app
 * that requests storage access to show a picker is asking for far more than it needs, and the
 * permission dialog says exactly that to the user.
 */
val readImagesPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        @Suppress("DEPRECATION")
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

val readVideoPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        @Suppress("DEPRECATION")
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/** Runtime-requested from API 33; granted at install below that. */
val notificationsPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        Manifest.permission.INTERNET
    }

private fun Context.permissionState(permission: String): PermissionState =
    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
        PermissionState.Granted
    } else {
        PermissionState.Unknown
    }

private fun Context.shouldShowRationale(permission: String): Boolean {
    val activity = findActivity() ?: return false
    return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
