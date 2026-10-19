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
        files = {path.relative_to(project).as_posix(): path for path in project.rglob("*") if not path.is_dir()}
        for relative in sorted(files):
            path = files[relative]
            info = zipfile.ZipInfo(f"{root}/{relative}", date_time=_TIMESTAMP)
            info.create_system = _UNIX
            # Without the mode in the high bits, some unzip tools extract every file as 0000, and
            # a `gradlew` without its executable bit is "permission denied" on the first command.
            mode = 0o755 if is_executable(relative, executable) else 0o644
            info.external_attr = (_REGULAR_FILE | mode) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            output.writestr(info, path.read_bytes())
    return archive


def git_init(project: Path, message: str, executable: tuple[str, ...]) -> list[str]:
    """
    Initialises a repository and commits the project as generated, with the files `executable`
    names recorded as 100755. Returns warnings.
    """
    if shutil.which("git") is None:
        return ["git was not found on PATH, so the repository was not initialised."]

    # The project's .gitattributes decides line endings, never this machine's core.autocrlf (true
    # by default in Git for Windows), so the commit holds the same blobs on every host. It applies
    # to each command that hashes the files.
    as_written = ("-c", "core.autocrlf=false")
    # The commit is the generator's, under its own name, so it is never signed with the user's key:
    # with commit.gpgSign set, signing would fail it or wait on a passphrase prompt nobody sees.
    as_the_generator = (
        "-c", "user.name=Project generator", "-c", "user.email=generator@localhost", "-c", "commit.gpgSign=false"
    )
    try:
        # Not `--initial-branch`: that needs git 2.28, and an older git fails with a usage error
        # rather than falling back to master.
        _git(project, (), "init")
        _git(project, (), "symbolic-ref", "HEAD", "refs/heads/main")
        _git(project, as_written, "add", ".")
        # Taken from the index: update-index refuses a path it does not hold, such as a script the
        # project's .gitignore keeps out.
        staged = _git(project, (), "ls-files", "-z").split("\0")
        scripts = [relative for relative in staged if relative and is_executable(relative, executable)]
        # On Windows `git init` sets core.fileMode to false and `git add` records every file as
        # 100644, so without this a clone on Linux or macOS could not run ./gradlew.
        _git(project, as_written, "update-index", "--chmod=+x", "--", *scripts)
        _git(project, as_the_generator, "commit", "--no-verify", "-m", message)
    except _GitFailed as failure:
        return [f"{failure} The project itself is fine."]
    return []


class _GitFailed(Exception):
    pass


def _git(project: Path, config: tuple[str, ...], *arguments: str) -> str:
    """Runs `git <arguments>` in `project` and returns what it printed. Raises _GitFailed."""
    result = subprocess.run(
        # Without the user's file system monitor: its hook would run on every command here, and its
        # daemon (Git for Windows starts one for core.fsmonitor=true) would hold the project open
        # while it is moved into place or deleted. Empty, not "false": git before 2.36 reads the
        # value as a hook path.
        ["git", "-c", "core.fsmonitor=", *config, *arguments],
        cwd=project,
        capture_output=True,
        # git prints UTF-8 on every host, and its messages can hold the project's path, which
        # Python 3.14 and older would read in the ANSI code page on Windows.
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip().splitlines()
        reason = detail[-1] if detail else f"exit code {result.returncode}"
        raise _GitFailed(f"`git {arguments[0]}` failed ({reason}).")
    return result.stdout
