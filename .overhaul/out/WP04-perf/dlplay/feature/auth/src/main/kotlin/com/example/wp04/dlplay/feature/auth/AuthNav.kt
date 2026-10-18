package com.example.wp04.dlplay.feature.auth

import com.example.wp04.dlplay.core.navigation.AppNavigator
import com.example.wp04.dlplay.core.navigation.NavGraphEntry
import com.example.wp04.dlplay.core.navigation.navGraph
import com.example.wp04.dlplay.core.navigation.navKeys
import com.example.wp04.dlplay.feature.auth.api.PasswordResetKey
import com.example.wp04.dlplay.feature.auth.api.SignInKey
import com.example.wp04.dlplay.feature.auth.api.SignUpKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * Nothing here navigates on success: the saved session swaps the app to the signed-in shell, which
 * starts on its own stack, so Back can never return to these screens.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AuthNavModule {

    @Provides
    @IntoSet
    fun authNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<SignInKey> { SignInRoute(navigator = navigator) }
        entry<SignUpKey> { SignUpRoute(navigator = navigator) }
        entry<PasswordResetKey> { PasswordResetRoute(navigator = navigator) }
    }

    @Provides
    @IntoSet
    fun authNavKeys(): SerializersModule = navKeys {
        subclass(SignInKey::class, SignInKey.serializer())
        subclass(SignUpKey::class, SignUpKey.serializer())
        subclass(PasswordResetKey::class, PasswordResetKey.serializer())
    }
}
