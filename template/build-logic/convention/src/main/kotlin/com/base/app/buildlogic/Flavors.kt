package com.base.app.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Project

/**
 * Product flavours, and the per-variant identity that follows from them.
 *
 * Only the two application modules carry flavours. Library modules deliberately do not: a library
 * with four flavours builds eight variants instead of two, which multiplies both sync time and
 * the amount AGP has to invalidate on a variant switch — and buys nothing, because the only
 * thing that actually varies per environment is configuration, which reaches the libraries
 * through Hilt from the application module rather than through a `BuildConfig` of their own.
 *
 * That is what makes switching from devDebug to stagingDebug fast: no library module is rebuilt.
 */
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

/**
 * Per-variant version name, and the variants that should not exist.
 *
 * The version name is set here rather than through `versionNameSuffix` because the rule is
 * conditional: every build that is not a shippable release is stamped with the variant that
 * produced it — `1.0.0-devDebug`, `1.0.0-stagingRelease` — so a tester reporting a bug names the
 * build in the same breath. `prodRelease` and `playstoreRelease` stay a bare `1.0.0`, because
 * that string ends up on a store listing and in a support conversation.
 */
internal fun Project.configureVariants(components: ApplicationAndroidComponentsExtension) {
    components.beforeVariants { variant ->
        // There is no such thing as a debuggable Play Store build, and leaving the variant
        // enabled costs a sync slot and an entry in every variant dropdown for something nobody
        // can legitimately build.
        if (variant.productFlavors.any { it.second == AppFlavor.PLAYSTORE.flavorName } &&
            variant.buildType == "debug"
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
 * Per-ABI APKs plus a universal one, for release APK builds only.
 *
 * Splitting costs a full extra packaging pass per ABI, which is dead weight on the debug builds
 * that make up almost every build anyone runs — so it is switched on only when the requested
 * tasks are actually assembling a release, or when `-PabiSplits=true` says so explicitly.
 *
 * App Bundles are unaffected either way: Play generates per-device artifacts from the bundle
 * itself, and splitting the APK first would only duplicate that work.
 */
internal fun Project.configureAbiSplits(extension: ApplicationExtension) {
    val requestedTasks = gradle.startParameter.taskNames
    val explicit = providers.gradleProperty("abiSplits").orNull?.toBooleanStrictOrNull()

    // AGP cannot produce ABI splits and an app bundle in one invocation — the shrunk-resources
    // output would be ambiguous, and it fails the build saying so. Any task that will build a
    // bundle therefore turns splitting off, which is also the right answer on its own terms:
    // Play splits the bundle itself, so per-ABI APKs alongside it are wasted work.
    val buildsBundle = requestedTasks.any { it.contains("bundle", ignoreCase = true) }
    val assemblingRelease = requestedTasks.any { it.contains("Release") } && !buildsBundle

    extension.splits.abi.apply {
        isEnable = explicit ?: assemblingRelease
        reset()
        include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        isUniversalApk = true
    }
}
