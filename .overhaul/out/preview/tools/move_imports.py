"""Rewrites imports of moved symbols and re-sorts each contiguous run of plain import lines (ASCII order, as the
template's files keep them). Usage: move_imports.py <old.pkg.Symbol=new.pkg.Symbol>... -- <file>..."""
import re
import sys

args = sys.argv[1:]
split = args.index("--")
moves = dict(pair.split("=", 1) for pair in args[:split])
for path in args[split + 1:]:
    text = open(path, encoding="utf-8", newline="").read()
    lines = text.split("\n")
    changed = False
    for i, line in enumerate(lines):
        m = re.fullmatch(r"import ([\w.]+)( as \w+)?", line)
        if m and m.group(1) in moves:
            lines[i] = f"import {moves[m.group(1)]}{m.group(2) or ''}"
            changed = True
    if not changed:
        continue
    out, run = [], []
    for line in lines + [None]:
        if line is not None and line.startswith("import "):
            run.append(line)
            continue
        if run:
            seen = []
            for item in sorted(run):
                if item not in seen:
                    seen.append(item)
            out.extend(seen)
            run = []
        if line is not None:
            out.append(line)
    open(path, "w", encoding="utf-8", newline="").write("\n".join(out))
    print("moved:", path)
