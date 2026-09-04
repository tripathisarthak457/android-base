"""
What a generated project is: the answers, the rules that validate them, and the catalogue of
optional features.

Kept free of I/O and of prompting on purpose. The wizard in `prompts.py` produces a `ProjectSpec`,
and the renderer in `render.py` consumes one — so the same generator can later be driven by an
HTTP request body with no change to anything here, and every rule below is unit-testable without
a terminal.
"""

from __future__ import annotations

import keyword
import re
from dataclasses import dataclass, field, replace

# ─────────────────────────────────────────────────────────────────────────────
# Android platform table
# ─────────────────────────────────────────────────────────────────────────────

#: API level → (Android version, codename). Shown at the minSdk prompt, because nobody remembers
#: which number is which, and picking minSdk blind is how a project ends up excluding a quarter of
#: its market for no reason anyone can later reconstruct.
API_LEVELS: dict[int, tuple[str, str]] = {
    24: ("7.0", "Nougat"),
    25: ("7.1", "Nougat"),
    26: ("8.0", "Oreo"),
    27: ("8.1", "Oreo"),
    28: ("9", "Pie"),
    29: ("10", "Quince Tart"),
    30: ("11", "Red Velvet Cake"),
    31: ("12", "Snow Cone"),
    32: ("12L", "Snow Cone v2"),
    33: ("13", "Tiramisu"),
    34: ("14", "Upside Down Cake"),
    35: ("15", "Vanilla Ice Cream"),
    36: ("16", "Baklava"),
    37: ("17", "Cinnamon Bun"),
}

#: Below this, `java.time` needs the desugaring library — which the build turns on automatically.
DESUGARING_THRESHOLD = 26

DEFAULT_MIN_SDK = 26
DEFAULT_TARGET_SDK = 37
DEFAULT_COMPILE_SDK = 37


def describe_api_level(level: int) -> str:
    version, codename = API_LEVELS.get(level, ("?", "unknown"))
    return f"API {level} · Android {version} ({codename})"


# ─────────────────────────────────────────────────────────────────────────────
# Optional features
# ─────────────────────────────────────────────────────────────────────────────


@dataclass(frozen=True)
class Feature:
    """
    One switchable capability.

    `requires` is resolved transitively before rendering, so a user who asks for push
    notifications gets Firebase without having to know that push is a Firebase product.

    `files` are removed from the generated project when the feature is off. `variant_dir` is a
    directory under `generator/variants/` whose contents are copied over the project in that same
    case — the mechanism for a file that has to *change* rather than disappear, which markers
    inside a single file cannot express without breaking the template's own build.
    """

    key: str
    title: str
    description: str
    default: bool
    requires: tuple[str, ...] = ()
    files: tuple[str, ...] = ()
    variant_dir: str | None = None
    #: Hidden from the wizard; enabled purely as a dependency of something else.
    implied_only: bool = False


