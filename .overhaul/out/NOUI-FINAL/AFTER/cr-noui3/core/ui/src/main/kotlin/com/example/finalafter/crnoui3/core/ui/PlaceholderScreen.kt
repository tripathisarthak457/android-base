package com.example.finalafter.crnoui3.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.finalafter.crnoui3.core.designsystem.theme.AppTheme

/**
 * What a route draws until the project has the components to build its screen: the screen's
 * [title], centred on the theme's background and clear of the system bars. The route around it is
 * finished (its ViewModel loads, its effects navigate), so replacing this call with the real screen
 * is the whole of the work. Tagged "placeholder", which UI tests and the benchmark find it by.
 */
@Composable
fun PlaceholderScreen(@StringRes title: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
            .testTag("placeholder"),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(title),
            modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter),
            style = AppTheme.typography.headline.copy(
                color = AppTheme.colors.contentSecondary,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
