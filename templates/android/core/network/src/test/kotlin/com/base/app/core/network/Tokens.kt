package com.base.app.core.network

import com.base.app.core.datastore.session.AuthTokens

internal fun tokens(access: String = "A1", refresh: String = "R1", owner: String = "ada") =
    AuthTokens(accessToken = access, refreshToken = refresh, owner = owner)
