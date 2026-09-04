"""
Scaffolding for the feature modules a user names at generation time.

Each name produces a matching `:data:<name>` and `:feature:<name>` pair, shaped exactly like the
reference feature: a repository behind an interface, an MVI contract, a ViewModel, a stateless
screen, a navigation key and the two Hilt contributions that register it.

The point is not to save typing — it is that the first feature somebody writes in a new project
sets the pattern for every feature after it, and a scaffold that already does the right thing is
a far more reliable way to establish that than a paragraph in a README.
"""

from __future__ import annotations

from pathlib import Path

from .spec import ProjectSpec


def pascal(name: str) -> str:
    """`order_history` → `OrderHistory`."""
    return "".join(part.capitalize() for part in name.split("_") if part)


def title(name: str) -> str:
    """`order_history` → `Order history`. What the scaffolded screen shows in its top bar."""
    words = [part for part in name.split("_") if part]
    return " ".join(words).capitalize() if words else name


def generated_blocks(spec: ProjectSpec) -> dict[str, list[str]]:
    """
    The content for every `<generated:…>` marker in the template.

    Returned as lists of lines including their newlines, because the marker substitution splices
    them directly into the surrounding file.
    """
    data_includes = [f'include(":data:{name}")\n' for name in spec.feature_modules]
    feature_includes = [f'include(":feature:{name}")\n' for name in spec.feature_modules]
    app_dependencies = [
        f'    implementation(project(":feature:{name}"))\n' for name in spec.feature_modules
    ]

    # Every named feature becomes a tab, in the order the user typed them — so the first one is
    # both the first tab and, by AppDestinations' own rule, the start destination.
    shell_tabs = [
        f'        ShellTab(key = {pascal(name)}ListKey, label = "{title(name)}", '
        f"icon = AppIcons.Grid),\n"
        for name in spec.feature_modules
    ]
    start_import = [
        f"import {spec.package_name}.feature.{name}.{pascal(name)}ListKey\n"
        for name in spec.feature_modules
    ]

    # `start` is `tabs.firstOrNull()?.key`, which is nullable however many tabs there are — so the
    # elvis is not a fallback for an empty list, it is what gives the property its type. The
    # template carries one behind an `<opt:sample>` block; this replaces it when that goes.
    start_destination: list[str] = []
    if not spec.has("sample"):
        if spec.feature_modules:
            fallback = f"{pascal(spec.feature_modules[0])}ListKey"
        elif spec.has("settings"):
            fallback = "SettingsKey"
        else:
            fallback = 'error("Set a start destination: name your first feature\'s nav key here.")'
        start_destination = [f"        ?: {fallback}\n"]

    return {
        "data-modules": data_includes,
        "feature-modules": feature_includes,
        "app-feature-dependencies": app_dependencies,
        "shell-tabs": shell_tabs,
        "start-destination-import": start_import,
        "start-destination": start_destination,
    }


def write_feature_modules(destination: Path, spec: ProjectSpec) -> None:
    for name in spec.feature_modules:
        _write_data_module(destination, spec, name)
        _write_feature_module(destination, spec, name)


# ─────────────────────────────────────────────────────────────────────────────
# :data:<name>
# ─────────────────────────────────────────────────────────────────────────────


