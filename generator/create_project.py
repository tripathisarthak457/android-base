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
import shutil
import sys
import tempfile
from dataclasses import asdict
from pathlib import Path

from genkit import icons, prompts, render, scaffold
from genkit.readme import write_readme
from genkit.spec import PRESETS, KeystoreSpec, ProjectSpec, SpecError

HERE = Path(__file__).resolve().parent
TEMPLATE_DIR = HERE.parent / "template"
VARIANTS_DIR = HERE / "variants"


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
        "--save-spec",
        type=Path,
        help="Write the answers to JSON, so the same project can be regenerated.",
    )
    args = parser.parse_args(argv)

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

    try:
        result = generate(
            spec,
            destination,
            zip_output=not args.no_zip,
            icon_source=args.icon,
            git_init=args.git,
        )
    except render.RenderError as error:
        print(prompts.red(f"\n{error}"))
        return 1

    report(spec, result, destination, zip_output=not args.no_zip)
    return 0


# ─────────────────────────────────────────────────────────────────────────────
# Generation
# ─────────────────────────────────────────────────────────────────────────────


def generate(
    spec: ProjectSpec,
    destination: Path,
    zip_output: bool,
    icon_source: Path | None = None,
    git_init: bool = False,
) -> render.RenderResult:
    """
    Builds the project in a temporary directory, then moves it into place.

    A failure halfway through then leaves nothing behind, rather than a half-written project the
    user has to recognise as broken and delete.
    """
    with tempfile.TemporaryDirectory(prefix="androidgen-") as staging:
        project_dir = Path(staging) / spec.pascal_name

        warnings = render.copy_template(TEMPLATE_DIR, project_dir, spec)
        render.overlay_variants(VARIANTS_DIR, project_dir, spec)
        scaffold.write_feature_modules(project_dir, spec)
        render.rewrite_all(project_dir, spec, scaffold.generated_blocks(spec))
        render.apply_build_settings(project_dir, spec)
        render.apply_app_name(project_dir, spec)
        render.apply_fonts(project_dir, spec)
        render.apply_accent(project_dir, spec)
        render.apply_feel(project_dir, spec)

        keystores = list(spec.keystores)
        generated, skipped, key_warnings = render.generate_keystores(project_dir, keystores)
        render.write_keystore_properties(project_dir, [k for k in keystores if k.name in generated])
        write_readme(project_dir, spec)

        warnings.extend(key_warnings)

        if icon_source is not None:
            warnings.extend(icons.generate(icon_source, project_dir, spec))

        # After everything is written, so the first commit is the project as it ships. A repo
        # whose initial commit is half the files is worse than no repo at all — the first `git
        # status` in a new project should be clean.
        if git_init:
            warnings.extend(render.git_init(project_dir, spec))

        destination.parent.mkdir(parents=True, exist_ok=True)
        if zip_output:
            render.zip_project(project_dir, destination)
        else:
            if destination.exists():
                shutil.rmtree(destination)
            shutil.move(str(project_dir), str(destination))

        return render.RenderResult(
            project_dir=destination,
            keystores_generated=generated,
            keystores_skipped=skipped,
            warnings=warnings,
        )


def resolve_destination(spec: ProjectSpec, args: argparse.Namespace) -> Path | None:
    default_name = spec.pascal_name if args.no_zip else f"{spec.pascal_name}.zip"

    if args.out:
        out = args.out.expanduser().resolve()
        if args.no_zip or out.suffix == ".zip":
            return out
        return out / default_name

    return ask_save_location(default_name, zip_output=not args.no_zip)


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
