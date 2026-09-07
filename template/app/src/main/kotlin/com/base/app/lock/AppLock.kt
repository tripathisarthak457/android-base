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

/**
 * Whether the app should be showing its own unlock screen right now.
 *
 * ## Why the clock is `elapsedRealtime`
 *
 * The grace period exists so that a camera picker or a share sheet does not re-prompt on the way
 * back. Measuring it against the wall clock would make the lock trivially bypassable: put the
 * phone in flight mode, move the date forward, and the timeout is over. `elapsedRealtime` counts
 * since boot and cannot be set by anybody.
 *
 * ## Why a cold start always locks
 *
 * A process that has just started has no record of when it was last used, and the safe reading of
 * "no record" is "long enough ago". The alternative — treating an unknown as recent — means the
 * lock is skipped exactly when the process was killed while backgrounded, which is the case it
 * most needs to cover.
 *
 * The enrolment check is separate from the setting on purpose. Somebody can turn the lock on and
 * then remove every fingerprint from the device; [canAuthenticate] going false is what stops that
 * from locking them out of their own app permanently.
 */
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

    /**
     * Whether this device can ask at all — a biometric enrolled, or a PIN, pattern or password.
     *
     * Both are accepted deliberately. A lock that insists on a fingerprint is a lock that shuts
     * out everybody whose sensor has stopped working, and Android's own credential fallback is
     * better tested than anything an app can put in its place.
     */
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
         * class instead. This is the exact fragmentation androidx.biometric exists to cover, and
         * one of the few places it does not cover it for you.
         */
        fun allowedAuthenticators(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
            } else {
                Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
            }
    }
}
