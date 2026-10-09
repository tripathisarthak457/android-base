"""The bottom-bar tabs in `AppDestinations.kt`, and the start destination they imply."""

from __future__ import annotations

from ...core.spec import Spec, module_title, pascal_case

#: The template's own tab sources in the order the bar shows them, with their labels. Scaffolded
#: modules go between search and profile, so settings stays last, where people look for it.
_LEADING = (("sample", "Home"), ("paging", "Feed"), ("search", "Search"))
_TRAILING = (("profile", "Profile"), ("settings", "Settings"))


def shell_tab(name: str) -> str:
    """A tab for a scaffolded module, labelled with that module's own title string."""
    pascal = pascal_case(name)
    return f"        ShellTab(key = {pascal}ListKey, label = {pascal}R.string.{name}_title, icon = AppIcons.Grid),\n"


def shell_tab_imports(package_name: str, name: str) -> list[str]:
    feature = f"{package_name}.feature.{name}"
    pascal = pascal_case(name)
    return [f"import {feature}.{pascal}ListKey\n", f"import {feature}.R as {pascal}R\n"]


def regions(spec: Spec) -> dict[str, list[str] | None]:
    # Every module becomes a tab in the order the user typed them, so the first one is both the
    # first tab and, by AppDestinations' own rule, the start destination.
    shell_tabs = [shell_tab(name) for name in spec.feature_modules]
    imports = [line for name in spec.feature_modules for line in shell_tab_imports(spec.package_name, name)]

    # `start` is `tabs.firstOrNull()?.key`, which is nullable however many tabs there are, so the
    # elvis is not a fallback for an empty list: it is what gives the property its type.
    start: list[str] = []
    if not spec.has("sample"):
        if spec.has("paging"):
            fallback = "FeedKey"
        elif spec.has("search"):
            fallback = "SearchKey"
        elif spec.feature_modules:
            fallback = f"{pascal_case(spec.feature_modules[0])}ListKey"
        elif spec.has("profile"):
            fallback = "ProfileKey"
        elif spec.has("settings"):
            fallback = "SettingsKey"
        else:
            fallback = 'error("Set a start destination: name your first feature\'s nav key here.")'
        start = [f"        ?: {fallback}\n"]

    return {
        "shell-tabs": shell_tabs,
        "start-destination-import": imports,
        "start-destination": start,
    }


def labels(spec: Spec) -> list[str]:
    """The tabs the bar will show, in order."""
    return (
        [label for key, label in _LEADING if spec.has(key)]
        + [module_title(name) for name in spec.feature_modules]
        + [label for key, label in _TRAILING if spec.has(key)]
    )
