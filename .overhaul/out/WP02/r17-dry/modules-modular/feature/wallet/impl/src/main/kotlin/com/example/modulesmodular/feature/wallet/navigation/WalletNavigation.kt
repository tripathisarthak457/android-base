package com.example.modulesmodular.feature.wallet.navigation

import com.example.modulesmodular.core.navigation.AppNavigator
import com.example.modulesmodular.core.navigation.NavGraphEntry
import com.example.modulesmodular.core.navigation.NavPane
import com.example.modulesmodular.core.navigation.navGraph
import com.example.modulesmodular.core.navigation.navKeys
import com.example.modulesmodular.feature.wallet.api.WalletDetailKey
import com.example.modulesmodular.feature.wallet.api.WalletListKey
import com.example.modulesmodular.feature.wallet.detail.WalletDetailRoute
import com.example.modulesmodular.feature.wallet.list.WalletListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object WalletNavigation {

    @Provides
    @IntoSet
    fun walletNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<WalletListKey>(pane = NavPane.List) {
            WalletListRoute(navigator = navigator)
        }
        entry<WalletDetailKey>(pane = NavPane.Detail) { key ->
            WalletDetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun walletNavKeys(): SerializersModule = navKeys {
        subclass(WalletListKey::class, WalletListKey.serializer())
        subclass(WalletDetailKey::class, WalletDetailKey.serializer())
    }
}
