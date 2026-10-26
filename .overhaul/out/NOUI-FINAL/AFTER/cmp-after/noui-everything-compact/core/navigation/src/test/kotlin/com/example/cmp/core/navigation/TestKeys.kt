package com.example.cmp.core.navigation

import com.example.cmp.core.common.navigation.AppNavKey
import com.example.cmp.core.common.navigation.FullScreenKey
import kotlinx.serialization.Serializable

@Serializable
internal data object FeedTab : AppNavKey

@Serializable
internal data object ProfileTab : AppNavKey

@Serializable
internal data class Post(val id: Int) : AppNavKey

@Serializable
internal data object Compose : AppNavKey

@Serializable
internal data object Player : FullScreenKey
