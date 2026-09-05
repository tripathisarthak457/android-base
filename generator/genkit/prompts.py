"""
The interactive wizard.

Everything here is I/O and nothing here is logic: it collects answers, hands them to
`ProjectSpec`, and re-asks whatever `ProjectSpec.validated()` rejects. That split is what lets the
same generator be driven later by an HTTP request with no duplicated validation — and what keeps
the rules testable without a terminal.
"""

from __future__ import annotations

import re
import sys
from dataclasses import replace

from .spec import (
    API_LEVELS,
    DEFAULT_COMPILE_SDK,
    DEFAULT_MIN_SDK,
    DEFAULT_TARGET_SDK,
    FEATURES,
    KEYSTORE_NAMES,
    MOTION_STYLES,
    PRESETS,
    KeystoreSpec,
    ProjectSpec,
    SpecError,
    describe_api_level,
    preset_features,
    validate_country,
    validate_dname_part,
    validate_keystore,
    validate_module_name,
    validate_package_name,
)

# Windows consoles still default to a legacy code page, which cannot encode the box-drawing and
# arrow characters this wizard prints — and an unhandled UnicodeEncodeError halfway through a
# summary is a spectacularly unhelpful failure. Reconfiguring is a no-op where the stream is
# already UTF-8.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass

# ANSI, guarded: a redirected stream or a terminal that does not understand escapes gets plain
# text rather than a screen full of `[1m`.
_COLOUR = sys.stdout.isatty()

#: Shown at the typeface prompt. Not a whitelist — any Google Fonts family works — just a nudge
#: away from picking the first thing that comes to mind, which is usually Roboto.
FONT_SUGGESTIONS = (
    "DM Sans          geometric, large x-height, reads well from 11sp to 32sp",
    "Inter            the safe choice; designed for screens, very neutral",
    "Plus Jakarta Sans  a little more character, good for consumer apps",
    "Manrope          rounder and friendlier, slightly less good at small sizes",
    "Figtree          warm and open; strong at display sizes",
)


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


def heading(text: str) -> None:
    print()
    print(bold(text))
    print(dim("─" * len(text)))


def ask(question: str, default: str | None = None, allow_empty: bool = False) -> str:
    suffix = f" {dim(f'[{default}]')}" if default else ""
    while True:
        answer = input(f"{question}{suffix}: ").strip()
        if not answer and default is not None:
            return default
        if answer or allow_empty:
            return answer
        print(red("  A value is required."))


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


def ask_int(question: str, default: int, minimum: int | None = None) -> int:
    while True:
        answer = ask(question, str(default))
        try:
            value = int(answer)
        except ValueError:
            print(red("  That is not a whole number."))
            continue
        if minimum is not None and value < minimum:
            print(red(f"  Must be {minimum} or greater."))
            continue
        return value


def ask_password(question: str, minimum_length: int = 6) -> str:
    """
    Read visibly rather than through `getpass`.

    A hidden prompt is the right default for a password that authenticates something. This one is
    written to `keystore.properties` in plain text moments later, so hiding it would imply a
    secrecy the storage does not provide — and a silently mistyped keystore password is not
    discovered until a release build fails.
    """
    while True:
        value = input(f"{question}: ").strip()
        if len(value) >= minimum_length:
            return value
        print(red(f"  At least {minimum_length} characters — keytool rejects anything shorter."))


# ─────────────────────────────────────────────────────────────────────────────
# Sections
# ─────────────────────────────────────────────────────────────────────────────


def ask_identity() -> tuple[str, str]:
    heading("Project")
    app_name = ask("App name", "My App")

    suggested_package = "com.example." + re.sub(r"[^a-z0-9]", "", app_name.lower()) or "com.example.app"
    while True:
        package_name = ask("Package name (applicationId)", suggested_package)
        try:
            validate_package_name(package_name)
            return app_name, package_name
        except SpecError as error:
            print(red(f"  {error}"))


