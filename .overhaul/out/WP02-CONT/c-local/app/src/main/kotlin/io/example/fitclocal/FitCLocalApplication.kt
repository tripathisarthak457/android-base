package io.example.fitclocal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.example.fitclocal.core.common.log.Log
import io.example.fitclocal.log.AndroidLogger

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class FitCLocalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)
    }
}
