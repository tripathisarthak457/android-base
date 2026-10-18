package com.example.wp04.base.feature.search

import com.example.wp04.base.core.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchKey : AppNavKey

@Serializable
data class SearchResultKey(val id: Int) : AppNavKey
