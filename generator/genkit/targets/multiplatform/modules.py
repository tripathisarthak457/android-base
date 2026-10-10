"""
The feature modules a spec names, scaffolded in the template's own shape: a data module (repository
and model in `api/`, its sources internal) and a feature module (key in `api/`, one list screen,
its Koin module), each registered in settings, in `:shared`, in Koin and as a tab.

An app with nothing to open on launch gets a `home` module, so every project starts on a screen of
its own rather than on settings or on nothing.
"""

from __future__ import annotations

from pathlib import Path

from ...core.spec import Spec, module_title, pascal_case

#: The module added when no feature would give the app a first tab.
HOME = "home"

#: Tab glyphs for scaffolded modules, outline and filled, handed out in order so no two tabs share
#: one. A module named `home` takes the house.
_TAB_ICONS: tuple[tuple[str, str], ...] = (
    ("Grid", "GridFilled"),
    ("Folder", "FolderFilled"),
    ("Calendar", "CalendarFilled"),
    ("Cart", "CartFilled"),
    ("Users", "UsersFilled"),
    ("Bookmark", "BookmarkFilled"),
    ("Tag", "TagFilled"),
    ("Package", "PackageFilled"),
    ("MapPin", "MapPinFilled"),
    ("Mail", "MailFilled"),
    ("Bell", "BellFilled"),
    ("Heart", "HeartFilled"),
)
_HOME_ICON = ("Home", "HomeFilled")


def modules(spec: Spec) -> list[str]:
    """The modules to scaffold: the spec's, or `home` when neither they nor `sample` give a first tab."""
    if spec.feature_modules:
        return list(spec.feature_modules)
    return [] if spec.has("sample") else [HOME]


def package_segment(name: str) -> str:
    """`order_history` → `orderhistory`: Kotlin packages are lower case words run together."""
    return name.replace("_", "")


def accessor(name: str) -> str:
    """`order_history` → `orderHistory`, the name Gradle's type-safe project accessors give it."""
    first, *rest = name.split("_")
    return first + "".join(part.capitalize() for part in rest)


def labels(spec: Spec) -> list[str]:
    """The tabs the bar shows, in order."""
    return (
        (["Home"] if spec.has("sample") else [])
        + [module_title(name) for name in modules(spec)]
        + (["Settings"] if spec.has("settings") else [])
    )


def notes(spec: Spec) -> list[str]:
    if not spec.feature_modules and not spec.has("sample"):
        return [f"No feature gives the app a first screen, so a '{HOME}' module was added as its first tab."]
    return []


def regions(spec: Spec) -> dict[str, list[str] | None]:
    names = modules(spec)
    pkg = spec.package_name
    icons = _icons(names)
    return {
        "settings-includes-data": [f'include(":data:{name}")\n' for name in names],
        "settings-includes-feature": [f'include(":feature:{name}")\n' for name in names],
        "shared-dependencies": [
            line
            for name in names
            for line in (
                f"            implementation(projects.data.{accessor(name)})\n",
                f"            implementation(projects.feature.{accessor(name)})\n",
            )
        ],
        "koin-imports": [
            line
            for name in names
            for line in (
                f"import {pkg}.data.{package_segment(name)}.di.{accessor(name)}DataModule\n",
                f"import {pkg}.feature.{package_segment(name)}.navigation.{accessor(name)}Module\n",
            )
        ],
        "koin-modules": [
            line
            for name in names
            for line in (f"    {accessor(name)}DataModule,\n", f"    {accessor(name)}Module,\n")
        ],
        "shell-tab-imports": [
            line
            for name in names
            for line in (
                f"import {pkg}.feature.{package_segment(name)}.api.{pascal_case(name)}ListKey\n",
                f"import {pkg}.shared.resources.tab_{name}\n",
            )
        ],
        "shell-tabs": [
            f"        ShellTab(\n"
            f"            key = {pascal_case(name)}ListKey,\n"
            f"            label = Res.string.tab_{name},\n"
            f"            icon = AppIcons.{icon},\n"
            f"            selectedIcon = AppIcons.{selected},\n"
            f"        ),\n"
            for name, (icon, selected) in zip(names, icons)
        ],
        "shell-tab-strings": [
            f'    <string name="tab_{name}" translatable="false">{_xml(module_title(name))}</string>\n'
            for name in names
        ],
    }


def write_feature_modules(project: Path, spec: Spec) -> None:
    for name in modules(spec):
        _Scaffold(project, spec, name).write()


def _icons(names: list[str]) -> list[tuple[str, str]]:
    others = iter(_TAB_ICONS)
    return [_HOME_ICON if name == HOME else next(others, _TAB_ICONS[0]) for name in names]


