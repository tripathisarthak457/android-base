"""
What a generated project is: the answers, the rules that validate them, and the catalogue of
optional features.
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

    `requires` is resolved transitively, so asking for push brings Firebase. `files` are removed
    from the project when the feature is off; `variant_dir` names a directory under
    `generator/variants/` copied over the project in that case, for a file that has to change
    rather than disappear.
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
            # module there is nothing for it to provide, and leaving it behind fails Hilt.
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
        files=(
            "core/network/src/main/kotlin/{pkg_path}/core/network/ResponseCache.kt",
            "core/network/src/test/kotlin/{pkg_path}/core/network/QueuedRequestReplayerTest.kt",
        ),
    ),
    Feature(
        key="coil",
        title="Image loading (Coil)",
        description="Remote images with a skeleton placeholder and a failure glyph.",
        default=True,
        files=("core/ui/src/main/kotlin/{pkg_path}/core/ui/AppNetworkImage.kt",),
    ),
    Feature(
        key="lottie",
        title="Lottie animations",
        description=(
            "An AppLottie component for raw, asset and URL animations, tintable to the theme and "
            "held on its last frame when the system's animations are off, with a success animation "
            "that plays when a password-reset link is sent."
        ),
        default=False,
        files=(
            "core/ui/src/main/kotlin/{pkg_path}/core/ui/AppLottie.kt",
            "core/ui/src/main/res/raw/lottie_success.json",
        ),
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
        description=(
            "Analytics, Crashlytics and Remote Config in one switch: the analytics seam sends to "
            "Firebase, every logged error becomes a Crashlytics breadcrumb, and feature flags "
            "resolve from Remote Config seeded with their declared defaults."
        ),
        default=False,
        requires=("analytics", "flags"),
        files=(
            # The placeholder json is renamed by the ordinary text pass, like any other file. It
            # has to be: the plugin matches the applicationId against a client entry and fails the
            # build when none matches, so a copy still saying `com.base.app` breaks the first build.
            "app/google-services.json",
            "core/analytics/src/main/kotlin/{pkg_path}/core/analytics/FirebaseAnalyticsTracker.kt",
            "core/analytics/src/main/kotlin/{pkg_path}/core/analytics/CrashlyticsReporter.kt",
            "core/flags/src/main/kotlin/{pkg_path}/core/flags/RemoteConfigFeatureFlags.kt",
        ),
        variant_dir="firebase-off",
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
        requires=("network",),
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
        key="database",
        title="App database (Room)",
        description=(
            "A database for the app's own data, separate from the network cache: an entity, "
            "a DAO returning flows, a hand-written migration and a test that replays it "
            "against a database that really was at the older version."
        ),
        default=False,
        requires=("room",),
        files=("core/database",),
    ),
    Feature(
        key="devtools",
        title="On-device inspector",
        description=(
            "A draggable badge naming the environment on every build except production, and "
            "a panel behind it holding the last 200 requests with their full bodies, timing "
            "and failure rate. Absent from a production build rather than hidden in one."
        ),
        default=True,
        files=(
            "core/devtools",
            "core/network/src/main/kotlin/{pkg_path}/core/network/NetworkRecording.kt",
            "app/src/main/kotlin/{pkg_path}/di/DevToolsModule.kt",
        ),
    ),
    Feature(
        key="licenses",
        title="Open source licences screen",
        description=(
            "The real dependency list, generated from the resolved classpath at build time "
            "and rendered by the design system. The build also fails on a licence the "
            "project has not allowed."
        ),
        default=False,
        requires=("settings",),
        files=(
            "feature/settings/src/main/kotlin/{pkg_path}/feature/settings/Licenses.kt",
            "build-logic/convention/src/main/kotlin/AndroidLicensesConventionPlugin.kt",
            "build-logic/convention/src/main/kotlin/{pkg_path}/buildlogic/BundleLicensesTask.kt",
        ),
    ),
    Feature(
        key="flags",
        title="Feature flag seam",
        description=(
            "Typed flags declared in one file with their defaults beside them, read through "
            "an interface that resolves locally until a vendor is bound. Lets both sides of "
            "a flag be written before anybody has chosen one."
        ),
        default=True,
        files=("core/flags",),
    ),
    Feature(
        key="applock",
        title="Biometric app lock",
        description=(
            "A fingerprint, face or screen-lock prompt when the app comes back from the "
            "background, a setting to turn it on, and the app kept out of the task "
            "switcher's thumbnail."
        ),
        default=False,
        # The toggle lives on the settings screen, and the unlock screen reads the stored
        # theme so it does not flash the wrong one on the way in.
        requires=("settings",),
        files=("app/src/main/kotlin/{pkg_path}/lock",),
    ),
    Feature(
        key="playstore",
        title="Play in-app update and review prompts",
        description=(
            "A flexible update offered in the background when a newer build is live, and a "
            "rating prompt on a schedule Play will actually honour rather than silently drop."
        ),
        default=False,
        files=("app/src/main/kotlin/{pkg_path}/playstore",),
    ),
    Feature(
        key="coverage",
        title="Coverage floor (Kover)",
        description=(
            "One merged coverage number for the whole project with a floor the build enforces, "
            "generated code excluded so the report is about code somebody wrote."
        ),
        default=False,
    ),
    Feature(
        key="depsanalysis",
        title="Dependency health report",
        description=(
            "Names every module declaring a dependency it never uses, or using one it only "
            "gets transitively — the second being the failure that surfaces months later as an "
            "unrelated module breaking. Printed on every pull request; a report rather than a "
            "gate, because two of this template's own decisions produce advice it is right to "
            "ignore."
        ),
        default=False,
    ),
    Feature(
        key="composemetrics",
        title="Compose stability check",
        description=(
            "Reads the Compose compiler's own report and fails on a design-system composable "
            "that restarts without being able to skip. A baseline holds what is already there, "
            "so it starts green and cannot get worse."
        ),
        default=False,
        files=(
            "build-logic/convention/src/main/kotlin/{pkg_path}/buildlogic/ComposeStability.kt",
            "config/compose-stability-baseline.txt",
        ),
    ),
    Feature(
        key="googlesignin",
        title="Sign in with Google",
        description=(
            "A Continue with Google button on the sign-in screen through Credential Manager. The "
            "ID token goes to your backend, which answers with its own tokens like any sign-in."
        ),
        default=False,
        requires=("auth",),
        files=("feature/auth/src/main/kotlin/{pkg_path}/feature/auth/GoogleSignIn.kt",),
    ),
    Feature(
        key="paging",
        title="Paged feed",
        description=(
            "A Feed tab on Paging 3: pages load as you scroll, a failed page keeps what is already "
            "loaded and offers a retry, and pull to refresh reloads from where you are. The list "
            "component lives in core/ui for your own feeds."
        ),
        default=False,
        requires=("network",),
        files=(
            "data/feed",
            "feature/feed",
            "core/ui/src/main/kotlin/{pkg_path}/core/ui/paging",
        ),
    ),
    Feature(
        key="search",
        title="Search screen",
        description=(
            "A Search tab: results as you type with a debounce, recent searches kept per user, "
            "and the empty, no-results and error states already drawn."
        ),
        default=False,
        requires=("network",),
        files=("data/search", "feature/search"),
    ),
    Feature(
        key="profile",
        title="Profile screen",
        description=(
            "A Profile tab with an avatar, name and bio, saved on the device per user. With the "
            "media picker on, the avatar can be a photo."
        ),
        default=False,
        files=("data/profile", "feature/profile"),
    ),
    Feature(
        key="language",
        title="In-app language picker",
        description=(
            "A Language row in settings. Uses the system's per-app language on Android 13+ so it "
            "also appears in the phone's settings, and applies it by hand on older releases."
        ),
        default=False,
        requires=("settings",),
        files=(
            "core/ui/src/main/kotlin/{pkg_path}/core/ui/locale",
            "app/src/main/res/resources.properties",
        ),
    ),
    Feature(
        key="browser",
        title="In-app browser",
        description=(
            "Web pages open in a Custom Tab coloured like the app rather than in the browser. "
            "Adds privacy and terms links to settings when settings is on."
        ),
        default=False,
        files=("core/ui/src/main/kotlin/{pkg_path}/core/ui/browser",),
    ),
    Feature(
        key="widget",
        title="Home-screen widget",
        description=(
            "A Glance widget in the launcher's colours showing the app's name and a line the app "
            "keeps current. Tapping it opens the app."
        ),
        default=False,
        files=(
            "app/src/main/kotlin/{pkg_path}/widget",
            "app/src/main/res/xml/app_widget_info.xml",
        ),
    ),
)

FEATURES_BY_KEY: dict[str, Feature] = {feature.key: feature for feature in FEATURES}

#: Feature keys from earlier releases, and what each became. None means it was removed.
LEGACY_FEATURES: dict[str, str | None] = {
    "analytics-firebase": "firebase",
    "crashlytics": "firebase",
    "flags-remote": "firebase",
    "staticanalysis": None,
    "ci": None,
    "fastlane": None,
    "architecturetests": None,
    "screenshottests": None,
}


def upgrade_features(features: set[str]) -> tuple[set[str], list[str]]:
    """Legacy keys translated to today's, with one line per change for whoever asked for them."""
    upgraded: set[str] = set()
    notes: list[str] = []
    for key in sorted(features):
        if key not in LEGACY_FEATURES:
            upgraded.add(key)
            continue
        replacement = LEGACY_FEATURES[key]
        if replacement is None:
            notes.append(f"'{key}' is no longer a feature and was left out.")
        else:
            upgraded.add(replacement)
            notes.append(f"'{key}' is now part of '{replacement}'.")
    return upgraded, notes


