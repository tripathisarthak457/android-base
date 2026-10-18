package com.example.hud.everything.feature.search.api

import com.example.hud.everything.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchKey : AppNavKey

@Serializable
data class SearchResultKey(val id: Int) : AppNavKey
