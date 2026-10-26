package com.example.finalafter.crui.data.feed.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeedPostDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("userId") val userId: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
)
