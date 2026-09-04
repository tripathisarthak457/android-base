package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The frame every screen sits in: an optional top bar, the content, an optional bottom bar, and
 * an optional floating action anchored over the content.
 *
 * Far simpler than Material's Scaffold, and deliberately so. It does not hand the content a
 * `PaddingValues` to apply, because the bars here are laid out *beside* the content rather than
 * over it — the content already occupies exactly the space that is left. The whole class of bug
 * where a list is hidden behind a bottom bar because someone forgot to consume the padding
 * cannot occur.
 *
 * Window insets are each bar's own responsibility: [AppTopBar] takes the status bar and
 * [AppBottomBar] takes the navigation bar. A screen with no bottom bar and content that needs
 * to clear the navigation bar adds `Modifier.navigationBarsPadding()` itself, which is one line
 * at the one place it is true.
 *
 * `imePadding` is applied here, once, so a text field near the bottom of any screen is pushed
 * above the keyboard without every screen remembering to ask.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingAction: @Composable (BoxScope.() -> Unit)? = null,
    background: Color = AppTheme.colors.background,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            Box(modifier = Modifier.weight(1f)) { content() }
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
