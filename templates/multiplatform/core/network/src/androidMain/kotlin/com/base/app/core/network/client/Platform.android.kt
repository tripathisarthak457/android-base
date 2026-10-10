package com.base.app.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.util.Locale

internal actual fun httpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp, configure)

internal actual fun currentLanguageTag(): String = Locale.getDefault().toLanguageTag()