def ask_sdk_levels() -> tuple[int, int, int]:
    heading("Android versions")
    print(dim("  Anything below API 26 turns on core-library desugaring automatically."))
    print()
    for level in sorted(API_LEVELS):
        marker = green(" ← default") if level == DEFAULT_MIN_SDK else ""
        print(f"  {describe_api_level(level)}{marker}")
    print()

    min_sdk = ask_int("minSdk", DEFAULT_MIN_SDK, minimum=min(API_LEVELS))
    target_sdk = ask_int("targetSdk", DEFAULT_TARGET_SDK, minimum=min_sdk)
    compile_sdk = ask_int("compileSdk", max(DEFAULT_COMPILE_SDK, target_sdk), minimum=target_sdk)
    return min_sdk, target_sdk, compile_sdk


def ask_version() -> tuple[str, int]:
    heading("Version")
    print(dim("  dev and staging builds are stamped 1.0.0-devDebug and so on; prod and playstore"))
    print(dim("  releases stay a bare 1.0.0, because that string ends up on a store listing."))
    print()
    version_name = ask("versionName", "1.0.0")
    version_code = ask_int("versionCode", 1, minimum=1)
    return version_name, version_code


def ask_preset() -> set[str] | None:
    """
    Offers the three starting points, or None when the user wants to choose feature by feature.

    Presented before the per-feature run rather than instead of it: eighteen yes/no questions is
    the right interface for someone who knows exactly what they want, and the wrong one for
    everybody else.
    """
    print(dim("  Start from a preset, or answer for each feature."))
    print()
    for index, preset in enumerate(PRESETS, start=1):
        print(f"  {bold(str(index))}. {bold(preset.title)}")
        for line in wrap(preset.description, 72):
            print(dim(f"     {line}"))
    print(f"  {bold(str(len(PRESETS) + 1))}. {bold('Choose individually')}")
    print()

    choice = ask_int("  Which", 2, minimum=1)
    if choice > len(PRESETS):
        return None
    return preset_features(PRESETS[choice - 1].key)


def ask_features(select_all: bool, preset: str | None = None) -> set[str]:
    heading("Features")
    if select_all:
        print(dim("  Everything on, per --all."))
        return {feature.key for feature in FEATURES if not feature.implied_only}

    if preset is not None:
        print(dim(f"  Using the {preset} preset."))
        return preset_features(preset)

    chosen = ask_preset()
    if chosen is not None:
        return chosen

    print(dim("  Anything you skip can be added later; each is one module or one file."))
    selected: set[str] = set()
    for feature in FEATURES:
        if feature.implied_only:
            continue
        print()
        print(f"  {bold(feature.title)}")
        for line in wrap(feature.description, 74):
            print(dim(f"    {line}"))
        if feature.requires:
            print(dim(f"    Requires: {', '.join(feature.requires)}"))
        if ask_yes_no("  Include?", feature.default):
            selected.add(feature.key)
    return selected


def ask_environments(features: set[str]) -> tuple[dict[str, str], dict[str, str]]:
    if "network" not in features:
        return {}, {}

    heading("Backend URLs")
    print(dim("  One per environment. They become BuildConfig fields, so switching environment is"))
    print(dim("  a variant switch — no code change, and no library module is rebuilt."))
    if "sample" in features:
        print(dim("  The reference feature reads a public demo API, which is the dev default."))
    print()

    api: dict[str, str] = {}
    sockets: dict[str, str] = {}
    defaults = {
        "dev": "https://jsonplaceholder.typicode.com/" if "sample" in features else "https://dev.example.com/api/",
        "staging": "https://staging.example.com/api/",
        "prod": "https://api.example.com/api/",
    }

    for flavour in ("dev", "staging", "prod"):
        api[flavour] = ask(f"  {flavour} API base URL", defaults[flavour])
    # Play Store builds hit exactly the same backend as production; a separate URL there is a
    # misconfiguration waiting to happen rather than a feature.
    api["playstore"] = api["prod"]

    if "websocket" in features:
        print()
        for flavour in ("dev", "staging", "prod"):
            guess = api[flavour].replace("https://", "wss://").replace("http://", "ws://")
            sockets[flavour] = ask(f"  {flavour} WebSocket URL", guess.rstrip("/") + "/ws")
        sockets["playstore"] = sockets["prod"]

    return api, sockets


def ask_deeplinks(features: set[str], app_name: str) -> tuple[str, str]:
    if "deeplink" not in features:
        return "", ""

    heading("Deep links")
    print(dim("  The custom scheme always works. App Links additionally need an assetlinks.json"))
    print(dim("  served from https://<host>/.well-known/ — without it the user gets a chooser."))
    print()
    scheme = ask("  Custom scheme", re.sub(r"[^a-z0-9]", "", app_name.lower()) or "app")
    host = ask("  App Links host", "example.com")
    return scheme, host


