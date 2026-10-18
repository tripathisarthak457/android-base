#!/usr/bin/env python3
"""
Migrates call sites outside the design system to WP03's APIs (MASTER-PLAN A4.1, A4.4, A4.7, A6.1).

    sweep_callsites.py <templates/android> [--check]

Re-runnable: every rewrite is idempotent, so it runs again after each merge of the integration
branch. It rewrites what is mechanical: typography roles, plain renames, button variants, the
TextFieldState bridge, removed components whose replacement takes the same arguments under other
names (CALLS, rewritten call by call with their named arguments mapped), imports of names that
moved package, the design-system imports a rewrite left unused, and the iOS fling on every lazy
list, grid and scroll modifier (REQUIREMENTS 41). It lists what needs a hand edit (a removed API
whose replacement has a different shape, a pager without the paging fling). --check rewrites
nothing and exits 1 when anything is left.
"""

import argparse
import re
import sys
from pathlib import Path

# Directories outside :core:designsystem that call it.
SCOPES = ["feature", "core/ui", "app", "core/devtools", "core/navigation", "core/media", "core/notification", "catalog"]

# A4.4: old role -> new role. The old scale had no labelLarge/labelMedium; `label` is 12sp, so it is
# caption1, and `caption` (11sp regular) is caption1 too, the nearer weight. The title* roles were
# semibold UI text: titleLarge (16) and titleMedium (14) read as headline and subheadline, titleSmall
# (13) as footnote. `button` was 15 bold, a headline in iOS terms.
TYPOGRAPHY = {
    "displayLarge": "largeTitle",
    "displayMedium": "title1",
    "displaySmall": "title2",
    "headingLarge": "title3",
    "headingMedium": "headline",
    "headingSmall": "headline",
    "titleLarge": "headline",
    "titleMedium": "subheadline",
    "titleSmall": "footnote",
    "bodyLarge": "body",
    "bodyMedium": "callout",
    "bodySmall": "subheadline",
    "label": "caption1",
    "labelSmall": "caption2",
    "caption": "caption1",
    "button": "headline",
}

# Plain renames: same parameters, same package, new name.
RENAMES = {
    "AppCircularProgress": "AppActivityIndicator",
    "AppPasswordField": "AppSecureField",
    "AppAlertDialog": "AppAlert",
}

# Expression-level rewrites that keep meaning (A4.1, A4.3, A4.5, A6.1).
EXPRESSIONS = [
    (re.compile(r"\bButtonVariant\.Primary\b"), "ButtonVariant.Filled"),
    (re.compile(r"\bButtonVariant\.Secondary\b"), "ButtonVariant.Tinted"),
    (re.compile(r"\bButtonVariant\.(?:Tertiary|Ghost)\b"), "ButtonVariant.Plain"),
    # The field takes the ViewModel's TextFieldState itself; the String bridge WP02-M1 left goes.
    (
        re.compile(r"\bvalue = (\w+)\.text\.toString\(\),\s*onValueChange = \1::setTextAndPlaceCursorAtEnd,"),
        r"state = \1,",
    ),
    (re.compile(r"\brememberReduceMotion\(\)"), "AppTheme.reduceMotion"),
    (re.compile(r"\bAppTheme\.motion\b"), "AppTheme.springs"),
    (re.compile(r"\bAppTheme\.springs\.sheet\(\)"), "AppTheme.springs.layout.size"),
    (re.compile(r"\blayout\.gridMinCellWidth\b"), "layout.gridMinCardWidth"),
    (re.compile(r"\bcolors\.divider\b"), "colors.separator"),
    (re.compile(r"\bcolors\.borderStrong\b"), "colors.outline"),
    (re.compile(r"\bAppTheme\.sizes\.borderWidth(Strong)?\b"), r"AppTheme.style.borderWidth\1"),
]