def _write_data_module(destination: Path, spec: ProjectSpec, name: str) -> None:
    pkg = spec.package_name
    root = destination / "data" / name
    source = root / "src/main/kotlin" / spec.package_path / "data" / name

    _write(root / "build.gradle.kts", f"""plugins {{
    id("{pkg}.android.data")
}}

android {{
    namespace = "{pkg}.data.{name}"
}}
""")

    class_name = pascal(name)
    if spec.has("network"):
        repository_impl = f"""@Singleton
class Default{class_name}Repository @Inject constructor(
    private val networkClient: NetworkClient,
) : {class_name}Repository {{

    override suspend fun items(): AppResult<List<{class_name}Item>> =
        networkClient.get<List<{class_name}Item>>(path = ITEMS_PATH)

    private companion object {{
        const val ITEMS_PATH = "{name}"
    }}
}}"""
        imports = f"""import {pkg}.core.common.AppResult
import {pkg}.core.network.NetworkClient
import {pkg}.core.network.get
import javax.inject.Inject
import javax.inject.Singleton"""
    else:
        repository_impl = f"""@Singleton
class Default{class_name}Repository @Inject constructor() : {class_name}Repository {{

    override suspend fun items(): AppResult<List<{class_name}Item>> =
        AppResult.Success(emptyList())
}}"""
        imports = f"""import {pkg}.core.common.AppResult
import javax.inject.Inject
import javax.inject.Singleton"""

    _write(source / f"{class_name}Repository.kt", f"""package {pkg}.data.{name}

{imports}

/**
 * The domain model for {title(name)}.
 *
 * Lives here rather than in `:core:model` because it belongs to this domain. Keep the wire format
 * separate the moment the two diverge: add a DTO with a mapper, and the backend renaming a field
 * stops being a change to every screen.
 */
data class {class_name}Item(
    val id: Int,
    val title: String,
)

/**
 * Everything the app can ask about {title(name)}.
 *
 * An interface so a ViewModel test injects a fake and never opens a socket.
 */
interface {class_name}Repository {{

    suspend fun items(): AppResult<List<{class_name}Item>>
}}

{repository_impl}
""")

    _write(source / "di" / f"{class_name}DataModule.kt", f"""package {pkg}.data.{name}.di

import {pkg}.data.{name}.Default{class_name}Repository
import {pkg}.data.{name}.{class_name}Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface {class_name}DataModule {{

    @Binds
    fun bind{class_name}Repository(impl: Default{class_name}Repository): {class_name}Repository
}}
""")


# ─────────────────────────────────────────────────────────────────────────────
# :feature:<name>
# ─────────────────────────────────────────────────────────────────────────────


