package com.base.app.core.flags

/** One flag, declared once, with the value it takes when nobody has said otherwise. */
sealed class Flag<T>(val key: String, val default: T) {

    class Bool(key: String, default: Boolean) : Flag<Boolean>(key, default)

    class Text(key: String, default: String) : Flag<String>(key, default)

    class Number(key: String, default: Long) : Flag<Long>(key, default)

    /** Every flag the app knows about. Add yours here, and delete the sample once you have one. */
    companion object {
        val SampleBanner = Bool("sample_banner_enabled", default = false)

        val ALL: List<Flag<*>> = listOf(SampleBanner)

        /** What the vendor SDK is seeded with, so a first launch with no network still agrees. */
        val DEFAULTS: Map<String, Any> = ALL.associate { it.key to it.default as Any }
    }
}
