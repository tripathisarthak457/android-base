package com.example.hud.fixture.feature.auth

import com.example.hud.fixture.core.navigation.AppNavigator
import com.example.hud.fixture.core.navigation.NavGraphEntry
import com.example.hud.fixture.core.navigation.navGraph
import com.example.hud.fixture.core.navigation.navKeys
import com.example.hud.fixture.feature.auth.api.AuthDestination
import com.example.hud.fixture.feature.auth.api.PasswordResetKey
import com.example.hud.fixture.feature.auth.api.SignInKey
import com.example.hud.fixture.feature.auth.api.SignUpKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * `resetTo` rather than `navigate`: after signing in, Back must not return to the sign-in screen.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AuthNavModule {

    @Provides
    @IntoSet
    fun authNavGraph(
        navigator: AppNavigator,
        destination: AuthDestination,
    ): NavGraphEntry = navGraph {
        entry<SignInKey> {
            SignInRoute(
                navigator = navigator,
                onSignedIn = { navigator.resetTo(destination.next) },
            )
        }
        entry<SignUpKey> {
            SignUpRoute(
                navigator = navigator,
                onSignedUp = { navigator.resetTo(destination.next) },
            )
        }
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
