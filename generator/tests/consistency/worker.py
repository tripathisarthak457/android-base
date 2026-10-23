"""
One run of the matrix: a project generated and checked, in a worker process.

The pool pickles `run_one` by module name. On a host that starts workers with spawn (Windows, macOS)
or forkserver (Linux from Python 3.14), a worker imports that module afresh, and `multiprocessing`
never imports a package's `__main__` there, so the function cannot live in `__main__.py`.
"""

from __future__ import annotations

import shutil
from functools import cache
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get

from .checks import check
from .hilt import missing_bindings
from .project import Project

PACKAGE = "com.example.consistency"


@cache
def reference(directory: str) -> Project:
    """Every feature on: what a stripped symbol is checked against."""
    project = Path(directory) / "reference"
    if not project.exists():
        target = get("android")
        keys = [feature.key for feature in target.manifest.selectable]
        raw = {"app_name": "Consistency", "package_name": PACKAGE, "features": keys, "feature_modules": ["orders"]}
        parsed, _ = pipeline.read_spec(raw, target)
        pipeline.build(parsed.spec, target, project, zip_output=False, commit="consistency")
    return Project(project)


def run_one(name: str, raw: dict, directory: str, keep: bool) -> tuple[str, list[str]]:
    target = get("android")
    project = Path(directory) / name
    try:
        parsed, _ = pipeline.read_spec(raw, target)
        pipeline.build(parsed.spec, target, project, zip_output=False, commit="consistency")
    except Exception as error:  # noqa: BLE001 - a crash is itself the finding
        return name, [f"GENERATION {type(error).__name__}: {error}"]
    hostile = name.startswith("hostile-")
    enabled = target.manifest.resolve(frozenset(parsed.spec.features))
    issues = check(
        project, raw["package_name"], None if hostile else reference(directory), identity=not hostile, enabled=enabled,
    )
    issues += missing_bindings(project)
    if not keep:
        shutil.rmtree(project)
    package = raw["package_name"]
    return name, [issue.replace(package.replace(".", "/"), "{pkg}").replace(package, "{package}") for issue in issues]
