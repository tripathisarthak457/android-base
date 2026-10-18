package com.example.wp04.dlplay.feature.search.api

import com.example.wp04.dlplay.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchKey : AppNavKey

@Serializable
data class SearchResultKey(val id: Int) : AppNavKey
