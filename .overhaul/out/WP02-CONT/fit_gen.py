"""Scaffolds every module-name length from 1 to 30 for each data shape into the ktlint harness.

    python -I -B fit_gen.py <generator dir> <harness dir>

Each case is a set of at most ten modules (a spec holds twelve): names a, ab, abc, ... per data shape, plus the
database tables the shape reads. The harness then runs spotlessApply over cases/ and the diff names every line whose
layout is not ktlint's.
"""

from __future__ import annotations

import shutil
import sys
from pathlib import Path

generator, harness = Path(sys.argv[1]).resolve(), Path(sys.argv[2]).resolve()
sys.path.insert(0, str(generator))

from genkit.core import pipeline  # noqa: E402
from genkit.targets import get  # noqa: E402
from genkit.targets.android import database, scaffold  # noqa: E402

ANDROID = get("android")
SHAPES = {
    "net-db": ["network", "database"],
    "net": ["network"],
    "local": ["settings"],
    "local-db": ["database", "settings"],
}
LETTERS = "abcdefghijklmnopqrstuvwxyzabcd"
NAMES = [LETTERS[:length] for length in range(1, len(LETTERS) + 1)]

cases = harness / "cases"
shutil.rmtree(cases, ignore_errors=True)
for shape, features in SHAPES.items():
    for start in range(0, len(NAMES), 10):
        raw = {
            "schema": 2,
            "platform": "android",
            "app_name": "Fit",
            "package_name": "io.example.fit",
            "features": features,
            "feature_modules": NAMES[start:start + 10],
        }
        spec = pipeline.read_spec(raw, ANDROID)[0].spec
        project = cases / f"{shape}-{start // 10}"
        scaffold.write_feature_modules(project, spec)
        database.write_tables(project, spec)
        print(f"{project.name}: {len(list(project.rglob('*.kt')))} Kotlin files")
