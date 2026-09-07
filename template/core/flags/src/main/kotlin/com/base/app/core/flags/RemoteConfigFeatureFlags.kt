package com.base.app.core.flags

import com.base.app.core.common.util.AppLogger
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Flag values from Firebase Remote Config.
 *
 * The SDK is seeded from [Flag.DEFAULTS], which is derived from the declarations rather than
 * maintained beside them — so a flag added in Kotlin and not yet created in the console reads as
 * its declared default instead of as false.
 *
 * ## The fetch interval is not the SDK's
 *
 * Remote Config's own default is twelve hours, which makes a flag change untestable: you flip it
 * in the console, relaunch, and nothing happens, so you assume the wiring is broken. Twelve hours
 * is right for release and useless for development, hence [MINIMUM_FETCH_SECONDS] — set it to
 * zero in a debug build if you are actively working on a flag, and expect the SDK to start
 * throttling you after a handful of fetches.
 */
@Singleton
class RemoteConfigFeatureFlags @Inject constructor() : FeatureFlags {

    private val config: FirebaseRemoteConfig = Firebase.remoteConfig.apply {
        setConfigSettingsAsync(
            remoteConfigSettings { minimumFetchIntervalInSeconds = MINIMUM_FETCH_SECONDS },
        )
        setDefaultsAsync(Flag.DEFAULTS)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(flag: Flag<T>): T = when (flag) {
        is Flag.Bool -> config.getBoolean(flag.key) as T
        is Flag.Text -> config.getString(flag.key) as T
        is Flag.Number -> config.getLong(flag.key) as T
    }

    /**
     * Wrapped by hand rather than by pulling in the Play Services coroutines adapter: it is one
     * callback, and the adapter is a dependency for the whole app to carry for this one call.
     */
    override suspend fun refresh(): Boolean = suspendCancellableCoroutine { continuation ->
        config.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppLogger.w("Remote config fetch failed: ${task.exception?.message}")
            }
            continuation.resume(task.isSuccessful && task.result == true)
        }
    }

    private companion object {
        const val MINIMUM_FETCH_SECONDS = 3600L
    }
}
