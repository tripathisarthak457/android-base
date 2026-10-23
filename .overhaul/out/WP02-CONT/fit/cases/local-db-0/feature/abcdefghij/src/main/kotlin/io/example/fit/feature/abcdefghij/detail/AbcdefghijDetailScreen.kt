package io.example.fit.feature.abcdefghij.detail

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
import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppError
import io.example.fit.core.designsystem.component.container.AppScaffold
import io.example.fit.core.designsystem.component.feedback.AppSkeleton
import io.example.fit.core.designsystem.component.feedback.AppStateView
import io.example.fit.core.designsystem.component.feedback.StateKind
import io.example.fit.core.designsystem.component.navigation.AppTopBar
import io.example.fit.core.designsystem.component.text.AppText
import io.example.fit.core.designsystem.foundation.layout.ReportScrollEdge
import io.example.fit.core.designsystem.foundation.layout.chromeContentPadding
import io.example.fit.core.designsystem.foundation.scroll.AppScrollDefaults
import io.example.fit.core.designsystem.preview.AppScreenPreviews
import io.example.fit.core.designsystem.theme.AppTheme
import io.example.fit.core.ui.text
import io.example.fit.data.abcdefghij.api.model.AbcdefghijItem
import io.example.fit.feature.abcdefghij.R

@Composable
internal fun AbcdefghijDetailScreen(
    state: AbcdefghijDetailState,
    onEvent: (AbcdefghijDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.item?.title ?: stringResource(R.string.abcdefghij_title)
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBack = { onEvent(AbcdefghijDetailEvent.BackClicked) },
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
                onAction = { onEvent(AbcdefghijDetailEvent.Retry) },
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
private fun AbcdefghijDetailScreenPreview() {
    AbcdefghijDetailScreen(
        state = AbcdefghijDetailState(
            loadState = LoadState.Success,
            item = AbcdefghijItem(id = "1", title = "Abcdefghij"),
        ),
        onEvent = {},
    )
}
