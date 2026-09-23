package com.base.app.data.sample

/**
 * The domain model. Lives in the data module that owns it rather than in `:core:model`, and is
 * exposed to feature modules simply by being public.
 */
data class SampleItem(
    val id: Int,
    val title: String,
    val body: String,
) {
    /** What a list row shows when the body is long. Derived here so every screen agrees. */
    val preview: String
        get() = body.replace('\n', ' ').trim().take(PREVIEW_LENGTH).let {
            if (it.length < body.trim().length) "$it…" else it
        }

    private companion object {
        const val PREVIEW_LENGTH = 90
    }
}
