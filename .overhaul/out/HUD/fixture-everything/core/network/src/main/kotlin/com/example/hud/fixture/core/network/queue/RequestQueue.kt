package com.example.hud.fixture.core.network.queue

import com.example.hud.fixture.core.network.model.NetworkRequest

/** Keeps a request that got no answer, to be sent again once the network is back. */
fun interface RequestQueue {
    suspend fun enqueue(request: NetworkRequest)
}
