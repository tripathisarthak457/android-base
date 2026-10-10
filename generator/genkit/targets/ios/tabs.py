"""The app's tabs: `BaseApp/AppTabs.swift`, written whole, and the app's own String Catalog."""

from __future__ import annotations

from dataclasses import dataclass

from ...core.spec import Spec, module_title, pascal_case

#: Symbols for scaffolded tabs, in the order modules take them: none is one the template's own tabs
#: use, so no two tabs look alike.
_MODULE_SYMBOLS = (
    "square.grid.2x2", "list.bullet.rectangle", "star", "bookmark", "chart.bar", "tray.full",
    "calendar", "person.2", "cart", "map", "bell", "folder",
)

#: The strings the app target itself shows besides the tab titles: the menu that holds the tab
#: shortcuts.
_APP_STRINGS = ("Navigate",)

#: The module scaffolded when nothing else gives the app a tab, since a tab view needs one.
FALLBACK_MODULE = "home"


@dataclass(frozen=True)
class Tab:
    case: str
    raw_value: str
    title: str
    symbol: str
    import_name: str
    root: tuple[str, ...]


def tab_modules(spec: Spec) -> tuple[str, ...]:
    """The feature modules to scaffold: the user's, or `home` when no feature makes a tab."""
    if spec.feature_modules or spec.has("sample") or spec.has("settings"):
        return spec.feature_modules
    return (FALLBACK_MODULE,)


def tabs(spec: Spec) -> list[Tab]:
    """The tabs in bar order: Home, then the modules as typed, then Settings, where people look for it."""
    result: list[Tab] = []
    if spec.has("sample"):
        arguments = ["client: model.services.apiClient"]
        if spec.has("database"):
            arguments.append("modelContainer: model.services.modelContainer")
        arguments += ["monitor: model.services.networkMonitor", "toasts: model.toasts"]
        result.append(Tab("sample", "sample", "Home", "house", "SampleFeature", _call("SampleRootView", arguments)))
    for index, name in enumerate(tab_modules(spec)):
        pascal = pascal_case(name)
        arguments = ["client: model.services.apiClient"] if spec.has("network") else []
        result.append(Tab(
            case=tab_case(name),
            raw_value=name,
            title=module_title(name),
            symbol=_MODULE_SYMBOLS[index % len(_MODULE_SYMBOLS)],
            import_name=f"{pascal}Feature",
            root=_call(f"{pascal}RootView", arguments),
        ))
    if spec.has("settings"):
        result.append(Tab(
            "settings", "settings", "Settings", "gearshape", "SettingsFeature",
            _call("SettingsRootView", ["settings: model.services.settings"]),
        ))
    return result


def labels(spec: Spec) -> list[str]:
    return [tab.title for tab in tabs(spec)]


def app_strings(spec: Spec) -> list[str]:
    """Every key of the app's String Catalog."""
    return [*(tab.title for tab in tabs(spec)), *_APP_STRINGS]


def app_tabs_swift(spec: Spec) -> list[str]:
    """`AppTabs.swift`, line by line."""
    shell = tabs(spec)
    imports = sorted({"Navigation", "SwiftUI", *(tab.import_name for tab in shell)})
    lines = [f"import {name}" for name in imports]
    lines += [
        "",
        "/// The app's tabs, in the order the tab bar shows them.",
        "nonisolated enum AppTab: String, CaseIterable, ShellTab {",
    ]
    for tab in shell:
        lines.append(f"    case {tab.case}" + ("" if tab.case == tab.raw_value else f' = "{tab.raw_value}"'))
    lines += ["", "    var id: String {", "        rawValue", "    }", ""]
    lines += ["    var title: LocalizedStringResource {", "        switch self {"]
    lines += [f'        case .{tab.case}: "{tab.title}"' for tab in shell]
    lines += ["        }", "    }", ""]
    lines += ["    var systemImage: String {", "        switch self {"]
    lines += [f'        case .{tab.case}: "{tab.symbol}"' for tab in shell]
    lines += ["        }", "    }", ""]
    lines += [
        "    /// The tab's first screen, built from the window's services.",
        "    @MainActor",
        "    @ViewBuilder",
        "    func root(in model: AppModel) -> some View {",
        "        switch self {",
    ]
    for tab in shell:
        lines.append(f"        case .{tab.case}:")
        lines += [f"            {line}" for line in tab.root]
    lines += ["        }", "    }", "}"]
    return [f"{line}\n" if line else "\n" for line in lines]


def _call(function: str, arguments: list[str]) -> tuple[str, ...]:
    if len(arguments) <= 1:
        return (f"{function}({''.join(arguments)})",)
    return (f"{function}(", *(f"    {argument}," for argument in arguments[:-1]), f"    {arguments[-1]}", ")")


def tab_case(name: str) -> str:
    """`order_history` → `orderHistory`, the tab's case."""
    first, *rest = name.split("_")
    return first + "".join(part.capitalize() for part in rest)
