"""
The template's identity (`com.base.app`, `BaseApp`, `base_app`, …) replaced by the project's, once.

One compiled alternation finds the tokens, longest first, and each match must sit on its token's
boundary. It runs on template lines and template paths only, before anything generated is
inserted, so a name the user chose is never scanned again: `com.base.apple` stays
`com.base.apple`, and a module called `database_app` stays `database_app`.

| Boundary | Matches when                                              | Why                                     |
|----------|-----------------------------------------------------------|-----------------------------------------|
| dotted   | not after `[A-Za-z0-9_.]`, not before `[A-Za-z0-9_]`      | `com.base.app.core`, never `com.base.apple` |
| path     | as dotted without the dot, or after a JVM descriptor `L`  | directories, and `HSPLcom/base/app/X;` in baseline profiles |
| prefix   | not after `[A-Za-z0-9_]`                                  | `BaseAppApplication`, `Theme.BaseApp`   |
| snake    | not after or before `[A-Za-z0-9]`; `_` separates          | `base_app_default` renames, `database_app` does not |
"""

from __future__ import annotations

import re
from collections.abc import Callable, Iterable

from .manifest import IdentityToken


def _word(character: str) -> bool:
    return character.isascii() and (character.isalnum() or character == "_")


def _alnum(character: str) -> bool:
    return character.isascii() and character.isalnum()


def _before(text: str, start: int) -> str:
    return text[start - 1] if start > 0 else ""


def _after(text: str, end: int) -> str:
    return text[end] if end < len(text) else ""


def _dotted(text: str, start: int, end: int) -> bool:
    before = _before(text, start)
    return not (before and (_word(before) or before == ".")) and not _word(_after(text, end))


def _path(text: str, start: int, end: int) -> bool:
    if _word(_after(text, end)):
        return False
    before = _before(text, start)
    if not before or not _word(before):
        return True
    # A JVM descriptor: `L`, after up to three baseline-profile flags (H, S, P), after a non-word.
    if before != "L":
        return False
    index = start - 2
    flags = 0
    while index >= 0 and flags < 3 and text[index] in "HSP":
        index -= 1
        flags += 1
    return index < 0 or not _word(text[index])


def _prefix(text: str, start: int, end: int) -> bool:
    return not _word(_before(text, start))


def _snake(text: str, start: int, end: int) -> bool:
    return not _alnum(_before(text, start)) and not _alnum(_after(text, end))


BOUNDARIES: dict[str, Callable[[str, int, int], bool]] = {
    "dotted": _dotted,
    "path": _path,
    "prefix": _prefix,
    "snake": _snake,
}


class IdentityRenamer:
    def __init__(self, tokens: Iterable[IdentityToken], names: dict[str, str]):
        ordered = sorted(tokens, key=lambda token: len(token.source), reverse=True)
        self._tokens = {
            token.source: (BOUNDARIES[token.boundary], token.replacement.format(**names)) for token in ordered
        }
        self._pattern = (
            re.compile("|".join(re.escape(token.source) for token in ordered)) if ordered else None
        )

    def rename(self, text: str) -> str:
        if self._pattern is None:
            return text

        def replace(match: re.Match[str]) -> str:
            on_boundary, replacement = self._tokens[match.group(0)]
            return replacement if on_boundary(text, match.start(), match.end()) else match.group(0)

        return self._pattern.sub(replace, text)
