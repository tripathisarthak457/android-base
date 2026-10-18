package com.example.wp04.abstd.feature.sample.api

import com.example.wp04.abstd.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations, and its whole public surface. Nothing in `:core:navigation` or
 * `:app` needs more than these to open a sample screen.
 */
@Serializable
data object SampleListKey : AppNavKey

@Serializable
data class SampleDetailKey(val itemId: Int) : AppNavKey
