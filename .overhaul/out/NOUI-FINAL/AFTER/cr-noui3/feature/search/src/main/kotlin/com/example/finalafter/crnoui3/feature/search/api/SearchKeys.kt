package com.example.finalafter.crnoui3.feature.search.api

import com.example.finalafter.crnoui3.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchKey : AppNavKey

@Serializable
data class SearchResultKey(val id: Int) : AppNavKey
