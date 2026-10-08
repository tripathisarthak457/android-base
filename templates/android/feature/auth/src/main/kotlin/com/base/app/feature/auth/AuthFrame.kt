package com.base.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.shouldUseTwoPanes
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.WindowWidth

/**
 * The frame the sign-in, sign-up and reset screens share.
 *
 * A phone gets one scrolling column. Wider windows keep the form at a width a form reads well at,
 * and centre it. With room for two panes — a tablet, an unfolded foldable, a phone held sideways —
 * the app's name sits on its accent colour beside the form, so the screen is designed for the
 * window rather than a phone layout marooned in the middle of it.
 */
@Composable
internal fun AuthFrame(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val twoPanes = shouldUseTwoPanes

    AppScaffold(modifier = modifier, topBar = topBar ?: {}, clearDisplayCutout = false) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (twoPanes) {
                BrandPanel(modifier = Modifier.weight(BRAND_WEIGHT).fillMaxHeight())
            }
            // A cutout on the brand panel's side is the panel's to clear; the form clears the rest.
            val formCutout = if (twoPanes) WindowInsetsSides.End else WindowInsetsSides.Horizontal
            // On a phone the form starts at the top, under the thumb; with room to spare it sits in
            // the middle of the window, where a short form on a tall screen looks placed, not lost.
            val centred = AppTheme.windowSize.width >= WindowWidth.Medium
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(formCutout)),
                contentAlignment = Alignment.TopCenter,
            ) {
                val viewport = maxHeight
                Column(
                    modifier = Modifier
                        .widthIn(max = AppTheme.layout.formMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(min = if (centred) viewport else 0.dp)
                            // Without a top bar the form carries the status-bar inset itself.
                            .then(if (topBar == null) Modifier.statusBarsPadding() else Modifier)
                            .navigationBarsPadding()
                            .padding(AppTheme.spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(
                            AppTheme.spacing.lg,
                            if (centred) Alignment.CenterVertically else Alignment.Top,
                        ),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandPanel(modifier: Modifier) {
    val context = LocalContext.current
    // The launcher label, so the panel always names the app it is in without a string of its own.
    val appName = remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }

    Box(
        modifier = modifier
            .background(AppTheme.colors.accent)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(AppTheme.spacing.xxl),
        contentAlignment = Alignment.BottomStart,
    ) {
        AppText(
            text = appName,
            style = AppTheme.typography.displayMedium,
            color = AppTheme.colors.onAccent,
        )
    }
}

private const val BRAND_WEIGHT = 0.8f
