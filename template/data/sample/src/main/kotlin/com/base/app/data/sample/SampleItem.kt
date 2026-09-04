package com.base.app.data.sample

/**
 * The domain model.
 *
 * Lives in the data module that owns it rather than in `:core:model`, and is exposed to feature
 * modules simply by being public. `:core:model` is for types that genuinely cross domains —
 * paging, sorting — and using it as the home for every model turns it into a module that every
 * other module depends on and that changes every week, which is the worst possible shape for the
 * bottom of a build graph.
 *
 * Deliberately not the DTO. The wire format is the backend's decision and it changes on their
 * schedule; this is the app's, and a mapper between the two is what stops a renamed JSON field
 * from being a change to six screens.
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
