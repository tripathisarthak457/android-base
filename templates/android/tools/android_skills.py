#!/usr/bin/env python3
"""
Refreshes Google's Android skills in this project from https://github.com/android/skills:

    python3 tools/android_skills.py --tag v1.0.18

It clones that release tag into a temporary folder, which it deletes afterwards, and copies every
skill this project ships from git's objects rather than from a checkout, so the files keep the
bytes upstream committed on any system and whatever `core.autocrlf` says. Upstream's LICENSE.txt
goes beside each SKILL.md, because the skill's front matter points there and upstream keeps the
file only at its root. `.agents/skills/README.md`, `.agents/skills/MANIFEST.sha256` and the
pointers in `.claude/skills/` are rewritten, and the summary lists every file that changed.
Nothing from the download is run.

A skill upstream added since the last refresh is named in the summary and left out until you
pass `--add <name>`; a skill upstream dropped is removed. Read the summary and the diff, then keep
the "Google's Android skills" table in CLAUDE.md and AGENTS.md to one row per shipped skill.
"""

from __future__ import annotations

import argparse
import hashlib
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path, PurePosixPath

UPSTREAM = "https://github.com/android/skills"
SKILLS_DIR = ".agents/skills"
POINTERS_DIR = ".claude/skills"
LICENCE = "LICENSE.txt"
#: Files in SKILLS_DIR that are written here rather than copied, so the manifest leaves them out.
OWN_FILES = ("README.md", "MANIFEST.sha256")

#: Official skills that no project made from this base needs, with the reason.
LEFT_OUT = {
    "styles": "it needs an alpha Compose release and experimental opt-ins, and this project uses stable "
              "releases only",
    "migrate-xml-views-to-jetpack-compose": "it migrates XML views, and this project has none",
    "leanback-to-compose-tv-migration": "it migrates Leanback screens, and this project has none",
}


class SkillsError(Exception):
    """A checkout or a release this tool refuses to copy from, with the reason."""


@dataclass(frozen=True)
class Release:
    tag: str
    commit: str


def git(checkout: Path, *command: str, stdin: bytes | None = None) -> bytes:
    try:
        result = subprocess.run(["git", "-C", str(checkout), *command], input=stdin, capture_output=True)
    except FileNotFoundError:
        raise SkillsError("git is not on the PATH") from None
    if result.returncode != 0:
        raise SkillsError(f"git {command[0]} failed in {checkout}: {result.stderr.decode(errors='replace').strip()}")
    return result.stdout


def read_release(checkout: Path, expected_tag: str | None) -> Release:
    commit = git(checkout, "rev-parse", "HEAD").decode().strip()
    tags = git(checkout, "tag", "--points-at", "HEAD").decode().split()
    if expected_tag is not None and expected_tag not in tags:
        raise SkillsError(f"{checkout} is at {commit[:12]}, not at {expected_tag}")
    if not tags:
        raise SkillsError(f"{checkout} is at {commit[:12]}, which no release tag names; check out a tag")
    return Release(tag=expected_tag or sorted(tags)[-1], commit=commit)


def tree(checkout: Path, *paths: str) -> dict[str, str]:
    """Every regular file at HEAD under `paths` (all of them when none is given), path to blob id."""
    listing = git(checkout, "ls-tree", "-r", "-z", "--full-tree", "HEAD", "--", *paths)
    files: dict[str, str] = {}
    for record in listing.split(b"\0"):
        if not record:
            continue
        info, raw_path = record.split(b"\t", 1)
        mode, kind, blob = info.decode().split()
        path = raw_path.decode("utf-8")
        parts = PurePosixPath(path).parts
        if path.startswith("/") or ".." in parts or "\\" in path:
            raise SkillsError(f"upstream path {path!r} would land outside the skills folder")
        if kind != "blob" or mode not in ("100644", "100755"):
            raise SkillsError(f"upstream {path} is not a regular file (mode {mode}); refusing to copy it")
        files[path] = blob
    return files


def read_blobs(checkout: Path, files: dict[str, str]) -> dict[str, bytes]:
    """The exact bytes git stores for each file: no checkout, so no line-ending or filter conversion."""
    paths = sorted(files)
    output = git(checkout, "cat-file", "--batch", stdin="".join(f"{files[path]}\n" for path in paths).encode())
    contents: dict[str, bytes] = {}
    offset = 0
    for path in paths:
        end = output.index(b"\n", offset)
        blob, kind, size = output[offset:end].decode().split()
        if blob != files[path] or kind != "blob":
            raise SkillsError(f"git returned {kind} {blob} for {path}")
        start = end + 1
        contents[path] = output[start:start + int(size)]
        offset = start + int(size) + 1
    return contents


