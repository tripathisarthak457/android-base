package com.example.hud.standard.core.perfhud.frame

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's frames, while [start]ed, from the window of the activity in front; nothing at all
 * otherwise, so an overlay that is not recording puts no listener on any window.
 *
 * While it runs it also keeps the current route in that window's `PerformanceMetricsState`, which
 * is how JankStats tells each frame which screen it belongs to, and it takes the route out again
 * on [stop], so the state exists only while something records. All calls on the main thread.
 */
@Singleton
internal class FrameFeed @Inject constructor(
    @ApplicationContext context: Context,
) : Application.ActivityLifecycleCallbacks {

    private var listener: JankStats.OnFrameListener? = null
    private var resumed: WeakReference<Activity>? = null
    private var route: String? = null
    private var jankStats: JankStats? = null

    /** Given the intent of each activity started by a fresh launch, rather than restored. */
    var onLaunch: ((Intent) -> Unit)? = null

    init {
        (context as Application).registerActivityLifecycleCallbacks(this)
    }

    fun start(listener: JankStats.OnFrameListener) {
        this.listener = listener
        resumed?.get()?.let(::attach)
    }

    fun stop() {
        listener = null
        stopFrames()
        resumed?.get()?.let { metricsState(it.window)?.removeState(FrameIntake.ROUTE_STATE) }
    }

    /** Labels the frames that follow with [name]. */
    fun setRoute(name: String) {
        route = name
        if (listener != null) resumed?.get()?.let { putRoute(it.window) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) onLaunch?.invoke(activity.intent)
    }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
        if (listener != null) attach(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() !== activity) return
        resumed = null
        if (listener != null) stopFrames()
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun attach(activity: Activity) {
        val listener = listener ?: return
        jankStats = JankStats.createAndTrack(activity.window, listener)
        putRoute(activity.window)
    }

    private fun stopFrames() {
        jankStats?.isTrackingEnabled = false
        jankStats = null
    }

    private fun putRoute(window: Window) {
        val name = route ?: return
        metricsState(window)?.putState(FrameIntake.ROUTE_STATE, name)
    }

    /** Null until a JankStats on the window has created it. */
    private fun metricsState(window: Window): PerformanceMetricsState? =
        PerformanceMetricsState.getHolderForHierarchy(window.decorView).state
}
