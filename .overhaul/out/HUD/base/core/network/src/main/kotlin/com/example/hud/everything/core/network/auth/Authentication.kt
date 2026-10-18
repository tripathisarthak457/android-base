package com.example.hud.everything.core.network.auth

import com.example.hud.everything.core.network.client.SkipAuthAttribute
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * Sends the session's access token with every request to [apiHost], and on a 401 renews it once
 * and sends the request again. The token is read from the store for each request rather than held
 * by a plugin, so nobody's request can ever carry the previous person's token.
 *
 * A request marked `requiresAuth = false` carries nothing, nor does any other host: a signed image
 * URL or a third-party API must not receive the session's credentials.
 */
internal fun HttpClient.authenticate(apiHost: String, tokens: SessionTokens) {
    plugin(HttpSend).intercept { request ->
        if (request.url.host != apiHost || request.attributes.getOrNull(SkipAuthAttribute) == true) {
            return@intercept execute(request)
        }
        val sent = tokens.current() ?: return@intercept execute(request)
        request.headers[HttpHeaders.Authorization] = "Bearer ${sent.accessToken}"
        val call = execute(request)
        if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call

        val renewed = tokens.refresh(stale = sent) ?: return@intercept call
        request.headers[HttpHeaders.Authorization] = "Bearer ${renewed.accessToken}"
        execute(request)
    }
}
