package com.base.app.feature.auth

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.auth.api.AuthDestination
import com.base.app.feature.auth.api.PasswordResetKey
import com.base.app.feature.auth.api.SignInKey
import com.base.app.feature.auth.api.SignUpKey
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
