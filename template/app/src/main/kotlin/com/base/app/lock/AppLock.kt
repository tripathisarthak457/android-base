package com.base.app.lock

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import com.base.app.core.datastore.AppSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the app should be showing its own unlock screen right now. */
@Singleton
class AppLock @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: AppSettingsStore,
) {

    private var lastVisibleAt = UNKNOWN

    /** Records the moment the app stopped being visible. Called from the Activity's `onStop`. */
    fun onHidden() {
        lastVisibleAt = SystemClock.elapsedRealtime()
    }

    /** Whether the unlock screen should be shown now. Called from the Activity's `onStart`. */
    suspend fun shouldLock(): Boolean {
        if (!settings.settings.first().appLockEnabled) return false
        if (!canAuthenticate()) return false
        if (lastVisibleAt == UNKNOWN) return true
        return SystemClock.elapsedRealtime() - lastVisibleAt >= GRACE_MILLIS
    }

    /** Called once the user has proved who they are. Starts the grace period again. */
    fun unlocked() {
        lastVisibleAt = SystemClock.elapsedRealtime()
    }

    /** Whether this device can ask at all — a biometric enrolled, or a PIN, pattern or password. */
    fun canAuthenticate(): Boolean =
        BiometricManager.from(context).canAuthenticate(allowedAuthenticators()) ==
            BiometricManager.BIOMETRIC_SUCCESS

    internal companion object {
        private const val UNKNOWN = -1L

        /**
         * Long enough to survive a picker or a share sheet, short enough that a phone left on a
         * desk is not open. Thirty seconds is the figure most banking apps settle on.
         */
        const val GRACE_MILLIS = 30_000L

        /**
         * `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` is documented as unsupported on API 28 and 29 —
         * the call throws rather than returning an error — so those two levels ask for the weak
         * class instead.
         */
        fun allowedAuthenticators(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
            } else {
                Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
            }
    }
}
