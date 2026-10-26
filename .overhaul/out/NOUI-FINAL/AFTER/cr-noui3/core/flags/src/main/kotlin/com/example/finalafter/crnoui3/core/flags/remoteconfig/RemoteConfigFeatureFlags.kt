package com.example.finalafter.crnoui3.core.flags.remoteconfig

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.finalafter.crnoui3.core.common.log.Log
import com.example.finalafter.crnoui3.core.flags.FeatureFlags
import com.example.finalafter.crnoui3.core.flags.Flag
import com.example.finalafter.crnoui3.core.flags.FlagOverrides
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Flag values from Firebase Remote Config: fetched at start-up, and activated again whenever the
 * console publishes a change while the app is open. Until the first fetch lands every flag reads its
 * declared default.
 */
@Singleton
class RemoteConfigFeatureFlags @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overrides: FlagOverrides,
) : FeatureFlags {

    /** Set up on first use, on the thread that reads a flag, rather than in whichever constructor injects this first. */
    private val config: FirebaseRemoteConfig by lazy {
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = fetchInterval() })
            setDefaultsAsync(Flag.DEFAULTS)
        }
    }

    /** Bumped on each activation, so [observe] reads again. */
    private val activations = MutableStateFlow(0)

    @Volatile
    private var listening = false

    @Suppress("UNCHECKED_CAST") // Each branch returns the type its flag class declares.
    override fun <T> get(flag: Flag<T>): T = overrides[flag] ?: when (flag) {
        is Flag.Bool -> config.getBoolean(flag.key) as T
        is Flag.Text -> config.getString(flag.key) as T
        is Flag.Number -> config.getLong(flag.key) as T
    }

    override fun <T> observe(flag: Flag<T>): Flow<T> =
        combine(activations, overrides.changes) { _, _ -> get(flag) }.distinctUntilChanged()

    /**
     * Wrapped by hand rather than through the Play Services coroutines adapter: it is one callback,
     * and the adapter would be a dependency of the whole app for it.
     */
    override suspend fun refresh(): Boolean {
        listenForUpdates()
        val changed = suspendCancellableCoroutine { continuation ->
            config.fetchAndActivate().addOnCompleteListener { task ->
                if (!task.isSuccessful) Log.w("Remote Config fetch failed", task.exception, TAG)
                continuation.resume(task.isSuccessful && task.result == true)
            }
        }
        if (changed) activations.update { it + 1 }
        return changed
    }

    private fun listenForUpdates() {
        if (listening) return
        listening = true
        config.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(update: ConfigUpdate) {
                    config.activate().addOnSuccessListener { activations.update { it + 1 } }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w("Remote Config updates stopped", error, TAG)
                }
            },
        )
    }

    /** A debuggable build fetches every time, so a value changed in the console shows on the next start. */
    private fun fetchInterval(): Long =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) 0L else RELEASE_FETCH_INTERVAL_SECONDS

    private companion object {
        const val TAG = "FeatureFlags"
        const val RELEASE_FETCH_INTERVAL_SECONDS = 3_600L
    }
}
