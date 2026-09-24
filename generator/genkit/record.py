"""
The record a generated project keeps of how it was made: `generator-spec.json` at its root.

It is a spec `create_project.py --spec` accepts as it is, so the same project can be generated
again from a newer template and the two compared. Signing keys are left out: the file is meant to
be committed, and a keystore password in a repository is a leaked password.
"""

from __future__ import annotations

import datetime
import json
import subprocess
from dataclasses import asdict
from pathlib import Path

from .spec import ProjectSpec

RECORD_NAME = "generator-spec.json"

#: The key holding where and when the project came from. Ignored when the file is read as a spec.
STAMP_KEY = "generator"


def write(project: Path, spec: ProjectSpec, repository: Path) -> None:
    data = asdict(spec)
    data.pop("keystores")
    data["features"] = sorted(spec.features)
    data["feature_modules"] = list(spec.feature_modules)
    data["languages"] = list(spec.languages)
    record = {
        STAMP_KEY: {
            "commit": _commit(repository),
            "generatedOn": datetime.date.today().isoformat(),
        },
        **data,
    }
    (project / RECORD_NAME).write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")


def _commit(repository: Path) -> str:
    """The generator's commit, or "unknown" where it runs from a copy without git history."""
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--short=12", "HEAD"],
            cwd=repository,
            capture_output=True,
            text=True,
            timeout=5,
            check=True,
        )
        return result.stdout.strip() or "unknown"
    except (OSError, subprocess.SubprocessError):
        return "unknown"
