"""
The record a generated project keeps of how it was made: `generator-spec.json` at its root.

It is always a schema-2 spec that `create_project.py --spec` accepts as it is, so the same project
can be generated again from a newer template and the two compared. Signing keys are left out: the
file is meant to be committed, and a keystore password in a repository is a leaked password.
"""

from __future__ import annotations

import datetime
import json
import subprocess
from pathlib import Path

from .copy import write_text
from .spec import STAMP_KEY, Spec, to_json

RECORD_NAME = "generator-spec.json"

REPOSITORY = Path(__file__).resolve().parents[3]


def write(project: Path, spec: Spec, commit: str) -> None:
    record = {
        STAMP_KEY: {"commit": commit, "generatedOn": datetime.date.today().isoformat()},
        **to_json(spec, keystores=False),
    }
    write_text(project / RECORD_NAME, json.dumps(record, indent=2) + "\n")


def generator_commit(repository: Path = REPOSITORY) -> str:
    """
    The generator's commit. A deployment has no git history, so CI writes the commit into
    generator/COMMIT before deploying; "unknown" only when neither is there.
    """
    stamped = repository / "generator" / "COMMIT"
    if stamped.is_file():
        return stamped.read_text(encoding="utf-8").strip() or "unknown"
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
