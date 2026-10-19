"""Turning a finished project directory into what the user receives: a zip, or a git repository."""

from __future__ import annotations

import functools
import os
import shutil
import subprocess
import tempfile
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

    # The commit is the generator's, under its own name, and holds the project alone, so git runs
    # with none of the caller's configuration. A system or global file can pass the files through
    # a clean filter (a global attributes file), drop gradle-wrapper.jar (an excludes file), refuse
    # or rewrite the commit (hooks, prepare-commit-msg among them, which --no-verify does not skip),
    # sign it or change its format; a template brings hooks and an info/exclude of its own; and a
    # GIT_ variable, such as the GIT_DIR git exports to every hook, can point each command at
    # another repository. The project's own .gitattributes and .gitignore still apply.
    as_the_generator = ("-c", "user.name=Project generator", "-c", "user.email=generator@localhost")
    with tempfile.TemporaryDirectory(prefix="git-home-") as home:
        git = functools.partial(_git, project, _without_the_callers_configuration(Path(home)))
        try:
            # Not `--initial-branch`: that needs git 2.28, and an older git fails with a usage error
            # rather than falling back to master.
            git((), "init", "--template=")
            if os.name == "nt":
                # Git for Windows skips a path past MAX_PATH unless this is on, and `git add` still
                # exits 0: where Windows' long paths are on, the commit would lack the project's
                # deepest files, and each later command in the repository would call them too long.
                git((), "config", "core.longPaths", "true")
            git((), "symbolic-ref", "HEAD", "refs/heads/main")
            git((), "add", ".")
            # Taken from the index: update-index refuses a path it does not hold, such as a script
            # the project's .gitignore keeps out.
            staged = git((), "ls-files", "-z").split("\0")
            scripts = [relative for relative in staged if relative and is_executable(relative, executable)]
            # On Windows `git init` sets core.fileMode to false and `git add` records every file as
            # 100644, so without this a clone on Linux or macOS could not run ./gradlew.
            git((), "update-index", "--chmod=+x", "--", *scripts)
            git(as_the_generator, "commit", "-m", message)
        except _GitFailed as failure:
            return [f"{failure} The project itself is fine."]
    return []


class _GitFailed(Exception):
    pass


def _without_the_callers_configuration(home: Path) -> dict[str, str]:
    """This process's environment for git, with no configuration but the repository's own."""
    environment = {
        name: value
        for name, value in os.environ.items()
        # GIT_EXEC_PATH only says where this git's own programs are.
        if not name.upper().startswith("GIT_") or name.upper() == "GIT_EXEC_PATH"
    }
    # An empty home holds no global configuration for git of any version (GIT_CONFIG_GLOBAL needs
    # 2.32), and git looks for its default excludes and attributes files there too.
    environment.update(HOME=str(home), XDG_CONFIG_HOME=str(home), GIT_CONFIG_NOSYSTEM="1")
    return environment


def _git(project: Path, environment: dict[str, str], config: tuple[str, ...], *arguments: str) -> str:
    """Runs `git <arguments>` in `project` and returns what it printed. Raises _GitFailed."""
    result = subprocess.run(
        ["git", *config, *arguments],
        cwd=project,
        env=environment,
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
