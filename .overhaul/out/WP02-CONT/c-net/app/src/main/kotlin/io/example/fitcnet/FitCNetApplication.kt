package io.example.fitcnet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.example.fitcnet.core.common.log.Log
import io.example.fitcnet.log.AndroidLogger

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class FitCNetApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)
    }
}
