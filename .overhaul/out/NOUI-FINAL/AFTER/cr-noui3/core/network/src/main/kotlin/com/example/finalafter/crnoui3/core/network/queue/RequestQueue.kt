package com.example.finalafter.crnoui3.core.network.queue

import com.example.finalafter.crnoui3.core.network.model.NetworkRequest

/** Keeps a request that got no answer, to be sent again once the network is back. */
fun interface RequestQueue {
    suspend fun enqueue(request: NetworkRequest)
}
