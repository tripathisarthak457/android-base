package com.example.finalafter.crui.core.testing

import com.example.finalafter.crui.core.common.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [NetworkMonitor] a test drives directly. */
class FakeNetworkMonitor(initiallyOnline: Boolean = true) : NetworkMonitor {

    private val online = MutableStateFlow(initiallyOnline)

    override val isOnline: Flow<Boolean> = online

    fun setOnline(value: Boolean) {
        online.value = value
    }
}
