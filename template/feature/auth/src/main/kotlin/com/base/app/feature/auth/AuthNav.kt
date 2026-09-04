package com.base.app.feature.auth

import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

@Serializable
data object SignInKey : AppNavKey

@Serializable
data object SignUpKey : AppNavKey

@Serializable
data object PasswordResetKey : AppNavKey

/**
 * Where the app goes once the user is authenticated.
 *
 * Supplied by the application module for the same reason as onboarding's: this feature must not
 * name the screen another feature owns.
 */
data class AuthDestination(val next: AppNavKey)

/**
 * `resetTo` rather than `navigate`: after signing in, Back must not return to the sign-in screen.
 *
 * Both sign-in and sign-up land on the same destination through the same call, so there is no
 * second path to keep in step — which is how "sign up works but back goes to the login form"
 * gets into a release.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthNavModule {

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
