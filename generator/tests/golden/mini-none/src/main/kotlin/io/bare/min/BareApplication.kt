package io.bare.min

import io.bare.min.core.Logger

/** Not renamed: com.base.apple, database_app, MyBaseApp, xbaseapp. Renamed: bare_default. */
class BareApplication {
    val withoutExtra = true
    val extra = false

    fun noExtra() = Unit

    val version = "1.2.3" // com.base.app stays
}
