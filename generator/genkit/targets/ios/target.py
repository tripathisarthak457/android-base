"""The SwiftUI target: `templates/ios`, its `ios.*` settings, and what it generates."""

from __future__ import annotations

import re
from functools import cached_property
from pathlib import Path
from typing import Any

from ... import tokens
from ...core import translations
from ...core.copy import write_text
from ...core.manifest import TemplateManifest
from ...core.spec import IosSettings, Spec, SpecError, Vocabulary, pascal_case
from ...core.target import BuildContext
from ...tokens import swift
from .. import foreign_options
from . import icons, project, scaffold, strings, tabs

TEMPLATE_DIR = Path(__file__).resolve().parents[4] / "templates" / "ios"

#: The iOS releases a project can target, oldest first. SwiftUI's `@Observable` and SwiftData set
#: the floor; a release past the newest has no SDK yet, so its first build would fail. Apple went
#: from 18 to 26.
IOS_VERSIONS: tuple[int, ...] = (17, 18, 26, 27)

#: Letters, digits and hyphens between dots, as Apple allows in a bundle identifier.
BUNDLE_ID_PATTERN = r"^[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+$"
MAX_BUNDLE_ID = 155
DEPLOYMENT_TARGET_PATTERN = r"^\d{2}\.\d$"
TEAM_ID_PATTERN = r"^[A-Z0-9]{10}$"

#: Words a module's tab case or type names cannot be, beyond the Kotlin and Java words every spec
#: already refuses.
SWIFT_KEYWORDS: frozenset[str] = frozenset({
    "Any", "Protocol", "Self", "Type", "actor", "as", "associatedtype", "async", "await", "break",
    "case", "catch", "class", "continue", "default", "defer", "deinit", "do", "else", "enum",
    "extension", "fallthrough", "false", "fileprivate", "for", "func", "guard", "if", "import", "in",
    "init", "inout", "internal", "is", "let", "nil", "open", "operator", "precedencegroup", "private",
    "protocol", "public", "repeat", "rethrows", "return", "self", "some", "static", "struct",
    "subscript", "super", "switch", "throw", "throws", "true", "try", "typealias", "var", "where",
    "while",
})

#: Module names that would make a `<Name>Data` target with the name of an Apple framework, which
#: then shadows it in every import.
_FRAMEWORK_COLLISIONS = frozenset({"core", "swift", "tabular"})

#: Modules every project may contain, besides the scaffolded ones. The app's own target is named
#: after the app and must not take one of these names, nor a system module's.
_PROJECT_MODULES = frozenset({
    "AppCore", "AppTesting", "DesignSystem", "Navigation", "Networking", "Persistence", "Core", "Features",
    "SampleData", "SampleFeature", "SettingsFeature", "OnboardingFeature",
})
_SYSTEM_MODULES = frozenset({
    "Accessibility", "Combine", "CoreData", "CoreGraphics", "Foundation", "Network", "OSLog",
    "Observation", "Swift", "SwiftData", "SwiftUI", "Testing", "UIKit", "XCTest", "os",
})
#: The app target's own types, which a module of the same name would hide.
_APP_TYPES = frozenset({"AppCommands", "AppModel", "AppServices", "AppTab", "AppWindow"})

_BUNDLE_ID = re.compile(BUNDLE_ID_PATTERN)
_DEPLOYMENT_TARGET = re.compile(DEPLOYMENT_TARGET_PATTERN)
_TEAM_ID = re.compile(TEAM_ID_PATTERN)


def bundle_id(spec: Spec) -> str:
    """The spec's bundle id, or the package name with `_` turned into `-`, which bundle ids refuse."""
    ios = spec.ios or IosSettings()
    return ios.bundle_id or spec.package_name.replace("_", "-")


