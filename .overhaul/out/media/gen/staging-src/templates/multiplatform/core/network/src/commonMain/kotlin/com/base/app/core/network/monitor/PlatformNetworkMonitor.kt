package com.base.app.core.network.monitor

import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.network.NetworkMonitor
import org.koin.core.scope.Scope

/**
 * Whether any network that claims internet access is up: `ConnectivityManager` on Android,
 * `NWPathMonitor` on iOS. A hint for the UI, never a gate: a captive portal or a private DNS
 * setting can leave a working network unvalidated, so requests are always attempted and their
 * failures classified instead.
 */
internal expect fun Scope.platformNetworkMonitor(scope: ApplicationScope): NetworkMonitor

/** Long enough that requests a few seconds apart reuse one registration with the system. */
internal const val MONITOR_STOP_TIMEOUT_MILLIS = 5_000L
