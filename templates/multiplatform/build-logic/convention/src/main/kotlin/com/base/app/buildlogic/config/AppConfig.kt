package com.base.app.buildlogic.config

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** Every number that describes *this app* rather than *this module*. */
object AppConfig {

    // <generated:app-config>
    const val APPLICATION_ID = "com.base.app"
    const val BASE_PACKAGE = "com.base.app"

    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val TARGET_SDK = 37
    // </generated:app-config>

    /**
     * Monotonic, and never derived from the version name. Play rejects a bundle whose code is not
     * strictly greater than the last one uploaded, so this is the number CI increments; the
     * version name is for humans and can go backwards during a hotfix without consequence.
     */
    // <generated:app-version>
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"
    // </generated:app-version>

    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17
    val JVM_TARGET: JvmTarget = JvmTarget.JVM_17
}
