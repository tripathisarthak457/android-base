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
    // <opt:coverage>
    // Applied here rather than `apply false`: the root project is where the merged report is
    // assembled, so it needs the plugin itself and not just the version.
    alias(libs.plugins.kover)
    // </opt:coverage>
    // <opt:depsanalysis>
    alias(libs.plugins.dependency.analysis)
    // </opt:depsanalysis>
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

    // Stated rather than inherited. The daemon runs on 17 (see gradle-daemon-jvm.properties), so
    // this agrees with it — but detekt takes its target from the running JVM by default, and a
    // developer who overrides the daemon JVM would otherwise get a detekt failure about a
    // --jvm-target they never chose.
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
    }
}
// </opt:staticanalysis>

// <opt:coverage>
/*
 * Coverage, merged across every module, with a floor the build enforces.
 *
 * One number for the whole project rather than one per module. Per-module minimums sound
 * fairer and are not: `:core:model` is data classes and reaches 90% by accident, while
 * `:core:network` is the module where a missing test actually costs something, and a
 * per-module floor low enough for the second is meaningless for the first.
 *
 * The floor is `coverageMinimum` in gradle.properties. It is a ratchet, not a target: raise it
 * when a release comfortably clears it, and never lower it to make a red build green — that is
 * the one edit that turns this from a check into a decoration.
 */
subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    subprojects.forEach { kover(it) }
}

kover {
    reports {
        filters {
            excludes {
                /*
                 * Generated code, which is most of what an uncovered-lines report is full of
                 * before anything is excluded. None of it is written here and none of it can be
                 * tested here: Hilt's factories and modules, Room's DAO implementations, the
                 * singletons the Compose compiler emits for every lambda, and the generated R
                 * and BuildConfig. Left in, they bury the classes that genuinely have no test.
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
                // A preview is sample data for the IDE. Requiring it to be covered would mean
                // writing a test that renders fake rows to satisfy a number.
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
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
 * What each module actually uses, against what it declares.
 *
 * The failure this catches has no other symptom: module A compiles only because module B put
 * something on its classpath transitively. Nothing is wrong until B drops that dependency, at
 * which point A fails to compile for a reason that has nothing to do with the commit that broke
 * it. The reverse — a dependency declared and never used — is quieter and still costs a
 * recompile of that module every time the unused library changes.
 *
 * `:projectHealth` is advice by default; this makes it a build failure, because advice that
 * does not fail is advice nobody reads.
 */
/*
 * Applied to every module, not only the root.
 *
 * The root plugin aggregates; it does not analyse. With it applied only there, `buildHealth`
 * succeeds and prints "No project health reports found" — a green build that checked nothing,
 * which is worse than a red one. This is also the arrangement that stays correct under project
 * isolation, which this build has partly on already.
 */
subprojects {
    apply(plugin = "com.autonomousapps.dependency-analysis")
}

dependencyAnalysis {
    issues {
        all {
            onAny { severity("fail") }
            // Two exceptions, both because the plugin cannot see why the dependency is there.
            //
            // Annotation processors and platforms have no classes to detect a use of, and the
            // design system exposes Compose as `api` on purpose — every module that draws
            // anything needs Modifier and Color in its own signatures, and re-declaring them per
            // module is how two modules end up on different Compose versions.
            onUnusedAnnotationProcessors { severity("ignore") }
            onUsedTransitiveDependencies { severity("warn") }
        }
    }
}
// </opt:depsanalysis>

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
