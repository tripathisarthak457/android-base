package com.example.deskstandard.buildlogic.android

import com.android.build.api.dsl.CommonExtension
import com.example.deskstandard.buildlogic.library
import com.example.deskstandard.buildlogic.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Compose, minus Material. The dependency set is foundation + ui + animation and nothing else. */
internal fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true

    // The stability gate reads the report of the modules every screen calls, so they always write
    // it. Elsewhere writing reports slows every compile, so they come only when asked for:
    // `./gradlew <task> -PcomposeReports`, then read build/compose_reports and build/compose_metrics.
    val requested = providers.gradleProperty("composeReports").orNull.toBoolean()

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Types the compiler cannot see into but which are immutable here, so composables taking
        // them are compared with equals rather than by identity.
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("config/compose-stability.conf"),
        )
        if (requested || path in STABILITY_CHECKED_MODULES) {
            reportsDestination.set(layout.buildDirectory.dir(COMPOSE_REPORTS))
        }
        if (requested) {
            metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
        }
    }

    configureComposeStability()

    // Compose's own lint checks, warnings by default: each is a recomposition, layout or state bug
    // the compiler accepts. Lint ignores an id it does not know without a word, so every one here
    // was checked against the lint jars of the Compose, lifecycle and navigation3 artifacts. Two
    // stay warnings: ComposableLambdaParameterNaming wants a lone trailing slot named `content`
    // even when it is a bar's actions, and ModifierFactoryExtensionFunction flags a factory that
    // needs a scope receiver such as LazyItemScope.
    extension.lint.error += COMPOSE_LINT_ERRORS

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

    // A Compose test rule launches an empty activity, which has to be in the manifest the test
    // runs against.
    onRobolectricTests {
        dependencies.add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
    }
}

internal const val COMPOSE_REPORTS = "compose_reports"

private val COMPOSE_LINT_ERRORS = setOf(
    // runtime
    "ComposableNaming",
    "CompositionLocalNaming",
    "UnrememberedMutableState",
    "UnrememberedState",
    "RememberReturnType",
    "AutoboxingStateCreation",
    "AutoboxingStateValueProperty",
    "MutableCollectionMutableState",
    "OpaqueUnitKey",
    "ProduceStateDoesNotAssignValue",
    "CoroutineCreationDuringComposition",
    "FlowOperatorInvokedInComposition",
    "StateFlowValueCalledInComposition",
    // ui
    "ModifierParameter",
    "ModifierFactoryReturnType",
    "ModifierFactoryUnreferencedReceiver",
    "UnnecessaryComposedModifier",
    "SuspiciousModifierThen",
    "NonObservableLocale",
    "MultipleAwaitPointerEventScopes",
    "ExitAwaitPointerEventScope",
    "ReturnFromAwaitPointerEventScope",
    "ConfigurationScreenWidthHeight",
    "LocalContextConfigurationRead",
    "LocalContextResourcesRead",
    // foundation and animation
    "UnusedBoxWithConstraintsScope",
    "UseOfNonLambdaOffsetOverload",
    "UnrememberedAnimatable",
    "RememberInComposition",
    "FrequentlyChangingValue",
    // lifecycle and navigation3
    "LifecycleCurrentStateInComposition",
    "SceneEqualsHashCode",
)
