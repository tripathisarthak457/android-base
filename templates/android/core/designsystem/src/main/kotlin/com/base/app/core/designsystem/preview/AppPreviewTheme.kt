package com.base.app.core.designsystem.preview

import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import androidx.compose.ui.unit.LayoutDirection
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.PlatformFonts

/**
 * The app's theme around a preview, with the platform's fonts: a preview never waits for a
 * downloadable font, so what it shows is what the first frame of the app shows.
 *
 * The layout direction follows the preview's locale. Android Studio does that by itself, but the
 * screenshot tests render in a library's test app, whose manifest declares no RTL support, so
 * without this their "Right to left" image would be laid out left to right.
 */
class AppPreviewTheme : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        val locale = LocalConfiguration.current.locales[0]
        val direction = if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            AppTheme(fonts = PlatformFonts, content = content)
        }
    }
}