def ask_fonts(features: set[str]) -> tuple[str, str]:
    """
    The app's typeface, by name.

    Only asked when downloadable fonts are on — without them the project uses the platform
    families and a name here would be a setting that does nothing.
    """
    if "googlefonts" not in features:
        return "DM Sans", "JetBrains Mono"

    heading("Typeface")
    print(dim("  Any family name from fonts.google.com, spelled as it is on the family's page."))
    print(dim("  It is written into one constant, and every text style in the app reads it."))
    print()
    for suggestion in FONT_SUGGESTIONS:
        print(dim(f"  {suggestion}"))
    print()

    font_name = ask("Font", "DM Sans")
    mono_font_name = ask("Monospace font", "JetBrains Mono")
    return font_name, mono_font_name


def ask_look_and_feel(features: set[str]) -> tuple[str, str, bool]:
    """
    The three decisions that change how the app looks and feels everywhere.

    Each is one value in one file: a hex that the whole accent ramp is derived from, an enum the
    press feedback reads, and a boolean the haptics read. Asking here rather than leaving them at
    a default is the difference between a generated project that looks generated and one that
    looks like the product it is going to be.
    """
    heading("Look and feel")

    print(dim("  The accent colour. Every shade of it — pressed, subtle, and the dark-theme"))
    print(dim("  versions — is derived from this one hex, along with the launcher icon's"))
    print(dim("  background and whether text on it is black or white."))
    print()
    accent = ask("Accent hex", "#2C6BED", allow_empty=True).strip()

    print()
    print(dim("  How controls respond to a finger."))
    print()
    for index, (name, description) in enumerate(MOTION_STYLES, start=1):
        print(f"  {bold(str(index))}. {bold(name)}")
        for line in wrap(description, 70):
            print(dim(f"     {line}"))
    print()
    choice = ask_int("  Which", 1, minimum=1)
    motion = MOTION_STYLES[min(choice, len(MOTION_STYLES)) - 1][0]

    print()
    print(dim("  Haptics are a light vibration when a control responds. The device's own"))
    print(dim("  setting always applies on top, so this cannot override someone who has"))
    print(dim("  turned them off."))
    haptics = ask_yes_no("  Haptics on by default?", True)

    return accent, motion, haptics


def ask_feature_modules() -> tuple[str, ...]:
    heading("Feature modules")
    print(dim("  Comma-separated, lowercase — e.g. auth, home, orders. Each produces a matching"))
    print(dim("  :data:<name> and :feature:<name> with a working screen, its navigation wiring"))
    print(dim("  and a test. Leave blank to add them yourself later."))
    print()

    while True:
        answer = ask("  Modules", "", allow_empty=True)
        if not answer:
            return ()
        names = tuple(part.strip().lower() for part in answer.split(",") if part.strip())
        try:
            for name in names:
                validate_module_name(name)
        except SpecError as error:
            print(red(f"  {error}"))
            continue
        if len(set(names)) != len(names):
            print(red("  Names must be unique."))
            continue
        return names