class IosTarget:
    key = "ios"
    template_dir = TEMPLATE_DIR

    @cached_property
    def manifest(self) -> TemplateManifest:
        return TemplateManifest.load(self.template_dir)

    @cached_property
    def reserved_module_names(self) -> frozenset[str]:
        """The template's own feature directories, and names that would shadow an Apple framework."""
        sources = self.template_dir / "Packages" / "Features" / "Sources"
        names = {
            re.sub(r"(?<!^)(?=[A-Z])", "_", path.name.removesuffix("Feature")).lower()
            for path in sources.iterdir()
            if path.is_dir() and path.name.endswith("Feature")
        }
        return frozenset(names | _FRAMEWORK_COLLISIONS)

    def vocabulary(self) -> Vocabulary:
        return Vocabulary(
            design_styles=tokens.design_style_requirements(),
            motion_styles=tokens.motion_style_names(),
            reserved_module_names=self.reserved_module_names,
            foreign_options=foreign_options(self.key),
        )

    def validate(self, spec: Spec) -> list[str]:
        if spec.architecture != "compact":
            raise SpecError(
                "The Swift template has one layout, compact: every feature is already a pair of "
                "Swift package targets, which is what modular means on Android.",
                "architecture",
            )
        validate_ios(spec.ios or IosSettings(), spec.package_name)

        offered = translations.available(self.key)
        for tag in spec.languages:
            if tag not in offered:
                raise SpecError(
                    f"No translation for '{tag}'. Choose from: {', '.join(offered)}. English is "
                    "always included.",
                    "languages",
                )

        for name in spec.feature_modules:
            case = tabs.tab_case(name)
            if case in SWIFT_KEYWORDS:
                raise SpecError(f"Module name '{name}' is a Swift keyword, so its tab would not compile.", "feature_modules")
            if case in tabs.MEMBERS:
                raise SpecError(
                    f"Module name '{name}' would make a tab called {case}, which the app's tab type already "
                    "uses for something else. Pick another name.",
                    "feature_modules",
                )

        taken = _PROJECT_MODULES | _SYSTEM_MODULES | _APP_TYPES | {
            f"{pascal_case(name)}{suffix}" for name in spec.feature_modules for suffix in ("Data", "Feature")
        }
        if spec.pascal_name in taken or spec.pascal_name in SWIFT_KEYWORDS:
            raise SpecError(
                f"The app name makes a Swift module called {spec.pascal_name}, which the project or the "
                "system already uses. Pick another app name; the display name can still say anything.",
                "app_name",
            )
        # Xcode expands build settings written as $(NAME) or ${NAME} in Info.plist values, so such
        # text would not reach the home screen as typed.
        if "$(" in spec.label or "${" in spec.label:
            raise SpecError("The display name cannot contain $( or ${ on iOS: Xcode reads them as build settings.", "display_name")

        warnings = []
        if tabs.tab_modules(spec) != spec.feature_modules:
            warnings.append(
                f"No feature gives the app a tab, so a '{tabs.FALLBACK_MODULE}' module was scaffolded "
                "as its first one."
            )
        if spec.has("database") and not spec.has("sample"):
            warnings.append(
                "The app database is set up, but nothing keeps data in it yet: the reference feature is "
                "off, and scaffolded modules read from the network or memory. When a data module needs "
                "it, give that module a @ModelActor and build the app's ModelContainer in AppServices "
                "with ModelContainerFactory."
            )
        if spec.has("language") and not spec.languages:
            warnings.append(
                "The language shortcut opens the app's page in Settings, which offers a language only "
                "when the app ships more than one. Add a language, or leave the feature out."
            )
        return warnings

    def identity(self, spec: Spec) -> dict[str, str]:
        return {
            "bundle_id": bundle_id(spec),
            "package_path": spec.package_path,
            "pascal": spec.pascal_name,
            "lower": spec.lower_name,
        }

    def regions(self, spec: Spec) -> dict[str, list[str] | None]:
        return {
            **project.regions(spec, self.manifest),
            **scaffold.regions(spec),
            "shell-tabs": tabs.app_tabs_swift(spec),
        }

    def overlays(self, spec: Spec) -> dict[str, Path]:
        return {}

    def transform(self, project_dir: Path, spec: Spec) -> list[str]:
        scaffold.write_feature_modules(project_dir, spec)
        write_text(project_dir / spec.pascal_name / "Localizable.xcstrings", strings.render(tabs.app_strings(spec)))
        return []

    def finish(self, project_dir: Path, spec: Spec, context: BuildContext) -> list[str]:
        # The template ships its token copy emitted for its own defaults; the project's carries
        # the user's brand and choices.
        resolved = tokens.resolve(spec, "ios")
        design_tokens = project_dir / swift.RELATIVE_PATH
        if design_tokens.is_file():
            write_text(design_tokens, swift.emit(resolved))
        write_text(project_dir / spec.pascal_name / swift.ACCENT_COLOR_PATH, swift.accent_colorset(resolved))
        strings.translate(
            project_dir, spec, scaffold.user_names(spec), scaffolded=frozenset(scaffold.feature_targets(spec))
        )
        if context.icon_source is None:
            return []
        return icons.write(context.icon_source, project_dir / spec.pascal_name)

    def outline(self, spec: Spec) -> dict[str, list[str]]:
        return {
            "modules": [
                f"{pascal_case(name)}{suffix}" for name in tabs.tab_modules(spec) for suffix in ("Data", "Feature")
            ],
            "tabs": tabs.labels(spec),
        }

    def environment(self) -> dict[str, Any]:
        return {}

    def catalogue(self) -> dict[str, Any]:
        defaults = IosSettings()
        return {
            "defaults": {"architecture": "compact", "ios": {"deployment_target": defaults.deployment_target}},
            "fields": [
                {"key": "ios.bundle_id", "type": "string", "rule": "ios_bundle_id", "max": MAX_BUNDLE_ID},
                {
                    "key": "ios.deployment_target", "type": "string", "default": defaults.deployment_target,
                    "choices": [{"value": f"{major}.0", "label": f"iOS {major}"} for major in IOS_VERSIONS],
                },
                {"key": "ios.team_id", "type": "string", "rule": "ios_team_id"},
            ],
            "architectures": [
                {
                    "key": "compact",
                    "title": "Local packages",
                    "description": "Core and Features Swift packages, with a data and a feature target per feature.",
                    "available": True,
                },
            ],
            "reserved_module_names": sorted(self.reserved_module_names),
            "rules": {
                "ios_bundle_id": BUNDLE_ID_PATTERN,
                "ios_bundle_id_max": MAX_BUNDLE_ID,
                "ios_deployment_target": DEPLOYMENT_TARGET_PATTERN,
                "ios_versions": list(IOS_VERSIONS),
                "ios_team_id": TEAM_ID_PATTERN,
                "swift_reserved_words": sorted(SWIFT_KEYWORDS),
            },
        }


