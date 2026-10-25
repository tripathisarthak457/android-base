"""
The interactive wizard. It asks, checks each answer as it is typed with the same validators the
spec uses, and returns a schema-2 spec as plain data. A typo in the first answer costs one retype,
not forty answers.
"""

from __future__ import annotations

import re
import secrets
import sys
from collections.abc import Callable
from typing import Any, TextIO

from .. import brand, tokens
from ..targets.android import keystores
from ..targets.android.target import (
    API_LEVELS,
    DESUGARING_THRESHOLD,
    MAX_VERSION_CODE,
    MIN_COMPILE_SDK,
    describe_api_level,
)
from ..targets.multiplatform import target as multiplatform
from . import translations
from .catalogue import FONT_SUGGESTIONS
from .spec import (
    ARCHITECTURES,
    DEFAULT_COMPILE_SDK,
    DEFAULT_DESIGN_STYLE,
    DEFAULT_MIN_SDK,
    DEFAULT_MOTION_STYLE,
    DEFAULT_TARGET_SDK,
    FONT_PATTERN,
    HEX_PATTERN,
    HOST_PATTERN,
    HTTP_URL_PATTERN,
    MAX_LANGUAGES,
    SCHEMA,
    SCHEME_PATTERN,
    SOCKET_URL_PATTERN,
    VERSION_NAME_PATTERN,
    IosSettings,
    KeystoreSpec,
    Spec,
    SpecError,
    validate_app_name,
    validate_display_name,
    validate_feature_modules,
    validate_package_name,
)
from .target import Target

# Windows consoles still default to a legacy code page, which cannot encode the box-drawing and
# arrow characters this wizard prints; an encoding error halfway through a summary helps no one.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass

# Guarded, so a redirected stream gets plain text rather than a screen full of `[1m`.
_COLOUR = sys.stdout.isatty()


def _style(text: str, code: str) -> str:
    return f"\033[{code}m{text}\033[0m" if _COLOUR else text


def bold(text: str) -> str:
    return _style(text, "1")


def dim(text: str) -> str:
    return _style(text, "2")


def green(text: str) -> str:
    return _style(text, "32")


def yellow(text: str) -> str:
    return _style(text, "33")


def red(text: str) -> str:
    return _style(text, "31")


def heading(text: str, stream: TextIO | None = None) -> None:
    out = stream or sys.stdout
    print(file=out)
    print(bold(text), file=out)
    print(dim("─" * len(text)), file=out)


