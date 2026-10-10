package com.base.app.core.network.monitor

import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.network.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import org.koin.core.scope.Scope
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

internal actual fun Scope.platformNetworkMonitor(scope: ApplicationScope): NetworkMonitor = PathNetworkMonitor(scope)

private class PathNetworkMonitor(scope: ApplicationScope) : NetworkMonitor {

    /** One `NWPathMonitor` shared by every reader; it reports the current path as soon as it starts. */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            channel.trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_set_queue(monitor, dispatch_queue_create(QUEUE_LABEL, null))
        nw_path_monitor_start(monitor)

        awaitClose { nw_path_monitor_cancel(monitor) }
    }
        .conflate()
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.WhileSubscribed(MONITOR_STOP_TIMEOUT_MILLIS, replayExpirationMillis = 0), replay = 1)

    private companion object {
        const val QUEUE_LABEL = "com.base.app.network-monitor"
    }
}
