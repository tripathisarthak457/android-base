package com.base.app.feature.settings.licenses

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.base.app.core.common.LoadState
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppStateView
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.foundation.layout.ReportScrollEdge
import com.base.app.core.designsystem.foundation.layout.chromeContentPadding
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.feature.settings.R

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
