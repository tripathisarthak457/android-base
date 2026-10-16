"""Sorts each Kotlin file's imports as the codebase does: IntelliJ's default layout, everything else
first, then java, javax and kotlin, then alias imports, each group in lexicographic order."""
import sys


def group(line: str) -> int:
    path = line[len("import "):]
    if " as " in path:
        return 4
    if path.startswith("java."):
        return 1
    if path.startswith("javax."):
        return 2
    if path.startswith("kotlin."):
        return 3
    return 0


for path in sys.argv[1:]:
    text = open(path, encoding="utf-8").read()
    lines = text.split("\n")
    start = next((i for i, l in enumerate(lines) if l.startswith("import ")), None)
    if start is None:
        continue
    end = start
    while end < len(lines) and (lines[end].startswith("import ") or lines[end] == ""):
        end += 1
    while end > start and lines[end - 1] == "":
        end -= 1
    block = [l for l in lines[start:end] if l.startswith("import ")]
    ordered = sorted(set(block), key=lambda l: (group(l), l[len("import "):]))
    if ordered != lines[start:end]:
        lines[start:end] = ordered
        open(path, "w", encoding="utf-8").write("\n".join(lines))
        print("sorted", path)
