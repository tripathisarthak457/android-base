package com.base.app.core.navigation

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import javax.inject.Inject
import javax.inject.Singleton

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
 *
 * Two registries — this and [navGraph] — rather than one, because they answer different questions
 * and are needed at different times: the graph is read during composition to find a screen, and
 * this is read off the main thread to encode a bundle. Merging them would mean the serialisation
 * layer holding composables.
 */
fun navKeys(builder: PolymorphicModuleBuilder<AppNavKey>.() -> Unit): SerializersModule =
    SerializersModule {
        polymorphic(AppNavKey::class, builderAction = builder)
    }

/**
 * The [Json] the back stack is written with, assembled from every feature's [navKeys].
 *
 * `ignoreUnknownKeys` matters more here than anywhere else in the app: the bundle being restored
 * was written by the *previous version* of the app, before the update that is now running. A key
 * that has since gained a field must still decode, or every user loses their place on the first
 * launch after every release.
 */
@Singleton
class NavKeySerialization @Inject constructor(
    modules: Set<@JvmSuppressWildcards SerializersModule>,
) {
    val json: Json = Json {
        serializersModule = modules.fold(EmptySerializersModule()) { accumulated, module ->
            accumulated + module
        }
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "key"
    }
}
