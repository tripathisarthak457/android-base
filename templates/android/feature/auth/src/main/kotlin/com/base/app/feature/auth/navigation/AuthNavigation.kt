package com.base.app.feature.auth.navigation

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.feature.auth.PasswordResetRoute
import com.base.app.feature.auth.SignInRoute
import com.base.app.feature.auth.SignUpRoute
import com.base.app.feature.auth.api.PasswordResetKey
import com.base.app.feature.auth.api.SignInKey
import com.base.app.feature.auth.api.SignUpKey
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
