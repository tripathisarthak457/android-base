package com.base.app.buildlogic.kmp

import com.base.app.buildlogic.config.AppConfig
import org.gradle.api.Project

/**
 * The module's root package, from its path: `:feature:order_history` is
 * `com.base.app.feature.orderhistory`. The package is the namespace, so a module's sources, its
 * Android namespace and its resource class always agree.
 */
internal fun Project.moduleNamespace(): String =
    AppConfig.BASE_PACKAGE + path.replace("_", "").replace(':', '.')
