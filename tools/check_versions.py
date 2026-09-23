"""
Every version in the template's catalogue against the newest stable release.

    python3 tools/check_versions.py            # report
    python3 tools/check_versions.py --apply    # rewrite libs.versions.toml to the newest stable

Coordinates are read from the catalogue itself: each version is checked through the first library
or plugin that refers to it, so a new entry needs nothing added here. Pre-releases — alpha, beta,
rc, dev, eap, milestones — are never offered. Standard library only, like the generator.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import re
import sys
import tomllib
import urllib.request
from pathlib import Path

CATALOGUE = Path(__file__).resolve().parent.parent / "template/gradle/libs.versions.toml"

REPOSITORIES = (
    "https://dl.google.com/android/maven2/",
    "https://repo1.maven.org/maven2/",
    "https://plugins.gradle.org/m2/",
)

_UNSTABLE = re.compile(r"(alpha|beta|rc|dev|eap|snapshot|preview|-m\d|\.m\d|-b\d)", re.IGNORECASE)


def _ordering(version: str) -> list[tuple[int, int | str]]:
    """Numbers compare as numbers, anything else as text, so 1.10 sorts after 1.9."""
    return [(0, int(part)) if part.isdigit() else (1, part) for part in re.split(r"[.\-]", version)]


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
    for repository in REPOSITORIES:
        try:
            with urllib.request.urlopen(repository + path, timeout=20) as response:
                metadata = response.read().decode()
        except OSError:
            continue
        versions = [
            version
            for version in re.findall(r"<version>([^<]+)</version>", metadata)
            if not _UNSTABLE.search(version)
        ]
        if versions:
            return max(versions, key=_ordering)
    return None


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    parser.add_argument("--apply", action="store_true", help="write the newer versions back")
    args = parser.parse_args(argv)

    text = CATALOGUE.read_text(encoding="utf-8")
    catalogue = tomllib.loads(text)
    declared: dict[str, str] = catalogue["versions"]
    coordinates = _coordinates(catalogue)

    with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
        newest = dict(zip(coordinates, pool.map(latest_stable, coordinates.values())))

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
        print(f"{key:28} {current:16} {status}")

    if args.apply and outdated:
        for key, version in outdated.items():
            text = re.sub(rf'^({re.escape(key)} = )"[^"]+"', rf'\g<1>"{version}"', text, count=1, flags=re.M)
        CATALOGUE.write_text(text, encoding="utf-8")
        print(f"\nUpdated {len(outdated)}. Build the template and both generator extremes before committing.")
    elif outdated:
        print(f"\n{len(outdated)} behind. Run with --apply to update them.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
