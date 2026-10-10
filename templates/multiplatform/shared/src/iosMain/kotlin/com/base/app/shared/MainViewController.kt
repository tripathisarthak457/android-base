package com.base.app.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.base.app.core.common.log.Log
import com.base.app.shared.di.initKoin
import com.base.app.shared.log.ConsoleLogger
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

/**
 * The iOS app's root view controller, which the SwiftUI app creates once for its window. iOS has no
 * `Application` that runs earlier, so the dependency graph starts here, on the first call.
 */
@Suppress("FunctionName") // Swift calls it as the type it returns.
fun MainViewController(environment: AppEnvironment): UIViewController {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        if (environment.isDebug) Log.logger = ConsoleLogger
        initKoin(environment)
    }
    return ComposeUIViewController { App() }
}