@dataclass(frozen=True)
class Preset:
    """A named starting point, so the common cases are one answer rather than eighteen."""

    key: str
    title: str
    description: str
    features: tuple[str, ...]


#: The features every preset includes, and the smallest project worth generating.
_LEAN = (
    "network",
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
    "flags",
    "devtools",
    "profile",
    "browser",
    "language",
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
            "The lean set plus offline cache, images, auth, onboarding, settings with a language "
            "picker, a profile tab and media. What most projects want on day one."
        ),
        features=_STANDARD,
    ),
    Preset(
        key="everything",
        title="Everything",
        description=(
            "The standard set plus Firebase, push, Google sign-in, a paged feed, search, a "
            "home-screen widget, Lottie, WorkManager, WebSocket and baseline profiles."
        ),
        features=_STANDARD
        + (
            "firebase",
            "analytics",
            "push",
            "googlesignin",
            "paging",
            "search",
            "widget",
            "lottie",
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
    """Adds everything the selection implies, transitively."""
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

#: The component looks, matching AppDesignStyle in the design system. The default first.
DESIGN_STYLES: tuple[tuple[str, str], ...] = (
    ("Utility", "Cool neutrals, hairline outlines, a docked tab bar. Tools, finance, admin."),
    ("Social", "Brand-tinted surfaces, pill buttons, heavier headlines, a floating tab bar. Feeds and chat."),
    ("Editorial", "Warm paper and ink, large tight display type, underlined fields. Reading and news."),
    ("Playful", "Cream surfaces, extra-bold type, thick outlines and offset shadows. Games and kids."),
)

DESIGN_STYLE_NAMES: tuple[str, ...] = tuple(name for name, _ in DESIGN_STYLES)

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

#: Names a scaffolded feature module may not take. The structural directories are the obvious half.
RESERVED_MODULE_NAMES: frozenset[str] = frozenset({
    "app", "core", "data", "feature", "build", "catalog", "benchmark",
    "auth", "onboarding", "sample", "settings", "feed", "search", "profile",
})

_PACKAGE_SEGMENT = re.compile(r"^[a-z][a-z0-9_]*$")
_HEX_COLOUR = re.compile(r"^#?[0-9A-Fa-f]{6}$")

_APP_NAME = re.compile(r"^[A-Za-z][A-Za-z0-9 ._-]*$")

#: A Google Fonts family name as the family page spells it: "Plus Jakarta Sans", "M PLUS 1p".
_FONT_NAME = re.compile(r"^[A-Za-z0-9][A-Za-z0-9 -]{0,59}$")

#: What a backend URL may contain. Deliberately narrower than RFC 3986: no quote, backslash or `$`,
#: because the value is written into a Kotlin string literal in build-logic, which Gradle compiles
#: and runs.
_URL_BODY = r"[A-Za-z0-9._~:/?#\[\]@!&'()*+,;=%-]+"
_HTTP_URL = re.compile(rf"^https?://{_URL_BODY}$")
_SOCKET_URL = re.compile(rf"^wss?://{_URL_BODY}$")

_SCHEME = re.compile(r"^[a-z][a-z0-9+.-]{0,31}$")
_HOST = re.compile(r"^(?=.{1,253}$)[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$")

#: The build flavours a URL can be set for. Anything else would be silently ignored.
FLAVOURS: tuple[str, ...] = ("dev", "staging", "prod", "playstore")


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
    #: The primary brand hex. The whole accent ramp — pressed, subtle, dark-theme — is derived
    #: from it, along with the launcher background and whether text on it is black or white.
    accent_colour: str = ""
    #: The supporting brand hexes. Blank means "work one out from the primary" rather than
    #: "leave the template's" — see render.brand_ramps for the rule and why it is that rule.
    secondary_colour: str = ""
    tertiary_colour: str = ""
    #: An AppMotionStyle name: how every control responds to a finger.
    motion_style: str = "Standard"
    #: An AppDesignStyle name: corners, borders, fields and the tab bar, chosen together.
    design_style: str = "Utility"
    #: Whether haptics are on by default. The user's own device setting still applies on top.
    haptics_enabled: bool = True
    #: Languages shipped besides English, as tags with a file in translations/: "es", "pt-BR".
    languages: tuple[str, ...] = ()

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
    def effective_deeplink_scheme(self) -> str:
        """The custom scheme to write into `strings.xml`, never empty."""
        return self.deeplink_scheme or self.lower_name or "app"

    @property
    def effective_deeplink_host(self) -> str:
        """The App Links host, never empty, for the same reason as the scheme."""
        return self.deeplink_host or "example.com"

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
        """Returns a spec with implied features resolved, or raises [SpecError]."""
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

        for keystore in self.keystores:
            validate_keystore(keystore)
        names = [keystore.name for keystore in self.keystores]
        if len(set(names)) != len(names):
            raise SpecError("Each signing key can only be described once.")

        features, _ = upgrade_features(set(self.features))
        unknown = features - set(FEATURES_BY_KEY)
        if unknown:
            raise SpecError(f"Unknown feature(s): {', '.join(sorted(unknown))}.")

        if self.design_style not in DESIGN_STYLE_NAMES:
            raise SpecError(
                f"Unknown design style '{self.design_style}'. "
                f"Choose one of: {', '.join(DESIGN_STYLE_NAMES)}."
            )

        if self.motion_style not in MOTION_STYLE_NAMES:
            raise SpecError(
                f"Unknown motion style '{self.motion_style}'. "
                f"Choose one of: {', '.join(MOTION_STYLE_NAMES)}."
            )

        for label, value in (("typeface", self.font_name), ("monospace typeface", self.mono_font_name)):
            if not _FONT_NAME.match(value):
                raise SpecError(
                    f"The {label} '{value}' is not a Google Fonts family name — letters, digits, "
                    "spaces and hyphens, as fonts.google.com spells it."
                )

        for label, urls, pattern, example in (
            ("API base URL", self.api_base_urls, _HTTP_URL, "https://api.example.com/"),
            ("WebSocket URL", self.web_socket_urls, _SOCKET_URL, "wss://api.example.com/ws"),
        ):
            for flavour, url in urls.items():
                if flavour not in FLAVOURS:
                    raise SpecError(
                        f"Unknown flavour '{flavour}' for an {label}. "
                        f"Choose from: {', '.join(FLAVOURS)}."
                    )
                if not pattern.match(url):
                    raise SpecError(
                        f"The {flavour} {label} must look like {example}, without quotes, "
                        "spaces, backslashes or $."
                    )

        if self.deeplink_scheme and not _SCHEME.match(self.deeplink_scheme):
            raise SpecError(
                "The deep-link scheme must start with a lowercase letter and contain only "
                "lowercase letters, digits, +, . and -."
            )
        if self.deeplink_host and not _HOST.match(self.deeplink_host):
            raise SpecError("The deep-link host must be a domain name, like example.com.")

        for label, value in (
            ("accent", self.accent_colour),
            ("secondary", self.secondary_colour),
            ("tertiary", self.tertiary_colour),
        ):
            if value and not _HEX_COLOUR.match(value.strip()):
                raise SpecError(
                    f"The {label} colour must be a six-digit hex, e.g. #2C6BED."
                )

        # Imported here: translations reads specs, so a module-level import would be circular.
        from .translations import available as available_languages

        offered = available_languages()
        for tag in self.languages:
            if tag not in offered:
                raise SpecError(
                    f"No translation for '{tag}'. Choose from: {', '.join(offered)}. English is "
                    "always included."
                )
        if len(set(self.languages)) != len(self.languages):
            raise SpecError("Each language can only be listed once.")

        return replace(self, features=frozenset(resolve_features(features)))


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
    if name in RESERVED_MODULE_NAMES:
        raise SpecError(
            f"Module name '{name}' is taken: the template already ships a module or directory "
            "called that. Pick another name."
        )


#: keytool's own floor for a PKCS12 store. Shorter is rejected several frames later, by a
#: subprocess whose stderr the user never sees.
MIN_KEYSTORE_PASSWORD = 6

#: The maximum `-validity`. Roughly a century, which is already past the point of meaning.
MAX_VALIDITY_DAYS = 36_500

_ALIAS = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
_COUNTRY = re.compile(r"^[A-Za-z]{2}$")

#: Characters with a meaning inside an X.500 distinguished name. Left in, they do not escape a
#: shell — `keytool` is run from a list, never a string — but they do split one DN field into two
#: and produce a certificate whose subject is not what was asked for.
_DNAME_SPECIALS = set(',=+<>#;\\"')


def validate_keystore(keystore: KeystoreSpec) -> None:
    """Checks one signing key, before `keytool` sees any of it."""
    if keystore.name not in KEYSTORE_NAMES:
        raise SpecError(
            f"Unknown signing key '{keystore.name}'. "
            f"The build has four: {', '.join(KEYSTORE_NAMES)}."
        )
    if not _ALIAS.match(keystore.alias):
        raise SpecError(
            f"The {keystore.name} key alias must be letters, digits, dots, hyphens or "
            "underscores, and start with a letter or digit."
        )

    # An existing key is copied rather than created, so its passwords are the file's, not ours.
    if keystore.existing_path is None:
        for label, password in (
            ("store", keystore.store_password),
            ("key", keystore.key_password),
        ):
            if len(password) < MIN_KEYSTORE_PASSWORD:
                raise SpecError(
                    f"The {keystore.name} {label} password must be at least "
                    f"{MIN_KEYSTORE_PASSWORD} characters — keytool rejects anything shorter."
                )
            if not password.isprintable():
                raise SpecError(
                    f"The {keystore.name} {label} password cannot contain control characters."
                )

    validate_dname_part(f"{keystore.name} common name", keystore.common_name)
    validate_dname_part(f"{keystore.name} organisation", keystore.organisation)
    validate_country(keystore.country)

    if not 1 <= keystore.validity_days <= MAX_VALIDITY_DAYS:
        raise SpecError(
            f"The {keystore.name} validity must be between 1 and {MAX_VALIDITY_DAYS} days."
        )


_JAVA_KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
    "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
    "native", "new", "package", "private", "protected", "public", "return", "short", "static",
    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
    "void", "volatile", "while", "val", "var", "fun", "object", "in", "is", "when",
}


def validate_dname_part(label: str, value: str) -> None:
    """One component of a certificate's subject."""
    if not value.strip():
        raise SpecError(f"The {label} cannot be empty.")
    if not value.isprintable() or any(character in _DNAME_SPECIALS for character in value):
        raise SpecError(
            f"The {label} cannot contain any of {''.join(sorted(_DNAME_SPECIALS))} — they "
            "separate the fields of the certificate's subject."
        )


def validate_country(value: str) -> None:
    if not _COUNTRY.match(value):
        raise SpecError("The country must be a two-letter code, like US or IN.")
