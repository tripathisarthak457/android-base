package com.base.app.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.IoDispatcher
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject

/**
 * One dependency, as the build recorded it. The licence is a list because a library can offer a
 * choice of them, and showing all of them is more honest than showing the first.
 */
data class LicensedArtifact(
    val name: String,
    val coordinates: String,
    val licences: List<String>,
)

data class LicensesState(
    val loadState: LoadState = LoadState.Idle,
    val artifacts: List<LicensedArtifact> = emptyList(),
) : UiState

sealed interface LicensesEvent : UiEvent {
    data object Load : LicensesEvent
    data object BackClicked : LicensesEvent
}

sealed interface LicensesEffect : UiEffect {
    data object NavigateBack : LicensesEffect
}

/** Reads the licence list the build generated into assets. */
@HiltViewModel
class LicensesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MviViewModel<LicensesState, LicensesEvent, LicensesEffect>(LicensesState()) {

    init {
        onEvent(LicensesEvent.Load)
    }

    override suspend fun handleEvent(event: LicensesEvent) {
        when (event) {
            LicensesEvent.Load -> load()
            LicensesEvent.BackClicked -> emitEffect(LicensesEffect.NavigateBack)
        }
    }

    private suspend fun load() {
        updateState { copy(loadState = LoadState.Loading) }
        val artifacts = withContext(io) { read() }
        updateState {
            copy(
                loadState = if (artifacts.isEmpty()) LoadState.Empty else LoadState.Success,
                artifacts = artifacts,
            )
        }
    }

    private fun read(): List<LicensedArtifact> = runCatching {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val licences = item.optJSONArray("spdxLicenses")
            LicensedArtifact(
                name = item.optString("name").ifBlank { item.optString("artifactId") },
                coordinates = listOf(
                    item.optString("groupId"),
                    item.optString("artifactId"),
                    item.optString("version"),
                ).joinToString(":"),
                licences = (0 until (licences?.length() ?: 0)).mapNotNull {
                    licences?.getJSONObject(it)?.optString("identifier")
                },
            )
        }.sortedBy { it.name.lowercase() }
    }.onFailure {
        AppLogger.w("No licence list in assets: ${it.message}")
    }.getOrDefault(emptyList())

    private companion object {
        const val ASSET_NAME = "licenses.json"
    }
}

@Composable
fun LicensesRoute(
    navigator: AppNavigator,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                LicensesEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        LicensesScreen(state = state, onEvent = onEvent)
    }
}

@Composable
fun LicensesScreen(state: LicensesState, onEvent: (LicensesEvent) -> Unit) {
    AppScaffold(
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.settings_open_source_licences),
                onBack = { onEvent(LicensesEvent.BackClicked) },
            )
        },
    ) {
        if (state.loadState == LoadState.Empty) {
            AppEmptyState(
                title = stringResource(R.string.settings_no_licences_recorded),
                message = stringResource(R.string.settings_no_licences_explanation),
            )
            return@AppScaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(AppTheme.spacing.lg),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                AppTheme.spacing.sm,
            ),
        ) {
            items(state.artifacts, key = { it.coordinates }) { artifact ->
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppListItem(
                        title = artifact.name,
                        supporting = artifact.licences.joinToString()
                            .ifBlank { "Licence not stated" },
                    )
                }
            }
        }
    }
}
