"""
fence_drift.py: for every Kotlin/Gradle template or variant file changed since HEAD, each line of code or comment
(other than markers) keyed by its text, with the fence conditions around it before and after. A line whose
conditions changed moved across a marker: the reformat would emit it in projects it was not meant for, or drop it.
"""
import re
import subprocess
from collections import Counter
from pathlib import Path

ROOT = Path("D:/projects/tools/.overhaul/wt/NOUI-FINAL")
OPEN = re.compile(r"^\s*// <(opt|opt-else):([^>]+)>\s*$")
CLOSE = re.compile(r"^\s*// </(opt|opt-else):([^>]+)>\s*$")
INLINE = re.compile(r"^\s*// <opt:([^>]+)>(.*\S.*)$")
FALLBACK = re.compile(r"^\s*//~ ?(.*)$")


def keyed(text):
    out = Counter()
    stack = []
    for line in text.split("\n"):
        if m := OPEN.match(line):
            stack.append(("" if m[1] == "opt" else "!") + m[2])
            continue
        if CLOSE.match(line):
            stack.pop()
            continue
        if line.strip().startswith("// <generated") or line.strip().startswith("// </generated"):
            continue
        cond = tuple(stack)
        if m := INLINE.match(line):
            cond = cond + (m[1],)
            body = m[2]
        elif m := FALLBACK.match(line):
            body = m[1]
        else:
            body = line
        body = re.sub(r"\s+", "", body)
        if not body or body in ("{", "}", ")", "),", "(", "]", "],"):
            continue
        out[(body, cond)] += 1
    return out


def main():
    changed = subprocess.run(["git", "-C", str(ROOT), "diff", "--name-only", "HEAD"], capture_output=True, text=True).stdout.split()
    for rel in changed:
        if not rel.endswith((".kt", ".kts")):
            continue
        new = (ROOT / rel).read_bytes().decode("utf-8") if (ROOT / rel).exists() else ""
        old = subprocess.run(["git", "-C", str(ROOT), "show", f"HEAD:{rel}"], capture_output=True, text=True, encoding="utf-8").stdout
        before, after = keyed(old), keyed(new)
        bodies_before = {}
        for (body, cond), n in before.items():
            bodies_before.setdefault(body, Counter())[cond] += n
        bodies_after = {}
        for (body, cond), n in after.items():
            bodies_after.setdefault(body, Counter())[cond] += n
        for body in set(bodies_before) & set(bodies_after):
            conds_before, conds_after = set(bodies_before[body]), set(bodies_after[body])
            normal = lambda conds: {tuple("|".join(sorted(c.split("|"))) for c in cond) for cond in conds}
            if normal(conds_before) != normal(conds_after):
                print(f"{rel}: {body[:90]}\n    before {dict(bodies_before[body])}\n    after  {dict(bodies_after[body])}")


if __name__ == "__main__":
    main()
