package com.example.cmp.core.common.environment

/**
 * Which build of the app is running and which backend it talks to, as the app module's BuildConfig
 * states them, for the modules that cannot read that BuildConfig.
 *
 * Dev tooling is its only reader. A shippable build carries no dev tooling, so nothing there reads
 * it and R8 removes it.
 *
 * @property name the product flavour: `dev`, `staging`, `prod` or `playstore`.
 */
data class AppEnvironment(
    val name: String,
    val versionName: String,
    val applicationId: String,
    val apiBaseUrl: String,
    val isDebugBuild: Boolean,
)
