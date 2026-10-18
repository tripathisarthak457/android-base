package com.base.app.core.network

import io.ktor.util.AttributeKey

/** Marks a request that must not carry the session token. */
internal val SkipAuthAttribute = AttributeKey<Boolean>("SkipAuth")
