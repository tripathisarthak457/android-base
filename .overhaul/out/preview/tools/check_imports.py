"""List com.base.app imports that do not resolve in a working tree's templates/android (WP06's merge_imports.py logic).

    python check_imports.py <worktree>
"""
import pathlib
import re
import sys

DECL = re.compile(
    r"^(?:@\S+\s+)*(?:(?:public|internal|private|protected|data|sealed|enum|abstract|open|value|annotation|inline|"
    r"const|expect|actual|suspend|operator|infix|tailrec|override)\s+)*"
    r"(?:fun\s+interface|class|interface|object|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.<>?, *]+\.)?`?(\w+)`?",
    re.M,
)

root = pathlib.Path(sys.argv[1]) / "templates" / "android"
files = {p.relative_to(root).as_posix(): p.read_text(encoding="utf-8", errors="replace")
         for p in root.rglob("*.kt") if "build" not in p.relative_to(root).parts}
symbols, packages = set(), set()
for text in files.values():
    m = re.search(r"^package\s+([\w.]+)", text, re.M)
    if not m:
        continue
    pkg = m.group(1)
    packages.add(pkg)
    for d in DECL.finditer(text):
        symbols.add(f"{pkg}.{d.group(1)}")
bad = 0
for path, text in sorted(files.items()):
    for imp in re.findall(r"^import\s+(com\.base\.app\.[\w.]+)", text, re.M):
        parts = imp.split(".")
        ok = False
        for i in range(len(parts) - 1, 2, -1):
            pkg = ".".join(parts[:i])
            if pkg in packages:
                ok = f"{pkg}.{parts[i]}" in symbols
                break
        if not ok and not imp.endswith((".R", ".BuildConfig")):
            bad += 1
            print(f"{path}: {imp}")
print(f"{bad} unresolved")
