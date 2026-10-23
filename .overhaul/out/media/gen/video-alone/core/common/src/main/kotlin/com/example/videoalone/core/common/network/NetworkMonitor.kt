package com.example.videoalone.core.common.network

import kotlinx.coroutines.flow.Flow

/** Whether the device currently has usable connectivity. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
