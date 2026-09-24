package com.base.app.core.ui.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Opens web pages in a Custom Tab coloured like the app, instead of throwing the user out to the
 * browser. For terms, privacy, help articles — pages that belong to the app but live on the web.
 *
 * ```
 * val openInApp = rememberInAppBrowser()
 * AppListItem(title = "Privacy policy", onClick = { openInApp("https://…") })
 * ```
 */
@Composable
fun rememberInAppBrowser(): (String) -> Unit {
    val context = LocalContext.current
    val toolbar = AppTheme.colors.surface.toArgb()
    val isLight = AppTheme.colors.isLight
    return remember(context, toolbar, isLight) {
        { url -> context.openInAppBrowser(url, toolbar, isLight) }
    }
}

/**
 * Only http and https are opened. URLs here often come from server data, and a `file:`, `intent:`
 * or custom-scheme link handed to ACTION_VIEW can reach other apps' components or local files.
 */
fun Context.openInAppBrowser(url: String, toolbarColor: Int, isLight: Boolean) {
    val uri = Uri.parse(url)
    if (uri.scheme?.lowercase() !in WEB_SCHEMES || uri.host.isNullOrBlank()) return
    val colours = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(toolbarColor)
        .setNavigationBarColor(toolbarColor)
        .build()
    val intent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .setColorScheme(if (isLight) CustomTabsIntent.COLOR_SCHEME_LIGHT else CustomTabsIntent.COLOR_SCHEME_DARK)
        .setDefaultColorSchemeParams(colours)
        .build()
    try {
        intent.launchUrl(this, uri)
    } catch (_: ActivityNotFoundException) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

private val WEB_SCHEMES = setOf("http", "https")
