package com.example.cmp.core.devtools.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.cmp.core.common.environment.AppEnvironment
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.button.ButtonSize
import com.example.cmp.core.designsystem.component.button.ButtonVariant
import com.example.cmp.core.designsystem.component.container.AppCard
import com.example.cmp.core.designsystem.component.container.AppDivider
import com.example.cmp.core.designsystem.component.feedback.AppStateView
import com.example.cmp.core.designsystem.component.feedback.AppStatusPill
import com.example.cmp.core.designsystem.component.list.AppRow
import com.example.cmp.core.designsystem.component.selection.AppSegmentedControl
import com.example.cmp.core.designsystem.component.text.AppMonoText
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.adaptive.WindowWidth
import com.example.cmp.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.devtools.DevTools
import com.example.cmp.core.devtools.log.NetworkExchange
import com.example.cmp.core.devtools.log.NetworkStats
import com.example.cmp.core.devtools.screen.CurrentScreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything the badge opens onto: the requests, what they cost, the feature flags, what this
 * build is, and the tabs other modules contribute. Full screen on a phone; a panel at the side
 * from an Expanded width, with the app still visible beside it. Back inside a request returns to
 * the list.
 *
 * @param initialTab the key of the tab to open on: a contributed page's key or a built-in tab's name.
 */
@Composable
internal fun DevToolsPanel(devTools: DevTools, initialTab: String?, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        // The panel draws its own background behind the system bars and pads its content inside
        // them, rather than leaving the platform's grey bands above and below.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val beside = AppTheme.windowClass.width >= WindowWidth.Expanded
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onClose) { detectTapGestures { onClose() } },
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .then(if (beside) Modifier.width(SidePanelWidth) else Modifier.fillMaxWidth())
                    .background(AppTheme.colors.background)
                    // Taps inside the panel stay in it rather than reaching the close gesture behind.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .safeDrawingPadding(),
            ) {
                PanelContent(devTools, initialTab, onClose)
            }
        }
    }
}

