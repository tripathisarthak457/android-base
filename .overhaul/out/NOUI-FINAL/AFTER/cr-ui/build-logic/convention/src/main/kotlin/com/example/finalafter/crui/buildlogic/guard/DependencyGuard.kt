package com.example.finalafter.crui.buildlogic.guard

import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPluginExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** What ships: every library the prod release carries, transitive ones included. */
internal const val GUARDED_CLASSPATH = "prodReleaseRuntimeClasspath"

/**
 * Keeps the release's runtime classpath in `app/dependencies/prodReleaseRuntimeClasspath.txt` and
 * compares it on every `check`. A library that arrives, leaves or changes version, directly or
 * through another one, fails the build with the diff until the list is rewritten on purpose
 * (`./gradlew dependencyGuardBaseline`) and reviewed with the change that caused it. The first
 * build writes the list, which belongs in version control from then on.
 */
internal fun Project.guardReleaseDependencies() {
    pluginManager.apply("com.dropbox.dependency-guard")
    extensions.configure<DependencyGuardPluginExtension> {
        configuration(GUARDED_CLASSPATH)
    }
    tasks.named { it == "check" }.configureEach { dependsOn("dependencyGuard") }
}