# Removed components whose replacement takes the same arguments under other names: the call is
# renamed, its import moved, and its named arguments mapped (None drops one). A callable value
# rewrites the argument from its source text.
DS = "com.base.app.core.designsystem.component"
CALLS = {
    "AppListItem": ("AppRow", f"{DS}.container.AppListItem", f"{DS}.list.AppRow", {"supporting": "subtitle"}, None),
    "AppEmptyState": ("AppStateView", f"{DS}.feedback.AppEmptyState", f"{DS}.feedback.AppStateView", {}, None),
    "AppErrorState": (
        "AppStateView",
        f"{DS}.feedback.AppErrorState",
        f"{DS}.feedback.AppStateView",
        {
            "onRetry": "onAction",
            "retryLabel": "actionLabel",
            "isOffline": lambda value: f"kind = if ({value}) StateKind.Offline else StateKind.Error",
        },
        ("kind", "kind = StateKind.Error", f"{DS}.feedback.StateKind"),
    ),
    "AppBackTopBar": ("AppTopBar", f"{DS}.navigation.AppBackTopBar", f"{DS}.navigation.AppTopBar", {"showDivider": None}, None),
    "AppSegmentedControl": ("AppSegmentedControl", None, None, {"selectedIndex": "selected"}, None),
    "AppTextField": ("AppTextField", None, None, {"keyboardActions": lambda value: keyboard_action(value)}, None),
    "AppSecureField": ("AppSecureField", None, None, {"keyboardActions": lambda value: keyboard_action(value)}, None),
}


# REQUIREMENTS 41: every scrolling container decelerates as iOS does. Android's spline fling is what a
# container gets when it names none, so each one names the design system's. A call whose line carries
# `allow(platform-fling)` keeps the platform's on purpose.
FLING_CALLS = (
    "LazyColumn", "LazyRow", "LazyVerticalGrid", "LazyHorizontalGrid",
    "LazyVerticalStaggeredGrid", "LazyHorizontalStaggeredGrid",
)
FLING_MODIFIER = re.compile(r"\.(?:verticalScroll|horizontalScroll)\(")
PAGER = re.compile(r"(?<![\w.])(?:HorizontalPager|VerticalPager)\(")
FLING = "flingBehavior = AppScrollDefaults.flingBehavior()"
SCROLL_DEFAULTS = "com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults"
SCROLL_PACKAGE = "package com.base.app.core.designsystem.foundation.scroll\n"
PLATFORM_FLING = "allow(platform-fling)"
LINE_LIMIT = 120


def keyboard_action(value: str) -> str:
    """`KeyboardActions(onDone = { … })` becomes the field's `onKeyboardAction = { … }` (A6.1)."""
    match = re.fullmatch(r"KeyboardActions\(\s*on\w+\s*=\s*(\{.*\})\s*,?\s*\)", value, flags=re.DOTALL)
    if match is None:
        raise ValueError(f"cannot map {value!r}: more than one keyboard action")
    return f"onKeyboardAction = {match.group(1)}"

# ADAPTIVE-PLAN §3.2: the shell decides from the window's class, everything else from the class of
# the pane it is laid out in.
SHELL = ("core/navigation/", "app/")
WINDOW_SIZE = re.compile(r"\bAppTheme\.windowSize\b")


def size_class(text: str, relative: str) -> str:
    replacement = "AppTheme.windowClass" if relative.startswith(SHELL) else "AppTheme.containerClass"
    return WINDOW_SIZE.sub(replacement, text)


# Colour roles whose replacement depends on what the colour is for.
COLOUR_BY_HAND = {
    "border": "separator (a decorative hairline) or outline (the edge of a control, 3:1)",
    "surfaceVariant": "fill (tracks and wells) or surfaceGrouped (rows of a grouped list)",
}

