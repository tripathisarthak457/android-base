#!/usr/bin/env python3
"""
Compares generated projects before and after a generator change.

    golden_check.py BEFORE AFTER ALLOW

ALLOW holds one line per allowed difference: `<spec>/<relative path><TAB><reason>`, globs allowed.
A reason starting with "whitespace" is verified with `diff -wB`: only white space and blank lines
may differ. Any differing or missing path that no line explains fails the check.
"""
import fnmatch
import subprocess
import sys
from pathlib import Path

before, after, allow_path = (Path(arg) for arg in sys.argv[1:4])
entries = []
for number, line in enumerate(allow_path.read_text(encoding="utf-8").splitlines(), 1):
    if not line.strip() or line.startswith("#"):
        continue
    pattern, _, reason = line.partition("\t")
    if not reason:
        sys.exit(f"{allow_path}:{number}: no reason after a tab")
    entries.append((pattern, reason))

result = subprocess.run(["diff", "-rq", str(before), str(after)], capture_output=True, text=True)
problems, explained, used = [], 0, set()
for line in result.stdout.splitlines():
    if line.startswith("Files "):
        left = line[len("Files "):line.index(" and ")]
        relative = Path(left).relative_to(before).as_posix()
        kind = "differs"
    elif line.startswith("Only in "):
        directory, _, name = line[len("Only in "):].partition(": ")
        base = before if Path(directory).is_relative_to(before) else after
        relative = (Path(directory) / name).relative_to(base).as_posix()
        kind = f"only in {'before' if base == before else 'after'}"
    else:
        problems.append(f"unparsed diff output: {line}")
        continue
    if relative.split("/")[0].startswith("_"):
        continue
    match = next(((pattern, reason) for pattern, reason in entries if fnmatch.fnmatch(relative, pattern)), None)
    if match is None:
        problems.append(f"{relative}: {kind}, and no allow-list line explains it")
        continue
    used.add(match[0])
    if match[1].startswith("whitespace"):
        if kind != "differs":
            problems.append(f"{relative}: {kind}, but its allow-list line only allows white space")
            continue
        check = subprocess.run(["diff", "-wB", str(before / relative), str(after / relative)], capture_output=True)
        if check.returncode != 0:
            problems.append(f"{relative}: allowed as white space only, but more than white space changed")
            continue
    explained += 1

for pattern, _ in entries:
    if pattern not in used:
        print(f"note: allow-list line '{pattern}' matched nothing")
print(f"{explained} differences explained by the allow-list")
if problems:
    print("\n".join(problems))
    sys.exit(1)
print("GOLDEN OK")
