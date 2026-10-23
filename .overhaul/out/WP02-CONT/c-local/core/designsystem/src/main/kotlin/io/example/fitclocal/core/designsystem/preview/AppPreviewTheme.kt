package io.example.fitclocal.core.designsystem.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.designsystem.theme.PlatformFonts

/**
 * The app's theme around a preview, with the platform's fonts: a preview never waits for a
 * downloadable font, so what it shows is what the first frame of the app shows.
 */
class AppPreviewTheme : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        AppTheme(fonts = PlatformFonts, content = content)
    }
}