# Removed APIs whose replacement takes different arguments: listed, never rewritten blindly.
BY_HAND = {
    "AppBackTopBar": "AppTopBar(title, onBack = …)",
    "AppLargeTitle": "AppLargeTitleTopBar(title, scrolled = { listState.scrolledFromTop() })",
    "AppEmptyState": "AppStateView(kind = StateKind.Empty, …)",
    "AppErrorState": "AppStateView(kind = StateKind.Error, actionLabel, onAction)",
    "AppListItem": "AppRow(title, subtitle, value, onClick, leading, trailing) inside AppSection",
    "AppAlertDialog": "AppAlert(visible, title, confirmLabel, onConfirm, onDismissRequest)",
    "AppDialog": "AppAlert",
    "AppBottomSheet": "AppSheet(visible, onDismissRequest) or a Presentation.Sheet destination",
    "AppSnackbar": "AppToast through LocalOverlayHost.current?.toasts",
    "AppSnackbarHost": "AppToast through LocalOverlayHost.current?.toasts",
    "AppTextArea": "AppTextField(state, lineLimits = TextFieldLineLimits.MultiLine(…))",
    "AppDropdownMenu": "AppMenu",
    "AppTooltip": "nothing (no touch-first use)",
    "AppFloatingActionButton": "a top-bar action or AppBottomActionBar",
    "AppSwipeToAction": "AppSwipeActions",
    "AppAccordion": "AppDisclosure",
    "AppResponsiveGrid": "LazyVerticalGrid(GridCells.Adaptive(AppTheme.layout.gridMinCellWidth))",
    "appClickable": "pressable(onClick, feedback = …)",
    "AppClickableSurface": "AppSurface + pressable(feedback = Scale)",
    "bounceClick": "pressable(feedback = PressFeedback.Scale)",
    "clickableNoIndication": "pressable(feedback = PressFeedback.None)",
    "HapticEffect": "AppHaptic (silent unless the moment is in A4.2)",
    "LocalAppHapticsEnabled": "LocalHapticsEnabled",
    "AppCurrencyField": "AppMoneyField (formfields)",
    "AppNumberField": "AppDecimalField (formfields)",
    "AppTagInput": "AppTagField (formfields)",
    "AppPagerIndicator": "AppPageControl",
    "AppRadioButton": "AppRadio",
    "AppDetailRow": "AppRow(title, value = …)",
    "AppLoadMoreFooter": "core:ui's AppPagingList footer",
    "rememberAppFonts": "AppFontPreload.fonts",
    "staggeredIn": "AppStaggeredColumn / appAnimateItem",
    "pressScale": "pressable(feedback = PressFeedback.Scale)",
    "AppIndication": "NoIndication (the default) or RowHighlight",
}
BARE_CLICKABLE = re.compile(r"\.(?:combinedC|c)lickable\(")
TEXT_FIELD_VALUE = re.compile(r"\bAppTextField\(\s*value\s*=")

DESIGN_SYSTEM = "core/designsystem/src/main/kotlin"
DECLARATION = re.compile(
    r"^(?:(?:public|internal|private|inline|data|enum|sealed|value|abstract|open|annotation|fun|const)\s+)*"
    r"(?:fun|class|object|interface|val|var|typealias)\s+(?:<[^>]+>\s+)?(?:[\w.]+\.)?(\w+)",
    flags=re.MULTILINE,
)


def design_system_index(root: Path) -> dict[str, set[str]]:
    """Every top-level name the design system declares, with the packages that declare it."""
    index: dict[str, set[str]] = {}
    for path in (root / DESIGN_SYSTEM).rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        package = re.search(r"^package ([\w.]+)", text, flags=re.MULTILINE).group(1)
        for match in DECLARATION.finditer(text):
            if not text[match.start():match.start() + 8].startswith("private"):
                index.setdefault(match.group(1), set()).add(package)
    return index


def move_imports(text: str, index: dict[str, set[str]]) -> str:
    """Points imports of design-system names that moved package at where they live now."""
    def moved(match: re.Match) -> str:
        package, name = match.group(1), match.group(2)
        homes = index.get(name, set())
        if package in homes or len(homes) != 1:
            return match.group(0)
        return f"import {next(iter(homes))}.{name}"
    text = re.sub(r"^import (com\.base\.app\.core\.designsystem[\w.]*)\.(\w+)$", moved, text, flags=re.MULTILINE)
    seen, kept = set(), []
    for line in text.split("\n"):
        if line.startswith("import ") and line in seen:
            continue
        seen.add(line)
        kept.append(line)
    return "\n".join(kept)


# Imports a rewrite can leave unused. Only these are pruned: operator and delegate imports such as
# getValue are used without their name appearing, so a general pass would break code.
PRUNABLE = re.compile(
    r"^import (com\.base\.app\.core\.designsystem\.[\w.]+"
    r"|androidx\.compose\.foundation\.text\.input\.setTextAndPlaceCursorAtEnd"
    r"|androidx\.compose\.foundation\.text\.KeyboardActions)$"
)


