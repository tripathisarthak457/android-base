package com.base.app.feature.sample

import com.base.app.core.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations.
 *
 * Owned entirely by the feature: nothing in `:core:navigation` or `:app` names them, and adding a
 * screen here touches no file outside this module. That is the whole point of the decentralised
 * registry — see [com.base.app.core.navigation.NavGraphEntry].
 *
 * Note that [SampleDetailKey] carries an id, not a `SampleItem`. A key is serialised into the
 * saved-state bundle, so a key holding a whole model is a key that both bloats the bundle and
 * goes stale the moment the app is backgrounded. The detail screen fetches by id.
 */
@Serializable
data object SampleListKey : AppNavKey

@Serializable
data class SampleDetailKey(val itemId: Int) : AppNavKey
