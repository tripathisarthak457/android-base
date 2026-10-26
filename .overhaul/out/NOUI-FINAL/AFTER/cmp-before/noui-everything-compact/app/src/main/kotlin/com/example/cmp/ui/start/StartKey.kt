package com.example.cmp.ui.start

import com.example.cmp.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/** The screen the app opens on. A data object, so it is the same key in every process. */
@Serializable
data object StartKey : AppNavKey