FEATURES: tuple[Feature, ...] = (
    Feature(
        key="network",
        title="REST networking (Ktor)",
        description=(
            "NetworkClient with typed helpers, bearer auth with transparent 401 refresh, "
            "classified failures, and a pluggable response-envelope unwrapper."
        ),
        default=True,
        files=(
            "core/network",
            # AppModule exists only to supply NetworkConfig from BuildConfig. Without the network
            # module there is nothing for it to provide, and leaving it behind fails Hilt. Named
            # file by file rather than by directory: `di/` also holds bindings that have nothing
            # to do with networking.
            "app/src/main/kotlin/{pkg_path}/di/AppModule.kt",
        ),
    ),
    Feature(
        key="websocket",
        title="WebSocket",
        description=(
            "One long-lived socket with exponential backoff, a jittered retry and a connection "
            "state a UI can render."
        ),
        default=False,
        requires=("network",),
        files=("core/network/src/main/kotlin/{pkg_path}/core/network/websocket",),
    ),
    Feature(
        key="room",
        title="Offline cache and request queue (Room)",
        description=(
            "Per-call-site response caching with a stale-on-failure fallback, plus a queue that "
            "replays failed mutations when connectivity returns."
        ),
        default=True,
        requires=("network",),
        files=("core/network/src/main/kotlin/{pkg_path}/core/network/ResponseCache.kt",),
    ),
    Feature(
        key="coil",
        title="Image loading (Coil)",
        description="Remote images with a skeleton placeholder and a failure glyph.",
        default=True,
        files=("core/ui/src/main/kotlin/{pkg_path}/core/ui/AppNetworkImage.kt",),
    ),
    Feature(
        key="workmanager",
        title="WorkManager",
        description=(
            "Deferrable background work with Hilt-injected workers, including the manifest fix "
            "that stops WorkManager self-initialising past the Hilt factory."
        ),
        default=False,
    ),
    Feature(
        key="analytics",
        title="Analytics and crash-reporting seam",
        description=(
            "Vendor-agnostic AnalyticsTracker and CrashReporter interfaces with no-op defaults, "
            "so instrumentation can be written before a vendor is chosen."
        ),
        default=True,
        files=("core/analytics",),
    ),
    Feature(
        key="firebase",
        title="Firebase",
        description="The google-services plugin and the Firebase BOM.",
        default=False,
        # The placeholder json is renamed by the ordinary text pass, like any other file. It has
        # to be: the plugin matches the applicationId against a client entry and fails the build
        # when none matches, so a copy still saying `com.base.app` breaks the very first build.
        files=("app/google-services.json",),
    ),
    Feature(
        key="analytics-firebase",
        title="Firebase Analytics",
        description="Binds the analytics seam to Firebase.",
        default=False,
        requires=("firebase", "analytics"),
        files=("core/analytics/src/main/kotlin/{pkg_path}/core/analytics/FirebaseAnalyticsTracker.kt",),
        variant_dir="analytics-firebase-off",
    ),
    Feature(
        key="crashlytics",
        title="Crashlytics",
        description=(
            "Binds the crash seam to Crashlytics, and routes every logged error through it as a "
            "breadcrumb."
        ),
        default=False,
        requires=("firebase", "analytics"),
        files=("core/analytics/src/main/kotlin/{pkg_path}/core/analytics/CrashlyticsReporter.kt",),
        variant_dir="crashlytics-off",
    ),
    Feature(
        key="push",
        title="Push notifications (FCM)",
        description=(
            "Notification channels, the runtime permission check, a messaging service, and token "
            "registration that re-submits on every launch."
        ),
        default=False,
        requires=("firebase",),
        files=(
            "core/notification",
            "app/src/main/kotlin/{pkg_path}/push",
        ),
    ),
    Feature(
        key="deeplink",
        title="Deep linking",
        description=(
            "A custom scheme plus verified App Links, resolved through one function, with the "
            "onNewIntent handling that is usually missing."
        ),
        default=True,
        files=("app/src/main/kotlin/{pkg_path}/deeplink",),
    ),
    Feature(
        key="catalog",
        title="Component catalog app",
        description=(
            "A second installable app showing every component in both themes. Depends on the "
            "design system alone, so design work rebuilds two modules instead of the whole graph."
        ),
        default=True,
        files=("catalog",),
    ),
    Feature(
        key="sample",
        title="Reference feature",
        description=(
            "A working list + detail feature against a live public API, with its tests. Proves "
            "every wire on first run; delete it once your own first feature exists."
        ),
        default=True,
        files=("data/sample", "feature/sample"),
    ),
    Feature(
        key="media",
        title="Media picker, permissions and compression",
        description=(
            "Photo/video/document/camera pickers on the modern APIs, a permission state that can "
            "tell 'denied' from 'denied for good', and tunable image and video compression."
        ),
        default=True,
        files=("core/media",),
    ),
    Feature(
        key="forms",
        title="Form state and validation",
        description=(
            "Composable validators, per-field touched/error state, and server-side errors mapped "
            "back onto the fields that caused them."
        ),
        default=True,
        files=(
            "core/common/src/main/kotlin/{pkg_path}/core/common/validation",
            "core/ui/src/main/kotlin/{pkg_path}/core/ui/form",
        ),
    ),
    Feature(
        key="auth",
        title="Sign in, sign up and password reset",
        description=(
            "Three screens against your auth endpoints, writing the encrypted token store — with "
            "server-side field errors mapped back onto the inputs that caused them."
        ),
        default=True,
        requires=("network", "forms"),
        files=("data/auth", "feature/auth"),
    ),
    Feature(
        key="settings",
        title="Settings screen",
        description=(
            "Theme switch, analytics opt-out, sign-out with confirmation and the build version — "
            "wired to the preference store and the session."
        ),
        default=True,
        files=("feature/settings",),
    ),
    Feature(
        key="onboarding",
        title="Onboarding flow",
        description=(
            "A paged walkthrough shown once, recorded in the preference store. Skipping counts "
            "as finishing, so it never comes back on the next launch."
        ),
        default=True,
        files=("feature/onboarding",),
    ),
    Feature(
        key="googlefonts",
        title="Downloadable Google Font",
        description=(
            "Inter and JetBrains Mono through the Play Services font provider — a real file per "
            "weight, which bundled variable fonts do not get on every OEM build."
        ),
        default=True,
        files=("core/designsystem/src/main/res/values/font_certs.xml",),
        variant_dir="googlefonts-off",
    ),
    Feature(
        key="staticanalysis",
        title="Detekt (with ktlint rules)",
        description="One static-analysis tool covering both style and formatting.",
        default=True,
        files=("config/detekt.yml",),
    ),
    Feature(
        key="leakcanary",
        title="LeakCanary (debug only)",
        description="Memory-leak detection in debug builds.",
        default=True,
    ),
    Feature(
        key="baselineprofile",
        title="Baseline profile and startup benchmark",
        description=(
            "A profile generator and a macrobenchmark that measures cold start with and without "
            "it. Typically a 20-30% cut in cold start for no code change."
        ),
        default=False,
        files=("benchmark",),
    ),
    Feature(
        key="fastlane",
        title="Fastlane release lanes",
        description=(
            "Version bump, changelog from git history, tag, signed artifacts, and a Play Store "
            "internal-track upload — each one lane."
        ),
        default=False,
        files=("fastlane", "Gemfile"),
    ),
    Feature(
        key="ci",
        title="GitHub Actions",
        description=(
            "Pull requests build devDebug, run detekt and the unit tests. Tags produce signed "
            "release artifacts."
        ),
        default=True,
        files=(".github",),
    ),
)