def validate_ios(ios: IosSettings, package_name: str) -> None:
    if ios.bundle_id:
        if len(ios.bundle_id) > MAX_BUNDLE_ID or not _BUNDLE_ID.fullmatch(ios.bundle_id):
            raise SpecError(
                "The bundle id must look like com.example.app: letters, digits and hyphens between "
                f"dots, at most {MAX_BUNDLE_ID} characters. Underscores are not allowed.",
                "ios.bundle_id",
            )
    elif len(package_name) > MAX_BUNDLE_ID:
        raise SpecError(
            f"The package name is longer than the {MAX_BUNDLE_ID} characters a bundle id may have; give "
            "a shorter ios.bundle_id.",
            "ios.bundle_id",
        )

    newest = IOS_VERSIONS[-1]
    if not _DEPLOYMENT_TARGET.fullmatch(ios.deployment_target) or int(
        ios.deployment_target.partition(".")[0]
    ) not in IOS_VERSIONS:
        raise SpecError(
            f"The deployment target must be an iOS release from {IOS_VERSIONS[0]}.0 to {newest}.x, "
            f"like 17.0: {', '.join(str(major) for major in IOS_VERSIONS)}.",
            "ios.deployment_target",
        )
    if ios.team_id and not _TEAM_ID.fullmatch(ios.team_id):
        raise SpecError("The team id is the 10 capital letters and digits Apple shows under Membership.", "ios.team_id")
