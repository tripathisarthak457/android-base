#!/usr/bin/env python3
"""Prints every public `@Composable fun App*` of the design system as a markdown list, grouped by
package, with its full parameter list on one line. Usage: component_signatures.py <designsystem root>"""
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1]) / "src/main/kotlin/com/base/app/core/designsystem"
groups = {}
for path in sorted(root.rglob("*.kt")):
    text = path.read_text()
    for match in re.finditer(r"@Composable\s*\nfun (<[^>]+> )?(App\w+)\(", text):
        start = match.end()
        depth, i = 1, start
        while depth:
            depth += {"(": 1, ")": -1}.get(text[i], 0)
            i += 1
        params = re.sub(r"\s+", " ", text[start:i - 1]).strip().rstrip(",")
        params = re.sub(r"\(\s+", "(", params)
        package = str(path.parent.relative_to(root))
        generic = match.group(1) or ""
        groups.setdefault(package, []).append(f"`{match.group(2)}{('' if not generic else '')}({params})`")
for package in sorted(groups):
    print(f"- `{package}`")
    for signature in groups[package]:
        print(f"  - {signature}")
