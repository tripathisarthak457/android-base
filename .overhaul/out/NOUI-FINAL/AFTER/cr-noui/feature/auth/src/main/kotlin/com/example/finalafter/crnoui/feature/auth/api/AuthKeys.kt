package com.example.finalafter.crnoui.feature.auth.api

import com.example.finalafter.crnoui.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object SignInKey : AppNavKey

@Serializable
data object SignUpKey : AppNavKey

@Serializable
data object PasswordResetKey : AppNavKey
