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
    // <opt:database>
    alias(libs.plugins.room3) apply false
    // </opt:database>
    alias(libs.plugins.roborazzi) apply false
    // <opt:firebase>
    alias(libs.plugins.google.services) apply false
    // </opt:firebase>
    // <opt:crashlytics>
    alias(libs.plugins.firebase.crashlytics) apply false
    // </opt:crashlytics>
    // <opt:performance>
    alias(libs.plugins.baselineprofile) apply false
    // </opt:performance>
    // <opt:coverage>
    // Applied here rather than `apply false`: the root project is where the merged report is
    // assembled, so it needs the plugin itself and not just the version.
    alias(libs.plugins.kover)
    // </opt:coverage>
    // <opt:depsanalysis>
    alias(libs.plugins.dependency.analysis)
    // </opt:depsanalysis>
    // <opt:formatting>
    alias(libs.plugins.spotless)
    // </opt:formatting>
    alias(libs.plugins.dependency.guard) apply false
}

// <opt:formatting>
/*
 * Formatting: `./gradlew spotlessApply` fixes it and `check` fails on what it would change. The
 * rules are ktlint's Android Studio style, read from .editorconfig, so the IDE formats the same way.
 */
spotless {
    // <generated:formatting-check>
    // The template's own sources move to this style in one mechanical commit once the UI work has
    // merged; until then its check leaves formatting out. Generated projects always enforce it.
    isEnforceCheck = false
    // </generated:formatting-check>
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "*/**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}
// </opt:formatting>

// <opt:coverage>
/*
 * Coverage, merged across every module, with a floor the build enforces. Each module applies Kover
 * through its convention plugin. The benchmarks apply none: they run on a phone and have no unit
 * tests to cover.
 */
dependencies {
    subprojects
        .filter { it.buildFile.isFile && it.path != ":benchmark" }
        .forEach { kover(project(it.path)) }
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
// </opt:coverage>

// <opt:depsanalysis>
/*
 * What each module actually uses, against what it declares: `./gradlew buildHealth` writes the
 * report to build/reports/dependency-analysis. It reports rather than fails: the advice is for a
 * person to weigh, and an unused declaration is not worth stopping a build over.
 *
 * Each module applies it through its convention plugin (the benchmarks, which apply none, are not
 * analysed); the root plugin aggregates, it does not analyse.
 */
dependencyAnalysis {
    issues {
        all {
            onAny { severity("warn") }
            // Annotation processors have no classes for the plugin to detect in the module's code.
            onUnusedAnnotationProcessors { severity("ignore") }
        }
    }
}
// </opt:depsanalysis>

// The convention plugins' own tests live in the included build, which `./gradlew check` does not
// reach by itself.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
