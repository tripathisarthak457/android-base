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
 * Opens web pages in a Custom Tab coloured like the app, instead of throwing the user out to
 * the browser. For terms, privacy, help articles — pages that belong to the app but live on the
 * web.
 *
 * ```
 * val openInApp = rememberInAppBrowser()
 * AppListItem(title = "Privacy policy", onClick = { openInApp("https://…") })
 * ```
 *
 * Falls back to whatever handles the link when no browser supports Custom Tabs, and does nothing
 * rather than crash when nothing handles it at all.
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

fun Context.openInAppBrowser(url: String, toolbarColor: Int, isLight: Boolean) {
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
        intent.launchUrl(this, Uri.parse(url))
    } catch (_: ActivityNotFoundException) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
