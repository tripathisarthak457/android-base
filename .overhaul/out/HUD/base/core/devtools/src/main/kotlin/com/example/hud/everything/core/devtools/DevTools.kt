package com.example.hud.everything.core.devtools

import com.example.hud.everything.core.devtools.log.DevToolsLog
import javax.inject.Inject
import javax.inject.Singleton
import com.example.hud.everything.core.flags.FeatureFlags
import com.example.hud.everything.core.flags.FlagOverrides

/** Everything the inspector shows, injected as one value into the app's root. */
@Singleton
class DevTools @Inject constructor(
    val environment: DevEnvironment,
    val log: DevToolsLog,
    internal val flags: FeatureFlags,
    internal val flagOverrides: FlagOverrides,
)
