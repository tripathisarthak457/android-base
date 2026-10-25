#!/usr/bin/env python3
"""
Generate a new project from a template.

    python3 create_project.py                                     # the wizard
    python3 create_project.py --preset standard                   # the wizard, features chosen
    python3 create_project.py --preset lean --no-ui-components    # the same, without UI components
    python3 create_project.py --spec app.json --out ../MyApp --no-zip
    python3 create_project.py --spec app.json --set package_name=com.example.demo --out demo.zip

`--out` is the project directory with --no-zip and the zip file otherwise. `--force` replaces
exactly that path and nothing else, once the new project is complete. --json needs --out.
"""

from __future__ import annotations

import argparse
import json
import sys
from contextlib import redirect_stdout
from dataclasses import asdict
from pathlib import Path
from typing import Any

from genkit import targets
from genkit.core import catalogue, pipeline, prompts
from genkit.core.copy import is_link, write_text
from genkit.core.headless import internal, invalid
from genkit.core.markers import TemplateError
from genkit.core.spec import SpecError, to_json
from genkit.core.target import RenderError
from genkit.core.translations import TranslationError
from genkit.targets.android.scaffold import UI_COMPONENTS

HERE = Path(__file__).resolve().parent

EXIT_OK = 0
EXIT_FAILED = 1
EXIT_INVALID = 2
EXIT_CANCELLED = 130


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="create_project", description="Generate a project from a template.")
    parser.add_argument(
        "--target",
        help="Which template: android or multiplatform. Defaults to the spec's platform, then android.",
    )
    parser.add_argument("--all", action="store_true", help="Every optional feature on.")
    parser.add_argument("--preset", help="Use a named feature preset: lean, standard or everything.")
    parser.add_argument(
        "--no-ui-components", action="store_true",
        help="Generate the architecture, a theme skeleton and every build guard, without UI components (Android).",
    )
    parser.add_argument("--icon", type=Path, help="A square image (1024 px or larger) for the launcher icons.")
    parser.add_argument("--git", action="store_true", help="git init the project and commit it as generated.")
    parser.add_argument("--spec", type=Path, help="Read the answers from a JSON spec instead of asking.")
    parser.add_argument(
        "--set", action="append", default=[], metavar="KEY=VALUE",
        help="Override one spec field, after the spec is read: --set package_name=com.example.demo, "
        "--set android.min_sdk=28. The value is read as JSON when it parses, as a string otherwise; "
        "null removes the field.",
    )
    parser.add_argument("--out", type=Path, help="The project directory (--no-zip) or the .zip to write.")
    parser.add_argument("--no-zip", action="store_true", help="Write a directory rather than a zip.")
    parser.add_argument("--force", action="store_true", help="Replace --out if it already exists.")
    parser.add_argument("--save-spec", type=Path, help="Also write the answers as a spec, to regenerate later.")
    parser.add_argument("--list-features", action="store_true", help="Print every feature and exit.")
    parser.add_argument("--dry-run", action="store_true", help="Say what would be generated, and write nothing.")
    parser.add_argument("--json", action="store_true", help="Print only JSON: the features, the plan or the result.")
    args = parser.parse_args(argv)

    try:
        target = targets.get(args.target or spec_platform(args.spec) or "android")
        options = {UI_COMPONENTS: False} if args.no_ui_components else {}
        if options and UI_COMPONENTS not in target.manifest.options:
            raise SpecError("No UI components is Android-only for now.", UI_COMPONENTS)
        if args.list_features:
            list_features(target, as_json=args.json, options=options)
            return EXIT_OK
        if args.preset is not None and args.preset not in target.manifest.presets:
            raise SpecError(
                f"Unknown preset '{args.preset}'. Choose one of: {', '.join(target.manifest.presets)}.", "preset"
            )
        if args.json and args.out is None and not args.dry_run:
            # Asking where to write would put a prompt on stdout, or wait on a script that cannot answer.
            raise SpecError("--json needs --out: the project's directory, or the .zip to write.", "out")
        overrides = [parse_override(item) for item in args.set] + list(options.items())
        raw = load_raw(args, target, options)
        parsed, warnings = pipeline.read_spec(raw, target, overrides)
    except SpecError as error:
        return fail(args.json, invalid(error), str(error), EXIT_INVALID)
    except (KeyboardInterrupt, EOFError):
        print("\nCancelled.", file=sys.stderr)
        return EXIT_CANCELLED
    except Exception:
        # Under --json even a bug answers with one JSON object; otherwise the traceback is the report.
        if not args.json:
            raise
        write_json(internal())
        return EXIT_FAILED

    spec = parsed.spec
    if args.dry_run:
        describe_plan(parsed, target, warnings, as_json=args.json)
        return EXIT_OK

    human = sys.stderr if args.json else sys.stdout
    for note in [*parsed.notes, *(f"Ignored '{field}', which nothing reads any more." for field in parsed.ignored_fields)]:
        print(prompts.yellow(f"  ! {note}"), file=sys.stderr)
    if not args.json:
        prompts.summarise(spec, human)
        if not args.spec and not prompts.ask_yes_no("\nGenerate?", True):
            print("Cancelled.")
            return EXIT_OK

    if args.save_spec:
        save_spec(spec, args.save_spec)

    try:
        destination = resolve_destination(spec.pascal_name, args)
        if destination is None:
            print("Cancelled.", file=human)
            return EXIT_OK
        replace = confirm_replace(destination, force=args.force, unattended=bool(args.spec) or args.json)
        result = pipeline.build(
            spec, target, destination,
            zip_output=not args.no_zip, icon_source=args.icon, git_init=args.git, replace=replace,
        )
    except SpecError as error:
        return fail(args.json, invalid(error), str(error), EXIT_INVALID)
    except (RenderError, TemplateError, TranslationError, OSError) as error:
        return fail(args.json, internal(), described(error), EXIT_FAILED)
    except (KeyboardInterrupt, EOFError):
        print("\nCancelled.", file=sys.stderr)
        return EXIT_CANCELLED
    except Exception:
        if not args.json:
            raise
        write_json(internal())
        return EXIT_FAILED

    if args.json:
        write_json({
            "ok": True,
            "target": target.key,
            "project_name": result.project_name,
            "path": str(destination),
            "resolved_features": sorted(spec.features),
            "feature_modules": list(spec.feature_modules),
            "notes": list(parsed.notes),
            "warnings": warnings + result.warnings,
            "ignored_fields": list(parsed.ignored_fields),
            "summary": result.summary,
        })
    else:
        report(spec, warnings + result.warnings, result.summary, destination, zip_output=not args.no_zip)
    return EXIT_OK


