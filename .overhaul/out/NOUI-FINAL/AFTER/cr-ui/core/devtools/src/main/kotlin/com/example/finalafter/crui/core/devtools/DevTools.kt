package com.example.finalafter.crui.core.devtools

import androidx.compose.runtime.Composable
import com.example.finalafter.crui.core.common.environment.AppEnvironment
import com.example.finalafter.crui.core.devtools.badge.DevToolsOverlay
import com.example.finalafter.crui.core.devtools.log.DevToolsLog
import com.example.finalafter.crui.core.devtools.page.DevToolsPage
import com.example.finalafter.crui.core.devtools.screen.CurrentScreen
import com.example.finalafter.crui.core.flags.FeatureFlags
import com.example.finalafter.crui.core.flags.FlagOverrides
import com.example.finalafter.crui.core.ui.tooling.DevTooling
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the inspector shows, and the app root's way in to it. */
@Singleton
internal class DevTools @Inject constructor(
    val environment: AppEnvironment,
    val log: DevToolsLog,
    val currentScreen: CurrentScreen,
    pages: Set<@JvmSuppressWildcards DevToolsPage>,
    val flags: FeatureFlags,
    val flagOverrides: FlagOverrides,
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
