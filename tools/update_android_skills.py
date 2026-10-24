#!/usr/bin/env python3
"""
Refreshes the Google Android skills the templates vendor, from a checkout of
https://github.com/android/skills:

    git clone --depth 1 --branch v1.0.17 https://github.com/android/skills <checkout>
    python3 tools/update_android_skills.py <checkout> --tag v1.0.17

Every project gets the whole official set except the skills in LEFT_OUT, so the templates ship
exactly SKILLS, and the tool stops when upstream has a skill that is in neither: add it to SKILLS,
or to LEFT_OUT in templates/android/tools/android_skills.py with its reason. The copying, the
README, the manifest and the pointers come from that file, which every generated project carries
as its own refresh command, so a project refreshed later writes the same files this tool wrote.
The files are read from git's objects, never from the checkout's working tree, so line endings
stay as upstream committed them whatever `core.autocrlf` says.

A template whose template.toml does not copy `.agents/skills/**` verbatim is skipped: the
generator would otherwise rename and marker-parse upstream's text. This repository's own pointers
into the Android copy are rewritten too.

The generator never runs this: run it by hand, read the summary and the diff, and update the
"Google's Android skills" table in each template's CLAUDE.md and AGENTS.md when a skill is added
or dropped.
"""

from __future__ import annotations

import argparse
import importlib.util
import shutil
import sys
import tomllib
from dataclasses import dataclass
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[1]
PROJECT_TOOL = REPOSITORY / "templates/android/tools/android_skills.py"


def load_project_tool():
    spec = importlib.util.spec_from_file_location("android_skills", PROJECT_TOOL)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


skills = load_project_tool()
SKILLS_DIR = skills.SKILLS_DIR
POINTERS_DIR = skills.POINTERS_DIR


@dataclass(frozen=True)
class Skill:
    name: str
    #: The folder in the upstream repository.
    upstream: str
    #: The Compose Multiplatform template ships it too.
    multiplatform: bool = True


#: The official set at v1.0.17 minus `skills.LEFT_OUT`. Every project ships all of them: a project
#: built from this base can grow into any of these areas, and a skill nobody reads costs only disk.
SKILLS = (
    Skill("adaptive", "jetpack-compose/adaptive"),
    # The skill itself says it does not apply to Kotlin Multiplatform projects.
    Skill("agp-9-upgrade", "build-system/agp/agp-9-upgrade", multiplatform=False),
    Skill("android-cli", "devtools/android-cli"),
    Skill("android-for-cars-parked-apps", "cars/android-for-cars-parked-apps"),
    Skill("android-intent-security", "security/android-intent-security"),
    Skill("android-permissions-security", "security/android-permissions-security"),
    Skill("android-profiler", "profilers/android-profiler"),
    Skill("anr-audit-and-fix", "performance/anr-audit-and-fix"),
    Skill("appfunctions", "device-ai/appfunctions"),
    Skill("camerax", "camera/camerax"),
    Skill("display-glasses-with-jetpack-compose-glimmer", "xr/display-glasses-with-jetpack-compose-glimmer"),
    Skill("edge-to-edge", "system/edge-to-edge"),
    Skill("engage-sdk-integration", "play/engage-sdk-integration"),
    Skill("glance-canonical-layouts", "system/glance-canonical-layouts"),
    Skill("jetpack-compose-performance", "jetpack-compose/jetpack-compose-performance"),
    Skill("litert-lm", "device-ai/litert/litert-lm"),
    Skill("litert-runtime", "device-ai/litert/litert-runtime"),
    Skill("media3-cast-integration", "media/media3-cast-integration"),
    Skill("ml-kit-genai-prompt-api", "device-ai/ml-kit-genai-prompt-api"),
    Skill("navigation-3", "navigation/navigation-3"),
    Skill("navigation-event", "navigation/navigation-event"),
    Skill("play-billing-library-version-upgrade", "play/play-billing-library-version-upgrade"),
    Skill("play-policy-insights", "play/play-policy-insights"),
    Skill("projected-hardware", "xr/projected-hardware"),
    Skill("r8-analyzer", "performance/r8-analyzer"),
    Skill("restore-credentials", "identity/restore-credentials"),
    Skill("testing-setup", "testing/testing-setup"),
    Skill("verified-email", "identity/verified-email"),
    Skill("wear-compose-m3", "wear/wear-compose-m3"),
    Skill("wear-widgets", "wear/wear-widgets"),
)

