package com.example.finalafter.crui.data.profile.api.model

/** What the person says about themselves. */
data class Profile(
    val name: String = "",
    val bio: String = "",
    /** A `file:` URI of the avatar in the app's own storage, or null for the initials avatar. */
    val photoUri: String? = null,
)
