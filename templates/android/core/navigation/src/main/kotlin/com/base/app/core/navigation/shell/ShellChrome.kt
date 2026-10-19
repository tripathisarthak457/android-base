package com.base.app.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
// <opt:liquidglass>
import com.base.app.core.designsystem.foundation.glass.GlassStage
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment
// </opt:liquidglass>

/**
 * The shell's layers: content, and the chrome drawn over it. A plain box; with liquid glass on,
 * this is where the glass stage records the content for the chrome to sample.
 */
@Composable
internal fun ShellChrome(
    modifier: Modifier,
    chrome: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    // <opt:liquidglass>
    if (AppTheme.style.bar == BarTreatment.Glass) {
        GlassStage(background = AppTheme.colors.background, chrome = chrome, modifier = modifier, content = content)
        return
    }
    // </opt:liquidglass>
    Box(modifier) {
        content()
        chrome()
    }
}
