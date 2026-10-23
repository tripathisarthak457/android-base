package io.example.fitcnetdb.feature.abcdefghi.detail

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
import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.common.result.AppError
import io.example.fitcnetdb.core.designsystem.component.container.AppScaffold
import io.example.fitcnetdb.core.designsystem.component.feedback.AppSkeleton
import io.example.fitcnetdb.core.designsystem.component.feedback.AppStateView
import io.example.fitcnetdb.core.designsystem.component.feedback.StateKind
import io.example.fitcnetdb.core.designsystem.component.navigation.AppTopBar
import io.example.fitcnetdb.core.designsystem.component.text.AppText
import io.example.fitcnetdb.core.designsystem.foundation.layout.ReportScrollEdge
import io.example.fitcnetdb.core.designsystem.foundation.layout.chromeContentPadding
import io.example.fitcnetdb.core.designsystem.foundation.scroll.AppScrollDefaults
import io.example.fitcnetdb.core.designsystem.preview.AppScreenPreviews
import io.example.fitcnetdb.core.designsystem.theme.AppTheme
import io.example.fitcnetdb.core.ui.text
import io.example.fitcnetdb.data.abcdefghi.api.model.AbcdefghiItem
import io.example.fitcnetdb.feature.abcdefghi.R

@Composable
internal fun AbcdefghiDetailScreen(
    state: AbcdefghiDetailState,
    onEvent: (AbcdefghiDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.item?.title ?: stringResource(R.string.abcdefghi_title)
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBack = { onEvent(AbcdefghiDetailEvent.BackClicked) },
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
                onAction = { onEvent(AbcdefghiDetailEvent.Retry) },
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
private fun AbcdefghiDetailScreenPreview() {
    AbcdefghiDetailScreen(
        state = AbcdefghiDetailState(
            loadState = LoadState.Success,
            item = AbcdefghiItem(id = "1", title = "Abcdefghi"),
        ),
        onEvent = {},
    )
}
