package com.base.app.buildlogic.config

/**
 * The build environments of the Android app. Library modules have no flavours (the multiplatform
 * library plugin does not support them), so whatever differs per environment reaches shared code
 * as an `AppEnvironment` that `androidApp` builds from `BuildConfig`.
 */
enum class AppFlavor(
    val flavorName: String,
    val applicationIdSuffix: String?,
    val apiBaseUrl: String,
) {
    Dev(flavorName = "dev", applicationIdSuffix = ".dev", apiBaseUrl = AppConfig.DEV_API_BASE_URL),
    Prod(flavorName = "prod", applicationIdSuffix = null, apiBaseUrl = AppConfig.PROD_API_BASE_URL),
    ;

    companion object {
        const val DIMENSION = "environment"
    }
}
