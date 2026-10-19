package com.base.app.core.network.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.network.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.platformNetworkMonitor(scope: ApplicationScope): NetworkMonitor =
    ConnectivityNetworkMonitor(androidContext(), scope)

private class ConnectivityNetworkMonitor(context: Context, scope: ApplicationScope) : NetworkMonitor {

    /**
     * One registration shared by every reader. Each callback is a binder call, and the system
     * refuses an app more than about a hundred at once, so a cold flow per request would register
     * one per request and eventually crash a burst of them.
     */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
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
        // The last answer is forgotten when the registration goes, so a reader after a quiet spell
        // waits for a fresh one rather than reading a stale "offline".
        .shareIn(scope, SharingStarted.WhileSubscribed(MONITOR_STOP_TIMEOUT_MILLIS, replayExpirationMillis = 0), replay = 1)
}

private fun ConnectivityManager.hasInternet(): Boolean =
    activeNetwork?.let(::getNetworkCapabilities)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