def ask_keystores(app_name: str, package_name: str) -> tuple[KeystoreSpec, ...]:
    heading("Signing keys")
    print(dim("  Four keys: dev, staging, prod and playstore."))
    print()
    print(dim("  dev and staging may share one — they never leave your team."))
    print(yellow("  prod and playstore must not share with anything. The Play upload key is the"))
    print(yellow("  one credential whose loss cannot be undone."))
    print()
    print(dim("  Skip this and every variant falls back to the debug key; the build says so."))
    print()

    if not ask_yes_no("  Set up signing keys now?", True):
        return ()

    default_alias = re.sub(r"[^a-z0-9]", "", app_name.lower()) or "app"

    # Checked here rather than at the end of the wizard: these two go into the certificate's
    # subject, and a comma in the organisation silently splits one field into two.
    while True:
        organisation = ask("  Organisation (for the certificate)", "Unknown")
        country = ask("  Country code (two letters)", "US").upper()[:2]
        try:
            validate_dname_part("organisation", organisation)
            validate_country(country)
            break
        except SpecError as error:
            print(red(f"  {error}"))

    keystores: list[KeystoreSpec] = []
    shared: KeystoreSpec | None = None

    for name in KEYSTORE_NAMES:
        print()
        print(f"  {bold(name)}")

        if name == "staging" and shared is not None:
            if ask_yes_no("    Reuse the dev key for staging?", True):
                keystores.append(replace(shared, name="staging"))
                continue

        while True:
            if ask_yes_no("    Use an existing .jks instead of generating one?", False):
                path = ask("    Path to .jks")
                alias = ask("    Key alias", f"{default_alias}-{name}")
                store_password = ask_password("    Keystore password")
                key_password = ask_password("    Key password")
                keystore = KeystoreSpec(
                    name=name,
                    alias=alias,
                    store_password=store_password,
                    key_password=key_password,
                    existing_path=path,
                )
            else:
                alias = ask("    Key alias", f"{default_alias}-{name}")
                store_password = ask_password("    Keystore password")
                key_password = ask("    Key password", store_password)
                keystore = KeystoreSpec(
                    name=name,
                    alias=alias,
                    store_password=store_password,
                    key_password=key_password,
                    common_name=f"{app_name} ({name})",
                    organisation=organisation,
                    country=country,
                )

            try:
                validate_keystore(keystore)
                break
            except SpecError as error:
                print(red(f"    {error}"))

        keystores.append(keystore)
        if name == "dev":
            shared = keystore

    return tuple(keystores)


# ─────────────────────────────────────────────────────────────────────────────
# The wizard
# ─────────────────────────────────────────────────────────────────────────────


def run_wizard(select_all: bool = False, preset: str | None = None) -> ProjectSpec:
    app_name, package_name = ask_identity()
    min_sdk, target_sdk, compile_sdk = ask_sdk_levels()
    version_name, version_code = ask_version()
    features = ask_features(select_all, preset)
    api_urls, socket_urls = ask_environments(features)
    scheme, host = ask_deeplinks(features, app_name)
    font_name, mono_font_name = ask_fonts(features)
    accent_colour, motion_style, haptics_enabled = ask_look_and_feel(features)
    feature_modules = ask_feature_modules()
    keystores = ask_keystores(app_name, package_name)

    spec = ProjectSpec(
        app_name=app_name,
        package_name=package_name,
        min_sdk=min_sdk,
        target_sdk=target_sdk,
        compile_sdk=compile_sdk,
        version_name=version_name,
        version_code=version_code,
        features=frozenset(features),
        feature_modules=feature_modules,
        keystores=keystores,
        api_base_urls=api_urls,
        web_socket_urls=socket_urls,
        deeplink_scheme=scheme,
        deeplink_host=host,
        font_name=font_name,
        mono_font_name=mono_font_name,
        accent_colour=accent_colour,
        motion_style=motion_style,
        haptics_enabled=haptics_enabled,
    )
    return spec.validated()


def summarise(spec: ProjectSpec) -> None:
    heading("Summary")
    rows = [
        ("App", f"{spec.app_name}  ({spec.pascal_name})"),
        ("Package", spec.package_name),
        ("Min SDK", describe_api_level(spec.min_sdk)),
        ("Target SDK", describe_api_level(spec.target_sdk)),
        ("Version", f"{spec.version_name} ({spec.version_code})"),
        ("Desugaring", "on — java.time below API 26" if spec.needs_desugaring else "off"),
        ("Features", ", ".join(sorted(spec.features)) or "none"),
        ("Modules", ", ".join(spec.feature_modules) or "none"),
        ("Typeface", f"{spec.font_name}  ·  {spec.mono_font_name}"),
        ("Accent", spec.accent_colour or "the template's blue"),
        ("Motion", f"{spec.motion_style}, haptics {'on' if spec.haptics_enabled else 'off'}"),
        ("Keys", ", ".join(k.name for k in spec.keystores) or "debug key only"),
    ]
    width = max(len(label) for label, _ in rows)
    for label, value in rows:
        for index, line in enumerate(wrap(value, 60)):
            prefix = f"  {label.rjust(width)}  " if index == 0 else "  " + " " * width + "  "
            print(prefix + line)


def wrap(text: str, width: int) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = f"{current} {word}".strip()
        if len(candidate) > width and current:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines or [""]
