#!/usr/bin/env python3
"""
Generate a new Android project from the template.

    py create_project.py                                   # the wizard
    py create_project.py --all                             # every optional feature on
    py create_project.py --spec spec.json --out ./build    # unattended

Standard library only, on purpose: this runs on a colleague's laptop with nothing installed, and
it is the same code that will sit behind an HTTP endpoint later. `tkinter` is the one optional
import — when it is absent, or there is no display, the save location is typed instead.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import asdict
from pathlib import Path

from genkit import build as builder
from genkit import catalogue, prompts, render
from genkit.spec import (
    FEATURES,
    FEATURES_BY_KEY,
    PRESETS,
    KeystoreSpec,
    ProjectSpec,
    SpecError,
    upgrade_features,
)

HERE = Path(__file__).resolve().parent
TEMPLATE_DIR = HERE.parent / "template"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="create_project",
        description="Generate a multi-module Jetpack Compose Android project.",
    )
    parser.add_argument("--all", action="store_true", help="Enable every optional feature.")
    parser.add_argument(
        "--preset",
        choices=[preset.key for preset in PRESETS],
        help="Skip the feature questions and use a named starting point.",
    )
    parser.add_argument(
        "--icon",
        type=Path,
        help="A square source image (1024px or larger) to generate launcher icons from.",
    )
    parser.add_argument(
        "--git",
        action="store_true",
        help="Run git init and make the first commit in the generated project.",
    )
    parser.add_argument("--spec", type=Path, help="Read answers from a JSON file instead of asking.")
    parser.add_argument("--out", type=Path, help="Write here instead of opening a save dialog.")
    parser.add_argument("--no-zip", action="store_true", help="Leave a directory rather than a .zip.")
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite the destination if it already has something in it.",
    )
    parser.add_argument(
        "--save-spec",
        type=Path,
        help="Write the answers to JSON, so the same project can be regenerated.",
    )
    parser.add_argument(
        "--list-features",
        action="store_true",
        help="Print every feature, its group and what it drags in, then exit.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Resolve the answers and print what would be written, without writing it.",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Machine-readable output. Applies to --list-features, --dry-run and the result.",
    )
    args = parser.parse_args(argv)

    # Answers nothing about a project, so it runs before the template is even looked for — it is
    # the thing to reach for when deciding whether this generator does what you need at all.
    if args.list_features:
        list_features(as_json=args.json)
        return 0

    if not TEMPLATE_DIR.is_dir():
        print(prompts.red(f"Template not found at {TEMPLATE_DIR}"))
        return 1

    try:
        if args.spec:
            spec = load_spec(args.spec)
        else:
            spec = prompts.run_wizard(select_all=args.all, preset=args.preset)
    except SpecError as error:
        print(prompts.red(str(error)))
        return 1
    except (KeyboardInterrupt, EOFError):
        print("\nCancelled.")
        return 130

    if args.dry_run:
        describe_plan(spec, as_json=args.json)
        return 0

    prompts.summarise(spec)

    if not args.spec and not prompts.ask_yes_no("\nGenerate?", True):
        print("Cancelled.")
        return 0

    if args.save_spec:
        save_spec(spec, args.save_spec)
        print(prompts.dim(f"  Answers written to {args.save_spec}"))

    destination = resolve_destination(spec, args)
    if destination is None:
        print("Cancelled.")
        return 0

    if not confirm_overwrite(destination, force=args.force, unattended=bool(args.spec)):
        return 1

    try:
        result = builder.build(
            spec,
            destination,
            zip_output=not args.no_zip,
            icon_source=args.icon,
            git_init=args.git,
        )
    except render.RenderError as error:
        print(prompts.red(f"\n{error}"))
        return 1

    if args.json:
        json.dump(
            {
                "ok": True,
                "projectName": spec.pascal_name,
                "packageName": spec.package_name,
                "features": sorted(spec.features),
                "featureModules": list(spec.feature_modules),
                "keystoresGenerated": result.keystores_generated,
                "keystoresSkipped": result.keystores_skipped,
                "path": str(destination),
                "warnings": result.warnings,
            },
            sys.stdout,
            indent=2,
        )
        sys.stdout.write("\n")
    else:
        report(spec, result, destination, zip_output=not args.no_zip)
    return 0


# ─────────────────────────────────────────────────────────────────────────────
# Answering without generating
# ─────────────────────────────────────────────────────────────────────────────


def list_features(as_json: bool) -> None:
    """
    Every feature, grouped as the website groups them.

    Read from the same catalogue the site renders, rather than from `FEATURES` directly, so the
    terminal cannot describe a different set of options from the one the site offers — and so a
    feature added without a group or a headline fails here too, rather than only on the web.
    """
    data = catalogue.catalogue()
    if as_json:
        json.dump(data["features"], sys.stdout, indent=2)
        sys.stdout.write("\n")
        return

    by_group: dict[str, list[dict]] = {}
    for feature in data["features"]:
        by_group.setdefault(feature["group"], []).append(feature)

    for group in data["groups"]:
        entries = by_group.get(group["name"], [])
        if not entries:
            continue
        prompts.heading(group["name"])
        if group["caption"]:
            print(prompts.dim(f"  {group['caption']}"))
            print()
        for feature in entries:
            mark = prompts.green("on ") if feature["default"] else prompts.dim("off")
            print(f"  {mark} {prompts.bold(feature['key'].ljust(28))} {feature['headline']}")
            if feature["implies"]:
                print(prompts.dim(f"      brings in {', '.join(feature['implies'])}"))

    print()
    print(prompts.dim(f"  {len(data['features'])} features. "
                      f"Presets: {', '.join(p['key'] for p in data['presets'])}."))
    print()


def describe_plan(spec: ProjectSpec, as_json: bool) -> None:
    """
    What generating would produce, without producing it.

    The two things worth knowing before waiting for a build: which features were turned on that
    nobody asked for — Crashlytics quietly brings Firebase and the analytics seam with it — and
    what is being left out.
    """
    result = builder.plan(spec)
    if as_json:
        json.dump({"projectName": spec.pascal_name, **result}, sys.stdout, indent=2)
        sys.stdout.write("\n")
        return

    prompts.summarise(spec)

    implied = sorted(spec.features - _requested(spec))
    prompts.heading("Dry run")
    print(f"  Project:  {spec.pascal_name}")
    print(f"  Enabled:  {len(result['enabled'])} of {len(FEATURES)} features")
    if implied:
        print(prompts.dim(f"  Implied:  {', '.join(implied)} — required by something you chose"))
    if result["modules"]:
        print(f"  Modules:  {', '.join(result['modules'])}")
    print(f"  Omitted:  {len(result['removed'])} paths a disabled feature owns")
    print()
    print(prompts.dim("  Nothing was written. Drop --dry-run to generate."))
    print()


def _requested(spec: ProjectSpec) -> set[str]:
    """
    The features that would have been asked for, given the resolved set.

    Every feature in the set that nothing else in the set requires. Not perfect — a feature both
    chosen *and* implied reads as implied — but it is the distinction that matters here, which is
    "you did not tick this and you have it".
    """
    required = {
        needed
        for key in spec.features
        for needed in FEATURES_BY_KEY[key].requires
    }
    return set(spec.features) - required


# ─────────────────────────────────────────────────────────────────────────────
# Destination
# ─────────────────────────────────────────────────────────────────────────────


def resolve_destination(spec: ProjectSpec, args: argparse.Namespace) -> Path | None:
    default_name = spec.pascal_name if args.no_zip else f"{spec.pascal_name}.zip"

    if args.out:
        out = args.out.expanduser().resolve()
        if args.no_zip or out.suffix == ".zip":
            return out
        return out / default_name

    return ask_save_location(default_name, zip_output=not args.no_zip)


def confirm_overwrite(destination: Path, force: bool, unattended: bool) -> bool:
    """
    Guards the one path that destroys work: regenerating over a directory that already exists.

    `--spec` plus `--out` is how a project is regenerated after editing its saved answers, and the
    generator replaces the destination wholesale — which is right, and is also indistinguishable
    from pointing it at the checkout somebody has been working in for a month. A save dialog asks
    this question itself; a command line has to be asked here.

    Unattended runs are refused rather than prompted, because there is nobody to answer: CI would
    hang on the input, and defaulting to yes would make the destructive case the silent one.
    """
    if force or not destination.exists():
        return True
    if destination.is_dir() and not any(destination.iterdir()):
        return True

    what = "directory" if destination.is_dir() else "file"
    print()
    print(prompts.yellow(f"  {destination} already exists and is not empty."))
    print(prompts.dim(f"  Generating replaces that {what} entirely."))

    if unattended:
        print(prompts.red("  Refusing to overwrite it unattended. Pass --force if that is what you want."))
        return False

    if prompts.ask_yes_no("  Replace it?", False):
        return True
    print("Cancelled.")
    return False


def ask_save_location(default_name: str, zip_output: bool) -> Path | None:
    """
    A native save dialog, falling back to a typed path.

    The fallback is not an edge case: this same function runs over SSH, in CI, and inside the
    container that will eventually host the web version, none of which have a display.
    """
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
        # An empty result is the user pressing Cancel, which is an answer — not a reason to fall
        # through to a text prompt asking the same question again.
        return Path(selected) if selected else None
    except Exception:
        prompts.heading("Where should it go?")
        answer = prompts.ask("  Path", str(Path.cwd() / default_name))
        return Path(answer).expanduser().resolve()


# ─────────────────────────────────────────────────────────────────────────────
# Spec files
# ─────────────────────────────────────────────────────────────────────────────


def load_spec(path: Path) -> ProjectSpec:
    data = json.loads(path.read_text(encoding="utf-8"))
    keystores = tuple(KeystoreSpec(**entry) for entry in data.pop("keystores", []))
    features = frozenset(data.pop("features", []))
    for note in upgrade_features(set(features))[1]:
        # stderr, because --json promises that stdout is JSON and nothing else.
        print(prompts.yellow(f"  ! {note}"), file=sys.stderr)
    modules = tuple(data.pop("feature_modules", []))
    return ProjectSpec(
        features=features,
        feature_modules=modules,
        keystores=keystores,
        **data,
    ).validated()


def save_spec(spec: ProjectSpec, path: Path) -> None:
    data = asdict(spec)
    data["features"] = sorted(spec.features)
    data["feature_modules"] = list(spec.feature_modules)
    data["keystores"] = [asdict(keystore) for keystore in spec.keystores]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


# ─────────────────────────────────────────────────────────────────────────────
# Output
# ─────────────────────────────────────────────────────────────────────────────


def report(spec: ProjectSpec, result: render.RenderResult, destination: Path, zip_output: bool) -> None:
    print()
    print(prompts.green(prompts.bold("  Done.")))
    print()
    print(f"  {'Archive' if zip_output else 'Project'}: {destination}")

    if result.keystores_generated:
        print(f"  Keystores:  {', '.join(result.keystores_generated)}")
    if result.keystores_skipped:
        print(prompts.yellow(f"  Skipped:    {', '.join(result.keystores_skipped)}"))

    for warning in result.warnings:
        print()
        for line in prompts.wrap(warning, 74):
            print(prompts.yellow(f"  ! {line}"))

    print()
    print(prompts.bold("  Next"))
    if zip_output:
        print("    1. Unzip it and open the folder in Android Studio.")
    else:
        print(f"    1. cd {destination} and open it in Android Studio.")
    print("    2. ./gradlew :app:installDevDebug")
    if spec.has("firebase"):
        print(prompts.yellow("    3. Replace app/google-services.json — the included one is a placeholder."))
    print()


if __name__ == "__main__":
    sys.exit(main())
