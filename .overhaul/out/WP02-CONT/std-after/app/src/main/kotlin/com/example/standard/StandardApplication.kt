package com.example.standard

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.standard.core.common.coroutines.ApplicationScope
import com.example.standard.core.common.log.Log
import com.example.standard.core.flags.FeatureFlags
import com.example.standard.log.AndroidLogger
import dagger.hilt.android.HiltAndroidApp
// Shared by several optional blocks below. Repeating it inside each would duplicate it when two
// are on, which is what the `a|b` marker form avoids.
import javax.inject.Inject
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class StandardApplication : Application() {
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
