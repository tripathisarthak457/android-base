package com.example.cmp.core.common.navigation

/**
 * A navigable destination. Deliberately *not* `androidx.navigation3.runtime.NavKey`.
 *
 * 1. **Annotate it `@Serializable` and register it.** The back stack survives process death by
 *    being serialised whole, and a key no feature registers with `entry<K>` cannot be restored:
 *    the stack falls back to the start destination, which the user experiences as the app
 *    forgetting where they were.
 *
 * 2. **Carry ids, not payloads.** A destination takes the id of the thing it shows and fetches it
 *    itself. Parameters that look like content, such as a name or an image URL, are acceptable only
 *    as a seed, so the screen has something to render before its own fetch resolves, and the screen
 *    must still fetch. A key holding real data goes stale the moment the user backgrounds the app,
 *    and it has to fit in a saved-state bundle.
 */
interface AppNavKey

/** A destination that covers the tab bar instead of being pushed above it: a player, an editor. */
interface FullScreenKey : AppNavKey
