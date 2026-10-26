package com.example.finalafter.crui.core.network.auth

import com.example.finalafter.crui.core.datastore.session.FakeAuthTokenStore
import com.example.finalafter.crui.core.network.client.SkipAuthAttribute
import com.example.finalafter.crui.core.network.tokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthenticationTest {

    private val store = FakeAuthTokenStore(tokens())
    private val refresher = TokenRefresher { TokenRefreshResult.Success("A2", "R2", expiresAtEpochMillis = null) }
    private val seen = mutableListOf<HttpRequestData>()

    private fun client(refuseToken: String? = null) = HttpClient(
        MockEngine { request ->
            seen += request
            val sent = request.headers[HttpHeaders.Authorization]
            if (refuseToken != null && sent == "Bearer $refuseToken") {
                respond("", HttpStatusCode.Unauthorized)
            } else {
                respond("{}", HttpStatusCode.OK)
            }
        },
    ) { expectSuccess = false }.apply { authenticate(API_HOST, SessionTokens(store, refresher, SessionEvents())) }

    @Test
    fun `the session's token goes to the api host and nowhere else`() = runTest {
        val client = client()

        client.get("https://$API_HOST/items")
        client.get("https://images.example.org/a.png")

        assertEquals("Bearer A1", seen[0].headers[HttpHeaders.Authorization])
        assertNull(seen[1].headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a 401 renews the token once and sends the request again`() = runTest {
        val client = client(refuseToken = "A1")

        val response = client.get("https://$API_HOST/items")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("Bearer A1", "Bearer A2"), seen.map { it.headers[HttpHeaders.Authorization] })
    }

    @Test
    fun `a request marked as needing no session carries no token`() = runTest {
        client().get("https://$API_HOST/auth/login") { attributes.put(SkipAuthAttribute, true) }

        assertNull(seen.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `after sign-out the next request carries nothing`() = runTest {
        val client = client()
        client.get("https://$API_HOST/items")

        store.clear()
        client.get("https://$API_HOST/items")

        assertNull(seen[1].headers[HttpHeaders.Authorization])
    }

    private companion object {
        const val API_HOST = "api.example.com"
    }
}
