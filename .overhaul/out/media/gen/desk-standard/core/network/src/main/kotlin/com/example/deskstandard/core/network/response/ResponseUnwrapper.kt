package com.example.deskstandard.core.network.response

import com.example.deskstandard.core.common.result.AppError
import com.example.deskstandard.core.network.client.NetworkJson
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The shape of your API's bodies, bound once in `NetworkModule` for every call the app makes.
 * Backends disagree about this and always will; replacing the binding is the whole adaptation.
 */
interface ResponseUnwrapper {

    /**
     * Decodes a successful [body] into the payload. An empty body (a 204) reads as `{}`, which
     * decodes into `Unit` or into any type whose fields all have defaults.
     *
     * @throws IllegalArgumentException when the body does not match [deserializer].
     */
    fun <T> payload(body: String, deserializer: DeserializationStrategy<T>): T

    /** What a refusal's [body] says: its message, a machine-readable code and per-field errors. */
    fun error(status: Int, body: String): AppError.Http
}

/**
 * For an API that returns the resource at the root of the body. The default.
 *
 * Errors are read from `message`, `error`, `detail` or `title`, a code from `code` or `error_code`,
 * and field errors from `errors`; an `error` object holding those keys is read the same way.
 */
class PassthroughUnwrapper : ResponseUnwrapper {

    override fun <T> payload(body: String, deserializer: DeserializationStrategy<T>): T =
        NetworkJson.decodeFromString(deserializer, body.ifBlank { EMPTY_OBJECT })

    override fun error(status: Int, body: String): AppError.Http {
        val root = body.parseObjectOrNull()
        val error = root?.get("error") as? JsonObject ?: root
        return AppError.Http(
            status = status,
            code = error?.firstText(CODE_KEYS),
            message = error?.firstText(MESSAGE_KEYS) ?: root?.firstText(MESSAGE_KEYS),
            fieldErrors = (error?.get(ERRORS_KEY) ?: root?.get(ERRORS_KEY))?.asFieldErrors().orEmpty(),
        )
    }

    private companion object {
        val MESSAGE_KEYS = listOf("message", "error", "detail", "title")
        val CODE_KEYS = listOf("code", "error_code")
        const val ERRORS_KEY = "errors"
    }
}

/**
 * For an API that wraps every payload in an envelope.
 *
 * ```
 * { "status": true, "code": "OK", "message": "…", "data": { … }, "errors": { "email": ["…"] } }
 * ```
 */
class EnvelopeUnwrapper(
    private val dataKey: String = "data",
    private val messageKey: String = "message",
    private val codeKey: String = "code",
    private val errorsKey: String = "errors",
) : ResponseUnwrapper {

    override fun <T> payload(body: String, deserializer: DeserializationStrategy<T>): T {
        val root = NetworkJson.parseToJsonElement(body.ifBlank { EMPTY_OBJECT })
        val payload = (root as? JsonObject)?.get(dataKey) ?: JsonObject(emptyMap())
        return NetworkJson.decodeFromJsonElement(deserializer, payload)
    }

    override fun error(status: Int, body: String): AppError.Http {
        val root = body.parseObjectOrNull()
        return AppError.Http(
            status = status,
            code = root?.firstText(listOf(codeKey)),
            message = root?.firstText(listOf(messageKey)),
            fieldErrors = root?.get(errorsKey)?.asFieldErrors().orEmpty(),
        )
    }
}

private const val EMPTY_OBJECT = "{}"

/** An error page from a proxy is HTML, not JSON; it has nothing to say, so it reads as nothing. */
private fun String.parseObjectOrNull(): JsonObject? {
    if (isBlank()) return null
    return try {
        NetworkJson.parseToJsonElement(this) as? JsonObject
    } catch (notJson: IllegalArgumentException) {
        null
    }
}

private fun JsonObject.firstText(keys: List<String>): String? =
    keys.firstNotNullOfOrNull { key -> (get(key) as? JsonPrimitive)?.content?.takeIf(String::isNotBlank) }

/** Reads `{"field": ["message"]}` and `{"field": "message"}` alike. */
private fun JsonElement.asFieldErrors(): Map<String, List<String>> {
    val fields = this as? JsonObject ?: return emptyMap()
    return fields.mapNotNull { (field, value) ->
        val messages = when (value) {
            is JsonPrimitive -> listOfNotNull(value.content.takeIf(String::isNotBlank))
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.content?.takeIf(String::isNotBlank) }
            else -> emptyList()
        }
        messages.takeIf { it.isNotEmpty() }?.let { field to it }
    }.toMap()
}
