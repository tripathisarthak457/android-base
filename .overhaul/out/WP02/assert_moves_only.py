#!/usr/bin/env python3
"""
Reads `git diff --cached -M --name-status` on stdin and fails unless every Kotlin entry is a pure
move (R100): the modular split may move sources but never change their text.
"""

import sys

failures = []
moves = 0
for line in sys.stdin:
    parts = line.rstrip("\n").split("\t")
    status, paths = parts[0], parts[1:]
    if not any(path.endswith(".kt") for path in paths):
        continue
    if status == "R100":
        moves += 1
    else:
        failures.append(line.rstrip())

if failures:
    print("Kotlin files changed, not just moved:", *failures, sep="\n  ")
    sys.exit(1)
if moves == 0:
    print("no Kotlin file moved: the split did nothing")
    sys.exit(1)
print(f"{moves} Kotlin files moved unchanged")
