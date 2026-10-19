package com.base.app

import android.app.Application
import android.util.Log as AndroidLog
import com.base.app.core.common.log.AppLogger
import com.base.app.core.common.log.Log
import com.base.app.core.common.log.LogLevel
import com.base.app.shared.AppEnvironment
import com.base.app.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

/** Starts the dependency graph once per process, before any Activity asks for it. */
class BaseAppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Log.logger = Logcat
        initKoin(environment()) { androidContext(this@BaseAppApplication) }
    }

    private fun environment() = AppEnvironment(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        name = BuildConfig.ENVIRONMENT,
        isDebug = BuildConfig.DEBUG,
        versionName = BuildConfig.VERSION_NAME,
    )
}

private object Logcat : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.Debug -> AndroidLog.d(tag, message, throwable)
            LogLevel.Info -> AndroidLog.i(tag, message, throwable)
            LogLevel.Warn -> AndroidLog.w(tag, message, throwable)
            LogLevel.Error -> AndroidLog.e(tag, message, throwable)
        }
    }
}
