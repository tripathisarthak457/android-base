package com.example.finalafter.crui.core.perfhud.frame

import android.hardware.display.DisplayManager
import android.view.Display

/**
 * The default display's refresh interval, kept current while recording. A phone with an adaptive
 * rate moves between 60 and 120 Hz as it sees fit, and the budget a frame is drawn against moves
 * with it. Updated on the main thread, read on JankStats'.
 */
internal class RefreshRate(private val displays: DisplayManager) {

    @Volatile
    var intervalNanos: Long = SIXTY_HZ_NANOS
        private set

    /** The rates the display offers, highest first, as of the last [start]. */
    var supportedHz: List<Float> = emptyList()
        private set

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) update()
        }

        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    fun start() {
        update()
        val display = displays.getDisplay(Display.DEFAULT_DISPLAY)
        supportedHz = display?.supportedModes?.map { it.refreshRate }?.distinct()?.sortedDescending().orEmpty()
        // A null handler delivers on the calling thread's looper: the main one.
        displays.registerDisplayListener(listener, null)
    }

    fun stop() {
        displays.unregisterDisplayListener(listener)
    }

    private fun update() {
        val rate = displays.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: return
        if (rate > 0f) intervalNanos = (NANOS_PER_SECOND / rate).toLong()
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000f
        const val SIXTY_HZ_NANOS = 16_666_667L
    }
}
