package com.base.app.core.navigation

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.plus
import javax.inject.Inject

/**
 * The [Json] the back stacks are written with: every key that a feature's [NavGraphEntry] registers
 * with `entry<K>`, and only those, survives the process being killed in the background.
 */
@ActivityRetainedScoped
class NavKeySerialization @Inject constructor(
    graphs: Set<@JvmSuppressWildcards NavGraphEntry>,
) {
    val json: Json = Json {
        serializersModule = graphs.fold(EmptySerializersModule()) { accumulated, graph -> accumulated + graph.keys }
        ignoreUnknownKeys = true
        encodeDefaults = true
        // Not a name a key's own property could have: a key with a `key` property made the whole
        // stack fail to encode, and the app came back from the background without it.
        classDiscriminator = "#type"
    }
}
