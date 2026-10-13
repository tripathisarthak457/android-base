package __PKG__.feature.__NAME__.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import __PKG__.core.common.LoadState
import __PKG__.core.common.result.AppError
import __PKG__.core.designsystem.component.container.AppScaffold
import __PKG__.core.designsystem.component.feedback.AppErrorState
import __PKG__.core.designsystem.component.feedback.AppSkeletonLine
import __PKG__.core.designsystem.component.navigation.AppBackTopBar
import __PKG__.core.designsystem.component.text.AppText
import __PKG__.core.designsystem.theme.AppTheme
import __PKG__.core.ui.text
import __PKG__.data.__NAME__.api.__P__Item
import __PKG__.feature.__NAME__.R
__PREVIEW_IMPORTS__

@Composable
internal fun __P__DetailScreen(
    state: __P__DetailState,
    onEvent: (__P__DetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppBackTopBar(
                title = state.item?.title ?: stringResource(R.string.__NAME___title),
                onBack = { onEvent(__P__DetailEvent.BackClicked) },
            )
        },
    ) {
        val loadState = state.loadState
        if (loadState is LoadState.Error) {
            AppErrorState(
                message = loadState.error.text(),
                isOffline = loadState.error == AppError.Offline,
                onRetry = { onEvent(__P__DetailEvent.Retry) },
            )
            return@AppScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.gutter),
        ) {
            val item = state.item
            if (item == null) {
                AppSkeletonLine()
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

__PREVIEW__
private fun __P__DetailScreenPreview() {
    __THEMED_START__
        __P__DetailScreen(
            state = __P__DetailState(
                loadState = LoadState.Success,
                item = __P__Item(id = "1", title = "__TITLE__"),
            ),
            onEvent = {},
        )
    __THEMED_END__
}
