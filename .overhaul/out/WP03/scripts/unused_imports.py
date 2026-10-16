#!/usr/bin/env python3
"""Lists imports whose simple name is not used elsewhere in the file. Usage: unused_imports.py <dir>..."""
import re
import sys
from pathlib import Path

for root in sys.argv[1:]:
    for path in sorted(Path(root).rglob("*.kt")):
        text = path.read_text(encoding="utf-8")
        body = "\n".join(l for l in text.splitlines() if not l.startswith("import ") and not l.startswith("package "))
        for m in re.finditer(r"^import ([\w.]+?)(?: as (\w+))?$", text, re.M):
            name = m.group(2) or m.group(1).rsplit(".", 1)[-1]
            if name in ("getValue", "setValue"):
                if " by " not in body:
                    print(f"{path}: {m.group(0)}")
                continue
            if not re.search(r"(?<![\w])" + re.escape(name) + r"(?![\w])", body):
                print(f"{path}: {m.group(0)}")
