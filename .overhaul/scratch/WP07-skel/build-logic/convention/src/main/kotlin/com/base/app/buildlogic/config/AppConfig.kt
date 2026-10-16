package com.base.app.buildlogic.config

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Every value that describes this app rather than one module. The iOS app reads its own copies
 * from `iosApp/Configuration/*.xcconfig`; the generator writes both from the same spec.
 */
object AppConfig {

    // <generated:app-config>
    const val APPLICATION_ID = "com.base.app"

    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val TARGET_SDK = 37

    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"

    const val DEV_API_BASE_URL = "https://dev.example.com/api/"
    const val PROD_API_BASE_URL = "https://api.example.com/api/"
    // </generated:app-config>

    /** The root Kotlin package. Every module's namespace sits under it. */
    const val BASE_PACKAGE = "com.base.app"

    val JVM_TARGET: JvmTarget = JvmTarget.JVM_17
}
