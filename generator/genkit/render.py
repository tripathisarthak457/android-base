"""
Turning a `ProjectSpec` and the template on disk into a project.

Four passes, in order:

1. **Copy** the template, skipping build output and anything the disabled features own.
2. **Overlay** the variant directories for features that are off but need a *different* file
   rather than no file.
3. **Rewrite** every text file: strip the optional-feature markers, substitute the generated
   blocks, and rename the package and app throughout — including in the directory names.
4. **Emit** what only exists per project: keystores, `keystore.properties`, the README, and the
   scaffolding for any feature modules that were asked for.
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import zipfile
from dataclasses import dataclass
from pathlib import Path

from .spec import FEATURES_BY_KEY, KeystoreSpec, ProjectSpec

# ─────────────────────────────────────────────────────────────────────────────
# Markers
# ─────────────────────────────────────────────────────────────────────────────

#: `// <opt:name>` … `// </opt:name>` — keep the inner lines only when `name` is enabled.
#:
#: `// <opt:a|b>` keeps them when *any* of the named features is on. For the lines that more than
#: one feature needs and none of them owns — the `javax.inject.Inject` import that three optional
#: blocks in the application class share, and which is an unused import the moment all three are
#: off. Repeating it inside each block would produce a duplicate import when two are on.
_BLOCK_OPEN = re.compile(r"<opt:([a-z0-9|-]+)>")
_BLOCK_CLOSE = re.compile(r"</opt:([a-z0-9|-]+)>")

#: `// <opt:!name>CODE` — emit `CODE` verbatim only when `name` is *disabled*.
#:
#: The alternative code has to live inside a comment, because the template itself must compile:
#: two live declarations of the same class, one per branch, would break the build this project
#: relies on to prove the template works.
_INLINE_ELSE = re.compile(r"<opt:!([a-z0-9-]+)>")

#: `// <generated:name>` — replaced wholesale by generated lines.
_GENERATED = re.compile(r"<generated:([a-z0-9-]+)>")

#: Copied byte-for-byte. Rewriting a jar or a png as text corrupts it.
_BINARY_SUFFIXES = {".jar", ".png", ".jpg", ".jpeg", ".webp", ".ttf", ".otf", ".jks", ".keystore", ".ico"}

#: Never copied out of the template: build output, IDE state, and the machine-local SDK path.
_SKIP_DIRS = {"build", ".gradle", ".kotlin", ".idea", ".git", "keys", ".cxx"}
_SKIP_FILES = {"local.properties", "keystore.properties", ".DS_Store"}


class RenderError(RuntimeError):
    """Something went wrong producing the project. The message is meant for the user."""


@dataclass
class RenderResult:
    project_dir: Path
    keystores_generated: list[str]
    keystores_skipped: list[str]
    warnings: list[str]


# ─────────────────────────────────────────────────────────────────────────────
# Text rewriting
# ─────────────────────────────────────────────────────────────────────────────


def strip_markers(text: str, enabled: set[str], generated: dict[str, list[str]]) -> str:
    """
    Resolves every marker in one pass.

    Nested blocks are supported and are common: an `<opt:crashlytics>` import inside an
    `<opt:firebase>` region. A disabled outer block suppresses everything inside it regardless of
    the inner state, which is what "Crashlytics needs Firebase" means at the file level.
    """
    output: list[str] = []
    #: Stack of (feature, keeping). `keeping` is false for the whole nested region once any
    #: enclosing block is disabled.
    stack: list[tuple[str, bool]] = []

    for line in text.splitlines(keepends=True):
        close = _BLOCK_CLOSE.search(line)
        if close:
            if stack and stack[-1][0] == close.group(1):
                stack.pop()
            continue

        open_match = _BLOCK_OPEN.search(line)
        if open_match and not _INLINE_ELSE.search(line):
            feature = open_match.group(1)
            wanted = any(name in enabled for name in feature.split("|"))
            keeping = all(keep for _, keep in stack) and wanted
            stack.append((feature, keeping))
            continue

        inline = _INLINE_ELSE.search(line)
        if inline:
            if all(keep for _, keep in stack) and inline.group(1) not in enabled:
                output.append(line[inline.end():])
            continue

        placeholder = _GENERATED.search(line)
        if placeholder:
            if all(keep for _, keep in stack):
                output.extend(generated.get(placeholder.group(1), []))
            continue

        if all(keep for _, keep in stack):
            output.append(line)

    return "".join(output)


def rename(text: str, spec: ProjectSpec) -> str:
    """
    The template's own identity, replaced by the project's.

    Order matters: the longest and most specific token first, so that `com.base.app` is consumed
    before `baseapp` could match part of it.
    """
    return (
        text.replace("com.base.app", spec.package_name)
        .replace("com/base/app", spec.package_path)
        .replace("BaseApp", spec.pascal_name)
        .replace("base_app", spec.snake_name)
        .replace("baseapp", spec.lower_name)
    )


def collapse_blank_runs(text: str) -> str:
    """
    Tidies the whitespace a stripped block leaves behind.

    Three or more consecutive blank lines become one: a project generated with half the features
    off would otherwise be full of six-line gaps, which reads as carelessness in the first file
    anyone opens.

    A blank line against either side of a brace is removed outright. Those two are not cosmetic:
    ktlint fails the build on both `NoBlankLineBeforeRbrace` and `NoEmptyFirstLineInMethodBlock`,
    so a block stripped from the start or the end of a function turns a generated project red
    before its author has written anything.

    Only a brace, deliberately. A blank line after an opening *parenthesis* is legal and is
    sometimes how a long argument list is laid out, and collapsing it would be the generator
    reformatting code it was not asked to touch.
    """
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"\n[ \t]*\n([ \t]*\})", r"\n\1", text)
    return re.sub(r"(\{[ \t]*)\n[ \t]*\n", r"\1\n", text)


# ─────────────────────────────────────────────────────────────────────────────
# Copying
# ─────────────────────────────────────────────────────────────────────────────


def _owned_paths(spec: ProjectSpec) -> set[str]:
    """Template-relative paths belonging to features that are switched off."""
    owned: set[str] = set()
    for feature in FEATURES_BY_KEY.values():
        if feature.key in spec.features:
            continue
        for pattern in feature.files:
            owned.add(pattern.format(pkg_path="com/base/app"))
    return owned


def _is_owned(relative: Path, owned: set[str]) -> bool:
    posix = relative.as_posix()
    return any(posix == path or posix.startswith(path + "/") for path in owned)


def copy_template(template: Path, destination: Path, spec: ProjectSpec) -> list[str]:
    """
    Copies the template, dropping anything a disabled feature owns. Returns warnings.

    `os.walk` with the skip list pruned in place, rather than `rglob("*")` filtered afterwards.
    The template is 256 files, but a checkout that has been built once also contains tens of
    thousands under `build/` and `.gradle/` — and `rglob` walks into them all before the filter
    gets a say. On this machine that was the difference between sixteen seconds and one.
    """
    warnings: list[str] = []
    owned = _owned_paths(spec)

    for directory, subdirectories, filenames in os.walk(template):
        subdirectories[:] = sorted(d for d in subdirectories if d not in _SKIP_DIRS)

        here = Path(directory)
        relative_dir = here.relative_to(template)
        if _is_owned(relative_dir, owned):
            subdirectories[:] = []
            continue

        (destination / Path(rename(relative_dir.as_posix(), spec))).mkdir(
            parents=True, exist_ok=True
        )

        for filename in sorted(filenames):
            if filename in _SKIP_FILES:
                continue
            source = here / filename
            relative = source.relative_to(template)
            if _is_owned(relative, owned):
                continue

            target = destination / Path(rename(relative.as_posix(), spec))
            target.parent.mkdir(parents=True, exist_ok=True)
            if source.suffix.lower() in _BINARY_SUFFIXES:
                shutil.copy2(source, target)
            else:
                try:
                    target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
                except UnicodeDecodeError:
                    warnings.append(f"{relative} is not UTF-8; copied without rewriting.")
                    shutil.copy2(source, target)

            _make_executable_if_needed(target)

    return warnings


#: Files that have to be executable in the generated project. `write_text` creates a 0644 file, so
#: without this the very first command in the README — `./gradlew` — exits 126 with "permission
#: denied" on macOS and Linux. The zip path sets the bit itself; a directory copy has to do it
#: here. Named rather than mode-copied from the source, because a Windows checkout has no bit to
#: copy: git stores the mode, the filesystem does not.
_EXECUTABLE_NAMES = {"gradlew"}
_EXECUTABLE_SUFFIXES = {".sh"}


def _make_executable_if_needed(path: Path) -> None:
    if path.name not in _EXECUTABLE_NAMES and path.suffix.lower() not in _EXECUTABLE_SUFFIXES:
        return
    mode = path.stat().st_mode
    # Whoever can read it can run it, which is the mode git records for an executable file.
    path.chmod(mode | ((mode & 0o444) >> 2))


def overlay_variants(variants_root: Path, destination: Path, spec: ProjectSpec) -> None:
    """
    Copies the "feature is off" version of any file that has one.

    Runs after the main copy so it overwrites, and before the rewrite pass so its files are
    renamed and marker-stripped like everything else.
    """
    for feature in FEATURES_BY_KEY.values():
        if feature.variant_dir is None or feature.key in spec.features:
            continue
        source_root = variants_root / feature.variant_dir
        if not source_root.is_dir():
            raise RenderError(f"Missing variant directory for '{feature.key}': {source_root}")

        for source in source_root.rglob("*"):
            if source.is_dir():
                continue
            relative = source.relative_to(source_root)
            target = destination / Path(rename(relative.as_posix(), spec))
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")


def rewrite_all(destination: Path, spec: ProjectSpec, generated: dict[str, list[str]]) -> None:
    """The text pass: markers out, names in, and anything left hollow deleted."""
    enabled = set(spec.features)
    for path in sorted(destination.rglob("*")):
        if path.is_dir() or path.suffix.lower() in _BINARY_SUFFIXES:
            continue
        try:
            original = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue

        rewritten = collapse_blank_runs(rename(strip_markers(original, enabled, generated), spec))

        if path.suffix == ".kt" and _is_hollow_kotlin(rewritten):
            path.unlink()
            continue

        if rewritten != original:
            path.write_text(rewritten, encoding="utf-8")

    _prune_empty_directories(destination)


def _is_hollow_kotlin(text: str) -> bool:
    """
    True when a Kotlin file has a package line and imports and nothing else.

    Stripping every optional block out of a file can leave a shell: the app module's
    `FeatureBindingsModule` exists to supply things to the settings, auth and onboarding features,
    and a project with none of them gets an empty Hilt module and three unused imports — which
    detekt fails on, so the project arrives red. A file with nothing in it is not a file the
    project needs.

    Deliberately conservative: a single declaration of any kind keeps the file. It is looking for
    "nothing survived", not "not much survived".
    """
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith(("package ", "import ", "//", "/*", "*", "*/")):
            continue
        return False
    return True


def _prune_empty_directories(destination: Path) -> None:
    """
    Removes directories left with nothing in them.

    Deleting the last file in a package leaves the package directory behind, and an empty source
    directory in a fresh project is a small mystery for whoever opens it — deep enough, Android
    Studio shows it as a package that exists for no reason. Deepest first, so a directory whose
    only content was another empty directory goes too.
    """
    for path in sorted(destination.rglob("*"), key=lambda p: len(p.parts), reverse=True):
        if path.is_dir() and not any(path.iterdir()):
            path.rmdir()


# ─────────────────────────────────────────────────────────────────────────────
# Per-project files
# ─────────────────────────────────────────────────────────────────────────────


def apply_build_settings(destination: Path, spec: ProjectSpec) -> None:
    """
    Writes the numbers and URLs into `AppConfig.kt`, which is where the build reads them from.

    A regex substitution on named constants rather than a template placeholder, so the file stays
    valid Kotlin in the template and can be edited by hand afterwards without the generator's
    formatting getting in the way.
    """
    config = destination / "build-logic/convention/src/main/kotlin" / spec.package_path / "buildlogic/AppConfig.kt"
    if not config.exists():
        raise RenderError(f"AppConfig.kt not found where expected: {config}")

    text = config.read_text(encoding="utf-8")
    substitutions = {
        "APPLICATION_ID": f'"{spec.package_name}"',
        "APP_NAME": f'"{spec.pascal_name}"',
        "COMPILE_SDK": str(spec.compile_sdk),
        "MIN_SDK": str(spec.min_sdk),
        "TARGET_SDK": str(spec.target_sdk),
        "VERSION_CODE": str(spec.version_code),
        "VERSION_NAME": f'"{spec.version_name}"',
    }
    for name, value in substitutions.items():
        text = re.sub(rf"(const val {name} = ).*", rf"\g<1>{value}", text, count=1)

    for flavour, url in spec.api_base_urls.items():
        text = _replace_flavour_field(text, flavour, "apiBaseUrl", url)
    for flavour, url in spec.web_socket_urls.items():
        text = _replace_flavour_field(text, flavour, "webSocketUrl", url)

    config.write_text(text, encoding="utf-8")


def apply_fonts(destination: Path, spec: ProjectSpec) -> None:
    """
    Writes the chosen typeface names into `AppFontNames`.

    That object is the only place in the project a typeface is named — every one of the fifteen
    text styles is built from it — so this is the whole of "set the app's font".

    Silently skipped when the file is absent, which is not a real case today but would be if the
    design system were ever made optional; a generator that hard-fails on a missing cosmetic file
    is a generator that blocks on the least important thing.
    """
    fonts = (
        destination
        / "core/designsystem/src/main/kotlin"
        / spec.package_path
        / "core/designsystem/theme/AppFonts.kt"
    )
    if not fonts.exists():
        return

    text = fonts.read_text(encoding="utf-8")
    text = re.sub(r'(const val Sans = )"[^"]*"', rf'\g<1>"{spec.font_name}"', text, count=1)
    text = re.sub(r'(const val Mono = )"[^"]*"', rf'\g<1>"{spec.mono_font_name}"', text, count=1)
    fonts.write_text(text, encoding="utf-8")


def apply_feel(destination: Path, spec: ProjectSpec) -> None:
    """
    Writes the motion style and the haptics default into the one place that reads them.

    Both are arguments to the single `AppTheme` call in `AppRoot`, so a project can still change
    either at runtime — the generator only decides where it starts.
    """
    root = (
        destination
        / "app/src/main/kotlin"
        / spec.package_path
        / "ui/AppRoot.kt"
    )
    if not root.exists():
        return

    text = root.read_text(encoding="utf-8")
    haptics = "settings.hapticsEnabled" if spec.haptics_enabled else "false"
    call = "\n".join([
        "AppTheme(",
        "        mode = themeMode,",
        f"        motionStyle = AppMotionStyle.{spec.motion_style},",
        f"        hapticsEnabled = {haptics},",
        "    ) {",
    ])
    text = text.replace(
        "AppTheme(mode = themeMode, hapticsEnabled = settings.hapticsEnabled) {",
        call,
        1,
    )

    theme_import = f"import {spec.package_name}.core.designsystem.theme.AppTheme\n"
    style_import = f"import {spec.package_name}.core.designsystem.theme.AppMotionStyle\n"
    if style_import not in text:
        text = text.replace(theme_import, style_import + theme_import, 1)


    root.write_text(text, encoding="utf-8")


def _replace_flavour_field(text: str, flavour: str, field: str, value: str) -> str:
    """
    Replaces one field of one flavour without touching the identically-named field of another.

    The flavour blocks are structurally identical, so a naive replace on `apiBaseUrl = "..."`
    would rewrite whichever one happened to come first, four times.
    """
    pattern = re.compile(
        rf'(flavorName = "{flavour}",.*?{field} = )"[^"]*"',
        re.DOTALL,
    )
    return pattern.sub(rf'\g<1>"{value}"', text, count=1)


def apply_app_name(destination: Path, spec: ProjectSpec) -> None:
    """
    The display name, which is the one string that keeps its spaces.

    Everything else derives from `pascal_name`; this is what a user sees under the icon.
    """
    for relative in ("app/src/main/res/values/strings.xml", "catalog/src/main/res/values/strings.xml"):
        path = destination / relative
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        label = spec.app_name if relative.startswith("app/") else f"{spec.app_name} Catalog"
        text = re.sub(
            r'(<string name="app_name">).*?(</string>)',
            rf"\g<1>{label}\g<2>",
            text,
            count=1,
        )
        if spec.has("deeplink"):
            text = re.sub(
                r'(<string name="deeplink_scheme" translatable="false">).*?(</string>)',
                rf"\g<1>{spec.effective_deeplink_scheme}\g<2>",
                text,
                count=1,
            )
            text = re.sub(
                r'(<string name="deeplink_host" translatable="false">).*?(</string>)',
                rf"\g<1>{spec.effective_deeplink_host}\g<2>",
                text,
                count=1,
            )
        path.write_text(text, encoding="utf-8")


def write_keystore_properties(destination: Path, keystores: list[KeystoreSpec]) -> None:
    if not keystores:
        return
    lines = [
        "# Generated. Never commit this file — it is in .gitignore for a reason.",
        "# Store paths are relative to the root of the build.",
        "",
    ]
    for keystore in keystores:
        lines += [
            f"{keystore.name}.storeFile=keys/{keystore.name}.jks",
            f"{keystore.name}.storePassword={keystore.store_password}",
            f"{keystore.name}.keyAlias={keystore.alias}",
            f"{keystore.name}.keyPassword={keystore.key_password}",
            "",
        ]
    (destination / "keystore.properties").write_text("\n".join(lines), encoding="utf-8")


def find_keytool() -> str | None:
    """
    Where `keytool` is, or None.

    Checked under JAVA_HOME as well as on PATH because a JDK installed by Android Studio or by a
    Docker base image very often is not on PATH, and "no keystores were created" is a confusing
    thing to be told on a machine that plainly has a JDK.
    """
    found = shutil.which("keytool")
    if found is not None:
        return found

    java_home = os.environ.get("JAVA_HOME")
    if not java_home:
        return None
    candidate = Path(java_home) / "bin" / "keytool"
    if candidate.with_suffix(".exe").exists():
        return str(candidate.with_suffix(".exe"))
    if candidate.exists():
        return str(candidate)
    return None


def generate_keystores(
    destination: Path,
    keystores: list[KeystoreSpec],
) -> tuple[list[str], list[str], list[str]]:
    """
    Creates the .jks files with `keytool`.

    Returns (generated, skipped, warnings). A missing `keytool` is a warning rather than an
    error: the project is still complete and buildable — the build falls back to debug signing
    and says so — and failing the whole generation over a JDK that is not on PATH would be a
    poor trade.
    """
    if not keystores:
        return [], [], []

    keys_dir = destination / "keys"
    keys_dir.mkdir(parents=True, exist_ok=True)

    keytool = find_keytool()
    if keytool is None:
        return [], [k.name for k in keystores], [
            "keytool was not found on PATH or under JAVA_HOME, so no keystores were created. "
            "The project still builds — every variant falls back to the debug key and the build "
            "tells you so. Create them later and fill in keystore.properties."
        ]

    generated: list[str] = []
    skipped: list[str] = []
    warnings: list[str] = []

    for keystore in keystores:
        target = keys_dir / f"{keystore.name}.jks"

        if keystore.existing_path:
            source = Path(keystore.existing_path).expanduser()
            if source.is_file():
                shutil.copy2(source, target)
                generated.append(keystore.name)
            else:
                skipped.append(keystore.name)
                warnings.append(f"Keystore '{keystore.name}': {source} does not exist; skipped.")
            continue

        command = [
            keytool, "-genkeypair", "-noprompt",
            "-keystore", str(target),
            "-alias", keystore.alias,
            "-storepass", keystore.store_password,
            "-keypass", keystore.key_password,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", str(keystore.validity_days),
            "-dname", keystore.dname,
            "-storetype", "PKCS12",
        ]
        result = subprocess.run(command, capture_output=True, text=True, check=False)
        if result.returncode == 0:
            generated.append(keystore.name)
        else:
            skipped.append(keystore.name)
            warnings.append(
                f"Keystore '{keystore.name}' could not be created: "
                f"{(result.stderr or result.stdout).strip().splitlines()[-1] if (result.stderr or result.stdout).strip() else 'unknown error'}"
            )

    return generated, skipped, warnings


# ─────────────────────────────────────────────────────────────────────────────
# Packaging
# ─────────────────────────────────────────────────────────────────────────────

#: Files that have to stay executable inside the archive. Unzipping a project whose `gradlew` has
#: lost its executable bit produces "permission denied" as the very first thing a user sees.
_EXECUTABLE = {"gradlew"}


def zip_project(project_dir: Path, archive_path: Path) -> Path:
    archive_path.parent.mkdir(parents=True, exist_ok=True)
    root_name = project_dir.name

    with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(project_dir.rglob("*")):
            if path.is_dir():
                continue
            relative = path.relative_to(project_dir)
            info = zipfile.ZipInfo(f"{root_name}/{relative.as_posix()}")
            # 0o755 for the wrapper script, 0o644 for everything else. The high 16 bits of
            # external_attr are the Unix mode; without them every extracted file is 0o000 on some
            # unzip implementations.
            mode = 0o755 if path.name in _EXECUTABLE else 0o644
            info.external_attr = (mode & 0xFFFF) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, path.read_bytes())

    return archive_path


# ─────────────────────────────────────────────────────────────────────────────
# Version control
# ─────────────────────────────────────────────────────────────────────────────


def git_init(project_dir: Path, spec: ProjectSpec) -> list[str]:
    """
    Initialises a repository and commits the project as generated.

    Returns warnings, never raises: a missing `git` must not cost the user the project they
    just waited for.

    The commit is made with `-c user.name=...` rather than by configuring the repository, so it
    works on a machine with no global git identity — a fresh CI container, or a colleague's new
    laptop — without leaving a committer identity behind that they did not choose.
    """
    if shutil.which("git") is None:
        return ["git was not found on PATH, so the repository was not initialised."]

    commands = [
        # Not `--initial-branch`: that flag needs git 2.28, and the failure on an older one is a
        # confusing usage error rather than a fallback to master.
        ["git", "init"],
        ["git", "symbolic-ref", "HEAD", "refs/heads/main"],
        ["git", "add", "."],
        [
            "git",
            "-c", "user.name=Project generator",
            "-c", "user.email=generator@localhost",
            "commit",
            "--no-verify",
            "-m", f"Initial commit: {spec.app_name} generated from the Android base template",
        ],
    ]

    for command in commands:
        result = subprocess.run(
            command,
            cwd=project_dir,
            capture_output=True,
            text=True,
            check=False,
        )
        if result.returncode != 0:
            detail = (result.stderr or result.stdout).strip().splitlines()
            reason = detail[-1] if detail else f"exit code {result.returncode}"
            return [f"`{' '.join(command[:2])}` failed ({reason}). The project itself is fine."]

    return []


# ─────────────────────────────────────────────────────────────────────────────
# Brand colour
# ─────────────────────────────────────────────────────────────────────────────


def _hex_to_rgb(value: str) -> tuple[float, float, float]:
    cleaned = value.strip().lstrip("#")
    if len(cleaned) != 6:
        raise RenderError(f"'{value}' is not a six-digit hex colour, e.g. #2C6BED.")
    try:
        return tuple(int(cleaned[i : i + 2], 16) / 255 for i in (0, 2, 4))  # type: ignore[return-value]
    except ValueError as error:
        raise RenderError(f"'{value}' is not a hex colour.") from error


def _to_argb(rgb: tuple[float, float, float]) -> str:
    return "0xFF" + "".join(f"{round(max(0.0, min(1.0, c)) * 255):02X}" for c in rgb)


def _shift(rgb: tuple[float, float, float], lightness: float, saturation: float = 1.0):
    """Moves a colour along its own hue rather than towards black or white."""
    import colorsys

    hue, existing_lightness, sat = colorsys.rgb_to_hls(*rgb)
    return colorsys.hls_to_rgb(
        hue,
        max(0.0, min(1.0, existing_lightness * lightness if lightness <= 1 else
                     1 - (1 - existing_lightness) / lightness)),
        max(0.0, min(1.0, sat * saturation)),
    )


def _mix(rgb: tuple[float, float, float], other: tuple[float, float, float], amount: float):
    return tuple(a + (b - a) * amount for a, b in zip(rgb, other))


def _relative_luminance(rgb: tuple[float, float, float]) -> float:
    """WCAG relative luminance, for deciding whether text on this colour is black or white."""
    channels = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in rgb]
    return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]


def apply_accent(destination: Path, spec: ProjectSpec) -> None:
    """
    Derives the whole accent ramp from one brand colour.

    Six values, not one: the resting accent, its pressed state, a tint for subtle fills, and the
    same three again for the dark palette — plus the launcher background. Asking for six hex codes
    would get six that do not agree with each other, and asking for one and using it everywhere
    gives a pressed state that is invisible and a "subtle" fill that is not subtle.

    Everything is moved along the colour's own hue rather than towards black or white, so a brand
    orange darkens to a deeper orange instead of to brown.
    """
    if not spec.accent_colour:
        return

    base = _hex_to_rgb(spec.accent_colour)
    white = (1.0, 1.0, 1.0)
    ink = (0.043, 0.063, 0.106)

    replacements = {
        "Accent": _to_argb(base),
        "AccentPressed": _to_argb(_shift(base, 0.78)),
        "AccentSubtleLight": _to_argb(_mix(base, white, 0.90)),
        # The dark palette needs a lighter, slightly desaturated version: the same hex that reads
        # as confident on white reads as muddy on near-black, and a fully saturated accent on a
        # dark surface vibrates.
        "AccentDark": _to_argb(_shift(_mix(base, white, 0.22), 1.0, 0.92)),
        "AccentDarkPressed": _to_argb(_shift(_mix(base, white, 0.40), 1.0, 0.85)),
        "AccentSubtleDark": _to_argb(_mix(ink, base, 0.14)),
    }

    palette = (
        destination
        / "core/designsystem/src/main/kotlin"
        / spec.package_path
        / "core/designsystem/theme/Palette.kt"
    )
    if palette.exists():
        text = palette.read_text(encoding="utf-8")
        for name, value in replacements.items():
            text = re.sub(
                rf"(internal val {name} = Color\()0x[0-9A-Fa-f]{{8}}(\))",
                rf"\g<1>{value}\g<2>",
                text,
                count=1,
            )
        palette.write_text(text, encoding="utf-8")

    _apply_on_accent(destination, spec, base)

    # The adaptive icon's background, so the launcher matches the app it opens.
    launcher = f"#{replacements['Accent'][2:]}"
    for relative in (
        "app/src/main/res/values/colors.xml",
        "catalog/src/main/res/values/colors.xml",
    ):
        path = destination / relative
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        text = re.sub(
            r'(<color name="launcher_background">)#[0-9A-Fa-f]{6,8}(</color>)',
            rf"\g<1>#FF{launcher[3:] if len(launcher) > 7 else launcher[1:]}\g<2>",
            text,
            count=1,
        )
        path.write_text(text, encoding="utf-8")


def _apply_on_accent(destination: Path, spec: ProjectSpec, base: tuple[float, float, float]) -> None:
    """
    Picks black or white for text sitting on the accent.

    A brand yellow with white text on it is the most common way a themed design system produces
    something unreadable, and it is entirely mechanical to avoid: compare the contrast both ways
    and take the better one.
    """
    luminance = _relative_luminance(base)
    on_white = (1.05) / (luminance + 0.05)
    on_black = (luminance + 0.05) / 0.05
    light_on_accent = "White" if on_white >= on_black else "Ink900"

    colors = (
        destination
        / "core/designsystem/src/main/kotlin"
        / spec.package_path
        / "core/designsystem/theme/AppColors.kt"
    )
    if not colors.exists():
        return

    text = colors.read_text(encoding="utf-8")
    text = re.sub(
        r"(accentSubtle = AccentSubtleLight,\s*\n\s*onAccent = )\w+",
        rf"\g<1>{light_on_accent}",
        text,
        count=1,
    )
    colors.write_text(text, encoding="utf-8")
