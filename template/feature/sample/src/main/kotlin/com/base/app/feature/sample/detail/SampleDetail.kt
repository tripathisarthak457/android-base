package com.base.app.feature.sample.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppErrorState
import com.base.app.core.designsystem.component.feedback.AppSkeletonLine
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.asString
import com.base.app.data.sample.SampleItem
import com.base.app.data.sample.SampleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@Immutable
data class SampleDetailState(
    val loadState: LoadState = LoadState.Idle,
    val item: SampleItem? = null,
) : UiState

sealed interface SampleDetailEvent : UiEvent {
    data object Retry : SampleDetailEvent
    data object BackClicked : SampleDetailEvent
}

sealed interface SampleDetailEffect : UiEffect {
    data object NavigateBack : SampleDetailEffect
}

/**
 * The detail ViewModel.
 *
 * The item id arrives through assisted injection, at construction, from the navigation key. Each
 * destination owns its ViewModel, so there is exactly one of these per open detail screen and it
 * never has to be told which item it is for after the fact.
 */
@HiltViewModel(assistedFactory = SampleDetailViewModel.Factory::class)
class SampleDetailViewModel @AssistedInject constructor(
    private val repository: SampleRepository,
    @Assisted private val itemId: Int,
) : MviViewModel<SampleDetailState, SampleDetailEvent, SampleDetailEffect>(SampleDetailState()) {

    @AssistedFactory
    interface Factory {
        fun create(itemId: Int): SampleDetailViewModel
    }

    init {
        onEvent(SampleDetailEvent.Retry)
    }

    override suspend fun handleEvent(event: SampleDetailEvent) {
        when (event) {
            SampleDetailEvent.Retry -> load()
            SampleDetailEvent.BackClicked -> emitEffect(SampleDetailEffect.NavigateBack)
        }
    }

    private suspend fun load() {
        updateState { copy(loadState = LoadState.Loading) }

        when (val result = repository.item(itemId)) {
            is AppResult.Success -> updateState {
                copy(loadState = LoadState.Success, item = result.data)
            }

            is AppResult.Failure -> updateState { copy(loadState = result.toLoadState()) }
        }
    }
}

@Composable
fun SampleDetailScreen(
    state: SampleDetailState,
    onEvent: (SampleDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            AppBackTopBar(
                title = state.item?.title ?: "Detail",
                onBack = { onEvent(SampleDetailEvent.BackClicked) },
            )
        },
    ) {
        when (val loadState = state.loadState) {
            is LoadState.Error -> AppErrorState(
                message = loadState.message.asString(),
                isOffline = loadState.isOffline,
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
            }
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
            ),
            onEvent = {},
        )
    }
}
