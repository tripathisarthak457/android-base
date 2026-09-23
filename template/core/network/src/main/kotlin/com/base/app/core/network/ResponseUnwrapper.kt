package com.base.app.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** The shape of your API's responses. Backends disagree about this and always will. */
interface ResponseUnwrapper {

    /** The element that holds the payload, given the whole parsed body. */
    fun payload(root: JsonElement): JsonElement

    /** A human-readable message from an error body, if there is one. */
    fun errorMessage(root: JsonElement): String?

    /** Per-field validation messages from an error body, keyed by field name. */
    fun fieldErrors(root: JsonElement): Map<String, List<String>>
}

/** For an API that returns the resource at the root of the body. The default. */
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

/** Reads `{"field": ["message"]}` and `{"field": "message"}` alike. */
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

/** The one [Json] the network layer parses and prints with. */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
}
