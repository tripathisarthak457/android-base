package com.example.finalafter.crnoui3.feature.settings.licenses

import android.content.Context
import com.example.finalafter.crnoui3.core.common.LoadState
import com.example.finalafter.crnoui3.core.common.coroutines.AppDispatchers
import com.example.finalafter.crnoui3.core.common.log.Log
import com.example.finalafter.crnoui3.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.json.JSONArray

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
