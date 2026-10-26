package com.example.finalafter.crnoui.data.sample.api.model

/** The domain model: what the app knows about a sample, with no trace of the wire format. */
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