FEATURES_BY_KEY: dict[str, Feature] = {feature.key: feature for feature in FEATURES}


@dataclass(frozen=True)
class Preset:
    """
    A named starting point, so the common cases are one answer rather than eighteen.

    `everything` is deliberately not "all features": a preset is a recommendation, and a
    recommendation that includes the baseline-profile benchmark module — which most teams never
    run — is not one. `--all` still exists for that.
    """

    key: str
    title: str
    description: str
    features: tuple[str, ...]


#: The features every preset includes, and the smallest project worth generating.
_LEAN = (
    "network",
    "staticanalysis",
    "leakcanary",
    "sample",
    "catalog",
)

_STANDARD = _LEAN + (
    "room",
    "coil",
    "googlefonts",
    "forms",
    "auth",
    "settings",
    "onboarding",
    "media",
    "deeplink",
    "ci",
    "fastlane",
)

PRESETS: tuple[Preset, ...] = (
    Preset(
        key="lean",
        title="Lean",
        description="Networking, the design system, the catalog and one reference feature.",
        features=_LEAN,
    ),
    Preset(
        key="standard",
        title="Standard",
        description=(
            "The lean set plus offline cache, images, forms, auth, settings, onboarding, media "
            "and CI. What most projects want on day one."
        ),
        features=_STANDARD,
    ),
    Preset(
        key="everything",
        title="Everything",
        description=(
            "The standard set plus Firebase with Crashlytics, push, analytics, WorkManager, "
            "WebSocket and baseline profiles."
        ),
        features=_STANDARD
        + (
            "firebase",
            "crashlytics",
            "analytics",
            "analytics-firebase",
            "push",
            "workmanager",
            "websocket",
            "baselineprofile",
        ),
    ),
)

PRESETS_BY_KEY: dict[str, Preset] = {preset.key: preset for preset in PRESETS}


def preset_features(key: str) -> set[str]:
    """The resolved feature set for a preset name. Raises [SpecError] for an unknown name."""
    preset = PRESETS_BY_KEY.get(key)
    if preset is None:
        known = ", ".join(p.key for p in PRESETS)
        raise SpecError(f"Unknown preset '{key}'. Choose one of: {known}.")
    return resolve_features(set(preset.features))