def _write_feature_module(destination: Path, spec: ProjectSpec, name: str) -> None:
    pkg = spec.package_name
    class_name = pascal(name)
    screen_title = title(name)
    root = destination / "feature" / name
    source = root / "src/main/kotlin" / spec.package_path / "feature" / name
    tests = root / "src/test/kotlin" / spec.package_path / "feature" / name

    _write(root / "build.gradle.kts", f"""plugins {{
    id("{pkg}.android.feature")
}}

android {{
    namespace = "{pkg}.feature.{name}"
}}

dependencies {{
    implementation(project(":data:{name}"))
}}
""")

    _write(source / f"{class_name}NavKeys.kt", f"""package {pkg}.feature.{name}

import {pkg}.core.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations. Owned entirely by this module — nothing in `:core:navigation` or
 * `:app` names them.
 *
 * Keys carry ids, never models: a key is serialised into the saved-state bundle, so one holding a
 * whole object both bloats the bundle and goes stale the moment the app is backgrounded.
 */
@Serializable
data object {class_name}ListKey : AppNavKey

@Serializable
data class {class_name}DetailKey(val itemId: Int) : AppNavKey
""")

    _write(source / f"{class_name}Contract.kt", f"""package {pkg}.feature.{name}

import androidx.compose.runtime.Immutable
import {pkg}.core.common.mvi.LoadState
import {pkg}.core.common.mvi.UiEffect
import {pkg}.core.common.mvi.UiEvent
import {pkg}.core.common.mvi.UiState
import {pkg}.data.{name}.{class_name}Item

@Immutable
data class {class_name}State(
    val loadState: LoadState = LoadState.Idle,
    val items: List<{class_name}Item> = emptyList(),
) : UiState

sealed interface {class_name}Event : UiEvent {{
    data object Load : {class_name}Event
    data object Retry : {class_name}Event
    data class ItemClicked(val id: Int) : {class_name}Event
}}

sealed interface {class_name}Effect : UiEffect {{
    data class OpenDetail(val id: Int) : {class_name}Effect
}}
""")

    _write(source / f"{class_name}ViewModel.kt", f"""package {pkg}.feature.{name}

import {pkg}.core.common.AppResult
import {pkg}.core.common.mvi.LoadState
import {pkg}.core.common.mvi.MviViewModel
import {pkg}.data.{name}.{class_name}Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Loads on creation rather than from a `LaunchedEffect` in the composable: `init` runs once per
 * ViewModel, which is once per screen instance, where a `LaunchedEffect(Unit)` re-runs whenever
 * the composable re-enters the composition.
 */
@HiltViewModel
class {class_name}ViewModel @Inject constructor(
    private val repository: {class_name}Repository,
) : MviViewModel<{class_name}State, {class_name}Event, {class_name}Effect>({class_name}State()) {{

    init {{
        onEvent({class_name}Event.Load)
    }}

    override suspend fun handleEvent(event: {class_name}Event) {{
        when (event) {{
            {class_name}Event.Load, {class_name}Event.Retry -> load()
            is {class_name}Event.ItemClicked -> emitEffect({class_name}Effect.OpenDetail(event.id))
        }}
    }}

    private suspend fun load() {{
        updateState {{ copy(loadState = LoadState.Loading) }}

        when (val result = repository.items()) {{
            is AppResult.Success -> updateState {{
                copy(
                    loadState = if (result.data.isEmpty()) LoadState.Empty else LoadState.Success,
                    items = result.data,
                )
            }}

            is AppResult.Failure -> updateState {{ copy(loadState = result.toLoadState()) }}
        }}
    }}
}}
""")

    _write(source / f"{class_name}Screen.kt", f"""package {pkg}.feature.{name}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import {pkg}.core.common.mvi.LoadState
import {pkg}.core.designsystem.component.container.AppCard
import {pkg}.core.designsystem.component.container.AppScaffold
import {pkg}.core.designsystem.component.feedback.AppEmptyState
import {pkg}.core.designsystem.component.feedback.AppErrorState
import {pkg}.core.designsystem.component.feedback.AppSkeletonListItem
import {pkg}.core.designsystem.component.navigation.AppLargeTitle
import {pkg}.core.designsystem.component.text.AppText
import {pkg}.core.designsystem.theme.AppTheme
import {pkg}.core.ui.asString
import {pkg}.data.{name}.{class_name}Item

/**
 * Stateless: it takes a state and emits events, and holds nothing of its own. That is what makes
 * the previews below work without a ViewModel, a network call, or a device.
 */
@Composable
fun {class_name}Screen(
    state: {class_name}State,
    onEvent: ({class_name}Event) -> Unit,
    modifier: Modifier = Modifier,
) {{
    AppScaffold(
        modifier = modifier,
        topBar = {{ AppLargeTitle(title = "{screen_title}") }},
    ) {{
        when (val loadState = state.loadState) {{
            is LoadState.Error -> AppErrorState(
                message = loadState.message.asString(),
                isOffline = loadState.isOffline,
                onRetry = {{ onEvent({class_name}Event.Retry) }},
            )

            LoadState.Empty -> AppEmptyState(
                title = "Nothing here yet",
                message = "When there is something to show, it appears on this screen.",
                actionLabel = "Reload",
                onAction = {{ onEvent({class_name}Event.Retry) }},
            )

            LoadState.Loading, LoadState.Idle -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            ) {{
                items(SKELETON_ROWS) {{ AppCard {{ AppSkeletonListItem(showLeading = false) }} }}
            }}

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            ) {{
                // Keyed on the item's own id: without a stable key, inserting a row at the top
                // re-maps every item to a different slot and loses the scroll position.
                items(items = state.items, key = {class_name}Item::id) {{ item ->
                    AppCard(
                        onClick = {{ onEvent({class_name}Event.ItemClicked(item.id)) }},
                        modifier = Modifier.fillMaxWidth(),
                    ) {{
                        AppText(
                            text = item.title,
                            style = AppTheme.typography.titleLarge,
                            color = AppTheme.colors.contentPrimary,
                        )
                    }}
                }}
            }}
        }}
    }}
}}

private const val SKELETON_ROWS = 6

@Preview(showBackground = true)
@Composable
private fun {class_name}ScreenPreview() {{
    AppTheme {{
        {class_name}Screen(
            state = {class_name}State(
                loadState = LoadState.Success,
                items = List(3) {{ {class_name}Item(id = it, title = "Row ${{it + 1}}") }},
            ),
            onEvent = {{}},
        )
    }}
}}
""")

    _write(source / f"{class_name}Route.kt", f"""package {pkg}.feature.{name}

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import {pkg}.core.navigation.AppNavigator
import {pkg}.core.ui.MviScreen

/**
 * Where the ViewModel, the screen and navigation meet. Kept apart from the screen so the screen
 * stays free of Hilt and of the navigator, and therefore previewable.
 */
@Composable
fun {class_name}ListRoute(
    navigator: AppNavigator,
    viewModel: {class_name}ViewModel = hiltViewModel(),
) {{
    MviScreen(
        viewModel = viewModel,
        onEffect = {{ effect ->
            when (effect) {{
                is {class_name}Effect.OpenDetail -> navigator.navigate({class_name}DetailKey(effect.id))
            }}
        }},
    ) {{ state, onEvent ->
        {class_name}Screen(state = state, onEvent = onEvent)
    }}
}}
""")

    _write(source / "di" / f"{class_name}NavModule.kt", f"""package {pkg}.feature.{name}.di

import {pkg}.core.navigation.AppNavigator
import {pkg}.core.navigation.NavGraphEntry
import {pkg}.core.navigation.navGraph
import {pkg}.core.navigation.navKeys
import {pkg}.feature.{name}.{class_name}DetailKey
import {pkg}.feature.{name}.{class_name}ListKey
import {pkg}.feature.{name}.{class_name}ListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the graph: what to render, and how to serialise its keys so the back
 * stack survives process death.
 *
 * Forgetting the second half is the mistake worth knowing about — the app works perfectly until
 * it is killed in the background, then comes back at the start destination.
 *
 * `{class_name}DetailKey` has no screen registered yet. Add one with `entry<{class_name}DetailKey> {{ … }}`
 * when you build the detail screen; until then, navigating to it fails loudly rather than
 * silently rendering nothing.
 */
@Module
@InstallIn(SingletonComponent::class)
object {class_name}NavModule {{

    @Provides
    @IntoSet
    fun {name}NavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {{
        entry<{class_name}ListKey> {{ {class_name}ListRoute(navigator = navigator) }}
    }}

    @Provides
    @IntoSet
    fun {name}NavKeys(): SerializersModule = navKeys {{
        subclass({class_name}ListKey::class, {class_name}ListKey.serializer())
        subclass({class_name}DetailKey::class, {class_name}DetailKey.serializer())
    }}
}}
""")

    _write(tests / f"{class_name}ViewModelTest.kt", f"""package {pkg}.feature.{name}

import {pkg}.core.common.AppResult
import {pkg}.core.common.mvi.LoadState
import {pkg}.core.testing.MainDispatcherRule
import {pkg}.data.{name}.{class_name}Item
import {pkg}.data.{name}.{class_name}Repository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class {class_name}ViewModelTest {{

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads on creation`() = runTest {{
        val items = listOf({class_name}Item(1, "One"))
        val viewModel = {class_name}ViewModel(Fake{class_name}Repository(AppResult.Success(items)))

        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }}

    @Test
    fun `an empty response is Empty, not Success with no rows`() = runTest {{
        val viewModel = {class_name}ViewModel(Fake{class_name}Repository(AppResult.Success(emptyList())))

        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }}

    @Test
    fun `a failure surfaces as an error state`() = runTest {{
        val viewModel = {class_name}ViewModel(
            Fake{class_name}Repository(AppResult.Failure(message = "Nope")),
        )

        advanceUntilIdle()

        assertTrue(viewModel.state.value.loadState is LoadState.Error)
    }}
}}

private class Fake{class_name}Repository(
    private val result: AppResult<List<{class_name}Item>>,
) : {class_name}Repository {{
    override suspend fun items(): AppResult<List<{class_name}Item>> = result
}}
""")


def _write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