def prune_imports(text: str) -> str:
    body = "\n".join(line for line in text.split("\n") if not line.startswith("import "))
    def keep(line: str) -> bool:
        match = PRUNABLE.match(line)
        return not match or re.search(rf"\b{match.group(1).rsplit('.', 1)[1]}\b", body) is not None
    return "\n".join(line for line in text.split("\n") if keep(line))


def import_order(line: str):
    """IntelliJ's KOTLIN_OFFICIAL layout: everything, then java, javax and kotlin, then aliases."""
    path = line.removeprefix("import ")
    group = 4 if " as " in path else 1 if path.startswith("java.") else 2 if path.startswith("javax.") else 3 if path.startswith("kotlin.") else 0
    return group, path


def sort_imports(text: str) -> str:
    """Sorts each run of import lines in place. Runs split by markers (`// <opt:x>`) stay apart."""
    lines, start = text.split("\n"), None
    for i in range(len(lines) + 1):
        is_import = i < len(lines) and lines[i].startswith("import ")
        if is_import and start is None:
            start = i
        elif not is_import and start is not None:
            lines[start:i] = sorted(lines[start:i], key=import_order)
            start = None
    return "\n".join(lines)


def kotlin_files(root: Path):
    for scope in SCOPES:
        base = root / scope
        if base.is_dir():
            yield from (p for p in base.rglob("*.kt") if "/build/" not in p.as_posix())


def call_spans(text: str, name: str):
    """The spans of every `name(…)` call: the index after its `(` and the index of its `)`."""
    for match in re.finditer(rf"(?<![\w.]){name}\(", text):
        if re.search(r"\bfun\s+$", text[max(0, match.start() - 8):match.start()]):
            continue
        depth, i, open_at = 1, match.end(), match.end()
        while depth and i < len(text):
            char = text[i]
            if char == '"':
                end = '"""' if text.startswith('"""', i) else '"'
                i = text.index(end, i + len(end)) + len(end)
                continue
            depth += {"(": 1, "[": 1, "{": 1, ")": -1, "]": -1, "}": -1}.get(char, 0)
            i += 1
        yield match.start(), open_at, i - 1


def arguments(text: str, start: int, end: int):
    """Top-level arguments in text[start:end] as (name or None, name start, value start, value end)."""
    found, depth, arg_start, i = [], 0, start, start
    while i <= end:
        char = text[i] if i < end else ","
        if char == '"' and i < end:
            close = '"""' if text.startswith('"""', i) else '"'
            i = text.index(close, i + len(close)) + len(close)
            continue
        if char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        elif char == "," and depth == 0:
            chunk = text[arg_start:i]
            if chunk.strip():
                lead = len(chunk) - len(chunk.lstrip())
                named = re.match(r"(\w+)\s*=(?!=)\s*", chunk[lead:])
                name_at = arg_start + lead
                if named:
                    found.append((named.group(1), name_at, name_at + named.end(), i))
                else:
                    found.append((None, name_at, name_at, i))
            arg_start = i + 1
        i += 1
    return found


def rewrite_calls(text: str) -> str:
    for old, (new, old_import, new_import, mapping, required) in CALLS.items():
        # Last call first, so the spans still to come keep their offsets.
        for span in reversed(list(call_spans(text, old))):
            text = _rewrite_call(text, span, new, mapping, required)
        if old_import and re.search(rf"^import {re.escape(old_import)}$", text, flags=re.MULTILINE):
            text = re.sub(rf"^import {re.escape(old_import)}\n", "", text, flags=re.MULTILINE)
            text = add_import(text, new_import)
        if required and re.search(r"\bStateKind\.", text):
            text = add_import(text, required[2])
    return text


