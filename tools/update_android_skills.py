#!/usr/bin/env python3
"""
Refreshes the Google Android skills the templates vendor, from a checkout of
https://github.com/android/skills:

    python3 tools/update_android_skills.py ~/src/android-skills --tag v1.0.14

Each skill folder is copied unmodified into `templates/<template>/.agents/skills/<name>/`, with
upstream's LICENSE.txt beside its SKILL.md, because the skill's front matter points there and
upstream keeps the file only at its root. README.md, MANIFEST.sha256 and the `.claude/skills/`
pointers are rewritten, and so are this repository's own pointers into the Android copy. A
template that does not exist yet is skipped.

The generator never runs this: run it by hand, read the summary and the diff, and update the
"Google's Android skills" table in each template's AGENTS.md when a skill is added or dropped.
"""

from __future__ import annotations

import argparse
import hashlib
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[1]
UPSTREAM = "https://github.com/android/skills"
SKILLS_DIR = ".agents/skills"
POINTERS_DIR = ".claude/skills"


@dataclass(frozen=True)
class Skill:
    name: str
    #: The folder in the upstream repository.
    upstream: str
    #: The Compose Multiplatform template ships it too.
    multiplatform: bool = True
    #: The feature whose projects get it; None for every project.
    feature: str | None = None


#: What the templates ship (STANDARDS-PLAN 4.1). `styles` is left out because it builds on an
#: experimental API, `android-cli` because its install step pipes a downloaded script into a shell,
#: and the rest because no template feature uses them.
SKILLS = (
    Skill("navigation-3", "navigation/navigation-3"),
    Skill("navigation-event", "navigation/navigation-event"),
    Skill("adaptive", "jetpack-compose/adaptive"),
    Skill("edge-to-edge", "system/edge-to-edge"),
    Skill("testing-setup", "testing/testing-setup"),
    Skill("android-profiler", "profilers/android-profiler"),
    Skill("r8-analyzer", "performance/r8-analyzer"),
    Skill("android-intent-security", "security/android-intent-security"),
    Skill("android-permissions-security", "security/android-permissions-security"),
    # The skill itself says it does not apply to Kotlin Multiplatform projects.
    Skill("agp-9-upgrade", "build-system/agp/agp-9-upgrade", multiplatform=False),
    Skill("restore-credentials", "identity/restore-credentials", feature="auth"),
    Skill("verified-email", "identity/verified-email", feature="auth"),
)

TEMPLATES = {"android": False, "multiplatform": True}


@dataclass(frozen=True)
class Release:
    tag: str
    commit: str


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="update_android_skills", description=__doc__.split("\n\n")[0])
    parser.add_argument("checkout", type=Path, help="A git checkout of android/skills.")
    parser.add_argument("--tag", help="Fail unless the checkout is at exactly this tag.")
    args = parser.parse_args(argv)

    checkout = args.checkout.expanduser().resolve()
    try:
        release = read_release(checkout, args.tag)
        missing = [skill.upstream for skill in SKILLS if not (checkout / skill.upstream / "SKILL.md").is_file()]
        if missing:
            raise SystemExit(f"{checkout} has no SKILL.md under: {', '.join(missing)}")
    except SystemExit as error:
        print(error, file=sys.stderr)
        return 1

    for template, multiplatform in TEMPLATES.items():
        root = REPOSITORY / "templates" / template
        if not root.is_dir():
            print(f"templates/{template}: not present, skipped")
            continue
        skills = [skill for skill in SKILLS if skill.multiplatform or not multiplatform]
        before = snapshot(root)
        vendor(checkout, root, skills, release)
        report(f"templates/{template}", before, snapshot(root))

    before = snapshot(REPOSITORY, (SKILLS_DIR, POINTERS_DIR))
    write_repository_pointers(release)
    report("repository pointers", before, snapshot(REPOSITORY, (SKILLS_DIR, POINTERS_DIR)))
    return 0


def read_release(checkout: Path, expected_tag: str | None) -> Release:
    def git(*command: str) -> str:
        result = subprocess.run(["git", "-C", str(checkout), *command], capture_output=True, text=True)
        if result.returncode != 0:
            raise SystemExit(f"{checkout} is not a git checkout of {UPSTREAM}: {result.stderr.strip()}")
        return result.stdout.strip()

    commit = git("rev-parse", "HEAD")
    tag = git("tag", "--points-at", "HEAD").splitlines()
    if expected_tag is not None and expected_tag not in tag:
        raise SystemExit(f"{checkout} is at {commit[:12]}, not at {expected_tag}")
    if not tag:
        raise SystemExit(f"{checkout} is at {commit[:12]}, which no release tag names; check out a tag")
    return Release(tag=expected_tag or sorted(tag)[-1], commit=commit)


