"""
Checks that the code the Android and Compose Multiplatform templates share has not drifted apart.

`templates/multiplatform/sync.toml` pairs each shared directory or file of the Android template
with its `commonMain` copy and lists every difference the two may have, each with its reason.
Import lines are left out of the comparison, because a platform type behind the same name lives in
another package on each side; any other line that differs, a file present on one side only, or an
allowance that no longer matches what is there fails the check. Whoever changes one side ports the
change to the other, or records the new difference and why.

    python3 tools/sync_check.py                     # the repository's templates
    python3 tools/sync_check.py --root DIR          # DIR/templates/{android,multiplatform}
"""

from __future__ import annotations

import argparse
import difflib
import sys
import tomllib
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = Path("templates/multiplatform/sync.toml")


@dataclass(frozen=True)
class Difference:
    """Lines one file of a pair may have that the other does not."""

    file: str
    reason: str
    android: tuple[str, ...]
    multiplatform: tuple[str, ...]


@dataclass(frozen=True)
class Pair:
    name: str
    android: Path
    multiplatform: Path
    differences: dict[str, Difference] = field(default_factory=dict)
    #: Files in the pair's Android directory with no `commonMain` copy, by name, with the reason.
    android_only: dict[str, str] = field(default_factory=dict)
    #: Files in the pair's `commonMain` directory with no Android original, by name, with the reason.
    multiplatform_only: dict[str, str] = field(default_factory=dict)


class ManifestError(ValueError):
    """sync.toml itself is malformed."""


def load(root: Path) -> list[Pair]:
    path = root / MANIFEST
    data = tomllib.loads(path.read_text(encoding="utf-8"))
    pairs = []
    for entry in data.get("pair", []):
        name = _text(entry, "name", path)
        differences = {}
        for raw in entry.get("difference", []):
            difference = Difference(
                file=_text(raw, "file", path),
                reason=_text(raw, "reason", path),
                android=tuple(raw.get("android", [])),
                multiplatform=tuple(raw.get("multiplatform", [])),
            )
            if difference.file in differences:
                raise ManifestError(f"{path}: pair '{name}' lists '{difference.file}' twice")
            differences[difference.file] = difference
        pairs.append(
            Pair(
                name=name,
                android=root / "templates/android" / _text(entry, "android", path),
                multiplatform=root / "templates/multiplatform" / _text(entry, "multiplatform", path),
                differences=differences,
                android_only=_reasons(entry.get("android_only", []), path),
                multiplatform_only=_reasons(entry.get("multiplatform_only", []), path),
            )
        )
    return pairs


def check(pairs: list[Pair]) -> list[str]:
    """Every way the pairs differ beyond what they allow, as messages. Empty when they are in sync."""
    problems: list[str] = []
    for pair in pairs:
        missing = [side for side in (pair.android, pair.multiplatform) if not side.exists()]
        if missing:
            problems += [f"{pair.name}: {side} does not exist" for side in missing]
            continue

        if pair.android.is_file():
            problems += _compare(pair, pair.android, pair.multiplatform, pair.android.name)
            continue

        android = _files(pair.android)
        multiplatform = _files(pair.multiplatform)
        for name in sorted(android - multiplatform):
            if name not in pair.android_only:
                problems.append(f"{pair.name}: {name} has no commonMain copy")
        for name in sorted(multiplatform - android):
            if name not in pair.multiplatform_only:
                problems.append(f"{pair.name}: {name} has no Android original")
        for name in sorted(pair.android_only.keys() - (android - multiplatform)):
            problems.append(f"{pair.name}: {name} is listed as Android-only but is not")
        for name in sorted(pair.multiplatform_only.keys() - (multiplatform - android)):
            problems.append(f"{pair.name}: {name} is listed as multiplatform-only but is not")
        for name in sorted(android & multiplatform):
            problems += _compare(pair, pair.android / name, pair.multiplatform / name, name)
        for name in sorted(pair.differences.keys() - (android & multiplatform)):
            problems.append(f"{pair.name}: a difference is allowed in {name}, which is not in both")
    return problems


def _compare(pair: Pair, android: Path, multiplatform: Path, name: str) -> list[str]:
    left = _comparable(android)
    right = _comparable(multiplatform)
    removed: Counter[str] = Counter()
    added: Counter[str] = Counter()
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(None, left, right, autojunk=False).get_opcodes():
        if tag != "equal":
            removed.update(left[i1:i2])
            added.update(right[j1:j2])
    # A line on both sides of a changed stretch is one the matcher aligned with a different
    # neighbour, not a difference: the allowances name lines, not their positions.
    unchanged = removed & added
    removed -= unchanged
    added -= unchanged

    allowed = pair.differences.get(name)
    allowed_android = Counter(allowed.android) if allowed else Counter()
    allowed_multiplatform = Counter(allowed.multiplatform) if allowed else Counter()
    problems = []
    for line in sorted((removed - allowed_android).elements()):
        problems.append(f"{pair.name}/{name}: only the Android copy has   {line.strip()!r}")
    for line in sorted((added - allowed_multiplatform).elements()):
        problems.append(f"{pair.name}/{name}: only the commonMain copy has {line.strip()!r}")
    # An allowance that matches nothing hides the next real difference on the same line, so it goes.
    for line in sorted((allowed_android - removed).elements()):
        problems.append(f"{pair.name}/{name}: allowed Android line no longer differs: {line.strip()!r}")
    for line in sorted((allowed_multiplatform - added).elements()):
        problems.append(f"{pair.name}/{name}: allowed commonMain line no longer differs: {line.strip()!r}")
    return problems


def _comparable(path: Path) -> list[str]:
    lines = [line.rstrip() for line in path.read_text(encoding="utf-8").splitlines()]
    if path.suffix not in (".kt", ".kts"):
        return lines
    return [line for line in lines if not line.startswith("import ")]


def _files(directory: Path) -> set[str]:
    return {path.relative_to(directory).as_posix() for path in directory.rglob("*") if path.is_file()}


def _text(table: dict, key: str, path: Path) -> str:
    value = table.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ManifestError(f"{path}: every entry needs a non-empty '{key}'")
    return value


def _reasons(entries: list[dict], path: Path) -> dict[str, str]:
    return {_text(entry, "file", path): _text(entry, "reason", path) for entry in entries}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0].strip())
    parser.add_argument("--root", type=Path, default=ROOT, help="The repository root.")
    args = parser.parse_args(argv)

    try:
        pairs = load(args.root)
    except (ManifestError, tomllib.TOMLDecodeError) as error:
        print(error, file=sys.stderr)
        return 2
    problems = check(pairs)
    if problems:
        print("The Android and multiplatform templates have drifted apart:\n", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        print(
            "\nPort the change to the other template, or allow the difference in "
            f"{MANIFEST} with the reason it has to exist.",
            file=sys.stderr,
        )
        return 1
    print(f"{len(pairs)} shared {'part' if len(pairs) == 1 else 'parts'} in sync.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
