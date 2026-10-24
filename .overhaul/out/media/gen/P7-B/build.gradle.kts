/*
 * Root build file.
 */

plugins {
    // The root's own `clean` and `check`; see the end of this file.
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.baselineprofile) apply false
    // Applied here rather than `apply false`: the root project is where the merged report is
    // assembled, so it needs the plugin itself and not just the version.
    alias(libs.plugins.kover)
    alias(libs.plugins.dependency.analysis)
}

/*
 * Coverage, merged across every module, with a floor the build enforces.
 */
subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    subprojects.forEach { kover(project(it.path)) }
}

kover {
    reports {
        filters {
            excludes {
                /*
                 * Generated code, which is most of what an uncovered-lines report is full of before
                 * anything is excluded.
                 */
                classes(
                    "*_Factory",
                    "*_Factory\$*",
                    "*_HiltModules*",
                    "*_MembersInjector",
                    "*_Impl",
                    "*_Impl\$*",
                    "*Hilt_*",
                    "*ComposableSingletons*",
                    "*.BuildConfig",
                    "*.R",
                    "*.R\$*",
                    "*.Manifest*",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                )
                // Composables are checked by eye, in previews and the catalog, not by unit tests —
                // counting them made the number mostly a measure of how much UI there is.
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "androidx.compose.runtime.Composable",
                )
                packages("*.catalog")
            }
        }

        verify {
            rule {
                minBound(providers.gradleProperty("coverageMinimum").get().toInt())
            }
        }
    }
}

/*
 * What each module actually uses, against what it declares: `./gradlew buildHealth` writes the
 * report to build/reports/dependency-analysis. It reports rather than fails: the advice is for a
 * person to weigh, and an unused declaration is not worth stopping a build over.
 *
 * Applied to every module, not only the root. The root plugin aggregates; it does not analyse.
 */
subprojects {
    apply(plugin = "com.autonomousapps.dependency-analysis")
}

dependencyAnalysis {
    issues {
        all {
            onAny { severity("warn") }
            // Annotation processors have no classes for the plugin to detect in the module's code.
            onUnusedAnnotationProcessors { severity("ignore") }
        }
    }
}

// The convention plugins' own tests live in the included build, which `./gradlew check` does not
// reach by itself.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
