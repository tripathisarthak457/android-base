// <opt:sample>
package com.base.app.deeplink

import com.base.app.core.navigation.DeepLinkRoute
import com.base.app.feature.sample.SampleDetailKey
import com.base.app.feature.sample.SampleListKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** `baseapp://items` opens the list, `baseapp://items/12` the item. */
@Module
@InstallIn(SingletonComponent::class)
object SampleLinkModule {

    @Provides
    @IntoSet
    fun sampleLinks(): DeepLinkRoute = DeepLinkRoute { link ->
        if (link.segments.first() != "items") return@DeepLinkRoute null
        link.segments.getOrNull(1)?.toIntOrNull()?.let(::SampleDetailKey) ?: SampleListKey
    }
}
// </opt:sample>
