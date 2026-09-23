package com.base.app.core.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.component.feedback.AppStatusPill
import com.base.app.core.designsystem.component.selection.AppSegmentedControl
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything the badge opens onto: the requests, what they cost, and what this build is.
 *
 * A dialog rather than a navigation destination, deliberately. The inspector has to be reachable
 * from any screen including one mid-flow, and pushing it onto the back stack would put a
 * debug-only entry inside the app's own navigation — which is then a thing to strip before a
 * release, and therefore a thing to get wrong.
 */
@Composable
fun DevToolsPanel(
    environment: DevEnvironment,
    log: DevToolsLog,
    onClose: () -> Unit,
) {
    val exchanges by log.exchanges.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var inspecting by remember { mutableStateOf<NetworkExchange?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .safeDrawingPadding(),
        ) {
            val current = inspecting

            PanelHeader(
                title = current?.let { it.method + " " + it.path } ?: "Inspector",
                onBack = if (current != null) ({ inspecting = null }) else null,
                onClose = onClose,
            )

            if (current != null) {
                ExchangeDetail(current)
                return@Column
            }

            AppSegmentedControl(
                options = TABS,
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
            )

            when (tab) {
                0 -> RequestList(
                    exchanges = exchanges,
                    onSelect = { inspecting = it },
                    onClear = log::clear,
                )

                1 -> StatsTab(NetworkStats.of(exchanges))
                else -> BuildTab(environment)
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, onBack: (() -> Unit)?, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        if (onBack != null) {
            AppButton(
                text = "Back",
                onClick = onBack,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Small,
            )
        }
        AppText(
            text = title,
            style = AppTheme.typography.headingSmall,
            color = AppTheme.colors.contentPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AppButton(
            text = "Close",
            onClick = onClose,
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Small,
        )
    }
}

@Composable
private fun RequestList(
    exchanges: List<NetworkExchange>,
    onSelect: (NetworkExchange) -> Unit,
    onClear: () -> Unit,
) {
    if (exchanges.isEmpty()) {
        AppEmptyState(
            title = "No requests yet",
            message = "Anything the app sends will appear here, newest first.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        items(exchanges, key = { it.id }) { exchange ->
            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppListItem(
                    title = exchange.method + "  " + exchange.path,
                    supporting = exchange.durationMillis.toString() + " ms  ·  " +
                        (exchange.responseBytes / BYTES_PER_KB).toString() + " kB  ·  " +
                        timeOf(exchange.startedAtMillis),
                    onClick = { onSelect(exchange) },
                    trailing = { StatusPill(exchange) },
                )
            }
        }

        item {
            AppButton(
                text = "Clear",
                onClick = onClear,
                variant = ButtonVariant.Secondary,
                fillWidth = true,
                modifier = Modifier.padding(top = AppTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun StatusPill(exchange: NetworkExchange) {
    val colors = when {
        exchange.failure != null -> AppTheme.colors.danger
        exchange.isFailure -> AppTheme.colors.warning
        else -> AppTheme.colors.success
    }
    AppStatusPill(text = exchange.status?.toString() ?: "ERR", status = colors)
}

@Composable
private fun StatsTab(stats: NetworkStats) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        StatRow("Requests", stats.total.toString())
        StatRow(
            "Failed",
            stats.failures.toString() + "  (" + (stats.failureRate * PERCENT).toInt() + "%)",
        )
        // The median rather than the mean: one thirty-second timeout drags an average somewhere
        // no real request ever went, and the number stops describing anything.
        StatRow("Median", stats.medianMillis.toString() + " ms")
        StatRow("Slowest", stats.slowestMillis.toString() + " ms")
        StatRow("Received", (stats.bytesReceived / BYTES_PER_KB).toString() + " kB")
    }
}

@Composable
private fun BuildTab(environment: DevEnvironment) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        StatRow("Environment", environment.name)
        StatRow("Version", environment.versionName)
        StatRow("Application id", environment.applicationId)
        StatRow("API", environment.apiBaseUrl)
        StatRow("Debuggable", environment.isDebugBuild.toString())
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.contentSecondary,
            modifier = Modifier.weight(1f),
        )
        AppMonoText(
            text = value,
            color = AppTheme.colors.contentPrimary,
            maxLines = 2,
            modifier = Modifier.weight(WIDE_COLUMN_WEIGHT),
        )
    }
}

/**
 * One request in full.
 *
 * Wrapped in a selection container because the reason to read a response body on a device is
 * almost always to get part of it into a bug report, and a body nobody can copy has to be
 * retyped by hand off a screen.
 */
@Composable
private fun ExchangeDetail(exchange: NetworkExchange) {
    SelectionContainer {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppMonoText(
                text = exchange.url,
                color = AppTheme.colors.contentSecondary,
                maxLines = MANY_LINES,
            )
            StatRow("Status", exchange.status?.toString() ?: "no response")
            StatRow("Took", exchange.durationMillis.toString() + " ms")
            exchange.failure?.let { StatRow("Failure", it) }

            Section("Request headers", exchange.requestHeaders.asText())
            Section("Request body", exchange.requestBody)
            AppDivider()
            Section("Response headers", exchange.responseHeaders.asText())
            Section("Response body", exchange.responseBody)
        }
    }
}

@Composable
private fun Section(title: String, body: String?) {
    if (body.isNullOrBlank()) return
    AppText(
        text = title,
        style = AppTheme.typography.labelSmall,
        color = AppTheme.colors.contentTertiary,
    )
    AppMonoText(
        text = body,
        color = AppTheme.colors.contentPrimary,
        maxLines = MANY_LINES,
    )
}

private fun Map<String, String>.asText(): String =
    entries.joinToString("\n") { (name, value) -> name + ": " + value }

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)

private fun timeOf(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(TimeFormat)

private val TABS = listOf("Network", "Stats", "Build")
private const val BYTES_PER_KB = 1024
private const val PERCENT = 100
private const val MANY_LINES = 400
private const val WIDE_COLUMN_WEIGHT = 2f
