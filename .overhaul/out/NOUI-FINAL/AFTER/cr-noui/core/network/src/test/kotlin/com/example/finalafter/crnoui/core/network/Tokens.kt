package com.example.finalafter.crnoui.core.network

import com.example.finalafter.crnoui.core.datastore.session.AuthTokens

internal fun tokens(access: String = "A1", refresh: String = "R1", owner: String = "ada") =
    AuthTokens(accessToken = access, refreshToken = refresh, owner = owner)
