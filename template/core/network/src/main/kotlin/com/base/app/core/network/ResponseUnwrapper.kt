package com.base.app.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * The shape of your API's responses.
 *
 * Backends disagree about this and always will. Some return the resource at the root; most wrap
 * it in an envelope — `{"status": true, "message": "...", "data": {...}}` — and each project's
 * envelope has slightly different field names.
 *
 * Making it an interface with a passthrough default means the client never guesses. The
 * alternative, which almost every hand-rolled network layer ends up with, is a decode that tries
 * the envelope, falls back to the root on failure, and silently produces an object of Kotlin
 * defaults when *both* fail — an "empty" response that looks like legitimately empty data and is
 * genuinely hard to trace back to a parse error.
 *
 * Bind [EnvelopeUnwrapper] in the app's own Hilt module if your API wraps its payloads, or write
 * an implementation for whatever shape it actually uses.
 */
interface ResponseUnwrapper {

    /** The element that holds the payload, given the whole parsed body. */
    fun payload(root: JsonElement): JsonElement

    /** A human-readable message from an error body, if there is one. */
    fun errorMessage(root: JsonElement): String?

    /** Per-field validation messages from an error body, keyed by field name. */
    fun fieldErrors(root: JsonElement): Map<String, List<String>>
}

/**
 * For an API that returns the resource at the root of the body. The default.
 */
class PassthroughUnwrapper : ResponseUnwrapper {

    override fun payload(root: JsonElement): JsonElement = root

    override fun errorMessage(root: JsonElement): String? =
        (root as? JsonObject)?.let { obj ->
            MESSAGE_KEYS.firstNotNullOfOrNull { key ->
                obj[key]?.jsonPrimitiveOrNull()?.contentOrNull()
            }
        }

    override fun fieldErrors(root: JsonElement): Map<String, List<String>> =
        (root as? JsonObject)?.get("errors")?.asFieldErrors().orEmpty()

    private companion object {
        val MESSAGE_KEYS = listOf("message", "error", "detail", "title")
    }
}

/**
 * For an API that wraps every payload in an envelope.
 *
 * ```
 * { "status": true, "code": 200, "message": "OK", "data": { … } }
 * ```
 *
 * The key names are constructor parameters, so adapting to a backend that calls it `result` or
 * `payload` is a change to the one line that binds this, not a new class.
 */
class EnvelopeUnwrapper(
    private val dataKey: String = "data",
    private val messageKey: String = "message",
    private val errorsKey: String = "errors",
) : ResponseUnwrapper {

    override fun payload(root: JsonElement): JsonElement =
        (root as? JsonObject)?.get(dataKey) ?: root

    override fun errorMessage(root: JsonElement): String? =
        (root as? JsonObject)?.get(messageKey)?.jsonPrimitiveOrNull()?.contentOrNull()

    override fun fieldErrors(root: JsonElement): Map<String, List<String>> =
        (root as? JsonObject)?.get(errorsKey)?.asFieldErrors().orEmpty()
}

/**
 * Reads `{"field": ["message"]}` and `{"field": "message"}` alike.
 *
 * Both shapes appear in the wild, frequently from the same backend on different endpoints, and a
 * parser that handles only one silently loses every error of the other kind — which surfaces as
 * a form that refuses to submit and says nothing about why.
 */
private fun JsonElement.asFieldErrors(): Map<String, List<String>> {
    val obj = this as? JsonObject ?: return emptyMap()
    return obj.mapNotNull { (field, value) ->
        val messages = when (value) {
            is JsonPrimitive -> value.contentOrNull()?.let(::listOf)
            else -> runCatching {
                value.jsonArray.mapNotNull { it.jsonPrimitiveOrNull()?.contentOrNull() }
            }.getOrNull()
        }
        messages?.takeIf { it.isNotEmpty() }?.let { field to it }
    }.toMap()
}

private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? =
    runCatching { jsonPrimitive }.getOrNull()

private fun JsonPrimitive.contentOrNull(): String? = content.takeIf { it.isNotBlank() }

/**
 * The one [Json] the network layer parses and prints with.
 *
 * `ignoreUnknownKeys` is not laziness: a backend adding a field is a routine, non-breaking change
 * on their side, and a client that throws on it turns every such deploy into an outage for
 * everyone who has not updated.
 */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
}
