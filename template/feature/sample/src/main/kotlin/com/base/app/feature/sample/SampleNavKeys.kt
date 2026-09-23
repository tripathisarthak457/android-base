package com.base.app.feature.sample

import com.base.app.core.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations. Owned entirely by the feature: nothing in `:core:navigation` or
 * `:app` names them, and adding a screen here touches no file outside this module.
 */
@Serializable
data object SampleListKey : AppNavKey

@Serializable
data class SampleDetailKey(val itemId: Int) : AppNavKey
