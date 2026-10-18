package com.example.hud.fixture.core.ui.tooling

import androidx.compose.runtime.Composable

/**
 * Developer tooling as the app's root sees it: the inspector's badge and panel, and whatever plugs
 * into them.
 *
 * Only dev-flavour and debug builds depend on the module that binds it. The root injects it as an
 * `Optional`, so a shippable build carries none of its code, composes nothing for it and reports
 * nothing to it.
 */
interface DevTooling {

    /** The destination on top of the back stack, by name, each time it changes. */
    fun onScreenChanged(name: String)

    /**
     * The tooling's own layer, filling the root above every screen. The root draws it under the app
     * lock: what the app sent is as private as what it shows.
     */
    @Composable
    fun Overlay()
}
