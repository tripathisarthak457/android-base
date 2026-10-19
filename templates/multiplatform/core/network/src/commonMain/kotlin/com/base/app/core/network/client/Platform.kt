package com.base.app.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/** An [HttpClient] on the platform's own stack: OkHttp on Android, `NSURLSession` on iOS. */
internal expect fun httpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient

/** The language the app is showing, as a BCP 47 tag for `Accept-Language`. */
internal expect fun currentLanguageTag(): String