def _xml(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


class _Scaffold:
    """One module's files. Every name below is derived from the module name the person typed."""

    def __init__(self, project: Path, spec: Spec, name: str):
        self.project = project
        self.name = name
        self.pkg = spec.package_name
        self.segment = package_segment(name)
        self.pascal = pascal_case(name)
        self.camel = accessor(name)
        self.title = module_title(name)
        self.network = spec.has("network")
        self.package_path = spec.package_path

    def write(self) -> None:
        self._data_module()
        self._feature_module()

    def _source(self, tier: str, source_set: str = "commonMain") -> Path:
        return (
            self.project / tier / self.name / "src" / source_set / "kotlin" / self.package_path / tier / self.segment
        )

    def _data_module(self) -> None:
        root = self.project / "data" / self.name
        source = self._source("data")
        pkg, p, data = self.pkg, self.pascal, f"{self.pkg}.data.{self.segment}"

        dependencies = (
            "\nkotlin {\n    sourceSets {\n        commonMain.dependencies {\n"
            "            implementation(projects.core.network)\n        }\n    }\n}\n"
            if self.network
            else ""
        )
        _write(root / "build.gradle.kts", f'plugins {{\n    id("{pkg}.kmp.data")\n}}\n{dependencies}')

        _write(source / "api" / "model" / f"{p}Item.kt", f"""package {data}.api.model

/** One {self.title.lower()} item, as the app knows it. */
data class {p}Item(
    val id: Int,
    val title: String,
)
""")

        _write(source / "api" / f"{p}Repository.kt", f"""package {data}.api

import {pkg}.core.common.result.AppResult
import {data}.api.model.{p}Item
import kotlinx.coroutines.flow.Flow

/** Everything the app can ask about {self.title.lower()}. */
interface {p}Repository {{

    /** The items as last fetched, updated by every [refresh]. Empty before the first. */
    val items: Flow<List<{p}Item>>

    /** Fetches the items again; [items] follows. */
    suspend fun refresh(): AppResult<Unit>
}}
""")

        if self.network:
            implementation = f"Network{p}Repository"
            _write(source / f"{implementation}.kt", f"""package {data}

import {pkg}.core.common.result.AppResult
import {pkg}.core.common.result.map
import {data}.api.{p}Repository
import {data}.api.model.{p}Item
import {data}.remote.{p}Api
import {data}.remote.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The items from the network, with the last answer kept in memory for this process. */
internal class {implementation}(
    private val api: {p}Api,
) : {p}Repository {{

    private val fetched = MutableStateFlow<List<{p}Item>>(emptyList())

    override val items: Flow<List<{p}Item>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map {{ rows -> fetched.value = rows.map {{ it.toDomain() }} }}
}}
""")
            _write(source / "remote" / "dto" / f"{p}Dto.kt", f"""package {data}.remote.dto

import kotlinx.serialization.Serializable

/** One item as the backend sends it. */
@Serializable
internal data class {p}Dto(
    val id: Int,
    val title: String = "",
)
""")
            _write(source / "remote" / f"{p}DtoMappers.kt", f"""package {data}.remote

import {data}.api.model.{p}Item
import {data}.remote.dto.{p}Dto

internal fun {p}Dto.toDomain(): {p}Item = {p}Item(id = id, title = title.trim())
""")
            _write(source / "remote" / f"{p}Api.kt", f"""package {data}.remote

import {pkg}.core.common.result.AppResult
import {pkg}.core.network.client.NetworkClient
import {pkg}.core.network.client.get
import {data}.remote.dto.{p}Dto

/** The {self.title.lower()} endpoints, relative to the environment's API base URL. */
internal class {p}Api(
    private val client: NetworkClient,
) {{
    suspend fun items(): AppResult<List<{p}Dto>> = client.get(ITEMS_PATH)

    private companion object {{
        const val ITEMS_PATH = "{self.name}"
    }}
}}
""")
            bindings = f"    single {{ {p}Api(get()) }}\n    single<{p}Repository> {{ {implementation}(get()) }}\n"
            imports = f"import {data}.{implementation}\nimport {data}.api.{p}Repository\nimport {data}.remote.{p}Api\n"
        else:
            implementation = f"InMemory{p}Repository"
            _write(source / f"{implementation}.kt", f"""package {data}

import {pkg}.core.common.result.AppResult
import {data}.api.{p}Repository
import {data}.api.model.{p}Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Items held in memory for this process. The project has no network feature, so this is where a
 * data source of your choice plugs in.
 */
internal class {implementation} : {p}Repository {{

    private val stored = MutableStateFlow<List<{p}Item>>(emptyList())

    override val items: Flow<List<{p}Item>> = stored.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)
}}
""")
            bindings = f"    single<{p}Repository> {{ {implementation}() }}\n"
            imports = f"import {data}.{implementation}\nimport {data}.api.{p}Repository\n"

        _write(source / "di" / f"{p}DataModule.kt", f"""package {data}.di

{imports}import org.koin.dsl.module

val {self.camel}DataModule = module {{
{bindings}}}
""")

    def _feature_module(self) -> None:
        root = self.project / "feature" / self.name
        source = self._source("feature")
        tests = self._source("feature", "commonTest")
        pkg, p, name = self.pkg, self.pascal, self.name
        feature, data = f"{pkg}.feature.{self.segment}", f"{pkg}.data.{self.segment}"

        _write(root / "build.gradle.kts", f"""plugins {{
    id("{pkg}.kmp.feature")
}}

kotlin {{
    sourceSets {{
        commonMain.dependencies {{
            implementation(projects.data.{self.camel})
        }}
    }}
}}
""")

        _write(root / "src" / "commonMain" / "composeResources" / "values" / "strings.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="{name}_title" translatable="false">{_xml(self.title)}</string>
    <string name="{name}_empty_title">Nothing here yet</string>
    <string name="{name}_empty_message">When there is something to show, it will appear on this screen.</string>
    <string name="{name}_reload">Reload</string>
</resources>
""")

        _write(source / "api" / f"{p}Keys.kt", f"""package {feature}.api

import {pkg}.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/** The first screen of the {self.title} tab. */
@Serializable
data object {p}ListKey : AppNavKey
""")

        _write(source / "list" / f"{p}ListContract.kt", f"""package {feature}.list

import androidx.compose.runtime.Immutable
import {pkg}.core.common.LoadState
import {pkg}.core.mvi.UiEvent
import {pkg}.core.mvi.UiState
import {data}.api.model.{p}Item

@Immutable
internal data class {p}ListState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<{p}Item> = emptyList(),
) : UiState

internal sealed interface {p}ListEvent : UiEvent {{
    data object Appeared : {p}ListEvent
    data object Retry : {p}ListEvent
}}
""")

        _write(source / "list" / f"{p}ListViewModel.kt", f"""package {feature}.list

import {pkg}.core.common.LoadState
import {pkg}.core.common.result.AppResult
import {pkg}.core.mvi.MviViewModel
import {data}.api.{p}Repository
import kotlinx.coroutines.flow.first

internal class {p}ListViewModel(
    private val repository: {p}Repository,
) : MviViewModel<{p}ListState, {p}ListEvent, Nothing>({p}ListState()) {{

    init {{
        observe(repository.items) {{ items -> copy(items = items) }}
    }}

    override fun handleEvent(event: {p}ListEvent) {{
        when (event) {{
            {p}ListEvent.Appeared -> if (current.loadState == LoadState.Idle) load()
            {p}ListEvent.Retry -> load()
        }}
    }}

    private fun load() {{
        launchLatest(LOAD) {{
            setState {{ copy(loadState = if (items.isEmpty()) LoadState.Loading else LoadState.Refreshing) }}

            val result = repository.refresh()
            // Read back rather than waiting for the observer, so Empty is never shown before the rows land.
            val stored = repository.items.first()
            val outcome = when {{
                stored.isNotEmpty() -> LoadState.Success
                result is AppResult.Failure -> LoadState.Error(result.error)
                else -> LoadState.Empty
            }}
            setState {{ copy(loadState = outcome, items = stored) }}
        }}
    }}

    private companion object {{
        const val LOAD = "load"
    }}
}}
""")

        _write(source / "list" / f"{p}ListRoute.kt", f"""package {feature}.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import {pkg}.core.ui.MviScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun {p}ListRoute(viewModel: {p}ListViewModel = koinViewModel()) {{
    LaunchedEffect(Unit) {{ viewModel.onEvent({p}ListEvent.Appeared) }}

    MviScreen(viewModel = viewModel) {{ state, onEvent ->
        {p}ListScreen(state = state, onEvent = onEvent)
    }}
}}
""")

        r = f"{feature}.resources"
        _write(source / "list" / f"{p}ListScreen.kt", f"""package {feature}.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import {pkg}.core.common.LoadState
import {pkg}.core.common.hasContent
import {pkg}.core.common.result.AppError
import {pkg}.core.designsystem.component.feedback.AppSkeletonRows
import {pkg}.core.designsystem.component.feedback.AppStateView
import {pkg}.core.designsystem.component.feedback.StateKind
import {pkg}.core.designsystem.component.list.AppRow
import {pkg}.core.designsystem.component.navigation.AppLargeTitle
import {pkg}.core.designsystem.theme.AppTheme
import {pkg}.core.ui.text
import {data}.api.model.{p}Item
import {r}.Res
import {r}.{name}_empty_message
import {r}.{name}_empty_title
import {r}.{name}_reload
import {r}.{name}_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun {p}ListScreen(
    state: {p}ListState,
    onEvent: ({p}ListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {{
    val background = modifier
        .fillMaxSize()
        .background(AppTheme.colors.background)
    if (state.loadState.hasContent) {{
        LazyColumn(modifier = background, contentPadding = WindowInsets.statusBars.asPaddingValues()) {{
            item(key = "title", contentType = "title") {{ AppLargeTitle(title = stringResource(Res.string.{name}_title)) }}
            items(items = state.items, key = {p}Item::id, contentType = {{ "{name}" }}) {{ item ->
                AppRow(title = item.title, modifier = Modifier.animateItem())
            }}
        }}
        return
    }}

    Column(modifier = background.windowInsetsPadding(WindowInsets.statusBars)) {{
        AppLargeTitle(title = stringResource(Res.string.{name}_title))
        val placeholder = Modifier.weight(1f)
        when (val loadState = state.loadState) {{
            is LoadState.Error -> AppStateView(
                modifier = placeholder,
                kind = if (loadState.error == AppError.Offline) StateKind.Offline else StateKind.Error,
                message = loadState.error.text(),
                onAction = {{ onEvent({p}ListEvent.Retry) }},
            )

            LoadState.Empty -> AppStateView(
                modifier = placeholder,
                title = stringResource(Res.string.{name}_empty_title),
                message = stringResource(Res.string.{name}_empty_message),
                actionLabel = stringResource(Res.string.{name}_reload),
                onAction = {{ onEvent({p}ListEvent.Retry) }},
            )

            else -> AppSkeletonRows(count = SkeletonRows, modifier = placeholder.padding(top = AppTheme.spacing.xl))
        }}
    }}
}}

private const val SkeletonRows = 6

@Preview
@Composable
private fun {p}ListPreview() {{
    AppTheme {{
        val items = List(3) {{ {p}Item(id = it, title = "Item ${{it + 1}}") }}
        {p}ListScreen(state = {p}ListState(loadState = LoadState.Success, items = items), onEvent = {{}})
    }}
}}
""")

        _write(source / "navigation" / f"{p}Navigation.kt", f"""package {feature}.navigation

import {pkg}.core.navigation.NavGraphEntry
import {pkg}.core.navigation.navGraph
import {pkg}.core.navigation.navKeys
import {feature}.api.{p}ListKey
import {feature}.list.{p}ListRoute
import {feature}.list.{p}ListViewModel
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The {self.title} screens' ViewModels, their entries, and their keys' serializers. */
val {self.camel}Module = module {{
    viewModelOf(::{p}ListViewModel)
    single(named<{p}ListKey>()) {{ {self.camel}NavGraph() }}
    single(named<{p}ListKey>()) {{ {self.camel}NavKeys() }}
}}

private fun {self.camel}NavGraph(): NavGraphEntry = navGraph {{
    entry<{p}ListKey> {{ {p}ListRoute() }}
}}

private fun {self.camel}NavKeys(): SerializersModule = navKeys {{
    subclass({p}ListKey::class, {p}ListKey.serializer())
}}
""")

        _write(tests / "list" / f"{p}ListViewModelTest.kt", f"""package {feature}.list

import {pkg}.core.common.LoadState
import {pkg}.core.common.result.AppError
import {pkg}.core.common.result.AppResult
import {pkg}.core.testing.runViewModelTest
import {data}.api.{p}Repository
import {data}.api.model.{p}Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

class {p}ListViewModelTest {{

    @Test
    fun appeared_showsTheFetchedItems() = runViewModelTest {{
        val items = listOf({p}Item(id = 1, title = "First"))
        val viewModel = {p}ListViewModel(Fake{p}Repository(AppResult.Success(Unit), items))
        viewModel.state.launchIn(backgroundScope)

        viewModel.onEvent({p}ListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }}

    @Test
    fun appeared_withNothingToShow_isEmpty() = runViewModelTest {{
        val viewModel = {p}ListViewModel(Fake{p}Repository(AppResult.Success(Unit), emptyList()))
        viewModel.state.launchIn(backgroundScope)

        viewModel.onEvent({p}ListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }}

    @Test
    fun failedFetch_withNothingStored_showsTheError() = runViewModelTest {{
        val viewModel = {p}ListViewModel(Fake{p}Repository(AppResult.Failure(AppError.Offline), emptyList()))
        viewModel.state.launchIn(backgroundScope)

        viewModel.onEvent({p}ListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)
    }}
}}

private class Fake{p}Repository(
    private val result: AppResult<Unit>,
    private val fetched: List<{p}Item>,
) : {p}Repository {{

    private val stored = MutableStateFlow<List<{p}Item>>(emptyList())

    override val items: Flow<List<{p}Item>> = stored

    override suspend fun refresh(): AppResult<Unit> {{
        if (result is AppResult.Success) stored.value = fetched
        return result
    }}
}}
""")


def _write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
