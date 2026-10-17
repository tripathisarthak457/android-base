"""
The one place that says what generating a project consists of, and in what order, for any target.

    1  raw spec → upgrade → --set overrides → validate (core rules, then the target's)
    2  copy the template, leaving out what disabled features own and what is template-only
    3  per text file, one pass: markers → rename template lines → insert generated lines
    4  rename every path (com/base/app → the package path, BaseApp* → the project's names)
    5  target.transform: structural steps whose output is final text
    6  target.finish: translations, look, icons, keystores
    7  record generator-spec.json
    8  zip, or move into place; optionally git init

Staging lives under $TMPDIR. The API points TMPDIR at its own work directory, so a killed process
leaves nothing behind that the API does not delete.
"""

from __future__ import annotations

import errno
import os
import random
import re
import shutil
import stat
import string
import sys
import tempfile
from collections.abc import Callable, Iterator
from contextlib import contextmanager
from dataclasses import dataclass
from functools import cache
from pathlib import Path
from typing import Any

from . import markers, package, record
from . import spec as specs
from .copy import is_executable, is_link, mark_executable, prune_empty_directories, template_files, write_text
from .manifest import TemplateManifest
from .markers import MARKER_TEXT, TemplateError
from .rename import IdentityRenamer
from .target import BuildContext, RenderError, Target


#: Without long paths, Windows takes MAX_PATH less the closing NUL for a file, and 12 less for a
#: directory so that an 8.3 name still fits in it.
_WINDOWS_MAX_FILE = 259
_WINDOWS_MAX_DIRECTORY = 247
#: What Windows says of a longer path. The build makes every directory before it writes into it, so
#: here, with long paths off, a missing path means nothing else.
_PATH_REFUSED = (errno.ENOENT, errno.ENAMETOOLONG)


@dataclass(frozen=True)
class Generated:
    destination: Path
    project_name: str
    warnings: list[str]
    summary: list[str]


def read_spec(
    raw: object, target: Target, overrides: list[tuple[str, Any]] | None = None
) -> tuple[specs.Parsed, list[str]]:
    """The validated spec, and the target's warnings about it. Raises SpecError."""
    parsed = specs.parse(raw, target.manifest, target.vocabulary(), overrides)
    return parsed, target.validate(parsed.spec)


def build(
    spec: specs.Spec,
    target: Target,
    destination: Path,
    *,
    zip_output: bool = True,
    icon_source: Path | None = None,
    git_init: bool = False,
    commit: str | None = None,
    replace: bool = False,
) -> Generated:
    """
    Generates `spec` into `destination`: a zip file, or a project directory that must not exist yet.
    With `replace`, whatever is at `destination` is deleted, but only once the new project has taken
    its place; what of it cannot be deleted then is left beside it, and a warning says where.
    """
    if not replace and not zip_output and destination.exists():
        raise RenderError(f"{destination} already exists.")
    if not replace and zip_output and destination.is_dir():
        raise RenderError(f"{destination} is a directory, not a path for the zip.")

    manifest = target.manifest
    with (
        _windows_path_limit_explained("app name or package name"),
        tempfile.TemporaryDirectory(prefix=f"{target.key}-gen-") as staging,
    ):
        project = Path(staging) / spec.pascal_name
        project.mkdir()
        warnings = render_template(project, spec, target)
        warnings += target.transform(project, spec)
        context = BuildContext(icon_source=icon_source)
        warnings += target.finish(project, spec, context)
        record.write(project, spec, commit or record.generator_commit())
        if git_init:
            warnings += package.git_init(
                project,
                f"Initial commit: {spec.app_name} generated from the {manifest.title} template",
                manifest.executable,
            )

        with _windows_path_limit_explained("destination, app name or package name"):
            destination.parent.mkdir(parents=True, exist_ok=True)
            # The result is finished beside the destination and renamed over it, so a failure never
            # leaves half a project where a caller expects a whole one, nor costs the one it replaces.
            with tempfile.TemporaryDirectory(prefix=f".{destination.name}.", dir=destination.parent) as beside:
                finished = Path(beside) / destination.name
                if zip_output:
                    package.zip_project(project, finished, manifest.executable)
                else:
                    _move(project, finished)
                if replace and os.path.lexists(destination):
                    replaced = _swap(finished, destination)
                    warnings += _delete_replaced(replaced, destination)
                else:
                    os.replace(finished, destination)

    return Generated(destination, spec.pascal_name, warnings, context.summary)


