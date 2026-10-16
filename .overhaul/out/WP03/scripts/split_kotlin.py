#!/usr/bin/env python3
"""
Splits top-level declarations out of a Kotlin file into new files in the same package.

    split_kotlin.py <file.kt> <NewFile.kt>=<from>-<to>[,<from>-<to>...] ...

Line ranges are 1-based and inclusive, in the original file. Each new file gets the package line
and the imports it uses; the original keeps everything not moved, and the imports it still uses.
An import is kept when its simple name appears in the code, or when it is an operator or delegate
import and the code uses that operator.
"""

import re
import sys
from pathlib import Path

OPERATOR_USE = {
    "getValue": r"\bby\b",
    "setValue": r"\bby\b",
    "provideDelegate": r"\bby\b",
    "times": r"\*",
    "div": r"/",
    "plus": r"\+",
    "minus": r"-",
    "unaryMinus": r"-",
    "contains": r"\bin\b",
    "rangeTo": r"\.\.",
    "compareTo": r"[<>]",
}
STRING_OR_COMMENT = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/')


def used_imports(imports: list[str], body: str) -> list[str]:
    # KDoc links ([Name]) count as uses: they resolve through imports too.
    code = STRING_OR_COMMENT.sub(lambda m: " ".join(re.findall(r"\[(\w+)", m.group(0))), body)
    kept = []
    for line in imports:
        name = line.split(" as ")[-1].rsplit(".", 1)[-1].strip()
        if re.search(rf"\b{re.escape(name)}\b", code) or (name in OPERATOR_USE and re.search(OPERATOR_USE[name], code)):
            kept.append(line)
    return kept


def main() -> int:
    source = Path(sys.argv[1])
    lines = source.read_text(encoding="utf-8").split("\n")
    package = next(line for line in lines if line.startswith("package "))
    imports = [line for line in lines if line.startswith("import ")]
    header_end = max(i for i, line in enumerate(lines) if line.startswith(("import ", "package "))) + 1
    taken: set[int] = set()

    for spec in sys.argv[2:]:
        name, ranges = spec.split("=")
        chunks = []
        for part in ranges.split(","):
            start, end = (int(n) for n in part.split("-"))
            chunks.append("\n".join(lines[start - 1:end]).strip("\n"))
            taken.update(range(start - 1, end))
        body = "\n\n".join(chunks) + "\n"
        head = [package, ""] + used_imports(imports, body)
        (source.parent / name).write_text("\n".join(head).rstrip("\n") + "\n\n" + body, encoding="utf-8")

    rest_lines = [line for i, line in enumerate(lines[header_end:], start=header_end) if i not in taken]
    rest = re.sub(r"\n{3,}", "\n\n", "\n".join(rest_lines)).strip("\n") + "\n"
    head = [package, ""] + used_imports(imports, rest)
    source.write_text("\n".join(head).rstrip("\n") + "\n\n" + rest, encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