def _rewrite_call(text: str, span, new: str, mapping, required) -> str:
    name_start, open_at, close_at = span
    edits = []
    args = arguments(text, open_at, close_at)
    for name, name_at, value_at, value_end in args:
        if name not in mapping:
            continue
        target = mapping[name]
        if target is None:
            line_start = text.rfind("\n", 0, name_at) + 1
            cut_start = line_start if not text[line_start:name_at].strip() else name_at
            cut_end = value_end + 1 if value_end < close_at else value_end
            if text[cut_end:cut_end + 1] == "\n" and cut_start == line_start:
                cut_end += 1
            edits.append((cut_start, cut_end, ""))
        elif callable(target):
            edits.append((name_at, value_end, target(text[value_at:value_end].strip())))
        else:
            edits.append((name_at, value_at, f"{target} = "))
    if required and not any(name == required[0] or (name and callable(mapping.get(name))) for name, *_ in args):
        first = args[0][1] if args else close_at
        indent = text[text.rfind("\n", 0, first) + 1:first]
        edits.append((first, first, f"{required[1]},\n{indent}" if "\n" in text[open_at:first] else f"{required[1]}, "))
    for start, end, replacement in sorted(edits, reverse=True):
        text = text[:start] + replacement + text[end:]
    return text[:name_start] + new + text[open_at - 1:]


def add_import(text: str, path: str) -> str:
    if re.search(rf"^import {re.escape(path)}$", text, flags=re.MULTILINE):
        return text
    imports = list(re.finditer(r"^import [\w.]+$", text, flags=re.MULTILINE))
    line = f"import {path}"
    for match in imports:
        if match.group(0) > line:
            return text[:match.start()] + line + "\n" + text[match.start():]
    at = imports[-1].end() if imports else text.index("\n") + 1
    return text[:at] + "\n" + line + text[at:]


def paren_spans(text: str, pattern: re.Pattern):
    """`(start, after its open paren, its close paren)` of each match of a pattern ending in `(`."""
    for match in pattern.finditer(text):
        if re.search(r"\bfun\s+$", text[max(0, match.start() - 8):match.start()]):
            continue
        depth, i = 1, match.end()
        while depth and i < len(text):
            depth += {"(": 1, ")": -1}.get(text[i], 0)
            i += 1
        yield match.start(), match.end(), i - 1


def _trimmed_end(text: str, value_at: int, value_end: int) -> int:
    end = value_end
    while end > value_at and text[end - 1].isspace():
        end -= 1
    return end


def _keeps_platform_fling(text: str, start: int) -> bool:
    line_start = text.rfind("\n", 0, start) + 1
    line_end = text.find("\n", start)
    return PLATFORM_FLING in text[line_start:line_end if line_end != -1 else len(text)]


def _with_fling(text: str, span, wrap: bool) -> str:
    """Names the design system's fling in one call, after its last argument."""
    name_start, open_at, close_at = span
    before = text[text.rfind("\n", 0, name_start) + 1:name_start].lstrip()
    if before.startswith(("*", "//", "/*")) or _keeps_platform_fling(text, name_start):
        return text
    args = arguments(text, open_at, close_at)
    if any(name == "flingBehavior" for name, *_ in args):
        return text
    # A list the person cannot scroll (a placeholder) never flings.
    if any(name == "userScrollEnabled" and text[at:end].strip() == "false" for name, _, at, end in args):
        return text
    if not args:
        return text[:open_at] + FLING + text[close_at:]
    _, name_at, value_at, value_end = args[-1]
    end = _trimmed_end(text, value_at, value_end)
    if "\n" in text[open_at:close_at]:
        indent = text[text.rfind("\n", 0, name_at) + 1:name_at]
        if value_end < close_at:
            return text[:value_end + 1] + f"\n{indent}{FLING}," + text[value_end + 1:]
        return text[:end] + f",\n{indent}{FLING}" + text[end:]
    line_start = text.rfind("\n", 0, name_start) + 1
    single = text[:end] + f", {FLING}" + text[end:]
    line_end = single.find("\n", name_start)
    if not wrap or (line_end if line_end != -1 else len(single)) - line_start <= LINE_LIMIT:
        return single
    # Too long for one line: one argument a line, as the formatter lays a long call out.
    indent = re.match(r"[ \t]*", text[line_start:]).group(0)
    parts = [text[at:_trimmed_end(text, v_at, v_end)] for _, at, v_at, v_end in args] + [FLING]
    body = "".join(f"{indent}    {part},\n" for part in parts)
    return text[:open_at] + "\n" + body + indent + text[close_at:]


