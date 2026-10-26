package com.base.app.core.applock

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.datastore.settings.AppSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * When the app is covered by its unlock screen.
 *
 * A challenge starts at every cold start and at every return to the foreground after more than
 * [GRACE_MILLIS] away, and only a successful unlock ends it. Leaving the app while it is locked
 * never ends one, so pressing Home on the unlock screen and coming straight back finds it locked.
 * Turning the setting on does not lock the app in front of the person who just did so; the next
 * return from the background does.
 *
 * The lock hides what is on screen from someone holding an unlocked phone. It does not encrypt
 * stored data, and the session's tokens are not tied to the authentication.
 */
@Singleton
class AppLock internal constructor(
    private val settings: AppSettingsStore,
    private val deviceCanAuthenticate: () -> Boolean,
    private val clock: () -> Long,
    scope: ApplicationScope,
) : DefaultLifecycleObserver {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        settings: AppSettingsStore,
        scope: ApplicationScope,
    ) : this(
        settings = settings,
        deviceCanAuthenticate = {
            BiometricManager.from(context).canAuthenticate(allowedAuthenticators()) ==
                BiometricManager.BIOMETRIC_SUCCESS
        },
        clock = SystemClock::elapsedRealtime,
        scope = scope,
    ) {
        // The process lifecycle accepts observers on the main thread only; whoever injects this
        // first may not be on it. A cold start needs no event: it starts challenged.
        ContextCompat.getMainExecutor(context).execute {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    /** When the current challenge began, on the [clock]; null while the app is not challenged. */
    private val challenge = MutableStateFlow<Long?>(clock())
    private var hiddenAt: Long? = null

    /** Asked again only when the app returns to the front, never on every frame. */
    private var canAuthenticate = deviceCanAuthenticate()

    init {
        scope.launch {
            settings.settings.map { it.appLockEnabled }.distinctUntilChanged().collect { enabled ->
                if (!enabled) challenge.value = null
            }
        }
    }

    /** Identifies the challenge in progress, so the prompt is shown once per challenge, not once per recreation. */
    internal val challenges: StateFlow<Long?> = challenge.asStateFlow()

    /** Whether the app is covered now. */
    val locked: Flow<Boolean> = combine(challenge, settings.settings) { started, current ->
        started != null && current.appLockEnabled && canAuthenticate
    }.distinctUntilChanged()

    /**
     * [locked] without suspending, for the first frame. The settings are in memory by then: the
     * splash screen waits for them.
     */
    fun isLockedNow(): Boolean =
        challenge.value != null && settings.current?.appLockEnabled == true && canAuthenticate

    /** Ends the challenge. Called by the unlock screen once the person has proved who they are. */
    internal fun unlocked() {
        challenge.value = null
    }

    override fun onStart(owner: LifecycleOwner) {
        val awaySince = hiddenAt ?: return
        hiddenAt = null
        canAuthenticate = deviceCanAuthenticate()
        if (clock() - awaySince > GRACE_MILLIS && challenge.value == null) challenge.value = clock()
    }

    override fun onStop(owner: LifecycleOwner) {
        // Leaving while challenged starts no clock: the challenge stands until it is passed. That
        // includes the system's own credential screen, which hides the app and may report success
        // only after the app is back in front; a clock started then would lock it again at once.
        hiddenAt = if (challenge.value == null) clock() else null
    }

    companion object {
        /**
         * Long enough to choose a photo, answer a share sheet or read a one-time code in another
         * app; short enough that a phone put down on a desk does not stay open.
         */
        const val GRACE_MILLIS: Long = 30_000L

        /**
         * `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` is documented as unsupported on API 28 and 29,
         * where the prompt throws rather than reporting an error, so those levels ask for the weak
         * class instead.
         */
        internal fun allowedAuthenticators(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
            } else {
                Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
            }
    }
}
