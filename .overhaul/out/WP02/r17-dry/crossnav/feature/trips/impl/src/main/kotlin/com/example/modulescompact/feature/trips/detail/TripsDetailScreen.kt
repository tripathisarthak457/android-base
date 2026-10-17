package com.example.modulescompact.feature.trips.detail

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
import androidx.compose.ui.tooling.preview.Preview
import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.common.result.AppError
import com.example.modulescompact.core.designsystem.component.container.AppScaffold
import com.example.modulescompact.core.designsystem.component.feedback.AppErrorState
import com.example.modulescompact.core.designsystem.component.feedback.AppSkeleton
import com.example.modulescompact.core.designsystem.component.navigation.AppBackTopBar
import com.example.modulescompact.core.designsystem.component.text.AppText
import com.example.modulescompact.core.designsystem.foundation.chromeContentPadding
import com.example.modulescompact.core.designsystem.theme.AppTheme
import com.example.modulescompact.core.ui.text
import com.example.modulescompact.data.trips.api.TripsItem
import com.example.modulescompact.feature.trips.R

@Composable
internal fun TripsDetailScreen(
    state: TripsDetailState,
    onEvent: (TripsDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.item?.title ?: stringResource(R.string.trips_title)
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppBackTopBar(
                title = title,
                onBack = { onEvent(TripsDetailEvent.BackClicked) },
            )
        },
    ) {
        val loadState = state.loadState
        if (loadState is LoadState.Error) {
            AppErrorState(
                message = loadState.error.text(),
                isOffline = loadState.error == AppError.Offline,
                onRetry = { onEvent(TripsDetailEvent.Retry) },
            )
            return@AppScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        style = AppTheme.typography.displaySmall,
                        color = Color.Transparent,
                    )
                    AppSkeleton(modifier = Modifier.matchParentSize(), shape = AppTheme.shapes.xs)
                }
            } else {
                AppText(
                    text = item.title,
                    style = AppTheme.typography.displaySmall,
                    color = AppTheme.colors.contentPrimary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripsDetailScreenPreview() {
    AppTheme {
        TripsDetailScreen(
            state = TripsDetailState(
                loadState = LoadState.Success,
                item = TripsItem(id = "1", title = "Trips"),
            ),
            onEvent = {},
        )
    }
}
