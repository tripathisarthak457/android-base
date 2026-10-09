"""
The machine interface every target shares: a spec on stdin, a zip path in argv, exactly one JSON
object on stdout, and an exit code that says whose fault a failure was.

    0  {"ok": true, "target", "project_name", "elapsed_ms", "zip_bytes", "resolved_features",
        "notes", "warnings", "ignored_fields", "generator_commit"}
    2  {"ok": false, "kind": "invalid", "error": "…", "field": "…"}         the spec was refused
    1  {"ok": false, "kind": "internal", "error": "The generator failed.", "detail": "<traceback>"}

Only a refused spec is `invalid`; everything else is a bug in the generator or the template, which
the API answers with a 500 and records with its traceback.
"""

from __future__ import annotations

import json
import sys
import time
import traceback
from contextlib import redirect_stdout
from pathlib import Path
from typing import Any, BinaryIO, TextIO

from .. import targets
from . import pipeline, record
from .spec import MAX_SPEC_BYTES, SpecError

EXIT_OK = 0
EXIT_INTERNAL = 1
EXIT_INVALID = 2

USAGE = "Usage: generate_headless.py --target <target> <output.zip> < spec.json"


def invalid(error: SpecError) -> dict[str, Any]:
    return {"ok": False, "kind": "invalid", "error": str(error), "field": error.field}


def internal() -> dict[str, Any]:
    return {"ok": False, "kind": "internal", "error": "The generator failed.", "detail": traceback.format_exc()}


def run(argv: list[str], stdin: BinaryIO, stdout: TextIO) -> int:
    started = time.perf_counter()
    try:
        target_key, output = _arguments(argv)
        raw = _read(stdin)
        # Anything printed while generating would break the promise of one JSON object on stdout.
        with redirect_stdout(sys.stderr):
            result = _generate(raw, target_key, output)
    except SpecError as error:
        _write(stdout, invalid(error))
        return EXIT_INVALID
    except Exception:  # noqa: BLE001 - the boundary: every other failure is the generator's
        _write(stdout, internal())
        return EXIT_INTERNAL
    result["elapsed_ms"] = round((time.perf_counter() - started) * 1000)
    _write(stdout, result)
    return EXIT_OK


def _arguments(argv: list[str]) -> tuple[str, Path]:
    target = "android"
    rest: list[str] = []
    arguments = iter(argv)
    for argument in arguments:
        if argument == "--target":
            target = next(arguments, "")
        elif argument.startswith("--target="):
            target = argument.partition("=")[2]
        else:
            rest.append(argument)
    if len(rest) != 1 or not target:
        raise SpecError(USAGE)
    return target, Path(rest[0])


def _read(stdin: BinaryIO) -> object:
    data = stdin.read(MAX_SPEC_BYTES + 1)
    if len(data) > MAX_SPEC_BYTES:
        raise SpecError(f"The spec is larger than {MAX_SPEC_BYTES // 1024} KB.")
    try:
        return json.loads(data.decode("utf-8"))
    except UnicodeDecodeError:
        raise SpecError("The spec is not UTF-8.") from None
    except json.JSONDecodeError as error:
        raise SpecError(f"The spec is not valid JSON: {error}.") from None


def _generate(raw: object, target_key: str, output: Path) -> dict[str, Any]:
    target = targets.get(target_key)
    parsed, warnings = pipeline.read_spec(raw, target)
    spec = parsed.spec
    if spec.android is not None and any(keystore.existing_path for keystore in spec.android.keystores):
        # A path on this machine: over the API that would be the server's file system.
        raise SpecError("`existing_path` is only accepted by create_project.py.", "android.keystores")

    commit = record.generator_commit()
    generated = pipeline.build(spec, target, output, zip_output=True, commit=commit)
    return {
        "ok": True,
        "target": target.key,
        "project_name": generated.project_name,
        "zip_bytes": output.stat().st_size,
        "resolved_features": sorted(spec.features),
        "notes": list(parsed.notes),
        "warnings": warnings + generated.warnings,
        "ignored_fields": list(parsed.ignored_fields),
        "generator_commit": commit,
    }


def _write(stdout: TextIO, payload: dict[str, Any]) -> None:
    json.dump(payload, stdout)
    stdout.write("\n")
    stdout.flush()
