package com.example.finalafter.crnoui3.buildlogic.config

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.Project

/** Product flavours, and the per-variant identity that follows from them. */
internal fun Project.configureFlavors(extension: ApplicationExtension) {
    extension.flavorDimensions += AppFlavor.DIMENSION

    // `-PapiBaseUrl=http://10.0.2.2:8080/api/` points dev and staging at a machine of your own
    // without editing AppConfig. The shipped flavours never take it, so a release cannot be built
    // against a laptop by accident.
    val apiBaseUrlOverride = providers.gradleProperty("apiBaseUrl").orNull

    AppFlavor.entries.forEach { flavor ->
        extension.productFlavors.create(flavor.flavorName).apply {
            dimension = AppFlavor.DIMENSION
            flavor.applicationIdSuffix?.let { applicationIdSuffix = it }

            val apiBaseUrl = apiBaseUrlOverride?.takeUnless { flavor.isShippable } ?: flavor.apiBaseUrl
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "WEB_SOCKET_URL", "\"${flavor.webSocketUrl}\"")
            buildConfigField("String", "ENVIRONMENT", "\"${flavor.flavorName}\"")
        }
    }
}

/** Per-variant version name, and the variants that should not exist. */
internal fun Project.configureVariants(components: ApplicationAndroidComponentsExtension) {
    components.beforeVariants { variant ->
        // A debuggable Play Store build makes no sense, so the variant is disabled.
        if (variant.productFlavors.any { it.second == AppFlavor.PLAYSTORE.flavorName } &&
            variant.buildType == "debug"
        ) {
            variant.enable = false
        }

        // Flavours differ only in BuildConfig values and the signing key, so every variant would run
        // identical unit tests. devDebug runs them once.
        if (variant.name != UNIT_TESTED_VARIANT) {
            variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
        }

        // The baseline-profile build types exist to generate and measure one profile, which is
        // merged into main and so ships with every flavour. Dev is the flavour that runs them.
        if (variant.buildType in PROFILE_BUILD_TYPES &&
            variant.productFlavors.none { it.second == AppFlavor.DEV.flavorName }
        ) {
            variant.enable = false
        }
    }

    components.onVariants { variant ->
        val flavor = AppFlavor.entries.firstOrNull { flavorEntry ->
            variant.productFlavors.any { it.second == flavorEntry.flavorName }
        }
        val isShippableRelease = flavor?.isShippable == true && variant.buildType == "release"
        val versionName =
            if (isShippableRelease) AppConfig.VERSION_NAME
            else "${AppConfig.VERSION_NAME}-${variant.name}"

        variant.outputs.forEach { output ->
            output.versionCode.set(AppConfig.VERSION_CODE)
            output.versionName.set(versionName)
        }
    }
}

/**
 * Per-ABI APKs plus a universal one, only with `-PabiSplits=true`. The template's native code is
 * a few small libraries, so splitting saves tens of kilobytes per APK, and Play already delivers
 * one ABI per device from the bundle.
 */
internal fun Project.configureAbiSplits(extension: ApplicationExtension) {
    extension.splits.abi.apply {
        isEnable = providers.gradleProperty("abiSplits").orNull.toBoolean()
        reset()
        include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        isUniversalApk = true
    }
}

private const val UNIT_TESTED_VARIANT = "devDebug"
private val PROFILE_BUILD_TYPES = setOf("benchmarkRelease", "nonMinifiedRelease")
