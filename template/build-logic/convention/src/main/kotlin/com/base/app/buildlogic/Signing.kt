package com.base.app.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.util.Properties

/**
 * Signing, read from a `keystore.properties` that is never committed.
 *
 * ```
 * dev.storeFile=keys/dev.jks
 * dev.storePassword=…
 * dev.keyAlias=…
 * dev.keyPassword=…
 * ```
 */
internal data class KeystoreEntry(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

internal fun Project.loadKeystoreEntries(): Map<String, KeystoreEntry> {
    val propertiesFile = rootProject.file("keystore.properties")
    if (!propertiesFile.exists()) return emptyMap()

    val properties = Properties().apply {
        propertiesFile.inputStream().use { load(it) }
    }

    return AppFlavor.entries
        .map { it.signingKeyName }
        .distinct()
        .mapNotNull { name ->
            val store = properties.getProperty("$name.storeFile")?.takeIf { it.isNotBlank() }
            val storePassword = properties.getProperty("$name.storePassword")
            val keyAlias = properties.getProperty("$name.keyAlias")
            val keyPassword = properties.getProperty("$name.keyPassword")

            if (store == null || storePassword == null || keyAlias == null || keyPassword == null) {
                return@mapNotNull null
            }
            if (!rootProject.file(store).exists()) {
                logger.warn(
                    "[signing] keystore.properties points '$name' at $store, which does not " +
                        "exist. Falling back to the debug key for that environment.",
                )
                return@mapNotNull null
            }
            name to KeystoreEntry(store, storePassword, keyAlias, keyPassword)
        }
        .toMap()
}

/**
 * Creates one signing config per distinct key named in [AppFlavor] and attaches it to the flavours
 * that asked for it.
 */
internal fun Project.configureSigning(extension: ApplicationExtension) {
    val entries = loadKeystoreEntries()

    entries.forEach { (name, entry) ->
        extension.signingConfigs.create(name).apply {
            storeFile = rootProject.file(entry.storeFile)
            storePassword = entry.storePassword
            keyAlias = entry.keyAlias
            keyPassword = entry.keyPassword
            // V1 is the pre-Nougat JAR signature. minSdk here is 24+, so it only inflates the
            // artifact and slows signing down.
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    extension.buildTypes.getByName("debug").signingConfig = null

    val debugConfig = extension.signingConfigs.getByName("debug")
    val missing = mutableListOf<String>()

    AppFlavor.entries.forEach { flavor ->
        val config = entries[flavor.signingKeyName]
            ?.let { extension.signingConfigs.getByName(flavor.signingKeyName) }
            ?: debugConfig.also { missing += flavor.flavorName }

        extension.productFlavors.getByName(flavor.flavorName).signingConfig = config
    }

    if (missing.isNotEmpty()) {
        logger.lifecycle(
            "[signing] No key configured for ${missing.joinToString()} — those variants will be " +
                "signed with the debug key. Copy keystore.properties.template to " +
                "keystore.properties and fill it in before cutting a release.",
        )
    }
}
