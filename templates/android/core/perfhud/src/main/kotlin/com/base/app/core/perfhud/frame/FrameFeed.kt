package com.base.app.core.perfhud.frame

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.metrics.performance.JankStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
// <opt:analytics>
import com.base.app.core.analytics.jank.FrameJankRecorder
// </opt:analytics>

/**
 * The app's frames, while [start]ed, from the window of the activity in front; nothing at all
 * otherwise, so an overlay that is not recording puts no listener on any window.
 *
 * It only listens: nothing is put on the window, whose `PerformanceMetricsState` other JankStats
 * read too (see [ScreenTimeline]). All calls on the main thread.
 */
@Singleton
internal class FrameFeed @Inject constructor(
    @ApplicationContext context: Context,
    // <opt:analytics>
    private val jankRecorder: FrameJankRecorder,
    // </opt:analytics>
) : Application.ActivityLifecycleCallbacks {

    private var listener: JankStats.OnFrameListener? = null
    private var resumed: WeakReference<Activity>? = null
    private var started = 0
    private val _foreground = MutableStateFlow(false)
    // <opt:!analytics>    private var jankStats: JankStats? = null

    /** Given the intent of each activity started by a fresh launch, rather than restored. */
    var onLaunch: ((Intent) -> Unit)? = null

    /** Whether any of the app's activities is started: what the app shows can be seen. */
    val foreground: StateFlow<Boolean> = _foreground.asStateFlow()

    init {
        (context as Application).registerActivityLifecycleCallbacks(this)
    }

    fun start(listener: JankStats.OnFrameListener) {
        this.listener = listener
        if (resumed?.get() != null) attach()
    }

    fun stop() {
        listener = null
        stopFrames()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) onLaunch?.invoke(activity.intent)
    }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
        if (listener != null) attach()
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() !== activity) return
        resumed = null
        if (listener != null) stopFrames()
    }

    override fun onActivityStarted(activity: Activity) {
        started++
        _foreground.value = true
    }

    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
        _foreground.value = started > 0
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun attach() {
        val listener = listener ?: return
        // <opt:analytics>
        // The analytics recorder's JankStats already watches the window: sharing its frames keeps
        // one listener on the window whether or not the overlay records.
        jankRecorder.frameListener = listener
        // </opt:analytics>
        // <opt-else:analytics>
        //~ // Two of the app's activities can be resumed at once (split screen, Android 10's
        //~ // multi-resume), and the first one's pause then finds another in front and leaves its
        //~ // JankStats alone: one window is watched at a time, the one resumed last.
        //~ val window = resumed?.get()?.window ?: return
        //~ jankStats?.isTrackingEnabled = false
        //~ jankStats = JankStats.createAndTrack(window, listener)
        // </opt-else:analytics>
    }

    private fun stopFrames() {
        // <opt:analytics>
        jankRecorder.frameListener = null
        // </opt:analytics>
        // <opt-else:analytics>
        //~ jankStats?.isTrackingEnabled = false
        //~ jankStats = null
        // </opt-else:analytics>
    }
}
