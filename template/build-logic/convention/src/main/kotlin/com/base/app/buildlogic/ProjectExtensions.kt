package com.base.app.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * Access to `gradle/libs.versions.toml` from precompiled plugin code, where the generated
 * `libs.` accessors of a normal build script are not available.
 */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Fails loudly and by name when an alias is missing.
 *
 * The alternative — `findLibrary(alias).get()` — throws `NoSuchElementException: No value
 * present`, which says nothing about which alias or which module, and costs a bisect to locate.
 */
fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalArgumentException(
            "No library alias '$alias' in gradle/libs.versions.toml. " +
                "Add it there rather than declaring the coordinate inline.",
        )
    }

fun VersionCatalog.pluginId(alias: String): String =
    findPlugin(alias).orElseThrow {
        IllegalArgumentException("No plugin alias '$alias' in gradle/libs.versions.toml.")
    }.get().pluginId
