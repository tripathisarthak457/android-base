package com.example.finalafter.crui2.core.designsystem.foundation.glass

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
// allow(designsystem-imports): battery saver's broadcast arrives as an Intent; only glass reads it
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What the device says about itself that decides the glass tier, as snapshot state: battery
 * saver, the thermal status, the system's high-contrast text and whether it is a low-RAM device.
 * Listening runs between [start] and [stop], on the main thread, and costs nothing per frame.
 */
@Stable
internal class DeviceGlassSignals(private val context: Context) {
    private val power = context.getSystemService(PowerManager::class.java)
    private val accessibility = context.getSystemService(AccessibilityManager::class.java)

    val lowRamDevice: Boolean = context.getSystemService(ActivityManager::class.java).isLowRamDevice

    var powerSave by mutableStateOf(power.isPowerSaveMode)
        private set

    var thermalStatus by mutableIntStateOf(currentThermalStatus())
        private set

    var highContrastText by mutableStateOf(currentHighContrastText())
        private set

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            powerSave = power.isPowerSaveMode
        }
    }
    private var stopListening: (() -> Unit)? = null

    fun start() {
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // A protected broadcast that only the system sends, so the receiver need not be
            // exported.
            context.registerReceiver(powerSaveReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(powerSaveReceiver, filter)
        }
        val stopThermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listenToThermalStatus()
        } else {
            null
        }
        val stopContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            listenToHighContrastText()
        } else {
            null
        }
        stopListening = {
            context.unregisterReceiver(powerSaveReceiver)
            stopThermal?.invoke()
            stopContrast?.invoke()
        }
        // Settings may have changed while nothing was listening.
        powerSave = power.isPowerSaveMode
        thermalStatus = currentThermalStatus()
        highContrastText = currentHighContrastText()
    }

    fun stop() {
        stopListening?.invoke()
        stopListening = null
    }

    private fun currentThermalStatus(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            power.currentThermalStatus
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

    private fun currentHighContrastText(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            accessibility.isHighContrastTextEnabled

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listenToThermalStatus(): () -> Unit {
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            thermalStatus = status
        }
        power.addThermalStatusListener(listener)
        return { power.removeThermalStatusListener(listener) }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun listenToHighContrastText(): () -> Unit {
        val listener = AccessibilityManager.HighContrastTextStateChangeListener { enabled ->
            highContrastText = enabled
        }
        accessibility.addHighContrastTextStateChangeListener(context.mainExecutor, listener)
        return { accessibility.removeHighContrastTextStateChangeListener(listener) }
    }
}
