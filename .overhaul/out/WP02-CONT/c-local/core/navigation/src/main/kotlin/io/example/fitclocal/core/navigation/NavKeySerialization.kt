package io.example.fitclocal.core.navigation

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.example.fitclocal.core.common.navigation.AppNavKey
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

/**
 * Declares a feature's navigation keys so the back stack can be restored after process death.
 *
 * ```
 * @Provides
 * @IntoSet
 * fun sampleNavKeys(): SerializersModule = navKeys {
 *     subclass(SampleListKey::class, SampleListKey.serializer())
 *     subclass(SampleDetailKey::class, SampleDetailKey.serializer())
 * }
 * ```
 */
fun navKeys(builder: PolymorphicModuleBuilder<AppNavKey>.() -> Unit): SerializersModule =
    SerializersModule {
        polymorphic(AppNavKey::class, builderAction = builder)
    }

/** The [Json] the back stack is written with, assembled from every feature's [navKeys]. */
@ActivityRetainedScoped
class NavKeySerialization @Inject constructor(
    modules: Set<@JvmSuppressWildcards SerializersModule>,
) {
    val json: Json = Json {
        serializersModule = modules.fold(EmptySerializersModule()) { accumulated, module ->
            accumulated + module
        }
        ignoreUnknownKeys = true
        encodeDefaults = true
        // Not a name a key's own property could have: a key with a `key` property made the whole
        // stack fail to encode, and the app came back from the background without it.
        classDiscriminator = "#type"
    }
}
