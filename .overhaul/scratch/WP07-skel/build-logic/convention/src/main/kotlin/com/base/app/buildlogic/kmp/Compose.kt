package com.base.app.buildlogic.kmp

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Compose, minus Material: runtime, foundation, ui and resources, on every target. */
internal fun Project.configureComposeMultiplatform() {
    pluginManager.apply("org.jetbrains.compose")
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("config/compose-stability.conf"),
        )
        if (providers.gradleProperty("enableComposeCompilerReports").orNull.toBoolean()) {
            reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
            metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
        }
    }

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonMain").dependencies {
            implementation(libs.library("compose-runtime"))
            implementation(libs.library("compose-foundation"))
            implementation(libs.library("compose-ui"))
            implementation(libs.library("compose-components-resources"))
            implementation(libs.library("compose-ui-tooling-preview"))
        }
    }

    val hasResources = file("src/commonMain/composeResources").isDirectory
    if (hasResources) {
        // Compose resources reach the Android app as assets merged from each library, which only
        // happens for a library whose Android resources are switched on.
        extensions.getByType<KotlinMultiplatformExtension>().let { kotlin ->
            (kotlin as ExtensionAware).extensions.getByType<KotlinMultiplatformAndroidLibraryTarget>()
                .androidResources.enable = true
        }
    }
    extensions.getByType<ComposeExtension>().extensions.configure<ResourcesExtension> {
        // `<module namespace>.resources.Res`, internal to the module: a string lives in the module
        // that shows it, and no other module can reach for it.
        packageOfResClass = "${moduleNamespace()}.resources"
    }
}
