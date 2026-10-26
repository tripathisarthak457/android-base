package com.base.app.core.devtools.di

import com.base.app.core.devtools.DevTools
// <opt:network>
import com.base.app.core.devtools.log.DevToolsLog
import com.base.app.core.devtools.log.NetworkExchange
import com.base.app.core.network.recording.HttpExchange
import com.base.app.core.network.recording.NetworkObserver
// </opt:network>
import com.base.app.core.ui.tooling.DevTooling
import dagger.Binds
import dagger.Module
// <opt:network>
import dagger.Provides
// </opt:network>
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
// <opt:network>
import dagger.multibindings.IntoSet
// </opt:network>

@Module
@InstallIn(SingletonComponent::class)
internal interface DevToolsModule {

    @Binds
    fun devTooling(devTools: DevTools): DevTooling
    // <opt:network>

    companion object {

        /** Every request the app's clients make, answered or not, into the inspector's log. */
        @Provides
        @IntoSet
        fun inspectorRecorder(log: DevToolsLog): NetworkObserver =
            NetworkObserver { log.record(it.toNetworkExchange()) }
    }
    // </opt:network>
}
// <opt:network>

private fun HttpExchange.toNetworkExchange() = NetworkExchange(
    method = method,
    url = url,
    status = status,
    startedAtMillis = startedAtMillis,
    durationMillis = durationMillis,
    requestHeaders = requestHeaders,
    responseHeaders = responseHeaders,
    requestBody = requestBody,
    responseBody = responseBody,
    responseBytes = responseBytes,
    failure = failure,
)
// </opt:network>