def upstream_skills(checkout: Path) -> dict[str, str]:
    """Each skill at HEAD, by name, with the folder that holds its SKILL.md."""
    files = tree(checkout)
    skills: dict[str, str] = {}
    for path, text in read_blobs(checkout, {p: b for p, b in files.items() if p.endswith("/SKILL.md")}).items():
        folder = str(PurePosixPath(path).parent)
        name = front_matter(text.decode("utf-8"), path)["name"].split(":", 1)[1].strip()
        if name != PurePosixPath(folder).name:
            raise SkillsError(f"upstream {path} is named {name!r}, not after its folder")
        if name in skills:
            raise SkillsError(f"upstream has two skills named {name!r}: {skills[name]} and {folder}")
        skills[name] = folder
    return skills


def export(checkout: Path, folders: list[str]) -> dict[str, bytes]:
    """LICENSE.txt and every file below `folders`, as git stores them."""
    return read_blobs(checkout, tree(checkout, LICENCE, *folders))


def front_matter(text: str, source: str) -> dict[str, str]:
    """The `name` and `description` lines of a SKILL.md, exactly as upstream wrote them."""
    lines = text.splitlines()
    if not lines or lines[0] != "---":
        raise SkillsError(f"{source} has no front matter")
    fields: dict[str, str] = {}
    current = None
    for line in lines[1:]:
        if line == "---":
            break
        if line.startswith((" ", "\t")) and current is not None:
            fields[current] += "\n" + line
            continue
        key = line.split(":", 1)[0]
        current = key if key in ("name", "description") else None
        if current is not None:
            fields[current] = line
    if set(fields) != {"name", "description"}:
        raise SkillsError(f"{source} lacks a name or a description")
    return fields


def vendor(root: Path, skills: dict[str, str], files: dict[str, bytes], release: Release) -> None:
    """Replaces the project's skills with `skills` (name to upstream folder) from `files`."""
    target = root / SKILLS_DIR
    pointers = root / POINTERS_DIR
    for directory in (target, pointers):
        if directory.exists():
            shutil.rmtree(directory)
    for name, folder in sorted(skills.items()):
        prefix = folder + "/"
        for path, content in files.items():
            if path.startswith(prefix):
                write(target / name / path[len(prefix):], content)
        write(target / name / LICENCE, files[LICENCE])
        skill_text = files[prefix + "SKILL.md"].decode("utf-8")
        write(pointers / name / "SKILL.md", pointer(name, front_matter(skill_text, prefix + "SKILL.md"), release))
    write(target / LICENCE, files[LICENCE])
    write(target / "README.md", readme(skills, release))
    write(target / "MANIFEST.sha256", manifest(target))


def pointer(name: str, fields: dict[str, str], release: Release) -> bytes:
    return (
        f"---\n{fields['name']}\n{fields['description']}\n---\n\n"
        f"Google's `{name}` skill is vendored at `{SKILLS_DIR}/{name}/` (Android skills {release.tag}, "
        "Apache-2.0).\n"
        f"First read the `{name}` row of the \"Google's Android skills\" table in `CLAUDE.md`: it lists where "
        "this project\n"
        "differs, and that table wins wherever the two disagree. Then read "
        f"`{SKILLS_DIR}/{name}/SKILL.md` and resolve every\n"
        "relative path in it from that folder.\n"
    ).encode("utf-8")


