package com.example.hud.fixture.feature.search.api

import com.example.hud.fixture.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchKey : AppNavKey

@Serializable
data class SearchResultKey(val id: Int) : AppNavKey
