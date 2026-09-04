package com.base.app.core.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.base.app.core.coroutines.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the device currently has usable connectivity.
 *
 * An interface rather than the implementation directly, because "the user is offline" is a state
 * every test of every repository needs to produce, and doing that against a real
 * `ConnectivityManager` means an instrumented test and a device with its radio turned off.
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NetworkMonitor {

    /**
     * Tracks the set of validated networks rather than a single boolean.
     *
     * Callbacks arrive per network, and a handover from Wi-Fi to cellular delivers `onAvailable`
     * for the new one and `onLost` for the old one in an order that is not guaranteed. A single
     * boolean flips to false in the middle of a handover that never actually dropped, which the
     * UI shows as an offline banner flashing on a working connection. Counting networks cannot
     * produce that.
     *
     * `NET_CAPABILITY_VALIDATED` is what separates "associated with an access point" from
     * "packets actually reach the internet" — the captive-portal case that otherwise reports
     * online while every request times out.
     */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        val networks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks += network
                channel.trySend(networks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                networks -= network
                channel.trySend(networks.isNotEmpty())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                val validated =
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (validated) networks += network else networks -= network
                channel.trySend(networks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Seed the flow. The callback only reports changes, so a collector that starts while
        // already connected would otherwise wait for the next network event to learn anything.
        channel.trySend(connectivityManager.hasValidatedConnection())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .conflate()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
}

private fun ConnectivityManager.hasValidatedConnection(): Boolean =
    activeNetwork
        ?.let(::getNetworkCapabilities)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
