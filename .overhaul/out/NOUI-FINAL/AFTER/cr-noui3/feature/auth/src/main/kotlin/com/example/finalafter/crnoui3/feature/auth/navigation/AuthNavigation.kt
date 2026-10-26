package com.example.finalafter.crnoui3.feature.auth.navigation

import com.example.finalafter.crnoui3.core.navigation.AppNavigator
import com.example.finalafter.crnoui3.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui3.core.navigation.navGraph
import com.example.finalafter.crnoui3.feature.auth.api.PasswordResetKey
import com.example.finalafter.crnoui3.feature.auth.api.SignInKey
import com.example.finalafter.crnoui3.feature.auth.api.SignUpKey
import com.example.finalafter.crnoui3.feature.auth.passwordreset.PasswordResetRoute
import com.example.finalafter.crnoui3.feature.auth.signin.SignInRoute
import com.example.finalafter.crnoui3.feature.auth.signup.SignUpRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

/**
 * Nothing here navigates on success: the saved session swaps the app to the signed-in shell, which
 * starts on its own stack, so Back can never return to these screens.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AuthNavigation {

    @Provides
    @IntoSet
    fun authNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<SignInKey> { SignInRoute(navigator = navigator) }
        entry<SignUpKey> { SignUpRoute(navigator = navigator) }
        entry<PasswordResetKey> { PasswordResetRoute(navigator = navigator) }
    }
}
