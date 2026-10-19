package com.base.app.feature.settings.language

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

// The app's own page in Settings, which lists Language once the app ships more than one.
@Composable
internal actual fun rememberLanguageSettings(): (() -> Unit)? = OpenAppSettings

private val OpenAppSettings: () -> Unit = {
    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}
