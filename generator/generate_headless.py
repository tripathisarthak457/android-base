#!/usr/bin/env python3
"""
Generate a project from a JSON spec on stdin, writing a zip to a path given on the command line.

    echo '{"app_name": "My App", "package_name": "com.acme.myapp"}' \\
      | py generate_headless.py /tmp/out.zip

This is what the web API invokes. It exists rather than the API shelling out to
`create_project.py --spec` for three reasons:

* **stdin, not a file.** The API never has to write the user's answers — which include nothing
  secret today, but would the moment keystores were added — to a path that something else could
  read.
* **JSON out, not prose.** The result is a machine-readable summary on stdout: warnings, the
  resolved feature set, timings. `create_project.py` prints a box-drawn report for a human.
* **One failure shape.** Anything that goes wrong exits non-zero with `{"error": "…"}` on stdout,
  so the API has exactly one thing to parse rather than a mix of tracebacks and exit codes.

Keystores are deliberately not generated here even when the spec asks for them. A production
upload key that was created on a server and sent back over the wire is a key whose custody cannot
be claimed, and losing control of a Play upload key is the one Android mistake that cannot be
undone. The zip ships `keystore.properties.template` and the README explains the four commands.
"""

from __future__ import annotations

import json
import shutil
import sys
import tempfile
import time
import traceback
from dataclasses import replace
from pathlib import Path

HERE = Path(__file__).resolve().parent
TEMPLATE_DIR = HERE.parent / "template"
VARIANTS_DIR = HERE / "variants"

sys.path.insert(0, str(HERE))

from genkit import render, scaffold  # noqa: E402
from genkit.readme import write_readme  # noqa: E402
from genkit.spec import ProjectSpec, SpecError  # noqa: E402


def fail(message: str, detail: str | None = None) -> int:
    json.dump({"error": message, "detail": detail}, sys.stdout)
    sys.stdout.write("\n")
    return 1


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        return fail("Usage: generate_headless.py <output.zip>")

    output = Path(argv[1])

    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError as error:
        return fail("The request body was not valid JSON.", str(error))

    # Anything the client sends that is not a field of ProjectSpec is dropped rather than passed
    # through — a stray key would otherwise be a TypeError halfway down, reported as a 500.
    allowed = {
        "app_name", "package_name", "min_sdk", "target_sdk", "compile_sdk",
        "version_name", "version_code", "features", "feature_modules",
        "api_base_urls", "web_socket_urls", "deeplink_scheme", "deeplink_host",
        "font_name", "mono_font_name", "accent_colour", "motion_style", "haptics_enabled",
    }
    unknown = sorted(set(payload) - allowed)
    fields = {key: value for key, value in payload.items() if key in allowed}

    for key in ("features", "feature_modules"):
        if key in fields and isinstance(fields[key], list):
            fields[key] = frozenset(fields[key]) if key == "features" else tuple(fields[key])

    try:
        spec = ProjectSpec(**fields).validated()
    except SpecError as error:
        return fail(str(error))
    except TypeError as error:
        return fail("The request body had the wrong shape.", str(error))

    # Never, whatever was asked for. See the module docstring.
    spec = replace(spec, keystores=())

    started = time.perf_counter()
    try:
        with tempfile.TemporaryDirectory(prefix="androidgen-") as staging:
            project = Path(staging) / spec.pascal_name

            warnings = render.copy_template(TEMPLATE_DIR, project, spec)
            render.overlay_variants(VARIANTS_DIR, project, spec)
            scaffold.write_feature_modules(project, spec)
            render.rewrite_all(project, spec, scaffold.generated_blocks(spec))
            render.apply_build_settings(project, spec)
            render.apply_app_name(project, spec)
            render.apply_fonts(project, spec)
            render.apply_accent(project, spec)
            render.apply_feel(project, spec)
            render.write_keystore_properties(project, [])
            write_readme(project, spec)

            output.parent.mkdir(parents=True, exist_ok=True)
            render.zip_project(project, output)
    except render.RenderError as error:
        return fail(str(error))
    except OSError as error:
        return fail("Could not write the project.", str(error))
    except Exception as error:  # noqa: BLE001 - the boundary; the API needs one failure shape.
        return fail(f"{type(error).__name__}: {error}", traceback.format_exc())

    json.dump(
        {
            "ok": True,
            "projectName": spec.pascal_name,
            "packageName": spec.package_name,
            "features": sorted(spec.features),
            "featureModules": list(spec.feature_modules),
            "zipPath": str(output),
            "zipBytes": output.stat().st_size,
            "elapsedMillis": round((time.perf_counter() - started) * 1000),
            "warnings": warnings,
            "ignoredFields": unknown,
        },
        sys.stdout,
    )
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