def vendor(checkout: Path, root: Path, skills: list[Skill], release: Release) -> None:
    target = root / SKILLS_DIR
    pointers = root / POINTERS_DIR
    for directory in (target, pointers):
        if directory.exists():
            shutil.rmtree(directory)
    licence = checkout / "LICENSE.txt"

    for skill in skills:
        destination = target / skill.name
        shutil.copytree(checkout / skill.upstream, destination, copy_function=shutil.copyfile)
        shutil.copyfile(licence, destination / "LICENSE.txt")
        write(pointers / skill.name / "SKILL.md", template_pointer(skill, destination / "SKILL.md", release))
    shutil.copyfile(licence, target / "LICENSE.txt")
    write(target / "README.md", readme(skills, release))
    write(target / "MANIFEST.sha256", manifest(target))


def front_matter(skill_file: Path) -> dict[str, str]:
    """The `name` and `description` lines of a SKILL.md, exactly as upstream wrote them."""
    lines = skill_file.read_text(encoding="utf-8").splitlines()
    if not lines or lines[0] != "---":
        raise SystemExit(f"{skill_file} has no front matter")
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
        raise SystemExit(f"{skill_file} lacks a name or a description")
    return fields


def template_pointer(skill: Skill, skill_file: Path, release: Release) -> str:
    fields = front_matter(skill_file)
    return (
        f"---\n{fields['name']}\n{fields['description']}\n---\n\n"
        f"Google's `{skill.name}` skill is vendored at `{SKILLS_DIR}/{skill.name}/` (Android skills "
        f"{release.tag}, Apache-2.0).\n"
        f"First read the `{skill.name}` row of the \"Google's Android skills\" table in `AGENTS.md`: it "
        "lists where this project\n"
        "differs, and `AGENTS.md` wins wherever the two disagree. Then read "
        f"`{SKILLS_DIR}/{skill.name}/SKILL.md` and resolve every\n"
        "relative path in it from that folder.\n"
    )


def repository_pointer(skill: Skill, skill_file: Path, release: Release) -> str:
    fields = front_matter(skill_file)
    folder = f"templates/android/{SKILLS_DIR}/{skill.name}"
    return (
        f"---\n{fields['name']}\n{fields['description']}\n---\n\n"
        f"Google's `{skill.name}` skill (Android skills {release.tag}, Apache-2.0) is vendored for the "
        f"generated projects at `{folder}/`.\n"
        f"Read `{folder}/SKILL.md` and resolve every relative path in it from that folder.\n"
        f"When you change a template, also read the `{skill.name}` row of the \"Google's Android "
        "skills\" table in that template's `AGENTS.md`:\n"
        "it lists where the generated projects differ, and it wins wherever the two disagree.\n"
    )


def write_repository_pointers(release: Release) -> None:
    android = REPOSITORY / "templates/android" / SKILLS_DIR
    for directory in (SKILLS_DIR, POINTERS_DIR):
        if (REPOSITORY / directory).exists():
            shutil.rmtree(REPOSITORY / directory)
    for skill in SKILLS:
        text = repository_pointer(skill, android / skill.name / "SKILL.md", release)
        for directory in (SKILLS_DIR, POINTERS_DIR):
            write(REPOSITORY / directory / skill.name / "SKILL.md", text)


def readme(skills: list[Skill], release: Release) -> str:
    rows = "".join(
        f"| `{skill.name}` | `{skill.upstream}` | "
        f"{f'with the `{skill.feature}` feature' if skill.feature else 'always'} |\n"
        for skill in skills
    )
    return (
        "# Google's Android skills\n\n"
        "Google's official Android skills for AI agents, from "
        f"{UPSTREAM} at tag {release.tag}\n(commit {release.commit}), under the Apache License 2.0 "
        "in `LICENSE.txt`.\n\n"
        f"| Skill | Upstream folder | Shipped |\n|---|---|---|\n{rows}\n"
        "Each skill folder is an unmodified upstream copy. The only addition is `LICENSE.txt` beside "
        "each `SKILL.md`,\nwhich the skill's front matter points at. `MANIFEST.sha256` holds the "
        "SHA-256 of every file below the skill\nfolders, so a test can prove they are unchanged.\n\n"
        "Where a skill disagrees with `AGENTS.md`, `AGENTS.md` wins. Its \"Google's Android skills\" "
        "table says when to\nread each skill and how this project differs.\n\n"
        "Codex, Gemini CLI and Android Studio read this folder. Claude Code reads the pointers in "
        "`.claude/skills/`,\nwhich name these files, so this folder stays the only copy.\n\n"
        "To refresh a skill, replace its folder with a newer upstream copy (`android skills add <name> "
        "--project=.` with\nthe Android CLI, or a checkout of the repository), put `LICENSE.txt` back "
        "beside its `SKILL.md`, and\nregenerate `MANIFEST.sha256` (`sha256sum` over every file below "
        "the skill folders, paths relative to this\nfolder).\n"
    )


def manifest(target: Path) -> str:
    entries = []
    for path in sorted(target.rglob("*")):
        relative = path.relative_to(target).as_posix()
        if path.is_file() and relative not in ("README.md", "MANIFEST.sha256"):
            entries.append(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {relative}\n")
    return "".join(entries)


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


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


if __name__ == "__main__":
    sys.exit(main())
