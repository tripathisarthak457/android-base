"""Turning a finished project directory into what the user receives: a zip, or a git repository."""

from __future__ import annotations

import shutil
import subprocess
import zipfile
from pathlib import Path

from .copy import is_executable

#: The zip format's own epoch. Every entry carries it, so the same project gives the same bytes.
_TIMESTAMP = (1980, 1, 1, 0, 0, 0)
_UNIX = 3
_REGULAR_FILE = 0o100000


def zip_project(project: Path, archive: Path, executable: tuple[str, ...]) -> Path:
    """
    Writes `project` into `archive` under one root folder named after it: sorted entries, fixed
    timestamps, 0755 for the files `executable` names and 0644 for everything else.
    """
    archive.parent.mkdir(parents=True, exist_ok=True)
    root = project.name
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as output:
        for path in sorted(project.rglob("*")):
            if path.is_dir():
                continue
            relative = path.relative_to(project).as_posix()
            info = zipfile.ZipInfo(f"{root}/{relative}", date_time=_TIMESTAMP)
            info.create_system = _UNIX
            # Without the mode in the high bits, some unzip tools extract every file as 0000, and
            # a `gradlew` without its executable bit is "permission denied" on the first command.
            mode = 0o755 if is_executable(relative, executable) else 0o644
            info.external_attr = (_REGULAR_FILE | mode) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            output.writestr(info, path.read_bytes())
    return archive


def git_init(project: Path, message: str) -> list[str]:
    """Initialises a repository and commits the project as generated. Returns warnings."""
    if shutil.which("git") is None:
        return ["git was not found on PATH, so the repository was not initialised."]

    commands = [
        # Not `--initial-branch`: that needs git 2.28, and an older git fails with a usage error
        # rather than falling back to master.
        ["git", "init"],
        ["git", "symbolic-ref", "HEAD", "refs/heads/main"],
        ["git", "add", "."],
        [
            "git",
            "-c", "user.name=Project generator",
            "-c", "user.email=generator@localhost",
            "commit", "--no-verify", "-m", message,
        ],
    ]
    for command in commands:
        result = subprocess.run(command, cwd=project, capture_output=True, text=True, check=False)
        if result.returncode != 0:
            detail = (result.stderr or result.stdout).strip().splitlines()
            reason = detail[-1] if detail else f"exit code {result.returncode}"
            return [f"`{' '.join(command[:2])}` failed ({reason}). The project itself is fine."]
    return []
