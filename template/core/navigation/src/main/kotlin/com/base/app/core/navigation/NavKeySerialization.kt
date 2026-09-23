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
 */
fun navKeys(builder: PolymorphicModuleBuilder<AppNavKey>.() -> Unit): SerializersModule =
    SerializersModule {
        polymorphic(AppNavKey::class, builderAction = builder)
    }

/** The [Json] the back stack is written with, assembled from every feature's [navKeys]. */
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
