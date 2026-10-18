package com.example.hud.standard.core.network.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.example.hud.standard.core.common.coroutines.AppDispatchers
import com.example.hud.standard.core.common.coroutines.ApplicationScope
import com.example.hud.standard.core.common.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether any network that claims internet access is up. A hint for the UI and a trigger for
 * replaying queued work, never a gate: a captive portal, a corporate proxy or a private DNS setting
 * can leave a working network unvalidated, so requests are always attempted and their failures
 * classified instead.
 */
@Singleton
internal class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    dispatchers: AppDispatchers,
    scope: ApplicationScope,
) : NetworkMonitor {

    /**
     * One registration shared by every reader. Each callback is a binder call, and the system
     * refuses an app more than about a hundred at once, so a cold flow read per request would
     * register one per request and eventually crash a burst of them.
     */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivity = context.getSystemService<ConnectivityManager>()
        if (connectivity == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        val networks = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks += network
                channel.trySend(true)
            }

            override fun onLost(network: Network) {
                networks -= network
                channel.trySend(networks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivity.registerNetworkCallback(request, callback)
        // The callback reports changes only; a reader that starts while connected needs a first value.
        channel.trySend(connectivity.hasInternet())

        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }
        .conflate()
        .distinctUntilChanged()
        .flowOn(dispatchers.io)
        // The last answer is forgotten when the registration goes, so a reader after a quiet spell
        // waits for a fresh one rather than reading a stale "offline".
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS, replayExpirationMillis = 0), replay = 1)

    private companion object {
        // Long enough that requests a few seconds apart reuse the registration.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun ConnectivityManager.hasInternet(): Boolean =
    activeNetwork?.let(::getNetworkCapabilities)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