def resolve_features(selected: set[str]) -> set[str]:
    """
    Adds everything the selection implies, transitively.

    Asking for Crashlytics and getting a build that fails because Firebase was not also ticked is
    the kind of paper cut that makes a generator feel unfinished.
    """
    resolved = set(selected)
    changed = True
    while changed:
        changed = False
        for key in list(resolved):
            for required in FEATURES_BY_KEY[key].requires:
                if required not in resolved:
                    resolved.add(required)
                    changed = True
    return resolved


# ─────────────────────────────────────────────────────────────────────────────
# Signing
# ─────────────────────────────────────────────────────────────────────────────

#: The motion personalities, matching AppMotionStyle in the design system. Ordered as the wizard
#: offers them: the default first.
MOTION_STYLES: tuple[tuple[str, str], ...] = (
    ("Standard", "Crisp, a trace of overshoot. Lively without asking for attention."),
    ("Bouncy", "Springy, with a visible overshoot. Playful; wrong for anything financial."),
    ("Calm", "No overshoot, slightly longer. For dense, professional interfaces."),
    ("Snappy", "The shortest duration that still reads as motion. For utilities."),
)

MOTION_STYLE_NAMES: tuple[str, ...] = tuple(name for name, _ in MOTION_STYLES)

#: The four keys, in the order the wizard asks for them. dev and staging may legitimately share;
#: prod and playstore must not share with anything.
KEYSTORE_NAMES: tuple[str, ...] = ("dev", "staging", "prod", "playstore")


@dataclass(frozen=True)
class KeystoreSpec:
    name: str
    alias: str
    store_password: str
    key_password: str
    #: When set, an existing .jks is copied instead of a new one being generated.
    existing_path: str | None = None
    #: X.500 distinguished name for a generated key.
    common_name: str = "Unknown"
    organisation: str = "Unknown"
    country: str = "US"
    validity_days: int = 10_000

    @property
    def dname(self) -> str:
        return f"CN={self.common_name}, O={self.organisation}, C={self.country}"


# ─────────────────────────────────────────────────────────────────────────────
# The spec
# ─────────────────────────────────────────────────────────────────────────────

_PACKAGE_SEGMENT = re.compile(r"^[a-z][a-z0-9_]*$")
_HEX_COLOUR = re.compile(r"^#?[0-9A-Fa-f]{6}$")

_APP_NAME = re.compile(r"^[A-Za-z][A-Za-z0-9 ._-]*$")


class SpecError(ValueError):
    """A rejected answer. Carries a message meant to be read by the person who typed it."""


