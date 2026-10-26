package com.example.finalafter.crui2.core.network.client

import io.ktor.util.AttributeKey

/** Marks a request that must not carry the session's token. */
internal val SkipAuthAttribute = AttributeKey<Boolean>("SkipAuth")

/** Marks a request whose body or answer holds credentials, so no log or recorder keeps either. */
internal val CredentialsAttribute = AttributeKey<Boolean>("CarriesCredentials")
