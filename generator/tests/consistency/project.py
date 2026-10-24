"""A generated project read for static checks: its Kotlin and Gradle sources, with comments and strings blanked."""

from __future__ import annotations

import re
from collections import defaultdict
from pathlib import Path

from genkit.core.copy import is_verbatim
from genkit.targets import get

#: Upstream files the template vendors byte for byte, such as the Kotlin samples in Google's skills: not the project's
#: code, so never read as its sources.
VERBATIM = get("android").manifest.verbatim

DECLARATION = re.compile(
    r"^[ \t]*(?:@[\w.]+(?:\([^)]*\))?[ \t]+)*"
    r"(?:(?:public|internal|private|protected|abstract|open|sealed|data|enum|annotation|inline|value|inner|const"
    r"|override|suspend|operator|infix|tailrec|external|lateinit|fun(?=[ \t]+interface))[ \t]+)*"
    r"(class|interface|object|typealias|fun|val|var)[ \t]+(?:<[^>]*>[ \t]*)?(?:[\w.<>?, *]+\.)?(`?\w+`?)",
    re.M,
)
PACKAGE = re.compile(r"^package[ \t]+([\w.]+)", re.M)
IMPORT = re.compile(r"^import[ \t]+([\w.`]+)(?:[ \t]+as[ \t]+(\w+))?[ \t]*$", re.M)


def strip_code(text: str) -> str:
    """Comments removed and string contents blanked, keeping `${…}` template expressions."""
    out = []
    index, length = 0, len(text)
    while index < length:
        if text.startswith("//", index):
            end = text.find("\n", index)
            index = length if end < 0 else end
        elif text.startswith("/*", index):
            end = text.find("*/", index + 2)
            index = length if end < 0 else end + 2
            out.append(" ")
        elif text.startswith('"""', index):
            end = text.find('"""', index + 3)
            out.append('""' + _templates(text[index + 3:(length if end < 0 else end)]) + '""')
            index = length if end < 0 else end + 3
        elif text[index] == '"':
            end = index + 1
            while end < length and text[end] not in '"\n':
                end += 2 if text[end] == "\\" else 1
            out.append('"' + _templates(text[index + 1:end]) + '"')
            index = end + 1
        elif text[index] == "'" and index + 2 < length and (text[index + 2] == "'" or text[index + 1] == "\\"):
            end = text.find("'", index + 2 if text[index + 1] == "\\" else index + 1)
            out.append("' '")
            index = end + 1 if end > 0 else length
        else:
            out.append(text[index])
            index += 1
    return "".join(out)


def _templates(body: str) -> str:
    parts = re.findall(r"\$\{([^}]*)\}", body) + re.findall(r"\$([A-Za-z_]\w*)", body)
    return " " + " ".join(parts) + " " if parts else ""


class Project:
    def __init__(self, root: Path):
        self.root = root
        self.kotlin = sorted(path for path in root.rglob("*.kt") if not self._verbatim(path))
        self.gradle = sorted(path for path in root.rglob("*.kts") if not self._verbatim(path))
        self.text = {path: path.read_text(encoding="utf-8") for path in self.kotlin + self.gradle}
        self.code = {path: strip_code(text) for path, text in self.text.items()}
        #: package → names declared in it.
        self.declared: dict[str, set[str]] = defaultdict(set)
        #: name → files declaring it.
        self.declared_in: dict[str, set[Path]] = defaultdict(set)
        #: (package, name) for every top-level declaration that is not private.
        self.top_level: set[tuple[str, str]] = set()
        self.package_of: dict[Path, str] = {}
        for path in self.kotlin:
            match = PACKAGE.search(self.text[path])
            package = match.group(1) if match else ""
            self.package_of[path] = package
            code = self.code[path]
            for declaration in DECLARATION.finditer(code):
                name = declaration.group(2).strip("`")
                self.declared[package].add(name)
                self.declared_in[name].add(path)
                line_start = code.rfind("\n", 0, declaration.start()) + 1
                prefix = code[line_start:declaration.start(1)]
                if prefix == prefix.lstrip() and not prefix.strip().startswith("private"):
                    self.top_level.add((package, name))
        self.namespaces = {
            match.group(1)
            for path in self.gradle
            for match in re.finditer(r'namespace\s*=\s*"([^"]+)"', self.text[path])
        }

    def relative(self, path: Path) -> str:
        return path.relative_to(self.root).as_posix()

    def _verbatim(self, path: Path) -> bool:
        return is_verbatim(self.relative(path), VERBATIM)
