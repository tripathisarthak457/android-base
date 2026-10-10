package com.base.app.di

import com.base.app.BuildConfig
import com.base.app.core.devtools.DevEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
// <opt:network>
import com.base.app.core.devtools.log.DevToolsLog
import com.base.app.core.devtools.log.NetworkExchange
import com.base.app.core.network.recording.HttpExchange
import com.base.app.core.network.recording.NetworkObserver
import dagger.multibindings.ElementsIntoSet
// </opt:network>

/** What this build is, for the inspector. Its own module because AppModule exists only with networking. */
@Module
@InstallIn(SingletonComponent::class)
object DevToolsModule {

    @Provides
    @Singleton
    fun provideDevEnvironment(): DevEnvironment = buildDevEnvironment()
    // <opt:network>

    /**
     * Records requests only in a build that shows the inspector. A production build installs no
     * observer at all, so its clients never copy a body for it.
     */
    @Provides
    @ElementsIntoSet
    fun provideInspectorRecorder(environment: DevEnvironment, log: DevToolsLog): Set<NetworkObserver> =
        if (environment.visible) setOf(NetworkObserver { log.record(it.toNetworkExchange()) }) else emptySet()
    // </opt:network>
}

internal fun buildDevEnvironment() = DevEnvironment(
    name = BuildConfig.ENVIRONMENT,
    versionName = BuildConfig.VERSION_NAME,
    applicationId = BuildConfig.APPLICATION_ID,
    apiBaseUrl = BuildConfig.API_BASE_URL,
    isDebugBuild = BuildConfig.DEBUG,
    isShippable = BuildConfig.IS_SHIPPABLE,
)
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
