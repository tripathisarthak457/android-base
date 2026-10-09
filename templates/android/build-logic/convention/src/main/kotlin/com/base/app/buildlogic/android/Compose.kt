package com.base.app.buildlogic.android

import com.android.build.api.dsl.CommonExtension
import com.base.app.buildlogic.library
import com.base.app.buildlogic.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Compose, minus Material. The dependency set is foundation + ui + animation and nothing else. */
internal fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true

    // Writing the reports costs about 15 % of every compile, so they are written only when asked
    // for: `./gradlew <task> -PcomposeReports`, then read build/compose_reports.
    val reports = providers.gradleProperty("composeReports").orNull.toBoolean()

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Marks java.time and similar types immutable so composables taking them can skip.
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("config/compose-stability.conf"),
        )
        if (reports) {
            reportsDestination.set(layout.buildDirectory.dir(COMPOSE_REPORTS))
            metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
        }
    }

    // <opt:composemetrics>
    if (reports) configureComposeStability()
    // </opt:composemetrics>

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            optIn.addAll(
                "androidx.compose.foundation.ExperimentalFoundationApi",
                "androidx.compose.ui.ExperimentalComposeUiApi",
                "androidx.compose.animation.ExperimentalAnimationApi",
            )
        }
    }

    dependencies {
        val bom = libs.library("androidx-compose-bom")
        add("implementation", platform(bom))

        add("implementation", libs.library("androidx-compose-runtime"))
        add("implementation", libs.library("androidx-compose-foundation"))
        add("implementation", libs.library("androidx-compose-ui"))
        add("implementation", libs.library("androidx-compose-ui-graphics"))
        add("implementation", libs.library("androidx-compose-animation"))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))

        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))

        if (file("src/androidTest").exists()) {
            add("androidTestImplementation", platform(bom))
            add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}

internal const val COMPOSE_REPORTS = "compose_reports"
