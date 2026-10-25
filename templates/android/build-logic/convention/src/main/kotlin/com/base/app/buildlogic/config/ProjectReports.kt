package com.base.app.buildlogic.config
// <opt:coverage|depsanalysis>

import org.gradle.api.Project

/**
 * Feeds the reports the root project assembles across every module: merged coverage, and which
 * dependencies a module declares without using. Each module applies the plugins through its own
 * convention, rather than the root reaching into every project with `subprojects { }`, so the
 * build keeps working with Gradle's Isolated Projects.
 */
internal fun Project.feedProjectReports() {
    // <opt:coverage>
    pluginManager.apply("org.jetbrains.kotlinx.kover")
    // </opt:coverage>
    // <opt:depsanalysis>
    pluginManager.apply("com.autonomousapps.dependency-analysis")
    // </opt:depsanalysis>
}
// </opt:coverage|depsanalysis>
