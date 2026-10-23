package com.example.standard.core.devtools.di

import com.example.standard.core.devtools.DevTools
import com.example.standard.core.devtools.log.DevToolsLog
import com.example.standard.core.devtools.log.NetworkExchange
import com.example.standard.core.network.recording.HttpExchange
import com.example.standard.core.network.recording.NetworkObserver
import com.example.standard.core.ui.tooling.DevTooling
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface DevToolsModule {

    @Binds
    fun devTooling(devTools: DevTools): DevTooling

    companion object {

        /** Every request the app's clients make, answered or not, into the inspector's log. */
        @Provides
        @IntoSet
        fun inspectorRecorder(log: DevToolsLog): NetworkObserver =
            NetworkObserver { log.record(it.toNetworkExchange()) }
    }
}

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
