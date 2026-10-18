package com.example.wp04.base.feature.wallet

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
import com.example.wp04.base.core.common.mvi.LoadState
import com.example.wp04.base.core.designsystem.component.container.AppCard
import com.example.wp04.base.core.designsystem.component.container.AppScaffold
import com.example.wp04.base.core.designsystem.component.feedback.AppEmptyState
import com.example.wp04.base.core.designsystem.component.feedback.AppErrorState
import com.example.wp04.base.core.designsystem.component.feedback.AppSkeletonListItem
import com.example.wp04.base.core.designsystem.component.navigation.AppLargeTitle
import com.example.wp04.base.core.designsystem.component.text.AppText
import com.example.wp04.base.core.designsystem.theme.AppTheme
import com.example.wp04.base.core.ui.asString
import com.example.wp04.base.data.wallet.WalletItem

/** Stateless, so the previews below need no ViewModel or network. */
@Composable
fun WalletScreen(
    state: WalletState,
    onEvent: (WalletEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        topBar = { AppLargeTitle(title = stringResource(R.string.wallet_title)) },
    ) {
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppErrorState(
                message = loadState.message.asString(),
                isOffline = loadState.isOffline,
                onRetry = { onEvent(WalletEvent.Retry) },
            )

            LoadState.Empty -> AppEmptyState(
                title = stringResource(R.string.wallet_empty_title),
                message = stringResource(R.string.wallet_empty_message),
                actionLabel = stringResource(R.string.wallet_reload),
                onAction = { onEvent(WalletEvent.Retry) },
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
                items(items = state.items, key = WalletItem::id) { item ->
                    AppCard(
                        onClick = { onEvent(WalletEvent.ItemClicked(item.id)) },
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
private fun WalletScreenPreview() {
    AppTheme {
        WalletScreen(
            state = WalletState(
                loadState = LoadState.Success,
                items = List(3) { WalletItem(id = it, title = "Row ${it + 1}") },
            ),
            onEvent = {},
        )
    }
}
