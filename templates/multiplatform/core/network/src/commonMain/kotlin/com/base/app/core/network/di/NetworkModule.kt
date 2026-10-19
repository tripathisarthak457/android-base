package com.base.app.core.network.di

import com.base.app.core.common.network.NetworkMonitor
import com.base.app.core.network.client.KtorNetworkClient
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.NetworkConfig
import com.base.app.core.network.client.NetworkJson
import com.base.app.core.network.client.httpClient
import com.base.app.core.network.monitor.platformNetworkMonitor
import com.base.app.core.network.response.PassthroughUnwrapper
import com.base.app.core.network.response.ResponseUnwrapper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module

/**
 * The network layer. Needs a [NetworkConfig], an `AppDispatchers` and an `ApplicationScope` from
 * the app. Replace the [ResponseUnwrapper] binding when the backend wraps its bodies.
 */
val networkModule = module {
    single<NetworkMonitor> { platformNetworkMonitor(get()) }
    single<ResponseUnwrapper> { PassthroughUnwrapper() }
    // The HttpClient is built here rather than bound: nothing but KtorNetworkClient may use it.
    single<NetworkClient> { KtorNetworkClient(createHttpClient(get()), get(), get(), get(), get()) }
}

private fun createHttpClient(config: NetworkConfig): HttpClient = httpClient {
    // Status codes are classified by the client, not thrown by Ktor.
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMillis
        connectTimeoutMillis = config.connectTimeoutMillis
    }
    install(ContentNegotiation) { json(NetworkJson) }
}
