#!/usr/bin/env python3
"""
Generate a project from a JSON spec on stdin, writing a zip to a path given on the command line.

    echo '{"app_name": "My App", "package_name": "com.acme.myapp"}' \
      | py generate_headless.py /tmp/out.zip

* **stdin, not a file.** The API never has to write the user's answers — which now include
  keystore passwords — to a path that something else could read.
* **JSON out, not prose.** The result is a machine-readable summary on stdout: warnings, the
  resolved feature set, timings. `create_project.py` prints a box-drawn report for a human.
* **One failure shape.** Anything that goes wrong exits non-zero with `{"error": "…"}` on stdout,
  so the API has exactly one thing to parse rather than a mix of tracebacks and exit codes.
"""

from __future__ import annotations

import json
import sys
import time
import traceback
from pathlib import Path

HERE = Path(__file__).resolve().parent

sys.path.insert(0, str(HERE))

from genkit import build as builder  # noqa: E402
from genkit import render  # noqa: E402
from genkit.spec import KeystoreSpec, ProjectSpec, SpecError  # noqa: E402


#: What a keystore entry may set. `existing_path` is absent on purpose — see the module docstring.
KEYSTORE_FIELDS = frozenset({
    "name", "alias", "store_password", "key_password",
    "common_name", "organisation", "country", "validity_days",
})


def keystore(entry: object) -> KeystoreSpec:
    """One keystore from the request. Raises TypeError for anything the dataclass will not take."""
    if not isinstance(entry, dict):
        raise TypeError("each keystore must be an object")
    return KeystoreSpec(
        **{key: value for key, value in entry.items()
           if key in KEYSTORE_FIELDS and value is not None}
    )


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
        "font_name", "mono_font_name", "accent_colour", "secondary_colour", "tertiary_colour",
        "motion_style", "design_style", "haptics_enabled", "languages",
        "keystores",
    }
    unknown = sorted(set(payload) - allowed)

    # `null` is dropped along with the unknown keys, so that omitting an optional field and sending
    # it as null behave the same.
    fields = {
        key: value for key, value in payload.items() if key in allowed and value is not None
    }

    for key in ("features", "feature_modules", "languages"):
        if key in fields and isinstance(fields[key], list):
            fields[key] = frozenset(fields[key]) if key == "features" else tuple(fields[key])

    if "keystores" in fields:
        try:
            fields["keystores"] = tuple(keystore(entry) for entry in fields["keystores"])
        except TypeError as error:
            return fail("A keystore entry had the wrong shape.", str(error))

    try:
        spec = ProjectSpec(**fields).validated()
    except SpecError as error:
        return fail(str(error))
    except TypeError as error:
        return fail("The request body had the wrong shape.", str(error))

    started = time.perf_counter()
    try:
        # The same nine render steps the wizard runs, in the same order, because they are the
        # same code. Keystores are generated only when the spec asked for them — see the module
        # docstring for why that is the caller's decision to have made out loud.
        result = builder.build(spec, output, zip_output=True)
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
            "keystoresGenerated": result.keystores_generated,
            "keystoresSkipped": result.keystores_skipped,
            "zipPath": str(output),
            "zipBytes": output.stat().st_size,
            "elapsedMillis": round((time.perf_counter() - started) * 1000),
            "warnings": result.warnings,
            "ignoredFields": unknown,
        },
        sys.stdout,
    )
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
