#!/usr/bin/env python3
"""Renames UPPER_SNAKE constants to PascalCase in Compose modules (CODE-STANDARDS K-4).

Usage: pascal_constants.py <root> [<root> ...] [--check]

Each root is a source tree (for example core/designsystem/src). Every `val`/`const val` whose name is
upper case is renamed to PascalCase across all roots given, as a whole word. A rename whose new name is
already used as an identifier in a file it would touch is reported and skipped.
"""
import pathlib
import re
import sys

DECL = re.compile(r"\b(?:const\s+val|val)\s+([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+|[A-Z][A-Z0-9]+)\b")


def pascal(name: str) -> str:
    return "".join(part[:1] + part[1:].lower() for part in name.split("_") if part)


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    check = "--check" in sys.argv
    files = [p for root in args for p in pathlib.Path(root).rglob("*.kt") if "/build/" not in str(p)]
    texts = {p: p.read_text() for p in files}
    names = {}
    for path, text in texts.items():
        for match in DECL.finditer(text):
            name = match.group(1)
            if name.isupper() and len(name) > 1:
                names.setdefault(name, set()).add(path)
    if check:
        for name, paths in sorted(names.items()):
            for path in sorted(paths):
                print(f"{path}: {name}")
        return 1 if names else 0
    renamed = 0
    for name in sorted(names, key=len, reverse=True):
        new = pascal(name)
        # Unqualified uses and the project's own holder; never a platform constant that shares the
        # name (LazyThreadSafetyMode.NONE).
        word = re.compile(rf"(?:(?<![\w.])|(?<=DecimalInput\.)){re.escape(name)}\b")
        imported = re.compile(rf"^(import com\.base\.app[\w.]*\.){re.escape(name)}$", re.M)
        touched = [p for p, t in texts.items() if word.search(t) or imported.search(t)]
        clash = [p for p in touched if re.search(rf"\b{re.escape(new)}\b", texts[p])]
        if clash:
            print(f"skip {name} -> {new}: already used in {', '.join(map(str, clash))}")
            continue
        for path in touched:
            texts[path] = imported.sub(rf"\g<1>{new}", word.sub(new, texts[path]))
        renamed += 1
    for path, text in texts.items():
        if text != path.read_text():
            path.write_text(text)
    print(f"renamed {renamed} of {len(names)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
