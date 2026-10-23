package com.base.app.catalog.media.photos

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock

/**
 * Collects garbage when the perf harness asks, so a memory reading counts what the app holds rather
 * than when ART last ran: an evicted bitmap's pixels stay in the graphics memory until its object is
 * collected. Registered only in perf-log runs, and only for senders holding `DUMP` (the shell and the
 * system); it does nothing but collect. Returns the call that unregisters it.
 */
internal fun registerPerfGc(activity: Activity): () -> Unit {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val runtime = Runtime.getRuntime()
            runtime.gc()
            System.runFinalization()
            runtime.gc()
            MediaPerfLog.log("gc done t=${SystemClock.elapsedRealtime()}")
        }
    }
    activity.registerReceiver(
        receiver,
        IntentFilter("${activity.packageName}.PERF_GC"),
        Manifest.permission.DUMP,
        null,
        Context.RECEIVER_EXPORTED,
    )
    return { activity.unregisterReceiver(receiver) }
}
