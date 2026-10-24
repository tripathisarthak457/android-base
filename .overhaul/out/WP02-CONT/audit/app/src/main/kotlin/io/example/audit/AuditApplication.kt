package io.example.audit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.example.audit.core.common.log.Log
import io.example.audit.log.AndroidLogger

/** The composition root. Deliberately thin. */
@HiltAndroidApp
class AuditApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.logger = AndroidLogger(verbose = BuildConfig.DEBUG)
    }
}
