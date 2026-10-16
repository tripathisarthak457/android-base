package com.base.app.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.Project

/** Product flavours, and the per-variant identity that follows from them. */
internal fun Project.configureFlavors(extension: ApplicationExtension) {
    extension.flavorDimensions += AppFlavor.DIMENSION

    AppFlavor.entries.forEach { flavor ->
        extension.productFlavors.create(flavor.flavorName).apply {
            dimension = AppFlavor.DIMENSION
            flavor.applicationIdSuffix?.let { applicationIdSuffix = it }

            buildConfigField("String", "API_BASE_URL", "\"${flavor.apiBaseUrl}\"")
            buildConfigField("String", "WEB_SOCKET_URL", "\"${flavor.webSocketUrl}\"")
            buildConfigField("String", "ENVIRONMENT", "\"${flavor.flavorName}\"")
            buildConfigField("boolean", "IS_SHIPPABLE", flavor.isShippable.toString())
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

/** Per-ABI APKs plus a universal one, for release APK builds only. */
internal fun Project.configureAbiSplits(extension: ApplicationExtension) {
    val requestedTasks = gradle.startParameter.taskNames
    val explicit = providers.gradleProperty("abiSplits").orNull?.toBooleanStrictOrNull()

    // AGP cannot produce ABI splits and an app bundle in one invocation — the shrunk-resources
    // output would be ambiguous, and it fails the build saying so.
    val buildsBundle = requestedTasks.any { it.contains("bundle", ignoreCase = true) }
    val assemblingRelease = requestedTasks.any { it.contains("Release") } && !buildsBundle

    extension.splits.abi.apply {
        isEnable = explicit ?: assemblingRelease
        reset()
        include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        isUniversalApk = true
    }
}

private const val UNIT_TESTED_VARIANT = "devDebug"
