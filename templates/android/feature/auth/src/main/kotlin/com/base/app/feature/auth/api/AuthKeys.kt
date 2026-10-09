package com.base.app.feature.auth.api

import com.base.app.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SignInKey : AppNavKey

@Serializable
data object SignUpKey : AppNavKey

@Serializable
data object PasswordResetKey : AppNavKey

/** Where the app goes once the user is authenticated. Supplied by `:app`, which knows the graph. */
data class AuthDestination(val next: AppNavKey)
