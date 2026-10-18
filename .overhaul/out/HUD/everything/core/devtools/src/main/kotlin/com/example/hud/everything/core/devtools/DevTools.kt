package com.example.hud.everything.core.devtools

import com.example.hud.everything.core.devtools.log.DevToolsLog
import com.example.hud.everything.core.devtools.page.DevToolsPage
import com.example.hud.everything.core.devtools.screen.CurrentScreen
import javax.inject.Inject
import javax.inject.Singleton
import com.example.hud.everything.core.flags.FeatureFlags
import com.example.hud.everything.core.flags.FlagOverrides

/** Everything the inspector shows, injected as one value into the app's root. */
@Singleton
class DevTools @Inject constructor(
    val environment: DevEnvironment,
    val log: DevToolsLog,
    val currentScreen: CurrentScreen,
    pages: Set<@JvmSuppressWildcards DevToolsPage>,
    internal val flags: FeatureFlags,
    internal val flagOverrides: FlagOverrides,
) {
    /** In a fixed order: a set from Hilt has none, and the tabs must not swap between launches. */
    internal val pages: List<DevToolsPage> = pages.sortedBy { it.key }
}
