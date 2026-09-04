package com.base.app.core.navigation

/**
 * A navigable destination.
 *
 * Deliberately *not* `androidx.navigation3.runtime.NavKey`. Feature modules implement this
 * interface, and this module is the only one that knows Navigation 3 exists — so replacing the
 * navigation library is a change to four files in `:core:navigation` rather than a change to
 * every feature in the project. That is the entire reason this wrapper exists.
 *
 * Two rules hold for every implementation:
 *
 * 1. **Annotate it `@Serializable` and register it.** The back stack survives process death by
 *    being serialised whole (see [AppBackStack]), and a key that is not registered in its
 *    feature's [navKeys] block cannot be restored — the stack falls back to the start
 *    destination, which the user experiences as the app forgetting where they were.
 *
 * 2. **Carry ids, not payloads.** A destination takes the id of the thing it shows and fetches it
 *    itself. Parameters that look like content — a name, an image URL — are acceptable only as a
 *    seed, so the screen has something to render before its own fetch resolves, and the screen
 *    must still fetch. A key holding real data goes stale the moment the user backgrounds the app,
 *    and it has to fit in a saved-state bundle.
 */
interface AppNavKey

/**
 * How a destination arrives and leaves.
 *
 * Declared per destination by the feature that owns it, in this module's own vocabulary — the
 * actual animations live in [NavTransitions] and the Navigation 3 metadata that carries them
 * never leaves this module.
 *
 * Four options, and that is on purpose. A screen that invents a fifth is a screen that will feel
 * like it came from a different app.
 */
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
