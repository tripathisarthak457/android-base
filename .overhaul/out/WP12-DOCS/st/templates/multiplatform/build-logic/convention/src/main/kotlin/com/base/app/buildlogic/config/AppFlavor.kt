package com.base.app.buildlogic.config

/**
 * The Android app's build environments. The iOS app has the same two, as the Dev and Prod build
 * configurations of `iosApp/project.yml`, reading `iosApp/Configuration/<name>.xcconfig`.
 */
enum class AppFlavor(
    val flavorName: String,
    val applicationIdSuffix: String?,
    val apiBaseUrl: String,
) {
    Dev(
        flavorName = "dev",
        applicationIdSuffix = ".dev",
        // <generated:api-url-dev>
        apiBaseUrl = "https://dev.example.com/api/",
        // </generated:api-url-dev>
    ),
    Prod(
        flavorName = "prod",
        applicationIdSuffix = null,
        // <generated:api-url-prod>
        apiBaseUrl = "https://api.example.com/api/",
        // </generated:api-url-prod>
    ),
    ;

    companion object {
        const val DIMENSION = "environment"
    }
}
