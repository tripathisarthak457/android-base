package com.base.app.feature.sample.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.common.LoadState
import com.base.app.core.common.result.AppError
import com.base.app.core.designsystem.component.feedback.AppSkeletonLine
import com.base.app.core.designsystem.component.feedback.AppStateView
import com.base.app.core.designsystem.component.feedback.StateKind
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.text
import com.base.app.data.sample.api.model.SampleItem
import com.base.app.feature.sample.component.SampleRow
import com.base.app.feature.sample.resources.Res
import com.base.app.feature.sample.resources.sample_detail_title
import com.base.app.feature.sample.resources.sample_related
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SampleDetailScreen(
    state: SampleDetailState,
    onEvent: (SampleDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        AppTopBar(
            title = state.item?.title ?: stringResource(Res.string.sample_detail_title),
            onBack = { onEvent(SampleDetailEvent.BackClicked) },
        )
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppStateView(
                modifier = Modifier.weight(1f),
                kind = if (loadState.error == AppError.Offline) StateKind.Offline else StateKind.Error,
                message = loadState.error.text(),
                onAction = { onEvent(SampleDetailEvent.Retry) },
            )

            else -> Content(state, onEvent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Content(state: SampleDetailState, onEvent: (SampleDetailEvent) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(AppTheme.spacing.gutter),
    ) {
        val item = state.item
        if (item == null) {
            repeat(SkeletonLines) {
                AppSkeletonLine(
                    modifier = Modifier.padding(bottom = AppTheme.spacing.sm),
                    widthFraction = if (it == SkeletonLines - 1) 0.5f else 1f,
                )
            }
            return@Column
        }

        AppMonoText(text = "#${item.id}", color = AppTheme.colors.contentTertiary)
        AppText(
            text = item.title,
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
            style = AppTheme.typography.title1,
            color = AppTheme.colors.contentPrimary,
        )
        AppText(
            text = item.body,
            modifier = Modifier.padding(top = AppTheme.spacing.lg),
            style = AppTheme.typography.body,
            color = AppTheme.colors.contentSecondary,
        )
        Related(state.related, onEvent)
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
            text = stringResource(Res.string.sample_related),
            style = AppTheme.typography.headline,
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

private const val SkeletonLines = 5

@Preview
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
