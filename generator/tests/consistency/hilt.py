"""Every type Hilt is asked to inject has an `@Inject` constructor or a `@Provides`/`@Binds` behind it."""

from __future__ import annotations

import re
from pathlib import Path

from .project import strip_code

#: Types the Android and Hilt runtimes provide themselves.
BUILT_IN = {
    "Context", "Application", "SavedStateHandle", "Activity", "ComponentActivity", "ViewModelLifecycle",
    "Resources", "WorkerParameters", "FragmentActivity", "HiltWorkerFactory",
}

_INJECT_CONSTRUCTOR = re.compile(r"class\s+(\w+)(?:<[^>]*>)?\s*(?:@\w+(?:\([^)]*\))?\s*)*@Inject\s+constructor\s*\(")
_PROVIDER = re.compile(
    r"@(Provides|Binds)\b((?:\s*@\w+(?:\([^)]*\))?)*)\s*(?:(?:internal|public|abstract|suspend)\s+)*fun\s+(\w+)\s*\("
)
_MULTIBINDS = re.compile(r"@Multibinds\s*(?:abstract\s+)?fun\s+\w+\(\)\s*:\s*([\w.<>, ?]+)")
_FIELD = re.compile(r"@Inject\s+(?:lateinit\s+)?(?:internal\s+)?var\s+\w+\s*:\s*([\w.<>, ?]+)")


def _parameters(signature: str) -> list[str]:
    depth, current, parts = 0, "", []
    for character in signature:
        if character in "(<[":
            depth += 1
        elif character in ")>]":
            depth -= 1
        if character == "," and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += character
    parts.append(current)
    return [_normal(part.split(":", 1)[1].split("=")[0]) for part in (p.strip() for p in parts) if ":" in part]


def _normal(type_name: str) -> str:
    return re.sub(r"\s+", "", type_name.strip().rstrip("?"))


def _close(code: str, start: int) -> int:
    depth, index = 1, start
    while depth and index < len(code):
        depth += {"(": 1, ")": -1}.get(code[index], 0)
        index += 1
    return index


def missing_bindings(root: Path) -> list[str]:
    provided: set[str] = set()
    needed: list[tuple[str, str, str]] = []
    for path in root.rglob("src/main/kotlin/**/*.kt"):
        code = strip_code(path.read_text(encoding="utf-8"))
        relative = path.relative_to(root).as_posix()
        for match in _INJECT_CONSTRUCTOR.finditer(code):
            provided.add(match.group(1))
            end = _close(code, match.end())
            needed += [(type_name, relative, match.group(1)) for type_name in _parameters(code[match.end():end - 1])]
        for match in _PROVIDER.finditer(code):
            end = _close(code, match.end())
            returned = re.match(r"\s*:\s*([\w.<>, ?*]+?)\s*(?:=|\{|\n)", code[end:end + 300])
            type_name = _normal(returned.group(1)) if returned else "?"
            qualifiers = match.group(2)
            into_set = ("IntoSet" in qualifiers or "IntoMap" in qualifiers) and "ElementsIntoSet" not in qualifiers
            provided.add(f"Set<{type_name}>" if into_set else type_name)
            needed += [(parameter, relative, match.group(3)) for parameter in _parameters(code[match.end():end - 1])]
        provided.update(_normal(match.group(1)) for match in _MULTIBINDS.finditer(code))
        needed += [(_normal(match.group(1)), relative, "field") for match in _FIELD.finditer(code)]

    missing = set()
    for type_name, relative, who in needed:
        base = re.sub(r"<.*", "", type_name)
        if base in BUILT_IN or type_name in provided or base in provided:
            continue
        inner = re.match(r"\w+<(?:@JvmSuppressWildcards)?(.*)>", type_name)
        if base in ("Lazy", "Provider") and inner and (inner.group(1) in provided or re.sub(r"<.*", "", inner.group(1)) in provided):
            continue
        if base in ("Set", "MutableSet") and inner:
            element = inner.group(1).replace("@JvmSuppressWildcards", "")
            if any(key.startswith("Set<") and element in key for key in provided):
                continue
        missing.add(f"HILT {relative}: {who} needs {type_name}")
    return sorted(missing)