@dataclass(frozen=True)
class ProjectSpec:
    app_name: str
    package_name: str
    min_sdk: int = DEFAULT_MIN_SDK
    target_sdk: int = DEFAULT_TARGET_SDK
    compile_sdk: int = DEFAULT_COMPILE_SDK
    version_name: str = "1.0.0"
    version_code: int = 1
    features: frozenset[str] = frozenset()
    feature_modules: tuple[str, ...] = ()
    keystores: tuple[KeystoreSpec, ...] = ()
    api_base_urls: dict[str, str] = field(default_factory=dict)
    web_socket_urls: dict[str, str] = field(default_factory=dict)
    deeplink_scheme: str = ""
    deeplink_host: str = ""
    #: A Google Fonts family name. Written into AppFontNames, which every text style reads from.
    font_name: str = "DM Sans"
    mono_font_name: str = "JetBrains Mono"
    #: One brand hex. The whole accent ramp — pressed, subtle, dark-theme — is derived from it.
    accent_colour: str = ""
    #: An AppMotionStyle name: how every control responds to a finger.
    motion_style: str = "Standard"
    #: Whether haptics are on by default. The user's own device setting still applies on top.
    haptics_enabled: bool = True

    # ── Derived names ────────────────────────────────────────────────────────

    @property
    def pascal_name(self) -> str:
        """`My Great App` → `MyGreatApp`. What class names and the Gradle root project use."""
        parts = re.split(r"[^A-Za-z0-9]+", self.app_name)
        return "".join(part[:1].upper() + part[1:] for part in parts if part)

    @property
    def lower_name(self) -> str:
        """`My Great App` → `mygreatapp`. Deep-link schemes and other lowercase identifiers."""
        return self.pascal_name.lower()

    @property
    def snake_name(self) -> str:
        """`My Great App` → `my_great_app`. Resource names and notification channel ids."""
        return re.sub(r"(?<!^)(?=[A-Z])", "_", self.pascal_name).lower()

    @property
    def package_path(self) -> str:
        return self.package_name.replace(".", "/")

    @property
    def needs_desugaring(self) -> bool:
        return self.min_sdk < DESUGARING_THRESHOLD

    def has(self, feature: str) -> bool:
        return feature in self.features

    # ── Validation ───────────────────────────────────────────────────────────

    def validated(self) -> "ProjectSpec":
        """
        Returns a spec with implied features resolved, or raises [SpecError].

        Validation lives on the spec rather than in the wizard so that the future HTTP entry point
        enforces exactly the same rules — and so these rules have tests.
        """
        if not _APP_NAME.match(self.app_name.strip()):
            raise SpecError(
                "App name must start with a letter and contain only letters, digits, spaces, "
                "dots, hyphens or underscores."
            )
        if not self.pascal_name:
            raise SpecError("App name must contain at least one letter or digit.")

        validate_package_name(self.package_name)

        if self.min_sdk not in API_LEVELS:
            raise SpecError(
                f"minSdk {self.min_sdk} is outside the supported range "
                f"({min(API_LEVELS)}–{max(API_LEVELS)})."
            )
        if self.target_sdk < self.min_sdk:
            raise SpecError("targetSdk cannot be lower than minSdk.")
        if self.compile_sdk < self.target_sdk:
            raise SpecError("compileSdk cannot be lower than targetSdk.")
        if self.version_code < 1:
            raise SpecError("versionCode must be 1 or greater.")
        if not re.match(r"^\d+\.\d+\.\d+$", self.version_name):
            raise SpecError("versionName must look like 1.0.0.")

        for module in self.feature_modules:
            validate_module_name(module)
        if len(set(self.feature_modules)) != len(self.feature_modules):
            raise SpecError("Feature module names must be unique.")

        unknown = set(self.features) - set(FEATURES_BY_KEY)
        if unknown:
            raise SpecError(f"Unknown feature(s): {', '.join(sorted(unknown))}.")

        if self.motion_style not in MOTION_STYLE_NAMES:
            raise SpecError(
                f"Unknown motion style '{self.motion_style}'. "
                f"Choose one of: {', '.join(MOTION_STYLE_NAMES)}."
            )

        if self.accent_colour and not _HEX_COLOUR.match(self.accent_colour.strip()):
            raise SpecError("The accent colour must be a six-digit hex, e.g. #2C6BED.")

        return replace(self, features=frozenset(resolve_features(set(self.features))))


def validate_package_name(package_name: str) -> None:
    segments = package_name.split(".")
    if len(segments) < 2:
        raise SpecError(
            "Package name needs at least two segments, like com.example.myapp — Play rejects a "
            "single-segment application id."
        )
    for segment in segments:
        if not _PACKAGE_SEGMENT.match(segment):
            raise SpecError(
                f"Package segment '{segment}' is invalid: lowercase letters, digits and "
                "underscores only, and it cannot start with a digit."
            )
        if keyword.iskeyword(segment) or segment in _JAVA_KEYWORDS:
            raise SpecError(
                f"Package segment '{segment}' is a reserved word — the generated directories "
                "would not be a valid package."
            )


def validate_module_name(name: str) -> None:
    if not _PACKAGE_SEGMENT.match(name):
        raise SpecError(
            f"Module name '{name}' is invalid: lowercase letters, digits and underscores only. "
            "It becomes both a Gradle path and a Kotlin package segment."
        )
    if name in {"sample", "app", "core", "data", "feature", "catalog", "benchmark"}:
        raise SpecError(f"Module name '{name}' is reserved by the project structure.")


_JAVA_KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
    "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
    "native", "new", "package", "private", "protected", "public", "return", "short", "static",
    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
    "void", "volatile", "while", "val", "var", "fun", "object", "in", "is", "when",
}
