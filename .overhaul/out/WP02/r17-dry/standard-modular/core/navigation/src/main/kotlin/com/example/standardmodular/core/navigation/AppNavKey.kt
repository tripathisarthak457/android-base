package com.example.standardmodular.core.navigation

/** How a destination arrives and leaves. */
enum class NavTransitionStyle {
    /** Deeper into a stack, and back out. The overwhelming majority of navigation. */
    Push,

    /** A layer over the current screen — a cart, a composer, a full-screen viewer. */
    Modal,

    /** A lateral move between peers: tab to tab, or a splash handing off. */
    Fade,

    /** No animation. What reduce-motion collapses everything to. */
    None,
}
