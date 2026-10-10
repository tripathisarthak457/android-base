package com.base.app.feature.sample.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.common.LoadState
import com.base.app.core.common.result.AppError
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppErrorState
import com.base.app.core.designsystem.component.feedback.AppSkeletonLine
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.text
import com.base.app.data.sample.api.model.SampleItem
import com.base.app.feature.sample.R
import com.base.app.feature.sample.component.SampleRow

@Composable
internal fun SampleDetailScreen(
    state: SampleDetailState,
    onEvent: (SampleDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppBackTopBar(
                title = state.item?.title ?: stringResource(R.string.sample_detail_title),
                onBack = { onEvent(SampleDetailEvent.BackClicked) },
            )
        },
    ) {
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppErrorState(
                message = loadState.error.text(),
                isOffline = loadState.error == AppError.Offline,
                onRetry = { onEvent(SampleDetailEvent.Retry) },
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.spacing.gutter),
            ) {
                val item = state.item
                if (item == null) {
                    repeat(SKELETON_LINES) {
                        AppSkeletonLine(
                            modifier = Modifier.padding(bottom = AppTheme.spacing.sm),
                            widthFraction = if (it == SKELETON_LINES - 1) 0.5f else 1f,
                        )
                    }
                    return@Column
                }

                AppMonoText(
                    text = "#${item.id}",
                    color = AppTheme.colors.contentTertiary,
                )
                AppText(
                    text = item.title,
                    modifier = Modifier.padding(top = AppTheme.spacing.sm),
                    style = AppTheme.typography.displaySmall,
                    color = AppTheme.colors.contentPrimary,
                )
                AppText(
                    text = item.body,
                    modifier = Modifier.padding(top = AppTheme.spacing.lg),
                    style = AppTheme.typography.bodyLarge,
                    color = AppTheme.colors.contentSecondary,
                )
                Related(state.related, onEvent)
            }
        }
    }
}

/** More samples, each opening another detail screen: the reference for a stack that goes deep. */
@Composable
private fun Related(items: List<SampleItem>, onEvent: (SampleDetailEvent) -> Unit) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier.padding(top = AppTheme.spacing.section),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
    ) {
        AppText(
            text = stringResource(R.string.sample_related),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.contentSecondary,
        )
        items.forEach { item ->
            SampleRow(
                item = item,
                onClick = { onEvent(SampleDetailEvent.RelatedClicked(item.id)) },
                modifier = Modifier.testTag("related_row"),
            )
        }
    }
}

private const val SKELETON_LINES = 5

@Preview(showBackground = true)
@Composable
private fun SampleDetailPreview() {
    AppTheme {
        SampleDetailScreen(
            state = SampleDetailState(
                loadState = LoadState.Success,
                item = SampleItem(1, "A sample title", "The body of the sample, at some length."),
                related = listOf(SampleItem(2, "Another sample", "Its body.")),
            ),
            onEvent = {},
        )
    }
}
