package com.base.app.feature.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.LoadState
import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.log.Log
import com.base.app.core.mvi.MviViewModel
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
// <opt:ui_components>
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppStateView
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.foundation.layout.ReportScrollEdge
import com.base.app.core.designsystem.foundation.layout.chromeContentPadding
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.theme.AppTheme
// </opt:ui_components>
// <opt-else:ui_components>
//~ import com.base.app.core.ui.PlaceholderScreen
// </opt-else:ui_components>

/**
 * One dependency, as the build recorded it. The licence is a list because a library can offer a
 * choice of them, and showing all of them is more honest than showing the first.
 */
internal data class LicensedArtifact(
    val name: String,
    val coordinates: String,
    val licences: List<String>,
)

internal data class LicensesState(
    val loadState: LoadState = LoadState.Idle,
    val artifacts: List<LicensedArtifact> = emptyList(),
) : UiState

internal sealed interface LicensesEvent : UiEvent {
    data object BackClicked : LicensesEvent
}

internal sealed interface LicensesEffect : UiEffect {
    data object NavigateBack : LicensesEffect
}

/** Reads the licence list the build generated into assets. */
@HiltViewModel
internal class LicensesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : MviViewModel<LicensesState, LicensesEvent, LicensesEffect>(LicensesState()) {

    init {
        onFirstObserved { load() }
    }

    override fun handleEvent(event: LicensesEvent) {
        when (event) {
            LicensesEvent.BackClicked -> emit(LicensesEffect.NavigateBack)
        }
    }

    private fun load() {
        launchLatest(LOAD) {
            setState { copy(loadState = LoadState.Loading) }
            val artifacts = withContext(dispatchers.io) { read() }
            setState {
                copy(
                    loadState = if (artifacts.isEmpty()) LoadState.Empty else LoadState.Success,
                    artifacts = artifacts,
                )
            }
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
        Log.w("No licence list in assets: ${it.message}")
    }.getOrDefault(emptyList())

    private companion object {
        const val ASSET_NAME = "licenses.json"
        const val LOAD = "load"
    }
}

@Composable
internal fun LicensesRoute(
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
    // <opt:ui_components>
    ) { state, onEvent ->
        LicensesScreen(state = state, onEvent = onEvent)
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ ) { _, _ ->
    //~     PlaceholderScreen(title = R.string.settings_open_source_licences)
    //~ }
    // </opt-else:ui_components>
}
// <opt:ui_components>

@Composable
internal fun LicensesScreen(state: LicensesState, onEvent: (LicensesEvent) -> Unit) {
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_open_source_licences),
                onBack = { onEvent(LicensesEvent.BackClicked) },
            )
        },
    ) {
        if (state.loadState == LoadState.Empty) {
            AppStateView(
                title = stringResource(R.string.settings_no_licences_recorded),
                message = stringResource(R.string.settings_no_licences_explanation),
            )
            return@AppScaffold
        }

        val listState = rememberLazyListState()
        ReportScrollEdge(listState)
        LazyColumn(
            state = listState,
            contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.lg)),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                AppTheme.spacing.sm,
            ),
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) {
            items(state.artifacts, key = { it.coordinates }) { artifact ->
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppRow(
                        title = artifact.name,
                        subtitle = artifact.licences.joinToString()
                            .ifBlank { stringResource(R.string.settings_licence_not_stated) },
                    )
                }
            }
        }
    }
}
// </opt:ui_components>
