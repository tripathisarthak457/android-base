package com.base.app.feature.search.api

import com.base.app.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchKey : AppNavKey

@Serializable
data class SearchResultKey(val id: Int) : AppNavKey
