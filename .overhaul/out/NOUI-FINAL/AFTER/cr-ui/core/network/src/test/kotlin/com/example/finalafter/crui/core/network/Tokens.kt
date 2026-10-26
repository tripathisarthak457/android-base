package com.example.finalafter.crui.core.network

import com.example.finalafter.crui.core.datastore.session.AuthTokens

internal fun tokens(access: String = "A1", refresh: String = "R1", owner: String = "ada") =
    AuthTokens(accessToken = access, refreshToken = refresh, owner = owner)