def readme(skills: dict[str, str], release: Release) -> bytes:
    rows = "".join(f"| `{name}` | `{folder}` |\n" for name, folder in sorted(skills.items()))
    left_out = "".join(f"- `{name}`: {reason}.\n" for name, reason in LEFT_OUT.items())
    return (
        "# Google's Android skills\n\n"
        f"Google's official Android skills for AI agents, from {UPSTREAM} at tag {release.tag}\n"
        f"(commit {release.commit}), under the Apache License 2.0 in `{LICENCE}`.\n\n"
        f"| Skill | Upstream folder |\n|---|---|\n{rows}\n"
        f"Left out on purpose:\n\n{left_out}\n"
        f"Each skill folder is an unmodified upstream copy. The only addition is `{LICENCE}` beside each "
        "`SKILL.md`,\nwhich the skill's front matter points at. `MANIFEST.sha256` holds the SHA-256 of every "
        "file below the skill\nfolders, so a test can prove they are unchanged.\n\n"
        "The \"Google's Android skills\" table in `CLAUDE.md` (and its mirror, `AGENTS.md`) says when to read "
        "each skill\nand how this project differs; where a skill disagrees with it, the table wins.\n\n"
        "Codex, Gemini CLI and Android Studio read this folder. Claude Code reads the pointers in "
        "`.claude/skills/`,\nwhich name these files, so this folder stays the only copy.\n\n"
        "To move every skill to a newer release, run `python3 tools/android_skills.py --tag <tag>` from the "
        "project root\nand review the diff. It never runs anything it downloads.\n"
    ).encode("utf-8")


def manifest(target: Path) -> bytes:
    entries = []
    for path in sorted(target.rglob("*")):
        relative = path.relative_to(target).as_posix()
        if path.is_file() and relative not in OWN_FILES:
            entries.append(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {relative}\n")
    return "".join(entries).encode("utf-8")


def write(path: Path, content: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)


def snapshot(root: Path, directories: tuple[str, ...] = (SKILLS_DIR, POINTERS_DIR)) -> dict[str, str]:
    files: dict[str, str] = {}
    for directory in directories:
        for path in (root / directory).rglob("*"):
            if path.is_file():
                files[path.relative_to(root).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    return files


def report(label: str, before: dict[str, str], after: dict[str, str]) -> None:
    added = sorted(set(after) - set(before))
    removed = sorted(set(before) - set(after))
    changed = sorted(path for path in set(before) & set(after) if before[path] != after[path])
    print(f"{label}: {len(added)} added, {len(changed)} changed, {len(removed)} removed")
    for sign, paths in (("+", added), ("~", changed), ("-", removed)):
        for path in paths:
            print(f"  {sign} {path}")


def shipped(root: Path) -> set[str]:
    target = root / SKILLS_DIR
    return {path.name for path in target.iterdir() if path.is_dir()} if target.is_dir() else set()


def refresh(root: Path, checkout: Path, tag: str, add: list[str]) -> int:
    release = read_release(checkout, tag)
    upstream = upstream_skills(checkout)
    unknown = sorted(set(add) - set(upstream))
    if unknown:
        raise SkillsError(f"{release.tag} has no skill named {', '.join(unknown)}")
    current = shipped(root)
    names = (current | set(add)) & set(upstream)
    files = export(checkout, [upstream[name] for name in sorted(names)])
    before = snapshot(root)
    vendor(root, {name: upstream[name] for name in names}, files, release)
    report(f"Android skills {release.tag}", before, snapshot(root))
    dropped = sorted(current - set(upstream))
    new = sorted(set(upstream) - names - set(LEFT_OUT))
    if dropped:
        print(f"Removed, because {release.tag} no longer has them: {', '.join(dropped)}")
    if new:
        print(f"Not shipped yet (add with --add <name>): {', '.join(new)}")
    if dropped or set(add) - current:
        print("Update the \"Google's Android skills\" table in CLAUDE.md and AGENTS.md to match.")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="android_skills", description=__doc__.strip().split("\n\n")[0])
    parser.add_argument("--tag", required=True, help="The release tag of android/skills to copy, such as v1.0.18.")
    parser.add_argument("--add", action="append", default=[], metavar="NAME",
                        help="Also ship this upstream skill. Repeat for more than one.")
    parser.add_argument("--checkout", type=Path,
                        help="Copy from this git checkout of android/skills instead of cloning the tag.")
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[1],
                        help="The project root (default: the folder above tools/).")
    args = parser.parse_args(argv)
    root = args.project.resolve()
    try:
        if args.checkout is not None:
            return refresh(root, args.checkout.expanduser().resolve(), args.tag, args.add)
        with tempfile.TemporaryDirectory(prefix="ask") as temporary:
            checkout = Path(temporary) / "skills"
            git(Path(temporary), "clone", "--quiet", "--depth", "1", "--no-checkout", "--branch", args.tag,
                UPSTREAM, str(checkout))
            return refresh(root, checkout, args.tag, args.add)
    except SkillsError as error:
        print(f"android_skills: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
