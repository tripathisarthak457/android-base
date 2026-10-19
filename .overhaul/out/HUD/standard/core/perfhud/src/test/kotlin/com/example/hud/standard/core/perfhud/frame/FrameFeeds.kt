package com.example.hud.standard.core.perfhud.frame

import android.app.Application

/** The feed as Hilt builds it: on the analytics recorder's frames when the project has one. */
internal fun frameFeed(
    application: Application,
): FrameFeed = FrameFeed(
    application,
)
