"""
The design-system look a spec chooses (brand colours, design and motion style, typefaces, haptics),
written into the project as its `DesignTokens`, which `AppTheme` and every component read. A project
without UI components gets the theme skeleton's tokens: the same look, without the design styles and
the values only the template's components read.
"""

from __future__ import annotations

from pathlib import Path

from ... import tokens
from ...core.copy import write_text
from ...core.spec import Spec
from ...core.target import RenderError
from ...tokens import kotlin
from .scaffold import UI_COMPONENTS


def apply(project: Path, spec: Spec) -> None:
    """Re-emits the template's token copy for the spec's brand and choices, under its package."""
    path = project / kotlin.RELATIVE_PATH.format(package_path=spec.package_path)
    if not path.is_file():
        raise RenderError(f"DesignTokens.kt is not where the design system keeps it: {path}")
    resolved = tokens.resolve(spec)
    if spec.option(UI_COMPONENTS):
        write_text(path, kotlin.emit(resolved, spec.package_name, kotlin.PROJECT_HEADER))
    else:
        write_text(path, kotlin.emit_skeleton(resolved, spec.package_name))
