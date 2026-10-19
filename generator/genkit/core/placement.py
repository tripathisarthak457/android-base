"""
Putting a finished project where the caller asked for it, zipped or as a directory, over what was
there or not: never half a project where a caller expects a whole one, never the old one lost to a
failure. On Windows, a path past the length limit is explained rather than reported as missing.
"""

from __future__ import annotations

import errno
import os
import random
import shutil
import stat
import string
import sys
from collections.abc import Callable, Iterator
from contextlib import contextmanager
from pathlib import Path

from . import package
from .copy import is_link
from .target import RenderError

#: Without long paths, Windows takes MAX_PATH less the closing NUL for a file, and 12 less for a
#: directory so that an 8.3 name still fits in it.
_WINDOWS_MAX_FILE = 259
_WINDOWS_MAX_DIRECTORY = 247
#: What Windows says of a longer path. The build makes every directory before it writes into it, so
#: here, with long paths off, a missing path means nothing else.
_PATH_REFUSED = (errno.ENOENT, errno.ENAMETOOLONG)


def place(
    project: Path, destination: Path, *, zip_output: bool, replace: bool, executable: tuple[str, ...]
) -> list[str]:
    """
    Puts `project` at `destination`, zipped with the files `executable` names marked or moved as it
    is, and returns warnings. With `replace`, what was there is deleted once the new output has
    taken its place; what of it cannot be deleted then is left beside it, and a warning says where.
    """
    _check_windows_path_limit(project, destination, zip_output)
    with windows_path_limit_explained("destination, app name or package name"):
        destination.parent.mkdir(parents=True, exist_ok=True)
        # The output is finished beside the destination and renamed over it, so a failure never
        # leaves half a project where a caller expects a whole one, nor costs the one it replaces.
        # Under a name as long as the destination's, so that whatever fits there fits on the way,
        # copied from staging on another volume included.
        finished = _free_sibling(destination)
        try:
            if zip_output:
                package.zip_project(project, finished, executable)
            else:
                _move(project, finished)
            if not (replace and os.path.lexists(destination)):
                os.replace(finished, destination)
                return []
            replaced = _swap(finished, destination)
        except BaseException as error:
            _delete_unplaced(finished, error.add_note)
            raise
        return _delete_replaced(replaced, destination)


@contextmanager
def windows_path_limit_explained(shorter: str) -> Iterator[None]:
    """
    Turns what Windows says about a path over its length limit, "no such file", into what happened
    and what to do: turn long paths on, or shorten what `shorter` names, the caller's parts of the
    paths written inside this block.
    """
    try:
        yield
    except OSError as error:
        path = _path_too_long(error)
        if os.name != "nt" or path is None or _windows_long_paths_on():
            raise
        raise _past_windows_limit(
            f"Windows refused a path of {_windows_length(path)} characters: {path}.", shorter
        ) from error


def _check_windows_path_limit(project: Path, destination: Path, zip_output: bool) -> None:
    """
    Refuses the output, before anything is made for it, when a path in it would pass Windows' length
    limit at `destination`. A project is put there by a rename, which Windows does however deep the
    files inside are, so nothing else would say so before Explorer, git or the build failed on them.
    """
    if os.name != "nt" or _windows_long_paths_on():
        return
    root = os.path.abspath(destination)
    if zip_output:
        if _past_limit(root, folder=False):
            raise _past_windows_limit(
                f"The zip would be a path of {_windows_length(root)} characters: {root}.", "destination"
            )
        return
    past: list[str] = []
    for folder, folders, files in os.walk(project):
        placed = root + folder[len(str(project)):]
        for name in [*folders, *files]:
            path = os.path.join(placed, name)
            if _past_limit(path, name in folders):
                past.append(path)
    if past:
        longest = max(past, key=_windows_length)
        raise _past_windows_limit(
            f"At {destination} the project would hold a path of {_windows_length(longest)} characters: {longest}.",
            "destination, app name or package name",
        )


def _past_windows_limit(what: str, shorter: str) -> RenderError:
    return RenderError(
        f"{what} With long paths off Windows takes {_WINDOWS_MAX_FILE} at most for a file and "
        f"{_WINDOWS_MAX_DIRECTORY} for a folder. Turn long paths on (LongPathsEnabled), which building the "
        f"project needs too, or choose a shorter {shorter}."
    )


def _path_too_long(error: OSError) -> str | None:
    """The longest path `error` names past Windows' limit, when it failed the way such a path fails."""
    if isinstance(error, shutil.Error) and error.args and isinstance(error.args[0], list):
        # A tree copy reports each failure as (source, destination, message), errno lost. With long
        # paths off, a destination past the limit for what its source is cannot have been created
        # whatever the message; one within it failed for some other reason.
        paths = [
            str(destination) for source, destination, _ in error.args[0]
            if _past_limit(str(destination), os.path.isdir(source))
        ]
    elif error.errno in _PATH_REFUSED:
        # A rename names both its paths, and either can be the one too long, a folder as well as a file.
        paths = [
            str(name) for name in (error.filename, error.filename2)
            if name is not None and _windows_length(str(name)) > _WINDOWS_MAX_DIRECTORY
        ]
    else:
        return None
    return max(paths, key=_windows_length, default=None)


def _past_limit(path: str, folder: bool) -> bool:
    limit = _WINDOWS_MAX_DIRECTORY if folder else _WINDOWS_MAX_FILE
    return _windows_length(path) > limit or _windows_length(os.path.dirname(path)) > _WINDOWS_MAX_DIRECTORY


