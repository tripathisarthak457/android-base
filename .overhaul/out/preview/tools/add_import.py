"""add_import.py <file> <fq.name>...: adds each import to the file's import run that holds the nearest import in
ASCII order (or the first run), keeps the run sorted, and skips imports already there."""
import sys

path, names = sys.argv[1], sys.argv[2:]
lines = open(path, encoding="utf-8", newline="").read().split("\n")
for name in names:
    line = f"import {name}"
    if line in lines:
        continue
    imports = [i for i, l in enumerate(lines) if l.startswith("import ")]
    before = [i for i in imports if lines[i] < line]
    at = (before[-1] + 1) if before else imports[0]
    lines.insert(at, line)
open(path, "w", encoding="utf-8", newline="").write("\n".join(lines))
