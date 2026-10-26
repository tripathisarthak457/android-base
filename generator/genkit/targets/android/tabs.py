"""
The tabs in `AppDestinations.kt`: their order, their icons, and the start destination they imply.

Order (QA-09): Home, then the scaffolded modules in the order given, then Feed, Search, Profile and
Settings. The shell's bar shows at most five; past that it shows the first four and a More tab
holding the rest, so the order is also what stays one tap away. Settings is last because it is the
least visited, and the first to move under More.

A project without UI components has no shell, so it has no tabs: its start screen comes from the
app's own `AppDestinations`, and every module is reached by its keys and deep links.
"""

from __future__ import annotations

import re
from functools import cache
from pathlib import Path

from ...core.spec import Spec, module_title, pascal_case
from . import scaffold

#: The most tabs the shell's bar and rail show before they move the rest under More; a sidebar
#: shows every tab.
BAR_LIMIT = 5
RAIL_LIMIT = 7

#: The template's own tabs, by the feature that brings them, with their bar labels and glyphs.
_LEADING = (("sample", "Home", "Home"),)
_TRAILING = (
    ("paging", "Feed", "ListView"),
    ("search", "Search", "Search"),
    ("profile", "Profile", "User"),
    ("settings", "Settings", "Settings"),
)

#: A scaffolded module's glyph, from a word in its name, best match first.
_GLYPHS: tuple[tuple[tuple[str, ...], tuple[str, ...]], ...] = (
    (("order", "shipment", "delivery", "parcel"), ("Package",)),
    (("cart", "shop", "store", "product"), ("Cart",)),
    (("wallet", "payment", "billing", "card", "invoice"), ("CreditCard",)),
    (("inbox", "chat", "message"), ("Message",)),
    (("mail",), ("Mail",)),
    (("calendar", "event", "booking", "schedule"), ("Calendar",)),
    (("notification", "alert"), ("Bell",)),
    (("favourite", "favorite", "saved", "wishlist"), ("Heart", "Bookmark")),
    (("file", "document"), ("Folder", "File")),
    (("map", "place", "location", "trip"), ("MapPin",)),
    (("explore", "discover"), ("Globe",)),
    (("photo", "gallery", "image"), ("ImageIcon", "Camera")),
    (("team", "contact", "people", "member"), ("Users",)),
)

#: Glyphs for a module whose name matches nothing, taken in turn so that no two tabs share one.
_FALLBACK = ("Grid", "Tag", "Bookmark", "Star", "Folder", "Globe", "Heart", "Calendar")

_TEMPLATE_DIR = Path(__file__).resolve().parents[4] / "templates" / "android"
_GLYPH = re.compile(r"^\s+(?:public\s+)?val\s+([A-Z]\w*)\b", re.MULTILINE)


@cache
def available_glyphs(root: Path = _TEMPLATE_DIR) -> frozenset[str]:
    """Every `AppIcons` accessor the design system under `root`, the template or a project, declares."""
    found: set[str] = set()
    for path in root.glob("core/designsystem/src/main/kotlin/**/icon/AppIcons.kt"):
        found.update(_GLYPH.findall(path.read_text(encoding="utf-8")))
    return frozenset(found)


def icons(names: tuple[str, ...], taken: set[str], glyphs: frozenset[str]) -> dict[str, tuple[str, str]]:
    """Each module's `(icon, selectedIcon)`, never a glyph in `taken` nor one another module got."""
    taken = set(taken)
    outlines = sorted(glyph for glyph in glyphs if not glyph.endswith("Filled"))
    chosen: dict[str, tuple[str, str]] = {}
    for name in names:
        wanted = [glyph for keys, options in _GLYPHS if any(key in name for key in keys) for glyph in options]
        free = [glyph for glyph in (*wanted, *_FALLBACK, *outlines) if glyph in glyphs and glyph not in taken]
        glyph = free[0] if free else "Grid"
        taken.add(glyph)
        filled = f"{glyph}Filled"
        chosen[name] = (glyph, filled if filled in glyphs else glyph)
    return chosen


def shell_tab(name: str, icon: str, selected: str) -> str:
    """A tab for a scaffolded module, labelled with that module's own title string."""
    pascal = pascal_case(name)
    return (
        f"        ShellTab(key = {pascal}ListKey, label = {pascal}R.string.{name}_title, "
        f"icon = AppIcons.{icon}, selectedIcon = AppIcons.{selected}),\n"
    )


def shell_tab_imports(package_name: str, name: str) -> list[str]:
    feature = f"{package_name}.feature.{name}"
    pascal = pascal_case(name)
    return [f"import {feature}.api.{pascal}ListKey\n", f"import {feature}.R as {pascal}R\n"]


def regions(spec: Spec) -> dict[str, list[str] | None]:
    if not spec.option(scaffold.UI_COMPONENTS):
        return {"shell-tabs": [], "start-destination-import": [], "start-destination": []}
    names = scaffold.modules(spec)
    template_glyphs = {glyph for key, _, glyph in _LEADING + _TRAILING if spec.has(key)}
    chosen = icons(names, template_glyphs, available_glyphs())
    shell_tabs = [shell_tab(name, *chosen[name]) for name in names]
    imports = [line for name in names for line in shell_tab_imports(spec.package_name, name)]

    # `start` is `tabs.firstOrNull()?.key`, nullable however many tabs there are, so the elvis is
    # what gives the property its type; it names the first tab so the two can never disagree.
    start: list[str] = []
    if not spec.has("sample"):
        first = f"{pascal_case(names[0])}ListKey" if names else _first_template_key(spec)
        start = [f"        ?: {first}\n"]

    return {
        "shell-tabs": shell_tabs,
        "start-destination-import": imports,
        "start-destination": start,
    }


def _first_template_key(spec: Spec) -> str:
    keys = {"paging": "FeedKey", "search": "SearchKey", "profile": "ProfileKey", "settings": "SettingsKey"}
    return next(keys[key] for key, _, _ in _TRAILING if spec.has(key))


def labels(spec: Spec) -> list[str]:
    """The tabs, in the order the app has them."""
    if not spec.option(scaffold.UI_COMPONENTS):
        return []
    return (
        [label for key, label, _ in _LEADING if spec.has(key)]
        + [module_title(name) for name in scaffold.modules(spec)]
        + [label for key, label, _ in _TRAILING if spec.has(key)]
    )


def warnings(spec: Spec) -> list[str]:
    tabs = labels(spec)
    if len(tabs) <= BAR_LIMIT:
        return []
    shown = ", ".join(tabs[: BAR_LIMIT - 1])
    rest = ", ".join(tabs[BAR_LIMIT - 1 :])
    if len(tabs) <= RAIL_LIMIT:
        wider = "a wider window's rail shows them all"
    else:
        wider = f"a wider window's rail shows {RAIL_LIMIT - 1} and More, and a large window's sidebar shows them all"
    return [
        f"The app has {len(tabs)} tabs. A phone's bar shows {shown} and More, which holds {rest}; {wider}. "
        "Reorder feature_modules to choose what stays in the bar."
    ]
