package com.base.app.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.designsystem.animation.AppAppear
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppErrorState
import com.base.app.core.designsystem.component.feedback.AppSkeletonLine
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.core.ui.asString
import com.base.app.data.search.SearchRepository
import com.base.app.data.search.SearchResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@Immutable
data class SearchResultState(
    val loadState: LoadState = LoadState.Loading,
    val result: SearchResult? = null,
) : UiState

sealed interface SearchResultEvent : UiEvent {
    data object Retry : SearchResultEvent
}

sealed interface SearchResultEffect : UiEffect

@HiltViewModel(assistedFactory = SearchResultViewModel.Factory::class)
class SearchResultViewModel @AssistedInject constructor(
    private val repository: SearchRepository,
    @Assisted private val id: Int,
) : MviViewModel<SearchResultState, SearchResultEvent, SearchResultEffect>(SearchResultState()) {

    @AssistedFactory
    interface Factory {
        fun create(id: Int): SearchResultViewModel
    }

    init {
        onEvent(SearchResultEvent.Retry)
    }

    override suspend fun handleEvent(event: SearchResultEvent) {
        when (event) {
            SearchResultEvent.Retry -> {
                updateState { copy(loadState = LoadState.Loading) }
                when (val result = repository.result(id)) {
                    is AppResult.Success -> updateState { copy(loadState = LoadState.Success, result = result.data) }
                    is AppResult.Failure -> updateState { copy(loadState = result.toLoadState()) }
                }
            }
        }
    }
}

@Composable
fun SearchResultRoute(
    id: Int,
    navigator: AppNavigator,
    viewModel: SearchResultViewModel = hiltViewModel<SearchResultViewModel, SearchResultViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) },
    ),
) {
    MviScreen(viewModel = viewModel) { state, onEvent ->
        AppScaffold(
            topBar = {
                AppBackTopBar(title = stringResource(R.string.search_title), onBack = navigator::navigateUp)
            },
        ) {
            when (val loadState = state.loadState) {
                is LoadState.Error -> AppErrorState(
                    message = loadState.message.asString(),
                    isOffline = loadState.isOffline,
                    onRetry = { onEvent(SearchResultEvent.Retry) },
                    retryLabel = stringResource(R.string.search_retry),
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AppTheme.spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    val result = state.result
                    if (result == null) {
                        AppSkeletonLine(widthFraction = 0.8f)
                        AppSkeletonLine()
                        AppSkeletonLine(widthFraction = 0.6f)
                    } else {
                        AppAppear {
                            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                                AppText(
                                    text = result.title,
                                    style = AppTheme.typography.headingMedium,
                                    color = AppTheme.colors.contentPrimary,
                                )
                                AppText(
                                    text = result.snippet,
                                    style = AppTheme.typography.bodyLarge,
                                    color = AppTheme.colors.contentSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
