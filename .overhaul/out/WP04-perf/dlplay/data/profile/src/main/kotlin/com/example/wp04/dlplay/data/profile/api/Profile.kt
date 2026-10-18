package com.example.wp04.dlplay.data.profile.api

data class Profile(
    val name: String = "",
    val bio: String = "",
    /** A content URI from the photo picker, or null for the initials avatar. */
    val photoUri: String? = null,
)
