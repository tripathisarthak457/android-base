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

/** Flag values from Firebase Remote Config. */
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
