#!/usr/bin/env python3
"""Import helpers for Kotlin files, in the template's order (IntelliJ's: everything else, then
java, javax, kotlin, each alphabetical). Never touches an import block that holds marker comments.

  kimports.py sort <file>...            re-sort clean import blocks
  kimports.py add <file> <fqname>...    add imports to the first contiguous run, in order
  kimports.py drop-unused <file>...     remove simple-name imports the file no longer uses
"""
import re
import sys


def key(line):
    name = line[len("import "):].strip()
    if name.startswith("java."):
        group = 1
    elif name.startswith("javax."):
        group = 2
    elif name.startswith("kotlin."):
        group = 3
    else:
        group = 0
    return (group, name)


def runs(lines):
    i = 0
    while i < len(lines):
        if lines[i].startswith("import "):
            j = i
            while j + 1 < len(lines) and lines[j + 1].startswith("import "):
                j += 1
            yield i, j
            i = j + 1
        else:
            i += 1


def sort_file(path):
    lines = open(path).read().split("\n")
    block = [i for i, l in enumerate(lines) if l.startswith("import ")]
    if not block:
        return
    region = lines[block[0]:block[-1] + 1]
    if any(not l.startswith("import ") and l.strip() for l in region):
        return  # markers inside: leave as is
    imps = sorted(set(l for l in region if l.startswith("import ")), key=key)
    lines[block[0]:block[-1] + 1] = imps
    open(path, "w").write("\n".join(lines))


def add(path, names):
    lines = open(path).read().split("\n")
    first = next(runs(lines))
    i, j = first
    run = lines[i:j + 1]
    for n in names:
        imp = "import " + n
        if imp not in lines:
            run.append(imp)
    run = sorted(set(run), key=key)
    lines[i:j + 1] = run
    open(path, "w").write("\n".join(lines))


def drop_unused(path):
    text = open(path).read()
    lines = text.split("\n")
    body = "\n".join(l for l in lines if not l.startswith("import "))
    out = []
    for l in lines:
        if l.startswith("import ") and " as " not in l:
            simple = l.split(".")[-1].strip()
            if simple != "*" and not re.search(r"\b" + re.escape(simple) + r"\b", body):
                continue
        out.append(l)
    open(path, "w").write("\n".join(out))


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "sort":
        for p in sys.argv[2:]:
            sort_file(p)
    elif cmd == "add":
        add(sys.argv[2], sys.argv[3:])
    elif cmd == "drop-unused":
        for p in sys.argv[2:]:
            drop_unused(p)