TEMPLATES = {"android": False, "multiplatform": True}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="update_android_skills", description=__doc__.split("\n\n")[0])
    parser.add_argument("checkout", type=Path, help="A git checkout of android/skills.")
    parser.add_argument("--tag", help="Fail unless the checkout is at exactly this tag.")
    args = parser.parse_args(argv)

    checkout = args.checkout.expanduser().resolve()
    try:
        release = skills.read_release(checkout, args.tag)
        check_planned(skills.upstream_skills(checkout))
        files = skills.export(checkout, [skill.upstream for skill in SKILLS])
    except skills.SkillsError as error:
        print(error, file=sys.stderr)
        return 1

    for template, multiplatform in TEMPLATES.items():
        root = REPOSITORY / "templates" / template
        if not root.is_dir():
            print(f"templates/{template}: not present, skipped")
            continue
        if not copies_skills_verbatim(root):
            print(f"templates/{template}: skipped, because its template.toml does not copy {SKILLS_DIR}/** verbatim")
            continue
        chosen = {skill.name: skill.upstream for skill in SKILLS if skill.multiplatform or not multiplatform}
        before = skills.snapshot(root)
        skills.vendor(root, chosen, files, release)
        skills.report(f"templates/{template}", before, skills.snapshot(root))

    before = skills.snapshot(REPOSITORY)
    write_repository_pointers(release)
    skills.report("repository pointers", before, skills.snapshot(REPOSITORY))
    return 0


def check_planned(upstream: dict[str, str]) -> None:
    """Upstream's skills are exactly SKILLS plus LEFT_OUT, each SKILLS entry in its planned folder."""
    problems = []
    for skill in SKILLS:
        if upstream.get(skill.name) != skill.upstream:
            found = upstream.get(skill.name)
            problems.append(f"{skill.name} is {'at ' + found if found else 'gone'}, not at {skill.upstream}")
    unplanned = sorted(set(upstream) - {skill.name for skill in SKILLS} - set(skills.LEFT_OUT))
    if unplanned:
        problems.append(f"new upstream, in neither SKILLS nor LEFT_OUT: {', '.join(unplanned)}")
    if problems:
        raise skills.SkillsError("; ".join(problems))


def copies_skills_verbatim(root: Path) -> bool:
    with (root / "template.toml").open("rb") as handle:
        manifest = tomllib.load(handle)
    return f"{SKILLS_DIR}/**" in manifest.get("files", {}).get("verbatim", [])


def repository_pointer(name: str, skill_file: Path, release) -> str:
    fields = skills.front_matter(skill_file.read_text(encoding="utf-8"), str(skill_file))
    folder = f"templates/android/{SKILLS_DIR}/{name}"
    return (
        f"---\n{fields['name']}\n{fields['description']}\n---\n\n"
        f"Google's `{name}` skill (Android skills {release.tag}, Apache-2.0) is vendored for the "
        f"generated projects at `{folder}/`.\n"
        f"Read `{folder}/SKILL.md` and resolve every relative path in it from that folder.\n"
        f"When you change a template, also read the `{name}` row of the \"Google's Android "
        "skills\" table in that template's `CLAUDE.md`:\n"
        "it lists where the generated projects differ, and it wins wherever the two disagree.\n"
    )


def write_repository_pointers(release) -> None:
    android = REPOSITORY / "templates/android" / SKILLS_DIR
    for directory in (SKILLS_DIR, POINTERS_DIR):
        if (REPOSITORY / directory).exists():
            shutil.rmtree(REPOSITORY / directory)
    for skill in SKILLS:
        text = repository_pointer(skill.name, android / skill.name / "SKILL.md", release).encode("utf-8")
        for directory in (SKILLS_DIR, POINTERS_DIR):
            skills.write(REPOSITORY / directory / skill.name / "SKILL.md", text)


if __name__ == "__main__":
    sys.exit(main())
