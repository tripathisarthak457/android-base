"""
Every version in every template's catalogue and Gradle wrapper, against the newest stable release.

    python3 tools/check_versions.py            # report
    python3 tools/check_versions.py --apply    # rewrite the catalogues and wrappers to the newest stable
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import re
import sys
import tomllib
import urllib.request
from pathlib import Path

TEMPLATES = Path(__file__).resolve().parent.parent / "templates"

GOOGLE = "https://dl.google.com/android/maven2/"
CENTRAL = "https://repo1.maven.org/maven2/"
PLUGINS = "https://plugins.gradle.org/m2/"

#: Where each group publishes, longest prefix first. A group missing here is looked up everywhere.
REPOSITORIES_BY_GROUP: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("androidx.room3", (GOOGLE,)),
    ("androidx", (GOOGLE,)),
    ("com.android", (GOOGLE,)),
    ("com.google.android", (GOOGLE,)),
    ("com.google.firebase", (GOOGLE,)),
    ("com.google.gms", (GOOGLE,)),
    ("com.google.dagger", (CENTRAL,)),
    ("io.github.kyant0", (CENTRAL,)),
    ("io.insert-koin", (CENTRAL,)),
    ("io.ktor", (CENTRAL,)),
    ("io.coil-kt", (CENTRAL,)),
    ("org.jetbrains.androidx", (CENTRAL,)),
    ("org.jetbrains.compose", (CENTRAL, PLUGINS)),
    ("org.jetbrains.kotlin", (CENTRAL, PLUGINS)),
    ("org.jetbrains.kotlinx", (CENTRAL,)),
)
_EVERYWHERE = (GOOGLE, CENTRAL, PLUGINS)

GRADLE_RELEASES = "https://services.gradle.org/versions/current"

_UNSTABLE = re.compile(r"(alpha|beta|rc|dev|eap|snapshot|preview|-m\d|\.m\d|-b\d)", re.IGNORECASE)
_WRAPPER_VERSION = re.compile(r"(distributionUrl=.*?gradle-)([0-9][^-]*)(-(?:bin|all)\.zip)")


def _ordering(version: str) -> list[tuple[int, int | str]]:
    """Numbers compare as numbers, anything else as text, so 1.10 sorts after 1.9."""
    return [(0, int(part)) if part.isdigit() else (1, part) for part in re.split(r"[.\-]", version)]


def repositories(group: str) -> tuple[str, ...]:
    for prefix, found in REPOSITORIES_BY_GROUP:
        if group == prefix or group.startswith(prefix + "."):
            return found
    return _EVERYWHERE


def catalogues() -> list[Path]:
    return sorted(TEMPLATES.glob("*/gradle/libs.versions.toml"))


def wrappers() -> list[Path]:
    return sorted(TEMPLATES.glob("*/gradle/wrapper/gradle-wrapper.properties"))


def _coordinates(catalogue: dict) -> dict[str, str]:
    """The first `group:artifact` that uses each version, plugins resolved to their marker."""

    def ref(entry: dict) -> str | None:
        # `version.ref = "x"` parses as a nested table, {"version": {"ref": "x"}}.
        version = entry.get("version")
        return version.get("ref") if isinstance(version, dict) else None

    found: dict[str, str] = {}
    for entry in catalogue.get("libraries", {}).values():
        key = ref(entry)
        if key and key not in found:
            found[key] = entry.get("module") or f"{entry['group']}:{entry['name']}"
    for entry in catalogue.get("plugins", {}).values():
        key = ref(entry)
        if key and key not in found:
            found[key] = f"{entry['id']}:{entry['id']}.gradle.plugin"
    return found


def latest_stable(coordinate: str) -> str | None:
    group, artifact = coordinate.split(":")
    path = f"{group.replace('.', '/')}/{artifact}/maven-metadata.xml"
    for repository in repositories(group):
        try:
            with urllib.request.urlopen(repository + path, timeout=20) as response:
                metadata = response.read().decode()
        except OSError:
            continue
        versions = [
            version for version in re.findall(r"<version>([^<]+)</version>", metadata)
            if not _UNSTABLE.search(version)
        ]
        if versions:
            return max(versions, key=_ordering)
    return None


def latest_gradle() -> str | None:
    try:
        with urllib.request.urlopen(GRADLE_RELEASES, timeout=20) as response:
            return json.loads(response.read().decode()).get("version")
    except (OSError, ValueError):
        return None


def check_catalogue(path: Path, apply: bool) -> int:
    text = path.read_text(encoding="utf-8")
    catalogue = tomllib.loads(text)
    declared: dict[str, str] = catalogue.get("versions", {})
    coordinates = _coordinates(catalogue)
    with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
        newest = dict(zip(coordinates, pool.map(latest_stable, coordinates.values())))

    print(f"\n{path.relative_to(TEMPLATES.parent)}")
    outdated: dict[str, str] = {}
    for key, current in declared.items():
        found = newest.get(key)
        if key not in coordinates:
            status = "not referenced by any library or plugin"
        elif found is None:
            status = "could not be looked up"
        elif _UNSTABLE.search(current):
            status = f"pre-release; stable is {found}"
            outdated[key] = found
        elif _ordering(found) > _ordering(current):
            status = f"-> {found}"
            outdated[key] = found
        else:
            status = "latest"
        print(f"  {key:28} {current:16} {status}")

    if apply and outdated:
        for key, version in outdated.items():
            text = re.sub(rf'^({re.escape(key)} = )"[^"]+"', rf'\g<1>"{version}"', text, count=1, flags=re.M)
        path.write_text(text, encoding="utf-8")
    return len(outdated)


def check_wrapper(path: Path, newest: str | None, apply: bool) -> int:
    text = path.read_text(encoding="utf-8")
    match = _WRAPPER_VERSION.search(text)
    if match is None:
        print(f"\n{path.relative_to(TEMPLATES.parent)}: no distributionUrl")
        return 0
    current = match.group(2)
    behind = newest is not None and _ordering(newest) > _ordering(current)
    status = "could not be looked up" if newest is None else (f"-> {newest}" if behind else "latest")
    print(f"\n{path.relative_to(TEMPLATES.parent)}\n  {'gradle':28} {current:16} {status}")
    if apply and behind:
        path.write_text(_WRAPPER_VERSION.sub(rf"\g<1>{newest}\g<3>", text, count=1), encoding="utf-8")
    return int(behind)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    parser.add_argument("--apply", action="store_true", help="write the newer versions back")
    args = parser.parse_args(argv)

    behind = sum(check_catalogue(path, args.apply) for path in catalogues())
    gradle = latest_gradle()
    behind += sum(check_wrapper(path, gradle, args.apply) for path in wrappers())

    if args.apply and behind:
        print(f"\nUpdated {behind}. Build every template and the generator's extremes before committing.")
    elif behind:
        print(f"\n{behind} behind. Run with --apply to update them.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