def fail(as_json: bool, payload: dict[str, Any], message: str, code: int) -> int:
    if as_json:
        write_json(payload)
    else:
        print(prompts.red(message), file=sys.stderr)
    return code


def described(error: BaseException) -> str:
    """
    `error`'s message, then the notes that name what the failure left behind: on `error`, or on its
    cause when an explanation, such as that of a path past Windows' length limit, took its place.
    """
    notes = [*getattr(error, "__notes__", []), *getattr(error.__cause__, "__notes__", [])]
    return "\n".join([str(error), *notes])


def write_json(data: object) -> None:
    json.dump(data, sys.stdout, indent=2)
    sys.stdout.write("\n")


def parse_override(item: str) -> tuple[str, Any]:
    key, separator, text = item.partition("=")
    if not separator or not key:
        raise SpecError(f"--set takes KEY=VALUE, not '{item}'.")
    try:
        return key.strip(), json.loads(text)
    except json.JSONDecodeError:
        return key.strip(), text


def spec_platform(path: Path | None) -> str | None:
    """The `platform` a spec file names, so `--spec` alone picks its template. Read errors wait for load_raw."""
    if path is None:
        return None
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None
    platform = raw.get("platform") if isinstance(raw, dict) else None
    return platform if isinstance(platform, str) else None


def load_raw(args: argparse.Namespace, target, options: dict[str, bool]) -> object:
    if args.spec is None:
        # With --json, stdout carries the result alone, so the questions go to stderr.
        with redirect_stdout(sys.stderr if args.json else sys.stdout):
            return prompts.run_wizard(target, select_all=args.all, preset=args.preset, options=options)
    try:
        raw = json.loads(args.spec.read_text(encoding="utf-8"))
    except FileNotFoundError:
        raise SpecError(f"No spec file at {args.spec}.") from None
    except json.JSONDecodeError as error:
        raise SpecError(f"{args.spec} is not valid JSON: {error}.") from None
    if isinstance(raw, dict) and (args.all or args.preset):
        manifest = target.manifest
        chosen = (
            {feature.key for feature in manifest.selectable} if args.all else manifest.preset_features(args.preset)
        )
        raw = {**raw, "features": sorted(chosen)}
    return raw


