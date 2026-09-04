package com.base.app.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.util.Properties

/**
 * Signing, read from a `keystore.properties` that is never committed.
 *
 * The file is expected at the root of the build and holds four values per environment:
 *
 * ```
 * dev.storeFile=keys/dev.jks
 * dev.storePassword=…
 * dev.keyAlias=…
 * dev.keyPassword=…
 * ```
 *
 * When the file — or one environment's block within it — is missing, that environment falls back
 * to the debug keystore and the build carries on. This is the whole point: a colleague or a CI
 * runner that has only just cloned the repository can still produce a running devDebug without
 * being handed secrets first, and only the person cutting a release needs the real keys. A build
 * that hard-failed on a missing keystore would make `assembleDevDebug` a credentials problem.
 *
 * The fallback is announced on the console rather than applied silently, so nobody discovers at
 * upload time that their "release" was signed with the debug key.
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
 * Creates one signing config per distinct key named in [AppFlavor] and attaches it to the
 * flavours that asked for it.
 *
 * The `debug` build type's own signing config is cleared first. AGP assigns the shared debug
 * keystore to it by default, and a build type's signing config wins over a flavour's — so
 * without this line `devDebug` would be signed with the generic debug key rather than the dev
 * key, and would refuse to install over a build that used the right one.
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
