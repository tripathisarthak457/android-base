package com.example.videoalone

import android.app.Application
import coil3.SingletonImageLoader
import com.example.videoalone.core.common.log.AppLogger
import com.example.videoalone.core.common.log.Log
import com.example.videoalone.core.common.log.LogLevel
import com.example.videoalone.core.image.MediaCallFactory
import com.example.videoalone.core.image.appImageLoader
import com.example.videoalone.core.video.AppVideoSetup
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Call

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class VideoAloneApplication : Application() {
    // Images and video share one client, built on their first request.
    private val mediaClient: Call.Factory = MediaCallFactory()

    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)

        SingletonImageLoader.setSafe { context -> appImageLoader(context, callFactory = { mediaClient }) }
        // Nothing of Media3 loads here: the client is kept, and the first feed that opens reads it.
        AppVideoSetup.install(callFactory = { mediaClient })
    }
}

/**
 * Logcat behind [Log]. Library modules have no BuildConfig of their own, so [verbose] is how the one
 * module that has one tells them what kind of build this is: below errors, release builds stay quiet.
 */
private class AndroidLogger(
    private val verbose: Boolean,
    private val onError: ((message: String) -> Unit)? = null,
) : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.Debug -> if (verbose) android.util.Log.d(tag, message, throwable)
            LogLevel.Info -> if (verbose) android.util.Log.i(tag, message, throwable)
            LogLevel.Warn -> if (verbose) android.util.Log.w(tag, message, throwable)
            LogLevel.Error -> {
                android.util.Log.e(tag, message, throwable)
                onError?.invoke(if (throwable == null) "$tag: $message" else "$tag: $message: $throwable")
            }
        }
    }
}
