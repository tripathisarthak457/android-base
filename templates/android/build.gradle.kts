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
    alias(libs.plugins.spotless)
    alias(libs.plugins.dependency.guard) apply false
}

/*
 * Spotless runs ktlint over every Kotlin source with the Compose Rules ruleset: the checks on how
 * a composable takes and uses its parameters that a pattern cannot make. `check` runs
 * `spotlessCheck`, which fails on a finding; .editorconfig says which rules run and why the others
 * do not.
 */
spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .customRuleSets(listOf(libs.compose.rules.ktlint.get().toString()))
            // <generated:template-comment-spacing>
            // The raw template's `//~` fallback lines, which ktlint would space as `// ~` and so
            // break; a generated project has none.
            .editorConfigOverride(mapOf("ktlint_standard_comment-spacing" to "disabled"))
        // </generated:template-comment-spacing>
        // ktlint wraps code at max_line_length (.editorconfig); what it cannot wrap, a comment, a
        // string or a long name, stays as written instead of failing the build.
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:max-line-length"
        }
        // <generated:template-import-order>
        // The template's import lists hold its markers, which ktlint takes for comments it will not
        // sort around. A project has none: the generator orders its imports, and its tests hold the
        // template's own order.
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:import-ordering"
        }
        // </generated:template-import-order>
    }
    // <opt:formatting>
    // Formatting: ktlint's Android Studio style over the Gradle files too, read from .editorconfig
    // so the IDE formats the same way. `./gradlew spotlessApply` fixes what `check` fails on.
    kotlinGradle {
        target("*.gradle.kts", "*/**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            // <generated:template-gradle-comment-spacing>
            .editorConfigOverride(mapOf("ktlint_standard_comment-spacing" to "disabled"))
        // </generated:template-gradle-comment-spacing>
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:max-line-length"
        }
    }
    // </opt:formatting>
}

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
