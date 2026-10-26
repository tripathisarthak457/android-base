package com.example.finalafter.crnoui.core.perfhud.frame

import android.app.Application
import com.example.finalafter.crnoui.core.analytics.AnalyticsTracker
import com.example.finalafter.crnoui.core.analytics.NoOpAnalyticsTracker
import com.example.finalafter.crnoui.core.analytics.jank.FrameJankRecorder

/** The feed as Hilt builds it: on the analytics recorder's frames when the project has one. */
internal fun frameFeed(
    application: Application,
    jankRecorder: FrameJankRecorder = jankRecorder(),
): FrameFeed = FrameFeed(
    application,
    jankRecorder,
)

/**
 * A real recorder rather than a stand-in, since what is tested is which listener it hands frames
 * to. Its constructor is internal to :core:analytics, where Hilt calls it, and public on the JVM.
 */
internal fun jankRecorder(): FrameJankRecorder =
    FrameJankRecorder::class.java.getConstructor(AnalyticsTracker::class.java).newInstance(NoOpAnalyticsTracker())
