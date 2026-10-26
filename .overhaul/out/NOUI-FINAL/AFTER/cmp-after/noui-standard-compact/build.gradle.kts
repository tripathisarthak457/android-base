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
        // ktlint wraps code at max_line_length (.editorconfig); what it cannot wrap, a comment, a
        // string or a long name, stays as written instead of failing the build.
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:max-line-length"
        }
    }
}

// The convention plugins' own tests live in the included build, which `./gradlew check` does not
// reach by itself.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
