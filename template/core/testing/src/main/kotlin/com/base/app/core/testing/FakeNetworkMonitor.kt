package com.base.app.core.testing

import com.base.app.core.common.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [NetworkMonitor] a test drives directly.
 *
 * "The user went offline mid-request" is a state every repository has to handle and none can
 * produce against the real implementation without an instrumented test and a device with its
 * radio off. Here it is one assignment.
 */
class FakeNetworkMonitor(initiallyOnline: Boolean = true) : NetworkMonitor {

    private val online = MutableStateFlow(initiallyOnline)

    override val isOnline: Flow<Boolean> = online

    fun setOnline(value: Boolean) {
        online.value = value
    }
}
