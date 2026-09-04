package com.base.app.core.network

import io.ktor.util.AttributeKey

/**
 * Marks a request that must not carry the session token.
 *
 * Sign-in, one-time-password verification and token refresh are all endpoints where sending a
 * stale bearer token is at best pointless and at worst rejected — several gateways return 401 for
 * *any* invalid Authorization header, including on an endpoint that needs no authentication at
 * all, which turns "your password is wrong" into "your session expired".
 *
 * A Ktor attribute rather than a header, because it has to survive the auth plugin's own
 * inspection: `sendWithoutRequest` runs before headers are finalised and needs something it can
 * read off the request itself.
 */
internal val SkipAuthAttribute = AttributeKey<Boolean>("SkipAuth")
