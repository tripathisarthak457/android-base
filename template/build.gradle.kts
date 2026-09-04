/*
 * Root build file.
 *
 * Every plugin is declared `apply false` to pin its version once for the whole build and put it
 * on the shared buildscript classpath, which is what lets the convention plugins in `build-logic`
 * apply them by id. Modules opt in through those conventions, never by repeating a version.
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // <opt:firebase>
    alias(libs.plugins.google.services) apply false
    // </opt:firebase>
    // <opt:crashlytics>
    alias(libs.plugins.firebase.crashlytics) apply false
    // </opt:crashlytics>
    // <opt:baselineprofile>
    alias(libs.plugins.baselineprofile) apply false
    // </opt:baselineprofile>
    // <opt:staticanalysis>
    alias(libs.plugins.detekt) apply false
    // </opt:staticanalysis>
}

// <opt:staticanalysis>
/*
 * Static analysis is applied from the root rather than from a convention plugin, so that the
 * container projects (:core, :data, :feature) and any module added later are covered without
 * anyone remembering to opt in. Every module is configured identically — a module with its own
 * rules is a module whose warnings nobody trusts.
 *
 * `detekt-formatting` is ktlint's rule set running inside detekt. One tool, one report, one
 * version to keep aligned, instead of two that disagree about the same line.
 */
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        parallel = true
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt.yml"))
        source.setFrom("src/main/kotlin", "src/test/kotlin", "src/androidTest/kotlin")
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
    }
}
// </opt:staticanalysis>

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
