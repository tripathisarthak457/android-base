package com.example.cmp.core.network.queue

import com.example.cmp.core.network.model.NetworkRequest

/** Keeps a request that got no answer, to be sent again once the network is back. */
fun interface RequestQueue {
    suspend fun enqueue(request: NetworkRequest)
}
