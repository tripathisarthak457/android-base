package __PKG__.feature.__NAME__.detail

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
import __PKG__.core.common.LoadState
import __PKG__.core.common.result.AppError
import __PKG__.core.designsystem.component.container.AppScaffold
import __PKG__.core.designsystem.component.feedback.AppSkeleton
import __PKG__.core.designsystem.component.feedback.AppStateView
import __PKG__.core.designsystem.component.feedback.StateKind
import __PKG__.core.designsystem.component.navigation.AppTopBar
import __PKG__.core.designsystem.component.text.AppText
import __PKG__.core.designsystem.foundation.layout.ReportScrollEdge
import __PKG__.core.designsystem.foundation.layout.chromeContentPadding
import __PKG__.core.designsystem.foundation.scroll.AppScrollDefaults
import __PKG__.core.designsystem.theme.AppTheme
import __PKG__.core.ui.text
import __PKG__.data.__NAME__.api.model.__P__Item
import __PKG__.feature.__NAME__.R
__PREVIEW_IMPORTS__

@Composable
internal fun __P__DetailScreen(
    state: __P__DetailState,
    onEvent: (__P__DetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.item?.title ?: stringResource(R.string.__NAME___title)
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBack = { onEvent(__P__DetailEvent.BackClicked) },
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
                onAction = { onEvent(__P__DetailEvent.Retry) },
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

__PREVIEW__
private fun __P__DetailScreenPreview() {
    __THEMED_START__
        __P__DetailScreen(
            state = __P__DetailState(
                loadState = LoadState.Success,
__FIT__
                item = __P__Item(id = "1", title = "__TITLE__"),
__ELSE__
                item = __P__Item(
                    id = "1",
                    title = "__TITLE__",
                ),
__END_FIT__
            ),
            onEvent = {},
        )
    __THEMED_END__
}
