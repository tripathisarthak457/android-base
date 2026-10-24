package io.example.audit.feature.order.detail

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
import io.example.audit.core.common.LoadState
import io.example.audit.core.common.result.AppError
import io.example.audit.core.designsystem.component.container.AppScaffold
import io.example.audit.core.designsystem.component.feedback.AppSkeleton
import io.example.audit.core.designsystem.component.feedback.AppStateView
import io.example.audit.core.designsystem.component.feedback.StateKind
import io.example.audit.core.designsystem.component.navigation.AppTopBar
import io.example.audit.core.designsystem.component.text.AppText
import io.example.audit.core.designsystem.foundation.layout.ReportScrollEdge
import io.example.audit.core.designsystem.foundation.layout.chromeContentPadding
import io.example.audit.core.designsystem.foundation.scroll.AppScrollDefaults
import io.example.audit.core.designsystem.preview.AppScreenPreviews
import io.example.audit.core.designsystem.theme.AppTheme
import io.example.audit.core.ui.text
import io.example.audit.data.order.api.model.OrderItem
import io.example.audit.feature.order.R

@Composable
internal fun OrderDetailScreen(
    state: OrderDetailState,
    onEvent: (OrderDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.item?.title ?: stringResource(R.string.order_title)
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBack = { onEvent(OrderDetailEvent.BackClicked) },
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
                onAction = { onEvent(OrderDetailEvent.Retry) },
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
private fun OrderDetailScreenPreview() {
    OrderDetailScreen(
        state = OrderDetailState(
            loadState = LoadState.Success,
            item = OrderItem(id = "1", title = "Order"),
        ),
        onEvent = {},
    )
}
