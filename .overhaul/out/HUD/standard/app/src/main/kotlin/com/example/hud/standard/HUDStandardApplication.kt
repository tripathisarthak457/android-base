package com.example.hud.standard

import android.app.Application
import com.example.hud.standard.core.common.log.AppLogger
import com.example.hud.standard.core.common.log.Log
import com.example.hud.standard.core.common.log.LogLevel
import dagger.hilt.android.HiltAndroidApp
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
import com.example.hud.standard.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.launch
import com.example.hud.standard.core.flags.FeatureFlags
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class HUDStandardApplication : Application() {
    @Inject
    lateinit var appScope: ApplicationScope

    @Inject
    lateinit var featureFlags: FeatureFlags

    // Lazy: injecting the client itself would build it during start-up, before any image needs it.
    @Inject
    lateinit var okHttp: dagger.Lazy<OkHttpClient>

    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)

        appScope.launch { featureFlags.refresh() }

        // Images go through the app's own client, so they share its connections and its certificate
        // pins; Coil's default would build a client of its own that has neither. Its HTTP cache is
        // left out because Coil keeps the images in a disk cache of its own.
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { okHttp.get().newBuilder().cache(null).build() },
                        ),
                    )
                }
                .build()
        }
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
