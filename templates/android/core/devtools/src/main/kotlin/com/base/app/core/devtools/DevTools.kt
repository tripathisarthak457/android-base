package com.base.app.core.devtools

import androidx.compose.runtime.Composable
import com.base.app.core.common.environment.AppEnvironment
import com.base.app.core.devtools.badge.DevToolsOverlay
import com.base.app.core.devtools.log.DevToolsLog
import com.base.app.core.devtools.page.DevToolsPage
import com.base.app.core.devtools.screen.CurrentScreen
// <opt:flags>
import com.base.app.core.flags.FeatureFlags
import com.base.app.core.flags.FlagOverrides
// </opt:flags>
import com.base.app.core.ui.tooling.DevTooling
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the inspector shows, and the app root's way in to it. */
@Singleton
internal class DevTools @Inject constructor(
    val environment: AppEnvironment,
    val log: DevToolsLog,
    val currentScreen: CurrentScreen,
    pages: Set<@JvmSuppressWildcards DevToolsPage>,
    // <opt:flags>
    val flags: FeatureFlags,
    val flagOverrides: FlagOverrides,
    // </opt:flags>
) : DevTooling {
    /** In a fixed order: a set from Hilt has none, and the tabs must not swap between launches. */
    val pages: List<DevToolsPage> = pages.sortedBy { it.key }

    /** `DEV · debug`, which is what somebody holding two installs needs to tell them apart. */
    val label: String = buildString {
        append(environment.name.uppercase())
        if (environment.isDebugBuild) append(" · debug")
    }

    override fun onScreenChanged(name: String) {
        currentScreen.report(name)
    }

    @Composable
    override fun Overlay() {
        DevToolsOverlay(this)
    }
}
