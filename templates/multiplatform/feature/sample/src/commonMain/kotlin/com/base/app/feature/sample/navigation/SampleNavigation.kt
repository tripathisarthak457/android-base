package com.base.app.feature.sample.navigation

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.NavPane
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.sample.api.SampleDetailKey
import com.base.app.feature.sample.api.SampleListKey
import com.base.app.feature.sample.detail.SampleDetailRoute
import com.base.app.feature.sample.detail.SampleDetailViewModel
import com.base.app.feature.sample.list.SampleListRoute
import com.base.app.feature.sample.list.SampleListViewModel
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * How this feature joins the app: its ViewModels, what to render for each key, and how to
 * serialise the keys so the back stack survives process death. The graph and the keys are
 * contributed under the feature's own qualifier; the app collects every contribution by type.
 */
val sampleModule = module {
    viewModelOf(::SampleListViewModel)
    viewModel { (itemId: Int) -> SampleDetailViewModel(get(), itemId) }
    single(named<SampleListKey>()) { sampleNavGraph(get()) }
    single(named<SampleListKey>()) { sampleNavKeys() }
}

private fun sampleNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
    entry<SampleListKey>(pane = NavPane.List) { SampleListRoute(navigator = navigator) }
    entry<SampleDetailKey>(pane = NavPane.Detail) { key ->
        SampleDetailRoute(itemId = key.itemId, navigator = navigator)
    }
}

private fun sampleNavKeys(): SerializersModule = navKeys {
    subclass(SampleListKey::class, SampleListKey.serializer())
    subclass(SampleDetailKey::class, SampleDetailKey.serializer())
}
