package com.base.app.core.navigation

import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.common.navigation.FullScreenKey
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
