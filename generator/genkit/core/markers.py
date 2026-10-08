"""
The marker engine: what keeps an optional feature's lines in a project, or takes them out.

Shown with `//`; `#` and `<!-- -->` files use the same markers in their own comment syntax.

    // <opt:EXPR>            block start, alone on its line
    …
    // </opt:EXPR>           block end; EXPR must match the start exactly
    // <opt-else:EXPR>       optional, directly after the block end of the same EXPR
    //~ fallback()           emitted without the '//~ ' prefix when EXPR is false
    // </opt-else:EXPR>
    // <opt:EXPR>code()      inline: the rest of the line is emitted when EXPR is true
    // <generated:NAME>      point: replaced by the target's lines for NAME
    // <generated:NAME>      region: the default lines up to the end marker are replaced,
    default()                so the template itself still compiles
    // </generated:NAME>

    EXPR := TERM ('|' TERM)*      TERM := ATOM ('&' ATOM)*      ATOM := '!'? key

Every malformed marker is an error with the file and line, never a silently kept or dropped line.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path


class TemplateError(RuntimeError):
    """The template itself is wrong. A bug for whoever maintains it, never the user's input."""

    def __init__(self, path: Path | str, line: int | None, message: str):
        where = f"{path}:{line}" if line is not None else f"{path}"
        super().__init__(f"{where}: {message}")
        self.path = str(path)
        self.line = line


@dataclass(frozen=True)
class Syntax:
    key: str
    open: str
    close: str
    #: Whether blank lines left next to a removed block are tidied. Markdown renders the same
    #: either way, and the README's spacing is part of what it says.
    collapse: bool


SLASH = Syntax("//", "//", "", True)
HASH = Syntax("#", "#", "", True)
XML = Syntax("<!-- -->", "<!--", "-->", True)
MARKDOWN = Syntax("<!-- -->", "<!--", "-->", False)
#: Markers are not allowed: JSON has no comments, and a binary file is never read as text.
NONE = Syntax("none", "", "", False)

_BY_SUFFIX: dict[str, Syntax] = {
    **dict.fromkeys((".kt", ".kts", ".java", ".swift", ".gradle", ".aidl"), SLASH),
    **dict.fromkeys(
        (".toml", ".properties", ".yml", ".yaml", ".pro", ".sh", ".cfg", ".gitignore", ".gitattributes"),
        HASH,
    ),
    **dict.fromkeys((".xml", ".html", ".plist", ".storyboard", ".xib"), XML),
    ".md": MARKDOWN,
    **dict.fromkeys((".json", ".xcstrings"), NONE),
}

_BY_NAME: dict[str, Syntax] = {
    **dict.fromkeys(
        ("Podfile", "Gemfile", "Fastfile", "Appfile", "Brewfile", ".gitignore", ".gitattributes"), HASH
    ),
}

_BY_KEY: dict[str, Syntax] = {"//": SLASH, "#": HASH, "<!-- -->": XML, "markdown": MARKDOWN, "none": NONE}


def syntax_for(path: Path, overrides: dict[str, str] | None = None) -> Syntax | None:
    """The comment syntax markers use in `path`, or None when nothing is known about it."""
    for key in (path.name, path.suffix.lower()):
        if overrides and key in overrides:
            if overrides[key] not in _BY_KEY:
                raise TemplateError(path, None, f"unknown marker syntax '{overrides[key]}'")
            return _BY_KEY[overrides[key]]
    return _BY_NAME.get(path.name) or _BY_SUFFIX.get(path.suffix.lower())


#: Anything that looks like a marker. A line containing one must be exactly one valid marker.
MARKER_TEXT = re.compile(r"</?(?:opt|opt-else|generated):")

_KEY = r"[a-z][a-z0-9_-]*"
_EXPR = rf"!?{_KEY}(?:[|&]!?{_KEY})*"
_NAME = r"[a-z][a-z0-9-]*"


