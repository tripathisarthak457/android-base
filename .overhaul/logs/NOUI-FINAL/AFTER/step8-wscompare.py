"""
wscompare.py BEFORE_DIR AFTER_DIR: compares two generated projects file by file with all whitespace removed, and
lists every difference that is not whitespace or a line break, grouped by what it is.

Groups: a trailing comma ktlint added or removed (`,)`), a `{ return x }` body ktlint made `= x`, an import list in
another order (the same set of imports), and anything else, shown with its context.
"""
import difflib
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

SKIP_PARTS = {"build", ".gradle", ".kotlin", ".git", ".agents"}
TEXT = {".kt", ".kts", ".xml", ".toml", ".md", ".properties", ".pro", ".json", ".yml", ".yaml", ".txt", ".gradle"}


def files(root: Path):
    return {
        p.relative_to(root).as_posix(): p
        for p in root.rglob("*")
        if p.is_file() and not (SKIP_PARTS & set(p.relative_to(root).parts)) and p.suffix in TEXT
    }


def squash(text: str) -> str:
    return re.sub(r"\s+", "", text)


def imports_only(text: str) -> tuple[str, list[str]]:
    lines = text.split("\n")
    imports = sorted(l.strip() for l in lines if l.startswith("import "))
    rest = "\n".join(l for l in lines if not l.startswith("import "))
    return rest, imports


def punctuation(squashed: str) -> str:
    return re.sub(r"[{},;]", "", squashed.replace("{return", "=").replace("=return", "="))


def tokens(text: str) -> list[str]:
    return re.findall(r"[A-Za-z0-9_@$.\"'`]+|[^\sA-Za-z0-9_{},;]", text)


def classify(a: str, b: str) -> str:
    """What a non-whitespace difference is, from the squashed snippets on each side."""
    if a.replace(",", "") == b.replace(",", ""):
        return "trailing comma"
    if re.fullmatch(r"\{return", a) or ("return" in a and "=" in b and a.replace("{return", "=").replace("}", "") == b):
        return "expression body"
    return "other"


def main():
    before, after = Path(sys.argv[1]), Path(sys.argv[2])
    fb, fa = files(before), files(after)
    only_before = sorted(set(fb) - set(fa))
    only_after = sorted(set(fa) - set(fb))
    groups = Counter()
    others = defaultdict(list)
    changed_files = 0
    for rel in sorted(set(fb) & set(fa)):
        ta = fb[rel].read_bytes().decode("utf-8", "replace")
        tb = fa[rel].read_bytes().decode("utf-8", "replace")
        if ta == tb:
            continue
        changed_files += 1
        sa, sb = squash(ta), squash(tb)
        if sa == sb:
            groups["whitespace only"] += 1
            continue
        ra, ia = imports_only(ta)
        rb, ib = imports_only(tb)
        if ia == ib and squash(ra) == squash(rb):
            groups["import order"] += 1
            continue
        if punctuation(sa) == punctuation(sb):
            # ktlint's own punctuation: trailing commas, braces round a multi-line if branch, semicolons
            # between statements on one line, `{ return x }` as `= x`.
            groups["ktlint punctuation"] += 1
            continue
        if sorted(tokens(ta)) == sorted(tokens(tb)):
            groups["same tokens, moved (an annotation or a comment)"] += 1
            continue
        matcher = difflib.SequenceMatcher(a=sa, b=sb, autojunk=False)
        for tag, i1, i2, j1, j2 in matcher.get_opcodes():
            if tag == "equal":
                continue
            kind = classify(sa[i1:i2], sb[j1:j2])
            groups[kind] += 1
            if kind == "other":
                others[rel].append((sa[max(0, i1 - 40):i2 + 40], sb[max(0, j1 - 40):j2 + 40]))
    print(f"{before.name}: {changed_files} files differ; only before {len(only_before)}, only after {len(only_after)}")
    for kind, count in groups.most_common():
        print(f"  {count:5d}  {kind}")
    for rel in only_before:
        print("  ONLY BEFORE", rel)
    for rel in only_after:
        print("  ONLY AFTER", rel)
    for rel, items in others.items():
        for a, b in items:
            print(f"  OTHER {rel}\n     before: {a}\n     after:  {b}")


if __name__ == "__main__":
    main()
