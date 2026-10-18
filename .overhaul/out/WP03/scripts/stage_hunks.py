"""
Stages some hunks of one file's working-tree diff, as `git add -p` would without a terminal.

    stage_hunks.py <repo> <path> --list
    stage_hunks.py <repo> <path> <index> [<index>...] [--drop REGEX]

--list prints each hunk's index and its first changed lines. Indices count from 0 in the diff
against the index (what is already staged is left alone). --drop leaves out the added lines of the
picked hunks that match REGEX, for a hunk that mixes two changes.
"""

import re
import subprocess
import sys


def hunks(repo, path):
    # Bytes, not text: on Windows a text pipe would turn every newline into CRLF and break the patch.
    diff = subprocess.run(["git", "-C", repo, "diff", "--", path], capture_output=True, check=True).stdout.decode("utf-8")
    lines = diff.splitlines(keepends=True)
    header, chunks, current = [], [], None
    for line in lines:
        if line.startswith("@@"):
            current = [line]
            chunks.append(current)
        elif current is None:
            header.append(line)
        else:
            current.append(line)
    return header, chunks


def main():
    repo, path, *rest = sys.argv[1:]
    header, chunks = hunks(repo, path)
    if rest == ["--list"]:
        for i, chunk in enumerate(chunks):
            changed = [l.rstrip("\n") for l in chunk[1:] if l[:1] in "+-"][:4]
            print(f"[{i}] {chunk[0].strip()}")
            for l in changed:
                print(f"      {l[:110]}")
        return
    drop = None
    if "--drop" in rest:
        at = rest.index("--drop")
        drop = re.compile(rest[at + 1])
        rest = rest[:at] + rest[at + 2:]
    picked = [chunks[int(i)] for i in rest]
    if drop is not None:
        picked = [[l for l in c if not (l.startswith("+") and drop.search(l))] for c in picked]
    patch = "".join(header) + "".join("".join(c) for c in picked)
    subprocess.run(["git", "-C", repo, "apply", "--cached", "--recount", "-"], input=patch.encode("utf-8"), check=True)


if __name__ == "__main__":
    main()
