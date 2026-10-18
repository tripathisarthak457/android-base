package com.example.hud.standard.di

import com.example.hud.standard.BuildConfig
import com.example.hud.standard.core.devtools.DevEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.hud.standard.core.devtools.log.DevToolsLog
import com.example.hud.standard.core.devtools.log.NetworkExchange
import com.example.hud.standard.core.network.recording.HttpExchange
import com.example.hud.standard.core.network.recording.NetworkObserver
import dagger.multibindings.ElementsIntoSet

/** What this build is, for the inspector. Its own module because AppModule exists only with networking. */
@Module
@InstallIn(SingletonComponent::class)
object DevToolsModule {

    @Provides
    @Singleton
    fun provideDevEnvironment(): DevEnvironment = buildDevEnvironment()

    /**
     * Records requests only in a build that shows the inspector. A production build installs no
     * observer at all, so its clients never copy a body for it.
     */
    @Provides
    @ElementsIntoSet
    fun provideInspectorRecorder(environment: DevEnvironment, log: DevToolsLog): Set<NetworkObserver> =
        if (environment.visible) setOf(NetworkObserver { log.record(it.toNetworkExchange()) }) else emptySet()
}

internal fun buildDevEnvironment() = DevEnvironment(
    name = BuildConfig.ENVIRONMENT,
    versionName = BuildConfig.VERSION_NAME,
    applicationId = BuildConfig.APPLICATION_ID,
    apiBaseUrl = BuildConfig.API_BASE_URL,
    isDebugBuild = BuildConfig.DEBUG,
    isShippable = BuildConfig.IS_SHIPPABLE,
)

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
