"""
The token copies checked into the templates, against a fresh emission from `tokens/*.json`.

    python3 -m genkit.tokens --check          # exit 1 and show the difference when a copy is stale
    python3 -m genkit.tokens --write          # rewrite every copy that exists
    python3 -m genkit.tokens --write --add android   # also create a target's copy the first time

A template's copy is emitted for the template's own identity and default choices; generation
re-emits it with the user's brand and choices.
"""

from __future__ import annotations

import argparse
import difflib
import sys
from collections.abc import Callable
from pathlib import Path

from . import kotlin, kotlin_common, resolve, swift

TEMPLATES = Path(__file__).resolve().parents[3] / "templates"

#: The template package every checked-in Kotlin copy is written under.
TEMPLATE_PACKAGE = "com.base.app"


def emissions() -> dict[str, tuple[Path, Callable[[], str]]]:
    """Target → (where its copy lives, what it should contain)."""
    return {
        "android": (
            TEMPLATES / "android" / kotlin.RELATIVE_PATH.format(package_path=TEMPLATE_PACKAGE.replace(".", "/")),
            lambda: kotlin.emit(resolve(None, "android"), TEMPLATE_PACKAGE),
        ),
        "multiplatform": (
            TEMPLATES / "multiplatform" / kotlin_common.RELATIVE_PATH.format(
                package_path=TEMPLATE_PACKAGE.replace(".", "/"),
            ),
            lambda: kotlin_common.emit(resolve(None, kotlin_common.PLATFORM), TEMPLATE_PACKAGE),
        ),
        "ios": (
            TEMPLATES / "ios" / swift.RELATIVE_PATH,
            lambda: swift.emit(resolve(None, "ios")),
        ),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="python3 -m genkit.tokens", description=__doc__.strip().splitlines()[0])
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument("--check", action="store_true", help="compare every checked-in copy with a fresh emission")
    action.add_argument("--write", action="store_true", help="rewrite every checked-in copy")
    parser.add_argument("--add", action="append", default=[], metavar="TARGET", help="with --write, create this target's copy")
    args = parser.parse_args(argv)

    known = emissions()
    unknown = [target for target in args.add if target not in known]
    if unknown:
        parser.error(f"no token emission for {', '.join(unknown)}; known: {', '.join(known)}")

    stale = 0
    checked = 0
    for target, (path, emit) in known.items():
        if not path.exists() and target not in args.add:
            continue
        expected = emit()
        if args.write:
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.exists() or path.read_text(encoding="utf-8") != expected:
                path.write_text(expected, encoding="utf-8")
                print(f"wrote {path.relative_to(TEMPLATES.parent)}")
            continue
        checked += 1
        actual = path.read_text(encoding="utf-8")
        if actual != expected:
            stale += 1
            relative = path.relative_to(TEMPLATES.parent).as_posix()
            sys.stdout.writelines(
                difflib.unified_diff(
                    actual.splitlines(keepends=True), expected.splitlines(keepends=True),
                    f"{relative} (checked in)", f"{relative} (from tokens/*.json)",
                )
            )

    if args.check:
        if stale:
            print(f"\n{stale} of {checked} token copies are stale. Run: python3 -m genkit.tokens --write")
            return 1
        print(f"{checked} token cop{'y' if checked == 1 else 'ies'} current.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
