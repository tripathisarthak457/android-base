package com.base.app.core.flags

/**
 * One flag, declared once, with the value it takes when nobody has said otherwise.
 *
 * ## The default lives with the declaration
 *
 * This is the whole point of the type. A remote-config setup normally keeps two lists: the keys
 * the code reads, and a defaults map handed to the SDK at startup. They drift — someone adds a
 * flag and forgets the defaults entry — and the symptom is a feature that is off for every user
 * whose first launch had no network, which nobody can reproduce on a desk. Here [DEFAULTS] is
 * derived from the declarations, so the two cannot disagree.
 *
 * ## Why a sealed class rather than a string key
 *
 * `flags.getBoolean("new_checkout")` compiles when the flag is a string on the server, returns
 * false, and is wrong in a way nothing catches. A declared [Flag] carries its type, so the read
 * is checked and every flag in the app can be listed by looking at one file.
 */
sealed class Flag<T>(val key: String, val default: T) {

    class Bool(key: String, default: Boolean) : Flag<Boolean>(key, default)

    class Text(key: String, default: String) : Flag<String>(key, default)

    class Number(key: String, default: Long) : Flag<Long>(key, default)

    /**
     * Every flag the app knows about. Add yours here; nothing else needs to change.
     *
     * The sample is deliberately something harmless. Delete it once you have a real one — an
     * empty registry is fine and the module still works.
     */
    companion object {
        val SampleBanner = Bool("sample_banner_enabled", default = false)

        val ALL: List<Flag<*>> = listOf(SampleBanner)

        /** What the vendor SDK is seeded with, so a first launch with no network still agrees. */
        val DEFAULTS: Map<String, Any> = ALL.associate { it.key to it.default as Any }
    }
}
