package com.base.app.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** Every number that describes *this app* rather than *this module*. */
object AppConfig {

    const val APPLICATION_ID = "com.base.app"
    const val APP_NAME = "BaseApp"

    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val TARGET_SDK = 37

    /**
     * Monotonic, and never derived from the version name. Play rejects a bundle whose code is not
     * strictly greater than the last one uploaded, so this is the number CI increments — the
     * version name is for humans and can go backwards during a hotfix without consequence.
     */
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"

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
        apiBaseUrl = "https://dev.example.com/api/",
        webSocketUrl = "wss://dev.example.com/ws",
        signingKeyName = "dev",
    ),
    STAGING(
        flavorName = "staging",
        applicationIdSuffix = ".staging",
        apiBaseUrl = "https://staging.example.com/api/",
        webSocketUrl = "wss://staging.example.com/ws",
        signingKeyName = "staging",
    ),
    PROD(
        flavorName = "prod",
        applicationIdSuffix = null,
        apiBaseUrl = "https://api.example.com/api/",
        webSocketUrl = "wss://api.example.com/ws",
        signingKeyName = "prod",
    ),

    /** Identical to [PROD] in every respect except the key it is signed with. */
    PLAYSTORE(
        flavorName = "playstore",
        applicationIdSuffix = null,
        apiBaseUrl = "https://api.example.com/api/",
        webSocketUrl = "wss://api.example.com/ws",
        signingKeyName = "playstore",
    );

    /** True for the two variants that are actually shipped, and only those. */
    val isShippable: Boolean get() = this == PROD || this == PLAYSTORE

    companion object {
        const val DIMENSION = "environment"
    }
}
