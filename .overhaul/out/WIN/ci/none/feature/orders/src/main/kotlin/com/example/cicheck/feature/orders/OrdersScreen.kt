package com.example.cicheck.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.cicheck.core.common.LoadState
import com.example.cicheck.core.common.result.AppError
import com.example.cicheck.core.designsystem.component.container.AppCard
import com.example.cicheck.core.designsystem.component.container.AppScaffold
import com.example.cicheck.core.designsystem.component.feedback.AppEmptyState
import com.example.cicheck.core.designsystem.component.feedback.AppErrorState
import com.example.cicheck.core.designsystem.component.feedback.AppSkeletonListItem
import com.example.cicheck.core.designsystem.component.navigation.AppLargeTitle
import com.example.cicheck.core.designsystem.component.text.AppText
import com.example.cicheck.core.designsystem.theme.AppTheme
import com.example.cicheck.core.ui.text
import com.example.cicheck.data.orders.OrdersItem

/** Stateless, so the previews below need no ViewModel or network. */
@Composable
fun OrdersScreen(
    state: OrdersState,
    onEvent: (OrdersEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        topBar = { AppLargeTitle(title = stringResource(R.string.orders_title)) },
    ) {
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppErrorState(
                message = loadState.error.text(),
                isOffline = loadState.error == AppError.Offline,
                onRetry = { onEvent(OrdersEvent.Retry) },
            )

            LoadState.Empty -> AppEmptyState(
                title = stringResource(R.string.orders_empty_title),
                message = stringResource(R.string.orders_empty_message),
                actionLabel = stringResource(R.string.orders_reload),
                onAction = { onEvent(OrdersEvent.Retry) },
            )

            LoadState.Loading, LoadState.Idle -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            ) {
                items(SKELETON_ROWS) { AppCard { AppSkeletonListItem(showLeading = false) } }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            ) {
                // Keyed on the id so inserts keep scroll position.
                items(items = state.items, key = OrdersItem::id) { item ->
                    AppCard(
                        onClick = { onEvent(OrdersEvent.ItemClicked(item.id)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AppText(
                            text = item.title,
                            style = AppTheme.typography.titleLarge,
                            color = AppTheme.colors.contentPrimary,
                        )
                    }
                }
            }
        }
    }
}

private const val SKELETON_ROWS = 6

@Preview(showBackground = true)
@Composable
private fun OrdersScreenPreview() {
    AppTheme {
        OrdersScreen(
            state = OrdersState(
                loadState = LoadState.Success,
                items = List(3) { OrdersItem(id = it, title = "Row ${it + 1}") },
            ),
            onEvent = {},
        )
    }
}
