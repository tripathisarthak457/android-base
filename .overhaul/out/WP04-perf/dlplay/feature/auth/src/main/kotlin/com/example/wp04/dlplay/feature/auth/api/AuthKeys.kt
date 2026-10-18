package com.example.wp04.dlplay.feature.auth.api

import com.example.wp04.dlplay.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SignInKey : AppNavKey

@Serializable
data object SignUpKey : AppNavKey

@Serializable
data object PasswordResetKey : AppNavKey
