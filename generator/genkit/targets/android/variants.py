"""
Whole-file replacements for a feature that is off but needs a different file rather than none:
`generator/variants/<name>/` mirrors the template's layout. Retired as each file gains
`opt-else` blocks.
"""

from __future__ import annotations

from pathlib import Path

from ...core.spec import Spec
from ...core.target import RenderError

VARIANTS_DIR = Path(__file__).resolve().parents[3] / "variants"

#: Feature → the variant directory used when it is off.
VARIANTS: dict[str, str] = {
    "firebase": "firebase-off",
    "googlefonts": "googlefonts-off",
}


def overlays(spec: Spec) -> dict[str, Path]:
    """Template-relative path → the variant file that replaces or adds it."""
    files: dict[str, Path] = {}
    for feature, directory in VARIANTS.items():
        if spec.has(feature):
            continue
        root = VARIANTS_DIR / directory
        if not root.is_dir():
            raise RenderError(f"Missing variant directory for '{feature}': {root}")
        for source in sorted(root.rglob("*")):
            if source.is_file():
                files[source.relative_to(root).as_posix()] = source
    return files
