package com.base.app.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.LoadState
import com.base.app.core.common.result.AppResult
import com.base.app.core.mvi.MviViewModel
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.data.search.api.SearchRepository
import com.base.app.data.search.api.model.SearchResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
// <opt:ui_components>
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.base.app.core.common.result.AppError
import com.base.app.core.designsystem.R as DesignSystemR
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppRedacted
import com.base.app.core.designsystem.component.feedback.AppStateView
import com.base.app.core.designsystem.component.feedback.StateKind
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.layout.ReportScrollEdge
import com.base.app.core.designsystem.foundation.layout.chromeContentPadding
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.text
// </opt:ui_components>
// <opt-else:ui_components>
//~ import com.base.app.core.ui.PlaceholderScreen
// </opt-else:ui_components>

@Immutable
internal data class SearchResultState(
    val loadState: LoadState = LoadState.Idle,
    val result: SearchResult? = null,
) : UiState

internal sealed interface SearchResultEvent : UiEvent {
    data object Appeared : SearchResultEvent
    data object Retry : SearchResultEvent
}

internal sealed interface SearchResultEffect : UiEffect

@HiltViewModel(assistedFactory = SearchResultViewModel.Factory::class)
internal class SearchResultViewModel @AssistedInject constructor(
    private val repository: SearchRepository,
    @Assisted private val id: Int,
) : MviViewModel<SearchResultState, SearchResultEvent, SearchResultEffect>(SearchResultState()) {

    @AssistedFactory
    interface Factory {
        fun create(id: Int): SearchResultViewModel
    }

    override fun handleEvent(event: SearchResultEvent) {
        when (event) {
            SearchResultEvent.Appeared -> if (current.loadState == LoadState.Idle) load()
            SearchResultEvent.Retry -> load()
        }
    }

    private fun load() {
        launchLatest(LOAD) {
            setState { copy(loadState = LoadState.Loading) }
            when (val result = repository.result(id)) {
                is AppResult.Success -> setState { copy(loadState = LoadState.Success, result = result.value) }
                is AppResult.Failure -> setState { copy(loadState = LoadState.Error(result.error)) }
            }
        }
    }

    private companion object {
        const val LOAD = "load"
    }
}

@Composable
internal fun SearchResultRoute(
    id: Int,
    navigator: AppNavigator,
    viewModel: SearchResultViewModel = hiltViewModel<SearchResultViewModel, SearchResultViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) },
    ),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(SearchResultEvent.Appeared) }

    // <opt:ui_components>
    MviScreen(viewModel = viewModel) { state, onEvent ->
        AppScaffold(
            contentMaxWidth = AppTheme.layout.readableMaxWidth,
            topBar = {
                AppTopBar(title = stringResource(R.string.search_title), onBack = navigator::navigateUp)
            },
        ) {
            val scroll = rememberScrollState()
            ReportScrollEdge(scroll)
            when (val loadState = state.loadState) {
                is LoadState.Error -> AppStateView(
                    message = loadState.error.text(),
                    kind = if (loadState.error == AppError.Offline) StateKind.Offline else StateKind.Error,
                    onAction = { onEvent(SearchResultEvent.Retry) },
                    actionLabel = stringResource(R.string.search_retry),
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())
                        .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.gutter))),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    val result = state.result
                    // The same text before and after the result arrives, so it lands where its
                    // placeholder was and nothing around it moves.
                    AppRedacted(
                        active = result == null,
                        contentDescription = stringResource(DesignSystemR.string.designsystem_loading),
                    ) {
                        val shown = result ?: ResultPlaceholder
                        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                            AppText(
                                text = shown.title,
                                style = AppTheme.typography.headline,
                                color = AppTheme.colors.contentPrimary,
                            )
                            AppText(
                                text = shown.snippet,
                                style = AppTheme.typography.body,
                                color = AppTheme.colors.contentSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ MviScreen(viewModel = viewModel) { _, _ ->
    //~     PlaceholderScreen(title = R.string.search_title)
    //~ }
    // </opt-else:ui_components>
}
// <opt:ui_components>

// Never shown (see AppRedacted). A one-line title and a snippet of about three lines, as results have.
private val ResultPlaceholder = SearchResult(
    -1,
    "Placeholder result title",
    "A result's snippet runs to about three lines on a phone, and this placeholder is about as long, " +
        "so its bones take close to the room the real snippet will.",
)
// </opt:ui_components>
