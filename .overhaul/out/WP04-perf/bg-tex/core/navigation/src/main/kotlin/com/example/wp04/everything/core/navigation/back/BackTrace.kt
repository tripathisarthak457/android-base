package com.example.wp04.everything.core.navigation.back

import android.os.Build
import android.os.Trace
import kotlin.math.roundToLong

/** Experiment-only trace counters (research/back-google/DESIGN.md); never committed. */
internal object BackTrace {
    fun write(progress: Float) = counter("BackCard.write", progress)

    fun apply(pose: Float, alpha: Float) {
        counter("BackCard.apply", pose)
        counter("BackCard.alpha", alpha)
    }

    fun phase(value: Int) = counter("BackCard.phase", value / 1000f)

    fun warm(on: Boolean) = counter("BackCard.warm", if (on) 0.001f else 0f)

    private fun counter(name: String, value: Float) {
        if (Build.VERSION.SDK_INT >= 29) Trace.setCounter(name, (value * 1000f).roundToLong())
    }
}