@contextmanager
def _windows_path_limit_explained(shorter: str) -> Iterator[None]:
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
        raise RenderError(
            f"Windows refused a path of {len(path)} characters: {path}. With long paths off it takes "
            f"{_WINDOWS_MAX_FILE} at most for a file and {_WINDOWS_MAX_DIRECTORY} for a folder. Turn long "
            f"paths on (LongPathsEnabled), which building the project needs too, or choose a shorter {shorter}."
        ) from error


def _path_too_long(error: OSError) -> str | None:
    """The longest path `error` names past Windows' limit, when it failed the way such a path fails."""
    if isinstance(error, shutil.Error) and error.args and isinstance(error.args[0], list):
        # A tree copy reports each failure as (source, destination, message), errno lost. With long
        # paths off, a destination past the limit for what its source is cannot have been created
        # whatever the message; one within it failed for some other reason.
        paths = [str(copy) for source, copy, _ in error.args[0] if _past_limit(str(copy), os.path.isdir(source))]
    elif error.errno in _PATH_REFUSED:
        # A rename names both its paths, and either can be the one too long, a folder as well as a file.
        paths = [
            str(name) for name in (error.filename, error.filename2)
            if name is not None and len(str(name)) > _WINDOWS_MAX_DIRECTORY
        ]
    else:
        return None
    return max(paths, key=len, default=None)


def _past_limit(path: str, folder: bool) -> bool:
    limit = _WINDOWS_MAX_DIRECTORY if folder else _WINDOWS_MAX_FILE
    return len(path) > limit or len(os.path.dirname(path)) > _WINDOWS_MAX_DIRECTORY


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
    replaced = _aside(destination)
    os.replace(destination, replaced)
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


def _aside(destination: Path) -> Path:
    """
    A free name beside `destination` and no longer than its own, so every path inside the old output
    stays as long as it was: what Windows' length limit let anyone open or delete there, it still does.
    """
    length = len(destination.name)
    for _ in range(100):
        name = "".join(random.choices(string.ascii_lowercase + string.digits, k=max(length - 1, 1)))
        # Hidden, like the folder the new output is finished in, where the name has room for the dot.
        if length > 1:
            name = "." + name
        aside = destination.with_name(name)
        # Compared without case: on a case-insensitive file system the two would be one entry.
        if name.lower() != destination.name.lower() and not os.path.lexists(aside):
            return aside
    raise RenderError(f"{destination} cannot be replaced: no name beside it is free to move it to meanwhile.")


def _delete_replaced(replaced: Path, destination: Path) -> list[str]:
    """
    Deletes the output `destination` held before. The new one is in place by now, so what cannot be
    deleted is a warning that says where it is, not a failure.
    """
    try:
        if is_link(replaced) or not replaced.is_dir():
            os.unlink(replaced)
        else:
            _delete_tree(replaced)
    except OSError as error:
        if os.path.lexists(replaced):
            return [
                # Only the reason: on Windows the error names the file in the extended-length form.
                f"{destination} holds the new project, but what it replaced could not all be deleted "
                f"({error.strerror or error}). Delete {replaced} yourself."
            ]
    return []


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


