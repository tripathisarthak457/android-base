package com.example.finalafter.crnoui.feature.search.api

import com.example.finalafter.crnoui.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchKey : AppNavKey

@Serializable
data class SearchResultKey(val id: Int) : AppNavKey
