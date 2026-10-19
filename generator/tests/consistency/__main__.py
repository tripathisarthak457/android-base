"""
The static consistency matrix: many generated projects, each checked without Gradle.

    python3 -m tests.consistency --quick     # presets, every feature alone, the pairwise sets
    python3 -m tests.consistency             # also every feature left out, 40 seeded subsets, hostile names
    python3 -m tests.consistency --keep DIR  # keep the projects for a look

Every project ships all the languages. An issue fails the run unless `known_issues.toml` lists it
with the package that will fix it.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import os
import random
import re
import shutil
import sys
import tempfile
import time
import tomllib
from pathlib import Path

from genkit.core import translations
from genkit.targets import get

from .worker import PACKAGE, reference, run_one

ROOT = Path(__file__).resolve().parents[3]
KNOWN_ISSUES = Path(__file__).resolve().parent / "known_issues.toml"
SEED = 20261009

#: Names that contain a template token or sit on a rule's edge. Their own spelling may contain a
#: token, so the leftover-identity check is off for them.
HOSTILE = (
    ("Trail Notes", "com.example.databaseapp", ["orders"]),
    ("Trail Notes", "com.firebaseapp.notes", ["orders"]),
    ("Trail Notes", "com.base.apple", ["orders"]),
    ("Trail Notes", "com.base.app.demo", ["orders"]),
    ("Trail Notes", "com.base_app.demo", []),
    ("Store", "com.example.store", ["database_app"]),
    ("Xbaseapp", "com.example.x", ["baseapp"]),
    ("Baseapp X", "com.example.bx", ["orders"]),
    ("NASA Tracker", "com.global.pass", ["v2", "order_history"]),
)


def combinations(quick: bool) -> dict[str, dict]:
    sys.path.insert(0, str(ROOT / "tools"))
    from feature_combinations import combinations as pairwise

    manifest = get("android").manifest
    keys = [feature.key for feature in manifest.selectable]
    languages = list(translations.available("android"))

    def spec(features, modules=(), app="Consistency", package=PACKAGE, architecture="compact"):
        return {
            "schema": 2, "platform": "android", "app_name": app, "package_name": package,
            "architecture": architecture, "features": sorted(features), "feature_modules": list(modules),
            "languages": languages,
        }

    architectures = ["compact", "modular"]
    runs = {
        f"preset-{preset}-{architecture}": spec(manifest.preset_features(preset), architecture=architecture)
        for preset in manifest.presets
        for architecture in architectures
    }
    runs["everything-modules"] = spec(keys, ["orders", "order_history"])
    runs["none-modules"] = spec([], ["orders"])
    runs.update({f"only-{key}": spec([key]) for key in keys})
    runs.update({f"pair-{index}": spec(features, ["orders"]) for index, features in enumerate(pairwise())})
    if quick:
        return runs

    everything = set(keys)
    # A feature something else requires comes straight back; that run then checks the full set.
    runs.update({f"without-{key}": spec(manifest.resolve(everything - {key})) for key in keys})
    rng = random.Random(SEED)
    for index in range(40):
        density = rng.choice((0.15, 0.35, 0.5, 0.65, 0.85))
        runs[f"random-{index}"] = spec(
            [key for key in keys if rng.random() < density], ["orders"] if rng.random() < 0.5 else []
        )
    for index, (app, package, modules) in enumerate(HOSTILE):
        runs[f"hostile-{index}"] = spec(keys, modules, app=app, package=package)
    return runs


def load_known() -> list[dict]:
    entries = tomllib.loads(KNOWN_ISSUES.read_text(encoding="utf-8")).get("issue", [])
    for entry in entries:
        entry["regex"] = re.compile(entry["match"])
    return entries


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="python3 -m tests.consistency", description=__doc__.strip().splitlines()[0])
    parser.add_argument("--quick", action="store_true", help="presets, single features and pairwise sets only")
    parser.add_argument("--keep", type=Path, help="generate into this directory and keep the projects")
    parser.add_argument("--jobs", type=int, default=min(4, os.cpu_count() or 1))
    args = parser.parse_args(argv)

    runs = combinations(args.quick)
    started = time.perf_counter()
    directory = args.keep or Path(tempfile.mkdtemp(prefix="consistency-"))
    directory.mkdir(parents=True, exist_ok=True)
    results: dict[str, list[str]] = {}
    try:
        reference(str(directory))
        with concurrent.futures.ProcessPoolExecutor(max_workers=args.jobs) as pool:
            futures = [
                pool.submit(run_one, name, raw, str(directory), args.keep is not None) for name, raw in runs.items()
            ]
            for future in concurrent.futures.as_completed(futures):
                name, issues = future.result()
                results[name] = issues
    finally:
        if args.keep is None:
            shutil.rmtree(directory)

    known = load_known()
    seen_known: set[int] = set()
    failures: dict[str, list[str]] = {}
    for name in sorted(results):
        for issue in results[name]:
            match = next((index for index, entry in enumerate(known) if entry["regex"].search(issue)), None)
            if match is None:
                failures.setdefault(name, []).append(issue)
            else:
                seen_known.add(match)

    for name, issues in failures.items():
        print(f"{name}:")
        for issue in issues[:20]:
            print(f"  {issue}")
        if len(issues) > 20:
            print(f"  … and {len(issues) - 20} more")
    stale = [entry for index, entry in enumerate(known) if index not in seen_known]
    for entry in stale:
        print(f"known issue no longer seen ({entry['owner']}): {entry['match']}")

    elapsed = time.perf_counter() - started
    known_count = sum(1 for issues in results.values() for issue in issues) - sum(len(v) for v in failures.values())
    print(f"\n{len(results)} projects in {elapsed:.0f} s: {sum(len(v) for v in failures.values())} new issues, "
          f"{known_count} known ({len(known) - len(stale)} of {len(known)} known-issue lines seen).")
    # A known issue that went away is only certain when the full matrix ran.
    if failures or (stale and not args.quick):
        return 1
    print("CONSISTENCY OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
