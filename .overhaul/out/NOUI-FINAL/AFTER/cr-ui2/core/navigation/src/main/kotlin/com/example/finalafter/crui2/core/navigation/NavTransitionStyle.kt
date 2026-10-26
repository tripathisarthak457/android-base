package com.example.finalafter.crui2.core.navigation

/** How a pushed destination arrives and leaves. A sheet or a cover is a [Presentation] instead. */
enum class NavTransitionStyle {
    /** Slides in from the trailing edge over the screen it covers, and back out. */
    Push,

    /** Appears without moving: a destination that replaces what was there rather than covering it. */
    None,
}
