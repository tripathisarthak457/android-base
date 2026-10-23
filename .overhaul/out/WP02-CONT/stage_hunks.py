"""Stages only the hunks of one file's diff whose text contains a marker.

    python stage_hunks.py <repo> <path> <marker> [<marker>...]
"""

import subprocess
import sys

repo, path, markers = sys.argv[1], sys.argv[2], sys.argv[3:]
diff = subprocess.run(["git", "-C", repo, "diff", "--", path], capture_output=True, check=True).stdout.decode("utf-8")
lines = diff.splitlines(keepends=True)
head_end = next(i for i, line in enumerate(lines) if line.startswith("@@"))
header, hunks, current = lines[:head_end], [], []
for line in lines[head_end:]:
    if line.startswith("@@") and current:
        hunks.append(current)
        current = []
    current.append(line)
hunks.append(current)
chosen = [h for h in hunks if any(m in "".join(h) for m in markers)]
if not chosen:
    sys.exit(f"no hunk of {path} contains {markers}")
patch = "".join(header) + "".join("".join(h) for h in chosen)
# Bytes: a text pipe on Windows would turn every newline into CRLF and the patch would not apply.
subprocess.run(["git", "-C", repo, "apply", "--cached", "-"], input=patch.encode("utf-8"), check=True)
print(f"staged {len(chosen)} of {len(hunks)} hunks of {path}")
