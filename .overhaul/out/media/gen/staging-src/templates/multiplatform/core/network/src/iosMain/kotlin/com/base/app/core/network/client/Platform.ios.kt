package com.base.app.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

internal actual fun httpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin, configure)

// The person's languages in order, already as BCP 47 tags (`en-GB`), unlike a locale identifier.
internal actual fun currentLanguageTag(): String = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