def render_template(project: Path, spec: specs.Spec, target: Target) -> list[str]:
    """Steps 2–4: every template file the spec keeps, written to `project` under its final name."""
    manifest = target.manifest
    regions = target.regions(spec)
    check_regions(manifest, regions)

    renamer = IdentityRenamer(manifest.identity, target.identity(spec))
    known = frozenset(manifest.features)
    excluded = manifest.owned_paths(spec.features) + list(manifest.template_only)
    warnings: list[str] = []

    for source in template_files(target.template_dir, excluded, target.overlays(spec)):
        relative = renamer.rename(source.relative)
        destination = project / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        data = source.source.read_bytes()

        if Path(source.relative).suffix.lower() in manifest.binary_suffixes:
            destination.write_bytes(data)
        else:
            try:
                text = data.decode("utf-8")
            except UnicodeDecodeError:
                warnings.append(f"{source.relative} is not UTF-8; copied without rewriting.")
                destination.write_bytes(data)
            else:
                rendered = render_text(text, source.relative, manifest, spec.features, known, regions, renamer)
                if rendered is None:
                    continue
                write_text(destination, rendered)

        if is_executable(relative, manifest.executable):
            mark_executable(destination)

    prune_empty_directories(project)
    return warnings


def render_text(
    text: str,
    relative: str,
    manifest: TemplateManifest,
    enabled: frozenset[str],
    known: frozenset[str],
    regions: dict[str, list[str] | None],
    renamer: IdentityRenamer,
) -> str | None:
    """One template file's final text, or None when nothing but scaffolding would be left of it."""
    if not MARKER_TEXT.search(text):
        return renamer.rename(text)

    syntax = markers.syntax_for(Path(relative), manifest.syntax)
    if syntax is None:
        raise TemplateError(relative, None, "has markers, but its comment syntax is not known")
    rendered = markers.render(
        text, path=relative, syntax=syntax, enabled=enabled, known=known, regions=regions
    )
    lines = markers.collapse(rendered.lines, rendered.seams) if syntax.collapse else rendered.lines

    parts: list[str] = []
    template_run: list[str] = []
    for line, generated in lines:
        if generated:
            parts.append(renamer.rename("".join(template_run)))
            template_run = []
            parts.append(line)
        else:
            template_run.append(line)
    parts.append(renamer.rename("".join(template_run)))
    result = "".join(parts)

    if relative.endswith(".kt") and is_hollow_kotlin(result):
        return None
    return result


def is_hollow_kotlin(text: str) -> bool:
    """True when a Kotlin file has a package line, imports and comments, and nothing else."""
    for line in text.splitlines():
        stripped = line.strip()
        if stripped and not stripped.startswith(("package ", "import ", "//", "/*", "*", "*/")):
            return False
    return True


def check_regions(manifest: TemplateManifest, regions: dict[str, list[str] | None]) -> None:
    """A target supplies only names the template declares, and the template has every one it declares."""
    source = manifest.root / "template.toml"
    undeclared = sorted(set(regions) - set(manifest.required_regions))
    if undeclared:
        raise TemplateError(source, None, f"the target supplies undeclared region(s): {', '.join(undeclared)}")
    missing = sorted(set(manifest.required_regions) - template_region_names(manifest.root))
    if missing:
        raise TemplateError(source, None, f"[regions] lists name(s) no template file has: {', '.join(missing)}")


_GENERATED_NAME = re.compile(r"<generated:([a-z][a-z0-9-]*)>")


@cache
def template_region_names(root: Path) -> frozenset[str]:
    """Every `generated` name in the full template, all features on."""
    names: set[str] = set()
    for source in template_files(root, [], {}):
        try:
            text = source.source.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        names.update(_GENERATED_NAME.findall(text))
    return frozenset(names)


def plan(parsed: specs.Parsed, target: Target, warnings: list[str]) -> dict[str, Any]:
    """What `build` would do, without doing any of it. Backs `--dry-run`."""
    spec = parsed.spec
    manifest = target.manifest
    renamer = IdentityRenamer(manifest.identity, target.identity(spec))
    outline = target.outline(spec)
    return {
        "enabled": sorted(spec.features),
        "disabled": sorted(set(manifest.features) - spec.features),
        "removed_paths": [renamer.rename(path) for path in manifest.owned_paths(spec.features)],
        "modules": outline["modules"],
        "tabs": outline["tabs"],
        "notes": list(parsed.notes),
        "warnings": warnings,
    }