def wrap(text: str, width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for word in text.split():
        candidate = f"{current} {word}".strip()
        if len(candidate) > width and current:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines or [""]


def ask(question: str, default: str | None = None, allow_empty: bool = False) -> str:
    suffix = f" {dim(f'[{default}]')}" if default else ""
    while True:
        answer = input(f"{question}{suffix}: ").strip()
        if not answer and default is not None:
            return default
        if answer or allow_empty:
            return answer
        print(red("  A value is required."))


def ask_valid(question: str, check: Callable[[str], None], default: str | None = None, allow_empty: bool = False) -> str:
    """Asks until `check` accepts the answer, showing its message each time it does not."""
    while True:
        answer = ask(question, default, allow_empty)
        try:
            check(answer)
            return answer
        except SpecError as error:
            print(red(f"  {error}"))


def ask_yes_no(question: str, default: bool) -> bool:
    hint = "Y/n" if default else "y/N"
    while True:
        answer = input(f"{question} {dim(f'[{hint}]')}: ").strip().lower()
        if not answer:
            return default
        if answer in {"y", "yes"}:
            return True
        if answer in {"n", "no"}:
            return False
        print(red("  Answer y or n."))


def ask_int(question: str, default: int, minimum: int | None = None, maximum: int | None = None) -> int:
    while True:
        answer = ask(question, str(default))
        try:
            value = int(answer)
        except ValueError:
            print(red("  That is not a whole number."))
            continue
        if minimum is not None and value < minimum:
            print(red(f"  Must be {minimum} or greater."))
        elif maximum is not None and value > maximum:
            print(red(f"  Must be {maximum} or less."))
        else:
            return value


def ask_choice(question: str, options: list[tuple[str, str]], default: int = 1) -> int:
    """A numbered menu. Returns the 0-based index; a number outside the menu is asked again."""
    for index, (name, description) in enumerate(options, start=1):
        print(f"  {bold(str(index))}. {bold(name)}")
        for line in wrap(description, 70):
            print(dim(f"     {line}"))
    print()
    return ask_int(question, default, minimum=1, maximum=len(options)) - 1


def ask_password(question: str, minimum_length: int = 6, generate_if_blank: bool = False) -> str:
    """
    Read visibly, not through `getpass`: the point of asking here is that the user records it, and
    a password typed blind is a password typed wrong into the password manager.
    """
    hint = " (Enter to generate one)" if generate_if_blank else ""
    while True:
        value = input(f"{question}{hint}: ").strip()
        if not value and generate_if_blank:
            value = _strong_password()
            print(yellow(f"    Generated: {value}   Save it in your password manager now."))
            return value
        if len(value) >= minimum_length:
            return value
        print(red(f"  At least {minimum_length} characters: keytool refuses anything shorter."))


def _strong_password() -> str:
    # No characters that are easy to misread or that need escaping in a properties file.
    alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return "".join(secrets.choice(alphabet) for _ in range(20))


def _pattern_check(pattern: str, message: str, field: str) -> Callable[[str], None]:
    compiled = re.compile(pattern)

    def check(value: str) -> None:
        if value and not compiled.fullmatch(value):
            raise SpecError(message, field)

    return check


def ask_identity() -> dict[str, str]:
    heading("Project")
    app_name = ask_valid("App name", validate_app_name, "My App")
    print(dim("  The launcher label. Any characters; Enter to use the app name."))
    display_name = ask_valid("Display name", validate_display_name, "", allow_empty=True)
    suggested = "com.example." + (re.sub(r"[^a-z0-9]", "", app_name.lower()) or "app")
    package_name = ask_valid("Package name (applicationId)", validate_package_name, suggested)
    return {"app_name": app_name, "display_name": display_name, "package_name": package_name}


def ask_features(
    target: Target, select_all: bool, preset: str | None, options: dict[str, bool]
) -> tuple[set[str], dict[str, bool]]:
    """
    The features asked for, and every option's value. The options come first, as the website's box
    sits above its presets: the presets and features are then described as the project will have
    them, and a feature an option leaves out is never asked about.
    """
    manifest = target.manifest
    heading("Features")
    if select_all or preset is not None:
        print(dim("  Everything on, per --all." if select_all else f"  Using the {preset} preset."))
        values = ask_options(target, options)
        chosen = {feature.key for feature in manifest.selectable} if select_all else manifest.preset_features(preset)
        return set(chosen) - set(manifest.disabled_features(values)), values

    values = ask_options(target, options)
    left_out = manifest.disabled_features(values)
    print()
    print(dim("  Start from a preset, or answer for each feature."))
    print()
    presets = list(manifest.presets.values())
    choices = [(preset.title, manifest.preset_description(preset, values)) for preset in presets]
    choices.append(("Choose individually", "One question per feature."))
    default = next(index for index, preset in enumerate(presets, start=1) if preset.key == manifest.default_preset)
    choice = ask_choice("  Which", choices, default)
    if choice < len(presets):
        return set(manifest.preset_features(presets[choice].key)) - set(left_out), values

    print(dim("  Anything you skip can be added later; each is one module or one file."))
    selected: set[str] = set()
    asked: set[str] = set()
    for feature in manifest.selectable:
        if feature.key in left_out:
            continue
        print()
        print(f"  {bold(feature.title)}")
        for line in wrap(manifest.feature_copy(feature, values).description, 74):
            print(dim(f"    {line}"))
        if feature.requires:
            print(dim(f"    Requires: {', '.join(feature.requires)}"))
        # A yes adds the requirements back, so a feature on by default must not undo an earlier no.
        declined = [key for key in feature.requires if key in asked and key not in selected]
        if declined:
            print(dim(f"    You left out {', '.join(declined)}; including this adds it back."))
        if ask_yes_no("  Include?", feature.default and not declined):
            selected.add(feature.key)
        asked.add(feature.key)
    return selected, values


def ask_options(target: Target, given: dict[str, bool]) -> dict[str, bool]:
    """
    Every option's value: the ones the command line gave, and a yes or no on each other option's
    checkbox, which is unticked unless the answer is yes.
    """
    manifest = target.manifest
    values = manifest.option_values(given)
    for option in manifest.options.values():
        if option.key in given:
            print(dim(f"  {option.label}, per the command line."))
            continue
        print()
        print(f"  {bold(option.label)}")
        for line in wrap(option.headline, 74):
            print(dim(f"    {line}"))
        if option.disables:
            print(dim(f"    Leaves out {', '.join(option.disables)}."))
        if ask_yes_no(f"  {option.label}?", False):
            values[option.key] = not option.default
    return values


def ask_architecture(target: Target) -> str:
    available = [entry for entry in target.catalogue()["architectures"] if entry["available"]]
    if len(available) < 2:
        return available[0]["key"] if available else ARCHITECTURES[0]
    heading("Architecture")
    choice = ask_choice("  Which", [(entry["title"], entry["description"]) for entry in available])
    return available[choice]["key"]


def ask_look(features: frozenset[str], hidden: frozenset[str] = frozenset()) -> dict[str, Any]:
    """
    The decisions that change how the app looks and feels everywhere. A field in `hidden` is not
    asked, because an option took away what it decides, and keeps its default.
    """
    heading("Look and feel")
    print(dim("  The brand colours. Every shade of each, pressed, subtle and the dark-theme"))
    print(dim("  versions, is derived from these, with the launcher icon's background and"))
    print(dim("  whether text on each is black or white."))
    print()
    hex_check = _pattern_check(HEX_PATTERN, "A six-digit hex, like #2C6BED.", "accent_colour")
    accent = ask_valid("Primary hex", hex_check, brand.TEMPLATE_ACCENT, allow_empty=True)
    print()
    print(dim("  Leave either blank and it is worked out from the primary: the same hue with"))
    print(dim("  the chroma taken out for the secondary, a sixth of a turn round the wheel for"))
    print(dim("  the tertiary."))
    print()
    secondary = ask_valid("Secondary hex", hex_check, "", allow_empty=True)
    tertiary = ask_valid("Tertiary hex", hex_check, "", allow_empty=True)

    motion = DEFAULT_MOTION_STYLE
    if "motion_style" not in hidden:
        motions = tokens.load()["motion"]["styles"]
        print()
        print(dim("  How controls respond to a finger."))
        print()
        motion = list(motions)[ask_choice("  Which", [(name, style["description"]) for name, style in motions.items()])]

    design = DEFAULT_DESIGN_STYLE
    if "design_style" not in hidden:
        styles = tokens.load()["styles"]
        print()
        print(dim("  What the components look like: corners, borders, fields and the tab bar together."))
        print()
        while True:
            design = list(styles)[ask_choice("  Which", [(name, style["description"]) for name, style in styles.items()])]
            missing = [key for key in styles[design]["requires"] if key not in features]
            if not missing:
                break
            print(red(f"  {design} needs {', '.join(missing)}, which is not in this project."))

    print()
    print(dim("  Haptics are a light vibration when a control responds. The device's own setting"))
    print(dim("  always applies on top."))
    haptics = ask_yes_no("  Haptics on by default?", True)
    return {
        "accent_colour": accent,
        "secondary_colour": secondary,
        "tertiary_colour": tertiary,
        "motion_style": motion,
        "design_style": design,
        "haptics_enabled": haptics,
    }


def ask_fonts(features: frozenset[str]) -> dict[str, str]:
    """Only asked with downloadable fonts: without them a name here would do nothing."""
    if "googlefonts" not in features:
        return {}
    heading("Typeface")
    print(dim("  Any family name from fonts.google.com, spelled as it is on the family's page."))
    print()
    for name, note in FONT_SUGGESTIONS[:5]:
        print(dim(f"  {name.ljust(18)} {note}"))
    print()
    check = _pattern_check(FONT_PATTERN, "Letters, digits, spaces and hyphens, as fonts.google.com spells it.", "font_name")
    return {
        "font_name": ask_valid("Font", check, FONT_SUGGESTIONS[0][0]),
        "mono_font_name": ask_valid("Monospace font", check, "JetBrains Mono"),
    }


def ask_feature_modules(target: Target) -> list[str]:
    heading("Feature modules")
    print(dim("  Comma-separated, lowercase, e.g. home, orders. Each produces a working screen, its"))
    print(dim("  navigation wiring and a test. Leave blank to add them later."))
    print()
    reserved = target.vocabulary().reserved_module_names
    while True:
        answer = ask("  Modules", "", allow_empty=True)
        names = tuple(part.strip().lower() for part in answer.split(",") if part.strip())
        try:
            validate_feature_modules(names, reserved)
            return list(names)
        except SpecError as error:
            print(red(f"  {error}"))


def ask_languages(target: Target) -> list[str]:
    offered = translations.available(target.key)
    heading("Languages")
    print(dim("  English is always included. Add others as comma-separated tags; the language"))
    print(dim("  picker in Settings lists them."))
    print(dim("  " + ", ".join(f"{tag} ({name})" for tag, name in offered.items())))
    print()
    while True:
        answer = ask("  Languages", "", allow_empty=True)
        tags = list(dict.fromkeys(part.strip() for part in answer.split(",") if part.strip()))
        unknown = [tag for tag in tags if tag not in offered]
        if unknown:
            print(red(f"  No translation for {', '.join(unknown)}."))
        elif len(tags) > MAX_LANGUAGES:
            print(red(f"  At most {MAX_LANGUAGES}."))
        else:
            return tags


#: The Android template's flavours, which get a URL each; playstore always shares prod's.
ANDROID_FLAVOURS: tuple[str, ...] = ("dev", "staging", "prod")
_STORE_FLAVOUR = "playstore"
_URL_DEFAULTS = {
    "dev": "https://dev.example.com/api/",
    "staging": "https://staging.example.com/api/",
    "prod": "https://api.example.com/api/",
}


def ask_android(
    features: frozenset[str],
    app_name: str,
    flavours: tuple[str, ...] = ANDROID_FLAVOURS,
    signing: bool = True,
) -> dict[str, Any]:
    """
    The Android settings. A template with other `flavours` is asked for a URL for each of those and
    has no playstore copy; one without `signing` is not asked for keystores.
    """
    heading("Android versions")
    print(dim("  Anything below API 26 turns on core-library desugaring automatically."))
    print()
    for level in sorted(API_LEVELS):
        marker = green(" ← default") if level == DEFAULT_MIN_SDK else ""
        print(f"  {describe_api_level(level)}{marker}")
    print()
    while True:
        min_sdk = ask_int("minSdk", DEFAULT_MIN_SDK, minimum=min(API_LEVELS), maximum=max(API_LEVELS))
        if min_sdk in API_LEVELS:
            break
        print(red(f"  {min_sdk} is not a level this template supports."))
    newest = max(API_LEVELS)
    target_sdk = ask_int("targetSdk", DEFAULT_TARGET_SDK, minimum=min_sdk, maximum=newest)
    compile_sdk = ask_int(
        "compileSdk", max(DEFAULT_COMPILE_SDK, target_sdk), minimum=max(target_sdk, MIN_COMPILE_SDK), maximum=newest
    )

    heading("Version")
    print(dim("  dev and staging builds are stamped 1.0.0-devDebug and so on; prod and playstore"))
    print(dim("  releases stay a bare 1.0.0, because that string ends up on a store listing."))
    print()
    version_name = ask_valid(
        "versionName", _pattern_check(VERSION_NAME_PATTERN, "Like 1.0.0.", "android.version_name"), "1.0.0"
    )
    version_code = ask_int("versionCode", 1, minimum=1, maximum=MAX_VERSION_CODE)

    settings: dict[str, Any] = {
        "min_sdk": min_sdk,
        "target_sdk": target_sdk,
        "compile_sdk": compile_sdk,
        "version_name": version_name,
        "version_code": version_code,
    }

    if "network" in features:
        heading("Backend URLs")
        print(dim("  One per environment. They become BuildConfig fields, so switching environment is"))
        print(dim("  a variant switch, with no code change."))
        print()
        http = _pattern_check(HTTP_URL_PATTERN, "Like https://api.example.com/, without quotes, spaces or $.", "android.api_base_urls")
        defaults = {flavour: _URL_DEFAULTS[flavour] for flavour in flavours}
        api = {flavour: ask_valid(f"  {flavour} API base URL", http, default) for flavour, default in defaults.items()}
        # Play Store builds hit the same backend as production; a separate URL there is a
        # misconfiguration waiting to happen.
        if flavours == ANDROID_FLAVOURS:
            api[_STORE_FLAVOUR] = api["prod"]
        settings["api_base_urls"] = api
        if "websocket" in features:
            print()
            socket = _pattern_check(SOCKET_URL_PATTERN, "Like wss://api.example.com/ws.", "android.web_socket_urls")
            sockets = {}
            for flavour in defaults:
                guess = api[flavour].replace("https://", "wss://").replace("http://", "ws://").rstrip("/") + "/ws"
                sockets[flavour] = ask_valid(f"  {flavour} WebSocket URL", socket, guess)
            if flavours == ANDROID_FLAVOURS:
                sockets[_STORE_FLAVOUR] = sockets["prod"]
            settings["web_socket_urls"] = sockets

    if "deeplink" in features:
        heading("Deep links")
        print(dim("  The custom scheme always works. App Links also need an assetlinks.json served"))
        print(dim("  from https://<host>/.well-known/; without it the user gets a chooser."))
        print()
        settings["deeplink_scheme"] = ask_valid(
            "  Custom scheme",
            _pattern_check(SCHEME_PATTERN, "A lowercase letter, then lowercase letters, digits, +, . or -.", "android.deeplink_scheme"),
            re.sub(r"[^a-z0-9]", "", app_name.lower()) or "app",
        )
        settings["deeplink_host"] = ask_valid(
            "  App Links host",
            _pattern_check(HOST_PATTERN, "A domain name, like example.com.", "android.deeplink_host"),
            "example.com",
        )

    if signing:
        settings["keystores"] = ask_keystores(app_name)
    return settings


def ask_ios(package_name: str) -> dict[str, Any]:
    """The iOS app's identity and oldest supported release, checked as the target checks them."""
    heading("iOS")
    print(dim("  Xcode signs the app with your team; leave the team blank to choose it in Xcode."))
    print()

    def checked(field: str) -> Callable[[str], None]:
        def check(value: str) -> None:
            multiplatform.validate_ios(IosSettings(**{field: value}), package_name)

        return check

    return {
        "bundle_id": ask_valid("  Bundle id", checked("bundle_id"), package_name.replace("_", "-")),
        "deployment_target": ask_valid(
            "  Oldest iOS release", checked("deployment_target"), IosSettings().deployment_target
        ),
        "team_id": ask_valid("  Team id", checked("team_id"), "", allow_empty=True),
    }


def ask_keystores(app_name: str) -> list[dict[str, Any]]:
    heading("Signing keys")
    print(dim("  Four keys: dev, staging, prod and playstore. dev and staging may share one."))
    print(yellow("  prod and playstore must not share with anything. The Play upload key is the one"))
    print(yellow("  credential whose loss cannot be undone."))
    print()
    print(dim("  Skip this and every variant falls back to the debug key; the build says so."))
    print()
    if not ask_yes_no("  Set up signing keys now?", True):
        return []

    default_alias = re.sub(r"[^a-z0-9]", "", app_name.lower()) or "app"
    while True:
        organisation = ask("  Organisation (for the certificate)", "Unknown")
        country = ask("  Country code (two letters)", "US").upper()
        try:
            keystores.validate_dname_part("organisation", organisation)
            keystores.validate_country(country)
            break
        except SpecError as error:
            print(red(f"  {error}"))

    entries: list[dict[str, Any]] = []
    for name in keystores.KEYSTORE_NAMES:
        print()
        print(f"  {bold(name)}")
        if name == "staging" and entries and ask_yes_no("    Reuse the dev key for staging?", True):
            entries.append({**entries[0], "name": "staging"})
            continue
        while True:
            if ask_yes_no("    Use an existing .jks instead of generating one?", False):
                entry = {
                    "name": name,
                    "existing_path": ask("    Path to .jks"),
                    "alias": ask("    Key alias", f"{default_alias}-{name}"),
                    "store_password": ask_password("    Keystore password"),
                    "key_password": ask_password("    Key password"),
                }
            else:
                entry = {
                    "name": name,
                    "alias": ask("    Key alias", f"{default_alias}-{name}"),
                    # PKCS12 has one password for the store and its key, so there is one question.
                    "store_password": ask_password("    Keystore password", generate_if_blank=True),
                    "common_name": f"{app_name} ({name})",
                    "organisation": organisation,
                    "country": country,
                }
            try:
                keystores.validate(KeystoreSpec(**entry))
                break
            except SpecError as error:
                print(red(f"    {error}"))
        entries.append(entry)
    return entries


def run_wizard(
    target: Target, select_all: bool = False, preset: str | None = None, options: dict[str, bool] | None = None
) -> dict[str, Any]:
    """
    Every answer, as a schema-2 spec ready for `pipeline.read_spec`. `options` are the ones the
    command line already answered.
    """
    manifest = target.manifest
    raw: dict[str, Any] = {"schema": SCHEMA, "platform": target.key, **ask_identity()}
    requested, values = ask_features(target, select_all, preset, options or {})
    features = manifest.resolve(requested)
    raw["features"] = sorted(requested)
    raw.update({key: value for key, value in values.items() if value != manifest.options[key].default})
    hidden = frozenset(field for key, option in manifest.options.items() if not values[key] for field in option.hides)
    raw["architecture"] = ask_architecture(target)
    if target.key == "android":
        raw["android"] = ask_android(features, raw["app_name"])
    elif target.key == "multiplatform":
        raw["android"] = ask_android(features, raw["app_name"], flavours=multiplatform.FLAVOURS, signing=False)
        raw["ios"] = ask_ios(raw["package_name"])
    raw.update(ask_fonts(features))
    raw.update(ask_look(features, hidden))
    raw["feature_modules"] = ask_feature_modules(target)
    raw["languages"] = ask_languages(target)
    return raw


def summarise(spec: Spec, stream: TextIO | None = None) -> None:
    out = stream or sys.stdout
    heading("Summary", out)
    rows = [
        ("App", f"{spec.label}  ({spec.pascal_name})"),
        ("Package", spec.package_name),
        ("Target", spec.platform),
        ("Architecture", spec.architecture),
    ]
    if spec.android is not None:
        android = spec.android
        rows += [
            ("Min SDK", describe_api_level(android.min_sdk)),
            ("Target SDK", describe_api_level(android.target_sdk)),
            ("Version", f"{android.version_name} ({android.version_code})"),
            ("Desugaring", f"on: java.time below API {DESUGARING_THRESHOLD}" if android.min_sdk < DESUGARING_THRESHOLD else "off"),
        ]
    rows.append(("Features", ", ".join(sorted(spec.features)) or "none"))
    changed = [f"{key} {'on' if on else 'off'}" for key, on in spec.options.items() if on != spec.option_defaults[key]]
    if changed:
        rows.append(("Options", ", ".join(changed)))
    rows += [
        ("Modules", ", ".join(spec.feature_modules) or "none"),
        ("Languages", ", ".join((translations.SOURCE_LANGUAGE, *spec.languages))),
        ("Typeface", f"{spec.font_name}  ·  {spec.mono_font_name}"),
        ("Colours", describe_brand_colours(spec)),
        ("Style", spec.design_style),
        ("Motion", f"{spec.motion_style}, haptics {'on' if spec.haptics_enabled else 'off'}"),
    ]
    if spec.android is not None:
        rows.append(("Keys", ", ".join(k.name for k in spec.android.keystores) or "debug key only"))
    width = max(len(label) for label, _ in rows)
    for label, value in rows:
        for index, line in enumerate(wrap(value, 60)):
            prefix = f"  {label.rjust(width)}  " if index == 0 else "  " + " " * width + "  "
            print(prefix + line, file=out)


def describe_brand_colours(spec: Spec) -> str:
    """The three brand colours for the summary, derived ones marked as such."""
    resolved = brand.brand_colours(spec.accent_colour, spec.secondary_colour, spec.tertiary_colour)
    given = {"Accent": spec.accent_colour, "Secondary": spec.secondary_colour, "Tertiary": spec.tertiary_colour}
    parts = []
    for role, rgb in resolved.items():
        swatch = brand.to_hex(rgb)
        parts.append(swatch if given[role] else f"{swatch} ({'template' if role == 'Accent' else 'derived'})")
    return "  ·  ".join(parts)
