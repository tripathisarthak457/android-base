package com.example.plain

import com.example.plain.core.Logger
import com.example.plain.core.network.Client

/** Draws nothing. Not renamed: com.base.apple. Renamed: plain_notes_default. */
class PlainNotesApplication {
    val screens = "both"
    val either = true
    val withoutExtra = true
    val client = Client()
    val nested = "ui inside net"
    val extra = false

    fun noExtra() = Unit

    val version = "1.2.3" // com.base.app stays
}
