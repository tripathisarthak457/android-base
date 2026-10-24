package com.example.deskstandard.feature.sample.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.deskstandard.core.common.LoadState
import com.example.deskstandard.core.common.result.AppError
import com.example.deskstandard.core.designsystem.R as DesignSystemR
import com.example.deskstandard.core.designsystem.component.container.AppScaffold
import com.example.deskstandard.core.designsystem.component.feedback.AppRedacted
import com.example.deskstandard.core.designsystem.component.feedback.AppStateView
import com.example.deskstandard.core.designsystem.component.feedback.StateKind
import com.example.deskstandard.core.designsystem.component.navigation.AppTopBar
import com.example.deskstandard.core.designsystem.component.text.AppMonoText
import com.example.deskstandard.core.designsystem.component.text.AppText
import com.example.deskstandard.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.deskstandard.core.designsystem.foundation.layout.chromeContentPadding
import com.example.deskstandard.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.deskstandard.core.designsystem.theme.AppTheme
import com.example.deskstandard.core.ui.text
import com.example.deskstandard.data.sample.api.model.SampleItem
import com.example.deskstandard.feature.sample.R
import com.example.deskstandard.feature.sample.component.SampleRow

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
            AppTopBar(
                title = state.item?.title ?: stringResource(R.string.sample_detail_title),
                onBack = { onEvent(SampleDetailEvent.BackClicked) },
            )
        },
    ) {
        val scroll = rememberScrollState()
        ReportScrollEdge(scroll)
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppStateView(
                message = loadState.error.text(),
                kind = if (loadState.error == AppError.Offline) StateKind.Offline else StateKind.Error,
                onAction = { onEvent(SampleDetailEvent.Retry) },
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())
                    .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.gutter))),
            ) {
                val item = state.item
                // The same composables before and after the item arrives: the text takes the place
                // its placeholder held, and nothing above it moves.
                AppRedacted(
                    active = item == null,
                    contentDescription = stringResource(DesignSystemR.string.designsystem_loading),
                ) {
                    Header(item ?: SampleDetailPlaceholder)
                }
                Related(state.related, onEvent)
            }
        }
    }
}

@Composable
private fun Header(item: SampleItem) {
    Column {
        AppMonoText(
            text = "#${item.id}",
            color = AppTheme.colors.contentTertiary,
        )
        AppText(
            text = item.title,
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
            style = AppTheme.typography.title2,
            color = AppTheme.colors.contentPrimary,
        )
        AppText(
            text = item.body,
            modifier = Modifier.padding(top = AppTheme.spacing.lg),
            style = AppTheme.typography.body,
            color = AppTheme.colors.contentSecondary,
        )
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
            style = AppTheme.typography.footnote,
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

// Never shown (see AppRedacted). A title on one line and a body of about four, as the samples have,
// and a two-digit id, as most of theirs are, for the `#id` line.
private val SampleDetailPlaceholder = SampleItem(
    10,
    "Placeholder sample title",
    "The body of a sample runs to about four lines on a phone, and this placeholder is about as " +
        "long, so its bones take close to the room the real text will take when it arrives.",
)

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