def add_fling(text: str) -> str:
    for name in FLING_CALLS:
        for span in reversed(list(call_spans(text, name))):
            text = _with_fling(text, span, wrap=True)
    for span in reversed(list(paren_spans(text, FLING_MODIFIER))):
        text = _with_fling(text, (span[0] + 1, span[1], span[2]), wrap=False)
    if "AppScrollDefaults." in text and SCROLL_PACKAGE not in text:
        text = add_import(text, SCROLL_DEFAULTS)
    return text


def rewrite(text: str) -> str:
    roles = "|".join(sorted(TYPOGRAPHY, key=len, reverse=True))
    text = re.sub(rf"(\btypography\.)({roles})\b", lambda m: m.group(1) + TYPOGRAPHY[m.group(2)], text)
    for old, new in RENAMES.items():
        text = re.sub(rf"\b{old}\b", new, text)
    for pattern, replacement in EXPRESSIONS:
        text = pattern.sub(replacement, text)
    return rewrite_calls(text)


def leftovers(path: Path, text: str):
    for number, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*"):
            continue
        for name, hint in BY_HAND.items():
            if re.search(rf"\b{name}\b", line):
                yield f"{path}:{number}: {name} -> {hint}"
        for role, hint in COLOUR_BY_HAND.items():
            if re.search(rf"\bcolors\.{role}\b", line) or re.search(rf"\.(success|warning|danger|info|neutral)\.{role}\b", line):
                yield f"{path}:{number}: colour {role} -> {hint}"
        if re.search(r"\.(success|warning|danger|info|neutral)\.border\b", line):
            yield f"{path}:{number}: status border -> base (status colours are base, content, subtle, on)"
        # Glance's GlanceModifier.clickable is a widget action, not a Compose touch target.
        glance = "import androidx.glance" in text
        if BARE_CLICKABLE.search(line) and not glance and "allow(bare-clickable)" not in line:
            yield f"{path}:{number}: bare clickable -> pressable(onClick, feedback = …)"
        if TEXT_FIELD_VALUE.search(line):
            yield f"{path}:{number}: AppTextField(value = …) bridge -> AppTextField(state = field)"
    for start, open_at, close_at in paren_spans(text, PAGER):
        if "flingBehavior" not in text[open_at:close_at] and not _keeps_platform_fling(text, start):
            number = text.count("\n", 0, start) + 1
            yield f"{path}:{number}: pager -> flingBehavior = AppScrollDefaults.pagerFlingBehavior(state)"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    changed, remaining = 0, []
    index = design_system_index(args.root)
    for path in kotlin_files(args.root):
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(args.root).as_posix()
        updated = prune_imports(move_imports(size_class(rewrite(text), relative), index))
        if re.search(r"\bAppTheme\.", updated) and not re.search(r"^import [\w.]+\.AppTheme$|^package com\.base\.app\.core\.designsystem\.theme$", updated, flags=re.MULTILINE):
            updated = add_import(updated, "com.base.app.core.designsystem.theme.AppTheme")
        if updated != text:
            updated = sort_imports(updated)
        # After the sort: the fling's one import goes in its place, and a file this alone changes keeps
        # the order its imports already had.
        updated = add_fling(updated)
        if updated != text:
            changed += 1
            if not args.check:
                # Never the platform's line separator: on Windows that would write CRLF.
                path.write_text(updated, encoding="utf-8", newline="\n")
        # The catalog demonstrates the design system itself (a bare clickable with RowHighlight,
        # the pressScale token), so only its imports and renames are swept.
        if not relative.startswith("catalog/"):
            remaining.extend(leftovers(path.relative_to(args.root), updated))
    print(f"{'would change' if args.check else 'changed'} {changed} files")
    for line in remaining:
        print(line)
    print(f"{len(remaining)} sites need a hand edit")
    return 1 if args.check and (changed or remaining) else 0


if __name__ == "__main__":
    sys.exit(main())
