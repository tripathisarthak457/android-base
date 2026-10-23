package io.example.fitcnetdb

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.example.fitcnetdb.core.common.log.Log
import io.example.fitcnetdb.log.AndroidLogger

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class FitCNetdbApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)
    }
}
