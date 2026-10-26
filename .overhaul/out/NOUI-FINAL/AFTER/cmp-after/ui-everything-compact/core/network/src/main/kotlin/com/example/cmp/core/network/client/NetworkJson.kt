package com.example.cmp.core.network.client

import kotlinx.serialization.json.Json

/**
 * The one [Json] the network layer reads and writes with. Strict about syntax, so a malformed body
 * fails as a decode error instead of turning into a half-filled object; forgiving about fields the
 * app does not know yet, so a server can add one without breaking every installed version.
 */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
}
