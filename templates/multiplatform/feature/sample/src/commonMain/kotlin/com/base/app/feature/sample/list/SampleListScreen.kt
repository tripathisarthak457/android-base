package com.base.app.feature.sample.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.common.LoadState
import com.base.app.core.common.hasContent
import com.base.app.core.common.result.AppError
import com.base.app.core.designsystem.component.feedback.AppSkeletonRows
import com.base.app.core.designsystem.component.feedback.AppStateView
import com.base.app.core.designsystem.component.feedback.StateKind
import com.base.app.core.designsystem.component.input.AppSearchField
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.text
import com.base.app.data.sample.api.model.SampleItem
import com.base.app.feature.sample.component.SampleRow
import com.base.app.feature.sample.resources.Res
import com.base.app.feature.sample.resources.sample_empty_explanation
import com.base.app.feature.sample.resources.sample_no_matches_for_query
import com.base.app.feature.sample.resources.sample_nothing_here_yet
import com.base.app.feature.sample.resources.sample_pulled_from_a_live_api
import com.base.app.feature.sample.resources.sample_reload
import com.base.app.feature.sample.resources.sample_search_titles
import com.base.app.feature.sample.resources.sample_title
import org.jetbrains.compose.resources.stringResource

/**
 * The list screen. Stateless by construction: it takes a state and the ViewModel's search field,
 * and emits events. The title and the search field scroll with the list, as a root screen's do on
 * iOS.
 */
@Composable
internal fun SampleListScreen(
    state: SampleListState,
    query: TextFieldState,
    onEvent: (SampleListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = modifier
        .fillMaxSize()
        .background(AppTheme.colors.backgroundGrouped)
    if (state.loadState.hasContent) {
        LazyColumn(
            modifier = background.testTag("primary_list"),
            contentPadding = WindowInsets.statusBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        ) {
            item(key = "header", contentType = "header") { Header(query) }
            rows(state.visibleItems, query, onEvent)
        }
        return
    }

    Column(modifier = background.windowInsetsPadding(WindowInsets.statusBars)) {
        Header(query)
        val placeholder = Modifier.weight(1f)
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppStateView(
                modifier = placeholder,
                kind = if (loadState.error == AppError.Offline) StateKind.Offline else StateKind.Error,
                message = loadState.error.text(),
                onAction = { onEvent(SampleListEvent.Retry) },
            )

            LoadState.Empty -> AppStateView(
                modifier = placeholder,
                title = stringResource(Res.string.sample_nothing_here_yet),
                message = stringResource(Res.string.sample_empty_explanation),
                actionLabel = stringResource(Res.string.sample_reload),
                onAction = { onEvent(SampleListEvent.Retry) },
            )

            // Idle is the first frame, before the route's Appeared has started the load.
            else -> AppSkeletonRows(
                count = SKELETON_ROWS,
                modifier = placeholder.padding(top = AppTheme.spacing.xl),
            )
        }
    }
}

@Composable
private fun Header(query: TextFieldState) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        AppLargeTitle(
            title = stringResource(Res.string.sample_title),
            subtitle = stringResource(Res.string.sample_pulled_from_a_live_api),
        )
        AppSearchField(
            state = query,
            placeholder = stringResource(Res.string.sample_search_titles),
            modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter),
        )
    }
}

private fun LazyListScope.rows(
    visible: List<SampleItem>,
    query: TextFieldState,
    onEvent: (SampleListEvent) -> Unit,
) {
    if (visible.isEmpty()) {
        item(key = "no-matches", contentType = "no-matches") {
            // Inside a lazy list, so it takes the height it needs rather than filling the screen.
            AppText(
                text = stringResource(Res.string.sample_no_matches_for_query, query.text.trim()),
                modifier = Modifier.padding(AppTheme.spacing.xl),
                style = AppTheme.typography.body,
                color = AppTheme.colors.contentSecondary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    // Keyed on the id so inserts keep the scroll position.
    items(items = visible, key = SampleItem::id, contentType = { "sample" }) { item ->
        SampleRow(
            item = item,
            onClick = { onEvent(SampleListEvent.ItemClicked(item.id)) },
            modifier = Modifier
                .padding(horizontal = AppTheme.spacing.gutter)
                .animateItem(),
        )
    }
}

private const val SKELETON_ROWS = 6

@Preview
@Composable
private fun SampleListContentPreview() {
    AppTheme {
        val items = List(4) {
            SampleItem(
                id = it,
                title = "A sample row number ${it + 1}",
                body = "Some supporting copy that gets truncated when it runs long enough.",
            )
        }
        SampleListScreen(
            state = SampleListState(loadState = LoadState.Success, items = items, visibleItems = items),
            query = rememberTextFieldState(),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun SampleListLoadingPreview() {
    AppTheme {
        SampleListScreen(
            state = SampleListState(loadState = LoadState.Loading),
            query = rememberTextFieldState(),
            onEvent = {},
        )
    }
}
