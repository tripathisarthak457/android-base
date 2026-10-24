"""
Whole-file replacements: `generator/variants/<name>/` mirrors the template's layout, and each file in
it replaces the template file at the same path, or adds one, when its owner is off. The owner is an
option that names the directory in its `variant`, or a feature in `VARIANTS` (retired as each file
gained `opt-else` blocks). Variant files go through markers and renaming like any template file.
"""

from __future__ import annotations

from pathlib import Path

from ...core.copy import is_under
from ...core.manifest import TemplateManifest
from ...core.markers import TemplateError
from ...core.spec import Spec
from ...core.target import RenderError

VARIANTS_DIR = Path(__file__).resolve().parents[3] / "variants"

#: Feature → the variant directory used when it is off.
VARIANTS: dict[str, str] = {}


def overlays(spec: Spec, manifest: TemplateManifest, root: Path = VARIANTS_DIR) -> dict[str, Path]:
    """Template-relative path → the variant file that replaces or adds it."""
    files: dict[str, Path] = {}
    for feature, directory in VARIANTS.items():
        if not spec.has(feature):
            files.update(_files(root / directory, f"feature '{feature}'"))
    for option in manifest.options.values():
        if option.variant is not None and not spec.option(option.key):
            files.update(_files(root / option.variant, f"option '{option.key}'"))
    return files


def check(manifest: TemplateManifest, root: Path = VARIANTS_DIR) -> None:
    """
    Every option's variant directory exists, and no file in it sits under a path the option owns:
    such a file would be left out with that path, without a word.
    """
    token = manifest.package_path_token
    for option in manifest.options.values():
        if option.variant is None:
            continue
        source = manifest.root / "options" / f"{option.key}.toml"
        directory = root / option.variant
        if not directory.is_dir():
            raise TemplateError(source, None, f"variant '{option.variant}' has no directory at {directory}")
        owned = [path.format(pkg_path=token) for path in option.owns]
        for relative in _files(directory, f"option '{option.key}'"):
            if is_under(relative, owned):
                raise TemplateError(source, None, f"variant file {relative} sits under a path the option owns")


def _files(directory: Path, owner: str) -> dict[str, Path]:
    if not directory.is_dir():
        raise RenderError(f"Missing variant directory for {owner}: {directory}")
    return {
        source.relative_to(directory).as_posix(): source
        for source in sorted(directory.rglob("*"))
        if source.is_file()
    }
