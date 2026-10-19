"""sort_imports.py <file>...: sorts each contiguous run of plain import lines in the template's layout (ktlint's
IntelliJ default: everything else in ASCII order, then java.*, javax.*, kotlin.*) and drops exact duplicates. Runs
split by comments (the generator's opt markers) stay where they are."""
import sys


def key(line):
    name = line[len("import "):]
    for rank, prefix in enumerate(("java.", "javax.", "kotlin."), start=1):
        if name.startswith(prefix):
            return rank, name
    return 0, name


for path in sys.argv[1:]:
    text = open(path, encoding="utf-8", newline="").read()
    lines = text.split("\n")
    out, run = [], []
    for line in lines + [None]:
        if line is not None and line.startswith("import "):
            run.append(line)
            continue
        if run:
            out.extend(sorted(dict.fromkeys(run), key=key))
            run = []
        if line is not None:
            out.append(line)
    new = "\n".join(out)
    if new != text:
        open(path, "w", encoding="utf-8", newline="").write(new)
        print("sorted:", path)