class _Patterns:
    """The marker forms for one comment syntax, compiled once."""

    def __init__(self, syntax: Syntax):
        lead = rf"^(?P<indent>[ \t]*){re.escape(syntax.open)}[ \t]*"
        tail = rf"[ \t]*{re.escape(syntax.close)}[ \t]*$" if syntax.close else r"[ \t]*$"
        self.block_start = re.compile(rf"{lead}<opt:(?P<expr>{_EXPR})>{tail}")
        self.block_end = re.compile(rf"{lead}</opt:(?P<expr>{_EXPR})>{tail}")
        self.else_start = re.compile(rf"{lead}<opt-else:(?P<expr>{_EXPR})>{tail}")
        self.else_end = re.compile(rf"{lead}</opt-else:(?P<expr>{_EXPR})>{tail}")
        self.generated_start = re.compile(rf"{lead}<generated:(?P<name>{_NAME})>{tail}")
        self.generated_end = re.compile(rf"{lead}</generated:(?P<name>{_NAME})>{tail}")
        if syntax.close:
            self.inline = re.compile(rf"{lead}<opt:(?P<expr>{_EXPR})>[ \t]*{re.escape(syntax.close)}(?P<rest>.*\S.*)$")
            self.else_line = re.compile(
                rf"^(?P<indent>[ \t]*){re.escape(syntax.open)}~ (?P<code>.*?)[ \t]*{re.escape(syntax.close)}[ \t]*$"
            )
        else:
            self.inline = re.compile(rf"{lead}<opt:(?P<expr>{_EXPR})>(?P<rest>.*\S.*)$")
            self.else_line = re.compile(rf"^(?P<indent>[ \t]*){re.escape(syntax.open)}~(?: (?P<code>.*))?$")


_PATTERNS: dict[str, _Patterns] = {}


def _patterns(syntax: Syntax) -> _Patterns:
    if syntax.key not in _PATTERNS:
        _PATTERNS[syntax.key] = _Patterns(syntax)
    return _PATTERNS[syntax.key]


@dataclass(frozen=True)
class Expression:
    """`a|b&!c`, parsed: any term true, where a term is all of its atoms."""

    text: str
    terms: tuple[tuple[tuple[str, bool], ...], ...]

    @classmethod
    def parse(cls, text: str, known: frozenset[str], path: Path | str, line: int) -> "Expression":
        terms = []
        for term in text.split("|"):
            atoms = []
            for atom in term.split("&"):
                negated = atom.startswith("!")
                key = atom[1:] if negated else atom
                if key not in known:
                    raise TemplateError(path, line, f"unknown feature '{key}' in <opt:{text}>")
                atoms.append((key, negated))
            terms.append(tuple(atoms))
        return cls(text, tuple(terms))

    def holds(self, enabled: frozenset[str]) -> bool:
        return any(all((key in enabled) != negated for key, negated in term) for term in self.terms)


@dataclass
class Rendered:
    """A file after its markers are resolved, before identity tokens are renamed."""

    #: (text with its line ending, True when the target generated it and it must not be renamed)
    lines: list[tuple[str, bool]]
    #: Output positions where lines were taken out, which is where blank lines may be tidied.
    seams: set[int] = field(default_factory=set)


@dataclass
class _Frame:
    kind: str
    expression: Expression
    #: The line it opened on, for "unclosed" errors.
    line: int
    #: Whether the lines inside are emitted, enclosing frames included.
    active: bool
    #: Whether everything around this frame was emitted, which an `opt-else` needs.
    parent_active: bool


