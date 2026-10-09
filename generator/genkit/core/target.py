"""What the engine asks of each target (Android, Compose Multiplatform, SwiftUI)."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Protocol

from .manifest import TemplateManifest
from .spec import Spec, Vocabulary


class RenderError(RuntimeError):
    """Producing the project failed for a reason that is not the user's input."""


@dataclass
class BuildContext:
    """What a target's `finish` may need beyond the spec."""

    #: An image to derive launcher icons from, when the user gave one.
    icon_source: Path | None = None
    #: Lines for the human report: what was produced besides the project itself.
    summary: list[str] = field(default_factory=list)


class Target(Protocol):
    key: str
    template_dir: Path
    manifest: TemplateManifest

    def vocabulary(self) -> Vocabulary:
        """The design styles, motion styles and taken module names a spec may use."""

    def validate(self, spec: Spec) -> list[str]:
        """Platform-specific checks. Raises SpecError; returns warnings."""

    def identity(self, spec: Spec) -> dict[str, str]:
        """The values the template's identity tokens format into: package, pascal, snake, …"""

    def regions(self, spec: Spec) -> dict[str, list[str] | None]:
        """
        Final text for every `generated` point or region, never renamed again. None keeps a
        region's default lines.
        """

    def overlays(self, spec: Spec) -> dict[str, Path]:
        """Files that replace template files for this spec, by template-relative path."""

    def transform(self, project: Path, spec: Spec) -> list[str]:
        """Structural steps after the text pass. Writes final text. Returns warnings."""

    def finish(self, project: Path, spec: Spec, context: BuildContext) -> list[str]:
        """Tokens, translations, icons, keystores. Returns warnings."""

    def outline(self, spec: Spec) -> dict[str, list[str]]:
        """For a dry run: the modules the spec adds and the tabs the app will have."""

    def catalogue(self) -> dict[str, Any]:
        """
        The target's own part of the catalogue: `fields` for its settings block, `defaults`,
        `architectures`, `reserved_module_names`, and `rules` merged into the shared rules.
        """

    def environment(self) -> dict[str, Any]:
        """What this machine can do for the target, for the live catalogue only."""
