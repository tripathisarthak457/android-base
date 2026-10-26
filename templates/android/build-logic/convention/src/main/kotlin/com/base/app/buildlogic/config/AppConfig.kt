package com.base.app.buildlogic.config

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** Every number that describes *this app* rather than *this module*. */
object AppConfig {

    // <generated:app-config>
    const val APPLICATION_ID = "com.base.app"
    const val APP_NAME = "BaseApp"

    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val TARGET_SDK = 37
    // </generated:app-config>

    /**
     * Monotonic, and never derived from the version name. Play rejects a bundle whose code is not
     * strictly greater than the last one uploaded, so this is the number CI increments — the
     * version name is for humans and can go backwards during a hotfix without consequence.
     */
    // <generated:app-version>
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"
    // </generated:app-version>

    /**
     * The languages the app ships, as resource qualifiers. Library translations for any other
     * language are dropped from the APK.
     */
    val LOCALES: List<String> = listOf(
        // <generated:locale-filters>
        "en",
        "de",
        "es",
        "fr",
        "hi",
        "pt-rBR",
        // </generated:locale-filters>
    )

    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17
    val JVM_TARGET: JvmTarget = JvmTarget.JVM_17

    /**
     * `java.time` is API 26+. Below that the desugaring library back-ports it, at the cost of a
     * step in every release build — so it is switched on only when [MIN_SDK] actually needs it
     * rather than left on out of habit.
     */
    val NEEDS_CORE_LIBRARY_DESUGARING: Boolean get() = MIN_SDK < 26
}

/** The build environments, and everything that differs between them. */
enum class AppFlavor(
    val flavorName: String,
    val applicationIdSuffix: String?,
    val apiBaseUrl: String,
    val webSocketUrl: String,
    val signingKeyName: String,
    val debuggableRelease: Boolean = false,
) {
    DEV(
        flavorName = "dev",
        applicationIdSuffix = ".dev",
        // <generated:api-url-dev>
        apiBaseUrl = "https://dev.example.com/api/",
        // </generated:api-url-dev>
        // <generated:socket-url-dev>
        webSocketUrl = "wss://dev.example.com/ws",
        // </generated:socket-url-dev>
        signingKeyName = "dev",
    ),
    STAGING(
        flavorName = "staging",
        applicationIdSuffix = ".staging",
        // <generated:api-url-staging>
        apiBaseUrl = "https://staging.example.com/api/",
        // </generated:api-url-staging>
        // <generated:socket-url-staging>
        webSocketUrl = "wss://staging.example.com/ws",
        // </generated:socket-url-staging>
        signingKeyName = "staging",
    ),
    PROD(
        flavorName = "prod",
        applicationIdSuffix = null,
        // <generated:api-url-prod>
        apiBaseUrl = "https://api.example.com/api/",
        // </generated:api-url-prod>
        // <generated:socket-url-prod>
        webSocketUrl = "wss://api.example.com/ws",
        // </generated:socket-url-prod>
        signingKeyName = "prod",
    ),

    /** Identical to [PROD] in every respect except the key it is signed with. */
    PLAYSTORE(
        flavorName = "playstore",
        applicationIdSuffix = null,
        // <generated:api-url-playstore>
        apiBaseUrl = "https://api.example.com/api/",
        // </generated:api-url-playstore>
        // <generated:socket-url-playstore>
        webSocketUrl = "wss://api.example.com/ws",
        // </generated:socket-url-playstore>
        signingKeyName = "playstore",
    ),
    ;

    /** True for the two variants that are actually shipped, and only those. */
    val isShippable: Boolean get() = this == PROD || this == PLAYSTORE

    companion object {
        const val DIMENSION = "environment"
    }
}
