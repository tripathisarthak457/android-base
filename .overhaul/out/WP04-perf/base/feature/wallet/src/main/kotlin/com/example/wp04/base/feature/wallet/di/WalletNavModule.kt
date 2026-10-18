package com.example.wp04.base.feature.wallet.di

import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.navigation.NavGraphEntry
import com.example.wp04.base.core.navigation.NavPane
import com.example.wp04.base.core.navigation.navGraph
import com.example.wp04.base.core.navigation.navKeys
import com.example.wp04.base.feature.wallet.WalletDetailKey
import com.example.wp04.base.feature.wallet.WalletListKey
import com.example.wp04.base.feature.wallet.WalletListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * Registers this feature's screens and key serializers. Without the serializers the back stack
 * does not survive process death.
 *
 * `WalletDetailKey` has no screen yet: add `entry<WalletDetailKey>(pane = NavPane.Detail) { … }`
 * with it, and on a wide window it opens beside the list.
 */
@Module
@InstallIn(SingletonComponent::class)
object WalletNavModule {
    @Provides
    @IntoSet
    fun walletNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<WalletListKey>(pane = NavPane.List) { WalletListRoute(navigator = navigator) }
    }

    @Provides
    @IntoSet
    fun walletNavKeys(): SerializersModule = navKeys {
        subclass(WalletListKey::class, WalletListKey.serializer())
        subclass(WalletDetailKey::class, WalletDetailKey.serializer())
    }
}
