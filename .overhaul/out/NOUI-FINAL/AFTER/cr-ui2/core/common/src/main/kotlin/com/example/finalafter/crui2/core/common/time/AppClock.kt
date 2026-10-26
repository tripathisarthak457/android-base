package com.example.finalafter.crui2.core.common.time

import kotlin.time.Clock
import kotlin.time.Instant

/** The wall clock, injected so a test sets the time instead of waiting for it. */
fun interface AppClock {

    fun now(): Instant

    companion object {
        val System: AppClock = AppClock { Clock.System.now() }
    }
}
