"""The one place that says what generating a project consists of, and in what order."""

from __future__ import annotations

import shutil
import tempfile
from dataclasses import dataclass
from pathlib import Path

from . import icons, render, scaffold
from .readme import write_readme
from .spec import FEATURES_BY_KEY, ProjectSpec

HERE = Path(__file__).resolve().parent.parent
TEMPLATE_DIR = HERE.parent / "template"
VARIANTS_DIR = HERE / "variants"


@dataclass
class BuildResult:
    """What was produced, for whichever caller asked."""

    destination: Path
    keystores_generated: list[str]
    keystores_skipped: list[str]
    warnings: list[str]


def build(
    spec: ProjectSpec,
    destination: Path,
    zip_output: bool = True,
    icon_source: Path | None = None,
    git_init: bool = False,
    template_dir: Path | None = None,
) -> BuildResult:
    """Renders [spec] into [destination], as a .zip or a directory."""
    template = template_dir or TEMPLATE_DIR
    if not template.is_dir():
        raise render.RenderError(f"Template not found at {template}")

    with tempfile.TemporaryDirectory(prefix="androidgen-") as staging:
        project = Path(staging) / spec.pascal_name

        warnings = render.copy_template(template, project, spec)
        # Before the rewrite pass, or the overlaid files keep the template's package name.
        render.overlay_variants(VARIANTS_DIR, project, spec)
        scaffold.write_feature_modules(project, spec)
        render.rewrite_all(project, spec, scaffold.generated_blocks(spec))
        render.apply_build_settings(project, spec)
        render.apply_app_name(project, spec)
        render.apply_fonts(project, spec)
        render.apply_accent(project, spec)
        render.apply_feel(project, spec)

        keystores = list(spec.keystores)
        generated, skipped, key_warnings = render.generate_keystores(project, keystores)
        # Only the keys that exist get a stanza: a properties file naming a .jks that is not in
        # the zip is a build failure rather than the fallback to debug signing.
        render.write_keystore_properties(
            project, [k for k in keystores if k.name in generated]
        )
        warnings.extend(key_warnings)

        write_readme(project, spec)

        if icon_source is not None:
            warnings.extend(icons.generate(icon_source, project, spec))

        # After everything is written, so the first commit is the project as it ships. A repo
        # whose initial commit is half the files is worse than no repo at all — the first `git
        # status` in a new project should be clean.
        if git_init:
            warnings.extend(render.git_init(project, spec))

        destination.parent.mkdir(parents=True, exist_ok=True)
        if zip_output:
            render.zip_project(project, destination)
        else:
            if destination.exists():
                shutil.rmtree(destination)
            shutil.move(str(project), str(destination))

    return BuildResult(
        destination=destination,
        keystores_generated=generated,
        keystores_skipped=skipped,
        warnings=warnings,
    )


def plan(spec: ProjectSpec) -> dict[str, list[str]]:
    """What a build would do, without doing any of it. Backs `--dry-run`."""
    enabled = sorted(spec.features)
    return {
        "enabled": enabled,
        "disabled": sorted(set(FEATURES_BY_KEY) - spec.features),
        # The paths a disabled feature owns, which is what "off" actually means here: the
        # directory is not written at all, rather than written and commented out.
        "removed": sorted(
            path.format(pkg_path=spec.package_path)
            for key, feature in FEATURES_BY_KEY.items()
            if key not in spec.features
            for path in feature.files
        ),
        "modules": [f":data:{name}" for name in spec.feature_modules]
        + [f":feature:{name}" for name in spec.feature_modules],
    }
