"""List com.base.app imports that do not resolve in a (possibly conflicted) merged tree."""
import re
import subprocess
import sys

REPO = "D:/projects/tools/android-base"


def git(*args, inp=None):
    return subprocess.run(["git", "-C", REPO, *args], input=inp, capture_output=True, check=False).stdout


def merged_tree(ours, theirs):
    out = git("merge-tree", "--write-tree", "--name-only", "--no-messages", ours, theirs).decode()
    lines = out.splitlines()
    return lines[0], set(lines[1:])


def read_kt(tree):
    names = [n for n in git("ls-tree", "-r", "--name-only", tree, "--", "templates/android").decode().splitlines()
             if n.endswith(".kt") or n.endswith(".kts")]
    blobs = "".join(f"{tree}:{n}\n" for n in names).encode()
    out = git("cat-file", "--batch", inp=blobs)
    files = {}
    pos = 0
    for n in names:
        header_end = out.index(b"\n", pos)
        size = int(out[pos:header_end].split()[2])
        files[n] = out[header_end + 1:header_end + 1 + size].decode("utf-8", "replace")
        pos = header_end + 1 + size + 1
    return files


DECL = re.compile(
    r"^(?:@\S+\s+)*(?:(?:public|internal|private|protected|data|sealed|enum|abstract|open|value|annotation|inline|"
    r"const|expect|actual|suspend|operator|infix|tailrec|override)\s+)*"
    r"(?:fun\s+interface|class|interface|object|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.<>?, *]+\.)?`?(\w+)`?",
    re.M,
)


def main():
    ours, theirs = sys.argv[1], sys.argv[2]
    tree, conflicts = merged_tree(ours, theirs)
    files = read_kt(tree)
    symbols = set()
    packages = set()
    for text in files.values():
        m = re.search(r"^package\s+([\w.]+)", text, re.M)
        if not m:
            continue
        pkg = m.group(1)
        packages.add(pkg)
        for d in DECL.finditer(text):
            symbols.add(f"{pkg}.{d.group(1)}")
    for path, text in sorted(files.items()):
        for imp in re.findall(r"^import\s+(com\.base\.app\.[\w.]+)", text, re.M):
            parts = imp.split(".")
            ok = False
            for i in range(len(parts) - 1, 2, -1):
                pkg = ".".join(parts[:i])
                if pkg in packages:
                    ok = f"{pkg}.{parts[i]}" in symbols
                    break
            if not ok:
                flag = " [conflicted]" if path in conflicts else ""
                print(f"{path}: {imp}{flag}")


main()
