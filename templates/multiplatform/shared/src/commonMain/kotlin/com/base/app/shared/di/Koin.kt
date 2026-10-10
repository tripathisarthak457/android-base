package com.base.app.shared.di

import com.base.app.shared.AppEnvironment
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

/**
 * Starts the dependency graph. Called once per process: from `Application.onCreate` on Android,
 * with [platform] adding the application context, and from `MainViewController` on iOS.
 */
fun initKoin(environment: AppEnvironment, platform: KoinApplication.() -> Unit = {}) {
    startKoin {
        platform()
        modules(appModules(environment))
    }
}
