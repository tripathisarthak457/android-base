"""Merges an earlier recording's flags into each module's new baseline profile, as `profman --merge` merges profiles:
a method keeps its rule from the new recording, with the H, S and P flags of both recordings OR-ed. Methods only the
earlier recording names are left out (the code they named has moved or gone); class rules and wildcards are the new
file's. Prints, per module, how many methods gained the hot flag.

    python3 merge-profile-flags.py <old ref> <template dir>     (old ref: a commit of the worktree, e.g. 20d134a)
"""
import re
import subprocess
import sys

MODULES = ("core/navigation", "core/designsystem", "core/ui", "app")
RULE = re.compile(r"^([HSP]+)(L[^;]+;->.+)$")


def flags_by_method(text):
    found = {}
    for line in text.splitlines():
        match = RULE.match(line)
        if match and "**" not in line:
            found[match.group(2)] = found.get(match.group(2), "") + match.group(1)
    return found


def merged(new_flags, old_flags):
    both = set(new_flags) | set(old_flags)
    return "".join(flag for flag in "HSP" if flag in both)


def main(old_ref, template):
    for module in MODULES:
        relative = f"{module}/src/main/baseline-prof.txt"
        path = f"{template}/{relative}"
        old = subprocess.run(["git", "-C", template, "show", f"{old_ref}:./{relative}"],
                             capture_output=True, text=True, check=True).stdout
        old_flags = flags_by_method(old)
        with open(path, encoding="utf-8") as handle:
            lines = handle.read().splitlines()
        gained = 0
        out = []
        for line in lines:
            match = RULE.match(line)
            if match and "**" not in line and match.group(2) in old_flags:
                flags = merged(match.group(1), old_flags[match.group(2)])
                gained += "H" in flags and "H" not in match.group(1)
                line = flags + match.group(2)
            out.append(line)
        with open(path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write("\n".join(out) + "\n")
        print(f"{module}: {gained} methods hot again, {sum(1 for l in out if l.startswith('H'))} hot rules")


if __name__ == "__main__":
    main(*sys.argv[1:])
