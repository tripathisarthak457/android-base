package com.example.modulesmodular.feature.wallet.detail

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
import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.common.result.AppError
import com.example.modulesmodular.core.designsystem.component.container.AppScaffold
import com.example.modulesmodular.core.designsystem.component.feedback.AppErrorState
import com.example.modulesmodular.core.designsystem.component.feedback.AppSkeleton
import com.example.modulesmodular.core.designsystem.component.navigation.AppBackTopBar
import com.example.modulesmodular.core.designsystem.component.text.AppText
import com.example.modulesmodular.core.designsystem.foundation.chromeContentPadding
import com.example.modulesmodular.core.designsystem.theme.AppTheme
import com.example.modulesmodular.core.ui.text
import com.example.modulesmodular.data.wallet.api.WalletItem
import com.example.modulesmodular.feature.wallet.R

@Composable
internal fun WalletDetailScreen(
    state: WalletDetailState,
    onEvent: (WalletDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.item?.title ?: stringResource(R.string.wallet_title)
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppBackTopBar(
                title = title,
                onBack = { onEvent(WalletDetailEvent.BackClicked) },
            )
        },
    ) {
        val loadState = state.loadState
        if (loadState is LoadState.Error) {
            AppErrorState(
                message = loadState.error.text(),
                isOffline = loadState.error == AppError.Offline,
                onRetry = { onEvent(WalletDetailEvent.Retry) },
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
private fun WalletDetailScreenPreview() {
    AppTheme {
        WalletDetailScreen(
            state = WalletDetailState(
                loadState = LoadState.Success,
                item = WalletItem(id = "1", title = "Wallet"),
            ),
            onEvent = {},
        )
    }
}