def _windows_length(path: str) -> int:
    # Windows measures a path in UTF-16 code units, two for a character outside the Basic
    # Multilingual Plane, such as an emoji in a folder name.
    return len(path.encode("utf-16-le", "surrogatepass")) // 2


def _windows_long_paths_on() -> bool:
    import winreg  # Windows' own module: importing it anywhere else fails.

    try:
        with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"SYSTEM\CurrentControlSet\Control\FileSystem") as key:
            return winreg.QueryValueEx(key, "LongPathsEnabled")[0] == 1
    except OSError:
        return False


def _move(project: Path, finished: Path) -> None:
    try:
        os.rename(project, finished)
    except OSError as error:
        if error.errno != errno.EXDEV:
            raise
        # Staging is on another volume. Not shutil.move: it deletes the source with a plain rmtree,
        # which Windows refuses for the read-only files in .git. The staging directory's own
        # cleanup deletes the source instead.
        shutil.copytree(project, finished, symlinks=True)


def _swap(finished: Path, destination: Path) -> Path:
    """
    Puts `finished` at `destination` and returns where what was there went: renamed aside, which
    fails whole while Windows holds a file in it open, where deleting it in place would stop halfway.
    A link is moved, never followed.
    """
    replaced = _free_sibling(destination)
    try:
        os.replace(destination, replaced)
    except PermissionError as error:
        message = f"{destination} could not be moved aside ({error.strerror or error}); nothing was changed."
        if os.name == "nt":
            # Windows refuses to rename a file a program has open, or a folder while a program has
            # a file in it open or the folder as its working directory.
            if destination.is_dir():
                message += " A terminal, an IDE or an indexer may have a file in it open: close it and run again."
            else:
                message += " A program may have it open, such as an archive viewer: close it and run again."
        raise RenderError(message) from error
    try:
        os.replace(finished, destination)
    except BaseException as error:
        try:
            os.replace(replaced, destination)
        except OSError:
            raise RenderError(
                f"{destination} could not be replaced, and what was there could not be put back: it is now {replaced}."
            ) from error
        raise
    return replaced


def _free_sibling(destination: Path) -> Path:
    """
    A free name beside `destination` and as long as its own, so every path inside what is put there
    is as long as at `destination`: what Windows' length limit lets anyone write, open or delete at
    one, it does at the other.
    """
    length = len(destination.name)
    for _ in range(100):
        name = "".join(random.choices(string.ascii_lowercase + string.digits, k=max(length - 1, 1)))
        # Linux and macOS leave a name with a leading dot out of listings, where it has room for one.
        if length > 1:
            name = "." + name
        sibling = destination.with_name(name)
        # Compared without case: on a case-insensitive file system the two would be one entry.
        if name.lower() != destination.name.lower() and not os.path.lexists(sibling):
            return sibling
    raise RenderError(f"No name beside {destination} and as long as its own is free to put the output under.")


def _delete_replaced(replaced: Path, destination: Path) -> list[str]:
    """
    Deletes the output `destination` held before. The new one is in place by now, so what cannot be
    deleted is a warning that says where it is, not a failure.
    """
    try:
        _delete(replaced)
    except OSError as error:
        if os.path.lexists(replaced):
            return [
                # Only the reason: on Windows the error names the file in the extended-length form.
                f"{destination} holds the new project, but what it replaced could not all be deleted "
                f"({error.strerror or error}). Delete {replaced} yourself."
            ]
    return []


def _delete_unplaced(output: Path, report: Callable[[str], object]) -> None:
    """
    Deletes the new output a failure kept from its place, as far as it was made. What stops the
    deletion goes to `report`, so it never takes the place of the error that brought the build here.
    """
    if not os.path.lexists(output):
        return
    try:
        _delete(output)
    except OSError as error:
        report(
            f"The new output was left at {output}: it could not be deleted ({error.strerror or error}). "
            "Delete it yourself."
        )


def _delete(path: Path) -> None:
    """Deletes `path` whole. A link is unlinked, never followed."""
    if is_link(path):
        os.unlink(path)
    elif path.is_dir():
        _delete_tree(path)
    else:
        _delete_file(path)


def _delete_file(path: Path) -> None:
    try:
        os.unlink(path)
    except PermissionError as error:
        if os.name != "nt":
            raise
        _writable_and_again(os.unlink, str(path), error)


def _delete_tree(directory: Path) -> None:
    if os.name != "nt":
        shutil.rmtree(directory)
        return
    # In the extended-length form a path is not held to MAX_PATH, and Gradle, long-path aware as
    # Java is, writes build files past it that would otherwise outlive the project they belong to.
    absolute = os.path.abspath(directory)
    if absolute.startswith(("\\\\?\\", "\\\\.\\")):
        extended = absolute
    elif absolute.startswith("\\\\"):
        extended = "\\\\?\\UNC\\" + absolute[2:]
    else:
        extended = "\\\\?\\" + absolute
    if sys.version_info >= (3, 12):
        shutil.rmtree(extended, onexc=_writable_and_again)
    else:
        shutil.rmtree(extended, onerror=lambda function, path, info: _writable_and_again(function, path, info[1]))


def _writable_and_again(function: Callable[[str], object], path: str, error: BaseException) -> None:
    # Windows deletes no read-only file, and git makes its objects read-only.
    if not isinstance(error, PermissionError):
        raise error
    os.chmod(path, stat.S_IWRITE)
    function(path)
