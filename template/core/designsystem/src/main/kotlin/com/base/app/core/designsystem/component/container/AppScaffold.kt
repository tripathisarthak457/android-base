package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.centeredMaxWidth
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The frame every screen sits in: an optional top bar, the content, an optional bottom bar, and an
 * optional floating action anchored over the content.
 *
 * [contentMaxWidth] stops the content stretching on wide windows and centres it, while the bars
 * and the background still span the window. Pass one of [AppTheme.layout]'s widths: `formMaxWidth`
 * for a single form, `readableMaxWidth` for text and settings, `contentMaxWidth` for mixed content.
 * The sides clear display cutouts, which a phone held sideways puts at one edge. A screen with a
 * full-bleed panel passes [clearDisplayCutout] false and pads that panel's content itself, so the
 * panel's colour reaches the edge of the glass.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingAction: @Composable (BoxScope.() -> Unit)? = null,
    background: Color = AppTheme.colors.background,
    contentMaxWidth: Dp = Dp.Unspecified,
    clearDisplayCutout: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (clearDisplayCutout) {
                        Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    } else {
                        Modifier
                    },
                ),
        ) {
            topBar()
            val width = if (contentMaxWidth == Dp.Unspecified) Modifier else Modifier.centeredMaxWidth(contentMaxWidth)
            Box(modifier = Modifier.weight(1f).then(width)) {
                content()
            }
            bottomBar()
        }

        floatingAction?.let { action ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppTheme.spacing.lg)
                    // Clears the bottom bar when there is one. A FAB overlapping the bar it sits
                    // above is the single most common layout defect in a screen that has both.
                    .padding(bottom = 8.dp),
                content = action,
            )
        }
    }
}
