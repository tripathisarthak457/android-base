package com.base.app.core.perfhud.frame

import android.app.Application
// <opt:analytics>
import com.base.app.core.analytics.AnalyticsTracker
import com.base.app.core.analytics.NoOpAnalyticsTracker
import com.base.app.core.analytics.jank.FrameJankRecorder
// </opt:analytics>

/** The feed as Hilt builds it: on the analytics recorder's frames when the project has one. */
// <opt:analytics>
internal fun frameFeed(
    application: Application,
    jankRecorder: FrameJankRecorder = jankRecorder(),
): FrameFeed = FrameFeed(application, jankRecorder)
// </opt:analytics>
// <opt-else:analytics>
//~ internal fun frameFeed(application: Application): FrameFeed = FrameFeed(application)
// </opt-else:analytics>
// <opt:analytics>

/**
 * A real recorder rather than a stand-in, since what is tested is which listener it hands frames
 * to. Its constructor is internal to :core:analytics, where Hilt calls it, and public on the JVM.
 */
internal fun jankRecorder(): FrameJankRecorder = FrameJankRecorder::class.java.getConstructor(
    AnalyticsTracker::class.java,
).newInstance(NoOpAnalyticsTracker())
// </opt:analytics>
