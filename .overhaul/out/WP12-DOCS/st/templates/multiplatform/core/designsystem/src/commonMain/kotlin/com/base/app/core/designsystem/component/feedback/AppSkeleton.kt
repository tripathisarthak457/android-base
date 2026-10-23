package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.resources.Res
import com.base.app.core.designsystem.resources.designsystem_loading
import com.base.app.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * A line of text that has not loaded yet, drawn as a still grey bar shaped like the line it stands
 * for. Still rather than shimmering: content that arrives in a few hundred milliseconds does not
 * need an animation that redraws every frame it is on screen.
 */
@Composable
fun AppSkeletonLine(modifier: Modifier = Modifier, widthFraction: Float = 1f) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(LineHeight)
            .background(AppTheme.colors.skeleton, AppTheme.shapes.sm),
    )
}

/**
 * [count] rows of a list that has not loaded yet, each a title and a line under it, announced
 * once as loading rather than once per bar.
 */
@Composable
fun AppSkeletonRows(count: Int, modifier: Modifier = Modifier) {
    val loading = stringResource(Res.string.designsystem_loading)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        repeat(count) {
            Column(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                AppSkeletonLine(widthFraction = 0.6f)
                AppSkeletonLine(widthFraction = 0.9f)
            }
        }
    }
}

private val LineHeight = 14.dp
