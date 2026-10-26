package com.example.cmp.core.network.response

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Instant

/**
 * How long a `Retry-After` header asks the client to wait, in whole seconds from [now], or null when
 * it says nothing usable. Servers send either form RFC 9110 allows: a number of seconds, or an
 * HTTP date.
 */
internal fun retryAfterSeconds(value: String?, now: Instant): Long? {
    val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    text.toLongOrNull()?.let { return it.coerceAtLeast(0) }
    val at = try {
        ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()
    } catch (notADate: DateTimeParseException) {
        return null
    }
    return (at - now.epochSeconds).coerceAtLeast(0)
}
