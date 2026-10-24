package com.base.app

import com.base.app.core.Logger
// <opt:net>
import com.base.app.core.network.Client
// </opt:net>

// <opt:visuals>
/** Not renamed: com.base.apple, database_app, MyBaseApp, xbaseapp. Renamed: base_app_default. */
// </opt:visuals>
// <opt-else:visuals>
//~ /** Draws nothing. Not renamed: com.base.apple. Renamed: base_app_default. */
// </opt-else:visuals>
class BaseAppApplication {

    // <opt:net&ui>
    val screens = "both"
    // </opt:net&ui>
    // <opt:extra|ui>
    val either = true
    // </opt:extra|ui>
    // <opt:!extra>    val withoutExtra = true
    // <opt:net>
    val client = Client()
    // <opt:ui>
    val nested = "ui inside net"
    // </opt:ui>
    // </opt:net>
    // <opt:extra>
    val extra = true
    // </opt:extra>
    // <opt-else:extra>
    //~ val extra = false
    //~
    //~ fun noExtra() = Unit
    // </opt-else:extra>

    // <generated:version>
    val version = "0.0.0"
    // </generated:version>

    // <opt:extra>
    fun onlyWithExtra() = Unit

    // </opt:extra>
}
