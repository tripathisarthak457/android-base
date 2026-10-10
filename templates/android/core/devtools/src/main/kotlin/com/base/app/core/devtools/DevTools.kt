package com.base.app.core.devtools

import com.base.app.core.devtools.log.DevToolsLog
import javax.inject.Inject
import javax.inject.Singleton
// <opt:flags>
import com.base.app.core.flags.FeatureFlags
import com.base.app.core.flags.FlagOverrides
// </opt:flags>

/** Everything the inspector shows, injected as one value into the app's root. */
@Singleton
class DevTools @Inject constructor(
    val environment: DevEnvironment,
    val log: DevToolsLog,
    // <opt:flags>
    internal val flags: FeatureFlags,
    internal val flagOverrides: FlagOverrides,
    // </opt:flags>
)