def save_spec(spec, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    write_text(path, json.dumps(to_json(spec, keystores=True), indent=2) + "\n")
    note = f"  Answers written to {path}."
    if spec.android is not None and spec.android.keystores:
        note += " It holds the keystore passwords: keep it out of version control."
    print(prompts.dim(note), file=sys.stderr)


def list_features(target, as_json: bool, options: dict[str, bool]) -> None:
    """
    Every feature as a project with these option values has it: none an option leaves out, and each
    in the words of an option that is off where its own words name what such a project lacks.
    """
    manifest = target.manifest
    values = manifest.option_values(options)
    left_out = manifest.disabled_features(values)
    entry = next(item for item in catalogue.catalogue()["targets"] if item["key"] == target.key)
    offered = [
        {**feature, **asdict(manifest.feature_copy(manifest.features[feature["key"]], values))}
        for feature in entry["features"]
        if feature["key"] not in left_out
    ]
    if as_json:
        write_json(offered)
        return
    for group in entry["groups"]:
        features = [feature for feature in offered if feature["group"] == group["key"] and not feature["implied_only"]]
        if not features:
            continue
        prompts.heading(group["title"])
        for feature in features:
            mark = prompts.green("on ") if feature["default"] else prompts.dim("off")
            print(f"  {mark} {prompts.bold(feature['key'].ljust(28))} {feature['headline']}")
            if feature["implies"]:
                print(prompts.dim(f"      brings in {', '.join(feature['implies'])}"))
    for option in entry["options"]:
        prompts.heading(f"Option: {option['checkbox']['label']}")
        print(f"  {prompts.bold(option['key'].ljust(28))} {option['headline']}")
        default = "on" if option["default"] else "off"
        print(prompts.dim(f"      {default} by default; --set {option['key']}={str(option['checkbox']['ticked_value']).lower()} to tick it"))
        for disabled in option["disables"]:
            print(prompts.dim(f"      leaves out {disabled['feature']}: {disabled['reason']}"))
        if option["key"] == UI_COMPONENTS and option["feature_copy"] and values[UI_COMPONENTS]:
            described = ", ".join(entry["feature"] for entry in option["feature_copy"])
            print(prompts.dim(f"      describes {described} differently; --no-ui-components lists them that way"))
        if option["locked"]:
            print(prompts.yellow(f"      {option['locked']['note']}"))
    print()
    presets = ", ".join(preset["key"] for preset in entry["presets"])
    print(prompts.dim(f"  {len(offered)} features. Presets: {presets}."))
    print()


def describe_plan(parsed, target, warnings: list[str], as_json: bool) -> None:
    plan = pipeline.plan(parsed, target, warnings)
    if as_json:
        write_json({"project_name": parsed.spec.pascal_name, **plan})
        return
    spec = parsed.spec
    prompts.summarise(spec)
    implied = sorted(spec.features - parsed.requested)
    prompts.heading("Dry run")
    print(f"  Project:  {spec.pascal_name}")
    print(f"  Enabled:  {len(plan['enabled'])} of {len(target.manifest.features)} features")
    if implied:
        print(prompts.dim(f"  Implied:  {', '.join(implied)}, required by something you chose"))
    if plan["modules"]:
        print(f"  Modules:  {', '.join(plan['modules'])}")
    if plan["tabs"]:
        print(f"  Tabs:     {', '.join(plan['tabs'])}")
    changed = [
        f"{key} {'on' if value else 'off'}"
        for key, value in plan["options"].items()
        if value != target.manifest.options[key].default
    ]
    if changed:
        print(f"  Options:  {', '.join(changed)}")
    print(f"  Omitted:  {len(plan['removed_paths'])} paths a disabled feature or option owns")
    for line in plan["notes"] + plan["warnings"]:
        print(prompts.yellow(f"  ! {line}"))
    print()
    print(prompts.dim("  Nothing was written. Drop --dry-run to generate."))
    print()


def resolve_destination(project_name: str, args: argparse.Namespace) -> Path | None:
    if args.out is not None:
        out = absolute(args.out)
        if not args.no_zip and out.suffix.lower() != ".zip":
            raise SpecError(f"--out {args.out} must end in .zip, or add --no-zip to write a directory.", "out")
        return out
    return ask_save_location(project_name if args.no_zip else f"{project_name}.zip", zip_output=not args.no_zip)


def confirm_replace(destination: Path, force: bool, unattended: bool) -> bool:
    """
    Whether something at `destination` is to be replaced. Only that exact path is: the build moves
    it aside once the new project is complete and deletes it once the new one is in its place,
    leaving what cannot be deleted beside it with a warning. Raises SpecError when it may not be.
    """
    if not destination.exists() and not is_link(destination):
        return False
    if destination.is_dir() and not is_link(destination) and not any(destination.iterdir()):
        return True

    refusal = _unsafe_to_delete(destination)
    if refusal:
        raise SpecError(f"Refusing to replace {destination}: {refusal}.", "out")
    if not force:
        what = "directory" if destination.is_dir() else "file"
        print(prompts.yellow(f"  {destination} already exists and is not empty."), file=sys.stderr)
        if unattended or not prompts.ask_yes_no(f"  Replace that {what} entirely?", False):
            raise SpecError(f"{destination} already exists. Pass --force to replace it.", "out")
    return True


def absolute(path: Path) -> Path:
    """
    `path` made absolute with its directories resolved but not its last part, so that a link given
    as the destination is what gets replaced, never the directory it points at. It is also the path
    the result reports, so its directories have one spelling however the caller named them: through
    a link, with `..`, or by a Windows 8.3 short name like the one `%TEMP%` often holds.
    """
    path = path.expanduser().absolute()
    if path.name in ("", ".."):
        return path.resolve()
    return path.parent.resolve() / path.name


def _unsafe_to_delete(destination: Path) -> str | None:
    # Only the link itself is ever deleted, but a link that stands in for the home directory is
    # checked as that directory too: refusing it costs nothing, and losing the way home costs a lot.
    paths = {destination, destination.resolve()}
    if any(path == Path(path.anchor) for path in paths):
        return "it is a filesystem root"
    for name, place in (("your home directory", Path.home()), ("the current directory", Path.cwd()), ("the generator", HERE)):
        for form in {place, place.resolve()}:
            # By identity, not spelling: on a case-insensitive file system, the default on macOS,
            # /users/me is the home directory too, while two POSIX paths compare case by case.
            if any(_same_file(destination, ancestor) for ancestor in (form, *form.parents)):
                return f"it is or contains {name}"
    return None


def _same_file(path: Path, other: Path) -> bool:
    try:
        return path.samefile(other)
    except OSError:
        return False


def ask_save_location(default_name: str, zip_output: bool) -> Path | None:
    """A native save dialog, falling back to a typed path."""
    try:
        import tkinter
        from tkinter import filedialog

        root = tkinter.Tk()
        root.withdraw()
        root.attributes("-topmost", True)
        if zip_output:
            selected = filedialog.asksaveasfilename(
                title="Save the generated project",
                initialfile=default_name,
                defaultextension=".zip",
                filetypes=[("Zip archive", "*.zip")],
            )
        else:
            directory = filedialog.askdirectory(title="Choose a folder for the generated project")
            selected = str(Path(directory) / default_name) if directory else ""
        root.destroy()
        # An empty result is the user pressing Cancel, which is an answer, not a reason to ask again.
        return Path(selected) if selected else None
    except Exception:  # noqa: BLE001 - no display, no Tk: fall back to typing a path
        prompts.heading("Where should it go?")
        answer = prompts.ask("  Path", str(Path.cwd() / default_name))
        return absolute(Path(answer))


def report(spec, warnings: list[str], summary: list[str], destination: Path, zip_output: bool) -> None:
    print()
    print(prompts.green(prompts.bold("  Done.")))
    print()
    print(f"  {'Archive' if zip_output else 'Project'}: {destination}")
    for line in summary:
        print(f"  {line}")
    for warning in warnings:
        print()
        for line in prompts.wrap(warning, 74):
            print(prompts.yellow(f"  ! {line}"))
    print()
    print(prompts.bold("  Next"))
    print("    1. Unzip it and open the folder in Android Studio." if zip_output else f"    1. cd {destination} and open it in Android Studio.")
    print("    2. ./gradlew :app:installDevDebug")
    step = 3
    if spec.platform == "android":
        # dependency-guard writes the list on the first check and passes; it guards nothing until committed.
        print("    3. Commit app/dependencies/ after the first ./gradlew check: it is the list of libraries the prod")
        print("       release ships, which every later check compares against.")
        step += 1
    if spec.has("firebase"):
        print(prompts.yellow(f"    {step}. Replace app/google-services.json: the included one is a placeholder."))
    print()


if __name__ == "__main__":
    sys.exit(main())
