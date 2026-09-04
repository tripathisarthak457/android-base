package com.base.app.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Every number that describes *this app* rather than *this module*.
 *
 * Deliberately one object rather than values scattered across build files: bumping the target SDK
 * or the version code is a single edit, and no module can disagree with another about what it
 * compiles against.
 */
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

/**
 * The build environments, and everything that differs between them.
 *
 * A flavour exists to answer three questions — which backend, whether it installs alongside the
 * others, and which key signs it — so all three live here together. Adding a fourth environment
 * is one entry; nothing else in the build has to learn about it.
 *
 * [signingKeyName] is the prefix looked up in `keystore.properties` (see [Signing]). Two flavours
 * may share a key: dev and staging commonly do. Production and Play Store must not.
 */
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

    /**
     * Identical to [PROD] in every respect except the key it is signed with.
     *
     * It exists so that the artifact uploaded to Play is produced by a build you can point at,
     * rather than by remembering to swap a signing config before running the release task. The
     * debug variant of this flavour is disabled — there is no such thing as a debuggable Play
     * build — which is why the matrix is seven variants and not eight.
     */
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
