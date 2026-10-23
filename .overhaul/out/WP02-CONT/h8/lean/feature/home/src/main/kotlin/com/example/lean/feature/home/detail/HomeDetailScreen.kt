package com.example.lean.feature.home.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.lean.core.common.LoadState
import com.example.lean.core.common.result.AppError
import com.example.lean.core.designsystem.component.container.AppScaffold
import com.example.lean.core.designsystem.component.feedback.AppSkeleton
import com.example.lean.core.designsystem.component.feedback.AppStateView
import com.example.lean.core.designsystem.component.feedback.StateKind
import com.example.lean.core.designsystem.component.navigation.AppTopBar
import com.example.lean.core.designsystem.component.text.AppText
import com.example.lean.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.lean.core.designsystem.foundation.layout.chromeContentPadding
import com.example.lean.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.lean.core.designsystem.preview.AppScreenPreviews
import com.example.lean.core.designsystem.theme.AppTheme
import com.example.lean.core.ui.text
import com.example.lean.data.home.api.HomeItem
import com.example.lean.feature.home.R

@Composable
internal fun HomeDetailScreen(
    state: HomeDetailState,
    onEvent: (HomeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.item?.title ?: stringResource(R.string.home_title)
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBack = { onEvent(HomeDetailEvent.BackClicked) },
            )
        },
    ) {
        val scroll = rememberScrollState()
        ReportScrollEdge(scroll)
        val loadState = state.loadState
        if (loadState is LoadState.Error) {
            AppStateView(
                message = loadState.error.text(),
                kind = if (loadState.error == AppError.Offline) {
                    StateKind.Offline
                } else {
                    StateKind.Error
                },
                onAction = { onEvent(HomeDetailEvent.Retry) },
            )
            return@AppScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())
                .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.gutter))),
        ) {
            val item = state.item
            if (item == null) {
                // A transparent line in the title's style sizes the bone, so the title takes its
                // place without moving anything.
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .clearAndSetSemantics {},
                ) {
                    AppText(
                        text = " ",
                        style = AppTheme.typography.title2,
                        color = Color.Transparent,
                    )
                    AppSkeleton(modifier = Modifier.matchParentSize(), shape = AppTheme.shapes.xs)
                }
            } else {
                AppText(
                    text = item.title,
                    style = AppTheme.typography.title2,
                    color = AppTheme.colors.contentPrimary,
                )
            }
        }
    }
}

@AppScreenPreviews
@Composable
private fun HomeDetailScreenPreview() {
    HomeDetailScreen(
        state = HomeDetailState(
            loadState = LoadState.Success,
            item = HomeItem(id = "1", title = "Home"),
        ),
        onEvent = {},
    )
}
