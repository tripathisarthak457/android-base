"""
The design-system look a spec chooses (brand colours, design and motion style, typefaces, haptics),
written into the project as its `DesignTokens`, which `AppTheme` and every component read.
"""

from __future__ import annotations

from pathlib import Path

from ... import tokens
from ...core.copy import write_text
from ...core.spec import Spec
from ...core.target import RenderError
from ...tokens import kotlin


def apply(project: Path, spec: Spec) -> None:
    """Re-emits the template's token copy for the spec's brand and choices, under its package."""
    path = project / kotlin.RELATIVE_PATH.format(package_path=spec.package_path)
    if not path.is_file():
        raise RenderError(f"DesignTokens.kt is not where the design system keeps it: {path}")
    write_text(path, kotlin.emit(tokens.resolve(spec), spec.package_name, kotlin.PROJECT_HEADER))