def render(
    text: str,
    *,
    path: Path | str,
    syntax: Syntax,
    enabled: frozenset[str],
    known: frozenset[str],
    regions: dict[str, list[str] | None],
) -> Rendered:
    """
    Resolves every marker in `text`. `regions` holds the target's lines for each `generated` name;
    None keeps a region's default lines. Raises TemplateError on anything malformed.
    """
    lines = text.splitlines(keepends=True)
    if syntax is NONE:
        for number, line in enumerate(lines, start=1):
            if MARKER_TEXT.search(line):
                raise TemplateError(path, number, "markers are not allowed in this kind of file")
        return Rendered([(line, False) for line in lines])

    patterns = _patterns(syntax)
    result = Rendered([])
    out = result.lines
    stack: list[_Frame] = []
    #: The block that ended on the previous line, which is the only place an opt-else may start.
    just_closed: _Frame | None = None

    def active() -> bool:
        return all(frame.active for frame in stack)

    def removed() -> None:
        result.seams.add(len(out))

    index = 0
    while index < len(lines):
        raw = lines[index]
        body = raw.rstrip("\r\n")
        ending = raw[len(body):]
        number = index + 1
        closed, just_closed = just_closed, None
        index += 1

        if stack and stack[-1].kind == "else" and not MARKER_TEXT.search(body):
            if body.strip():
                match = patterns.else_line.match(body)
                if not match:
                    raise TemplateError(
                        path, number, f"every line inside <opt-else:{stack[-1].expression.text}> must "
                        f"start with '{syntax.open}~ '"
                    )
                code = match.group("code")
                body = match.group("indent") + code if code else ""
            if active():
                out.append((body + ending, False))
            else:
                removed()
            continue

        if not MARKER_TEXT.search(body):
            if active():
                out.append((raw, False))
            else:
                removed()
            continue

        if stack and stack[-1].kind == "else" and not patterns.else_end.match(body):
            raise TemplateError(
                path, number, f"only </opt-else:{stack[-1].expression.text}> may follow inside an opt-else block"
            )

        if match := patterns.block_start.match(body):
            expression = Expression.parse(match.group("expr"), known, path, number)
            enclosing = active()
            stack.append(_Frame("opt", expression, number, enclosing and expression.holds(enabled), enclosing))
            removed()
        elif match := patterns.block_end.match(body):
            expr = match.group("expr")
            if not stack or stack[-1].kind != "opt":
                raise TemplateError(path, number, f"</opt:{expr}> closes nothing")
            if stack[-1].expression.text != expr:
                raise TemplateError(
                    path, number,
                    f"</opt:{expr}> closes <opt:{stack[-1].expression.text}> from line {stack[-1].line}",
                )
            just_closed = stack.pop()
            removed()
        elif match := patterns.else_start.match(body):
            expr = match.group("expr")
            if closed is None or closed.expression.text != expr:
                raise TemplateError(path, number, f"<opt-else:{expr}> must directly follow </opt:{expr}>")
            stack.append(
                _Frame(
                    "else", closed.expression, number,
                    closed.parent_active and not closed.expression.holds(enabled), closed.parent_active,
                )
            )
            removed()
        elif match := patterns.else_end.match(body):
            expr = match.group("expr")
            if not stack or stack[-1].kind != "else" or stack[-1].expression.text != expr:
                raise TemplateError(path, number, f"</opt-else:{expr}> closes nothing")
            stack.pop()
            removed()
        elif match := patterns.inline.match(body):
            expression = Expression.parse(match.group("expr"), known, path, number)
            if active() and expression.holds(enabled):
                out.append((match.group("rest") + ending, False))
            else:
                removed()
        elif match := patterns.generated_start.match(body):
            name = match.group("name")
            end = _region_end(lines, index, name, patterns, path)
            default = lines[index:end] if end is not None else []
            index = end + 1 if end is not None else index
            if not active():
                removed()
                continue
            if name not in regions:
                raise TemplateError(path, number, f"the target supplies nothing for <generated:{name}>")
            supplied = regions[name]
            if supplied is None:
                out.extend((line, False) for line in default)
                if not default:
                    removed()
            elif supplied:
                out.extend((line if line.endswith("\n") else line + ending, True) for line in supplied)
            else:
                removed()
        elif patterns.generated_end.match(body):
            raise TemplateError(path, number, "a </generated:…> with no start")
        else:
            raise TemplateError(
                path, number,
                f"not a marker this engine understands, or a marker sharing its line with code "
                f"(markers in this file are written '{syntax.open} <opt:key>{(' ' + syntax.close) if syntax.close else ''}')",
            )

    if stack:
        frame = stack[-1]
        tag = "opt" if frame.kind == "opt" else "opt-else"
        raise TemplateError(path, frame.line, f"<{tag}:{frame.expression.text}> is never closed")
    return result


def _region_end(lines: list[str], start: int, name: str, patterns: _Patterns, path: Path | str) -> int | None:
    """The index of the line ending the region that starts before `start`, or None for a point."""
    for index in range(start, len(lines)):
        body = lines[index].rstrip("\r\n")
        if not MARKER_TEXT.search(body):
            continue
        end = patterns.generated_end.match(body)
        if end and end.group("name") == name:
            return index
        if patterns.generated_start.match(body):
            return None
        if end:
            raise TemplateError(path, index + 1, f"</generated:{end.group('name')}> inside <generated:{name}>")
        # Any other marker means the start was a point: a region holds plain default lines only.
        return None
    return None


def collapse(lines: list[tuple[str, bool]], seams: set[int]) -> list[tuple[str, bool]]:
    """
    Tidies the blank lines next to where something was removed, and nowhere else: a blank run is cut
    to one line, and dropped entirely after `{` or before `}`. Untouched code keeps its spacing.
    """
    def blank(index: int) -> bool:
        return not lines[index][0].strip()

    drop: set[int] = set()
    count = len(lines)
    for seam in seams:
        before = seam - 1
        while before >= 0 and blank(before):
            before -= 1
        after = seam
        while after < count and blank(after):
            after += 1
        run = list(range(before + 1, after))
        if not run:
            continue
        if after < count and lines[after][0].lstrip().startswith("}"):
            drop.update(run)
        elif before >= 0 and lines[before][0].rstrip().endswith("{"):
            drop.update(run)
        elif len(run) > 1:
            drop.update(run[1:])
    return [line for index, line in enumerate(lines) if index not in drop]
