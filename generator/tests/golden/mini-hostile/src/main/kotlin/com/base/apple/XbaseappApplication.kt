package com.base.apple

import com.base.apple.core.Logger
import com.base.apple.core.network.Client

/** Not renamed: com.base.apple, database_app, MyBaseApp, xbaseapp. Renamed: xbaseapp_default. */
class XbaseappApplication {
    val screens = "both"
    val either = true
    val withoutExtra = true
    val client = Client()
    val nested = "ui inside net"
    val extra = false

    fun noExtra() = Unit

    val version = "1.2.3" // com.base.app stays
}