@Composable
private fun ColumnScope.PanelContent(devTools: DevTools, initialTab: String?, onClose: () -> Unit) {
    val exchanges by devTools.log.exchanges.collectAsStateWithLifecycle()
    val pages = devTools.pages
    val tabKeys = remember(pages) { PanelTab.entries.map { it.name } + pages.map { it.key } }
    val pageTitles = pages.map { stringResource(it.title) }
    // The same list instance while the titles hold, so the tab row skips the recomposition every new
    // request in the log brings.
    val tabTitles = remember(pageTitles) { PanelTabTitles + pageTitles }
    var tabKey by rememberSaveable { mutableStateOf(initialTab ?: PanelTab.entries.first().name) }
    val tab = tabKeys.indexOf(tabKey).coerceAtLeast(0)
    var inspectingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val inspecting = inspectingId?.let { id -> exchanges.firstOrNull { it.id == id } }

    val back = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = back,
        isBackEnabled = inspecting != null,
        onBackCompleted = {
            inspectingId =
                null
        },
    )

    PanelHeader(
        title = inspecting?.let { it.method + " " + it.path } ?: "Inspector",
        onBack = if (inspecting != null) ({ inspectingId = null }) else null,
        onClose = onClose,
    )

    if (inspecting != null) {
        ExchangeDetail(inspecting)
        return
    }

    AppSegmentedControl(
        options = tabTitles,
        selected = tab,
        onSelect = { tabKey = tabKeys[it] },
        modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
    )

    if (tab >= PanelTab.entries.size) {
        pages[tab - PanelTab.entries.size].content()
        return
    }
    when (PanelTab.entries[tab]) {
        PanelTab.Network -> RequestList(
            exchanges = exchanges,
            onSelect = { inspectingId = it.id },
            onClear = devTools.log::clear,
        )

        PanelTab.Stats -> StatsTab(exchanges)

        PanelTab.Flags -> FlagsTab(flags = devTools.flags, overrides = devTools.flagOverrides)

        PanelTab.Build -> BuildTab(devTools.environment, devTools.currentScreen)
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
                variant = ButtonVariant.Plain,
                size = ButtonSize.Small,
            )
        }
        AppText(
            text = title,
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AppButton(
            text = "Close",
            onClick = onClose,
            variant = ButtonVariant.Tinted,
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
        AppStateView(
            title = "No requests yet",
            message = "Anything the app sends will appear here, newest first.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        flingBehavior = AppScrollDefaults.flingBehavior(),
    ) {
        items(exchanges, key = { it.id }, contentType = { ExchangeContentType }) { exchange ->
            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppRow(
                    title = exchange.method + " " + exchange.path,
                    subtitle = listOf(
                        exchange.durationMillis.toString() + " ms",
                        formatBytes(exchange.responseBytes),
                        timeOf(exchange.startedAtMillis),
                    ).joinToString(" · "),
                    onClick = { onSelect(exchange) },
                    trailing = { StatusPill(exchange) },
                )
            }
        }

        item(key = ClearKey, contentType = ClearKey) {
            AppButton(
                text = "Clear",
                onClick = onClear,
                variant = ButtonVariant.Tinted,
                modifier = Modifier.padding(top = AppTheme.spacing.md).fillMaxWidth(),
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
private fun StatsTab(exchanges: List<NetworkExchange>) {
    // The median sorts every duration: once per new list, not again when the panel recomposes.
    val stats = remember(exchanges) { NetworkStats.of(exchanges) }
    Column(
        modifier = Modifier
            .verticalScroll(
                rememberScrollState(),
                flingBehavior = AppScrollDefaults.flingBehavior(),
            )
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        StatRow("Requests", stats.total.toString())
        StatRow(
            "Failed",
            stats.failures.toString() + " (" + (stats.failureRate * Percent).toInt() + "%)",
        )
        // The median rather than the mean: one thirty-second timeout drags an average somewhere
        // no real request ever went, and the number stops describing anything.
        StatRow("Median", stats.medianMillis.toString() + " ms")
        StatRow("Slowest", stats.slowestMillis.toString() + " ms")
        // Decoded: a compressed answer cost fewer bytes on the wire than this.
        StatRow("Received (decoded)", formatBytes(stats.bytesReceived))
    }
}

@Composable
private fun BuildTab(environment: AppEnvironment, currentScreen: CurrentScreen) {
    val screen by currentScreen.name.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .verticalScroll(
                rememberScrollState(),
                flingBehavior = AppScrollDefaults.flingBehavior(),
            )
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        StatRow("Environment", environment.name)
        StatRow("Version", environment.versionName)
        StatRow("Application id", environment.applicationId)
        StatRow("API", environment.apiBaseUrl)
        StatRow("Debuggable", environment.isDebugBuild.toString())
        screen?.let { StatRow("Screen", it) }
    }
}

@Composable
internal fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.callout,
            color = AppTheme.colors.contentSecondary,
            modifier = Modifier.weight(1f),
        )
        AppMonoText(
            text = value,
            color = AppTheme.colors.contentPrimary,
            maxLines = 2,
            modifier = Modifier.weight(WideColumnWeight),
        )
    }
}

/** One request in full. */
@Composable
private fun ExchangeDetail(exchange: NetworkExchange) {
    SelectionContainer {
        Column(
            modifier = Modifier
                .verticalScroll(
                    rememberScrollState(),
                    flingBehavior = AppScrollDefaults.flingBehavior(),
                )
                .padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppMonoText(
                text = exchange.url,
                color = AppTheme.colors.contentSecondary,
                maxLines = ManyLines,
            )
            StatRow("Status", exchange.status?.toString() ?: "no response")
            StatRow("Took", exchange.durationMillis.toString() + " ms")
            StatRow("Size (decoded)", formatBytes(exchange.responseBytes))
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
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.contentTertiary,
    )
    AppMonoText(
        text = body,
        color = AppTheme.colors.contentPrimary,
        maxLines = ManyLines,
    )
}

private fun Map<String, String>.asText(): String =
    entries.joinToString("\n") { (name, value) -> name + ": " + value }

/** Bytes below a kilobyte, so a small answer does not read as nothing at all. */
internal fun formatBytes(bytes: Long): String = when {
    bytes < BytesPerKb -> "$bytes B"

    bytes < BytesPerKb * BytesPerKb -> String.format(
        Locale.US,
        "%.1f kB",
        bytes / BytesPerKb.toDouble(),
    )

    else -> String.format(Locale.US, "%.1f MB", bytes / (BytesPerKb * BytesPerKb).toDouble())
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)

private fun timeOf(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(TimeFormat)

private enum class PanelTab(val title: String) {
    Network("Network"),
    Stats("Stats"),
    Flags("Flags"),
    Build("Build"),
}

private val PanelTabTitles = PanelTab.entries.map { it.title }
private const val ExchangeContentType = "exchange"
private const val ClearKey = "clear"
private const val BytesPerKb = 1024L
private const val Percent = 100
private const val ManyLines = 400
private const val WideColumnWeight = 2f
private val SidePanelWidth = 420.dp
