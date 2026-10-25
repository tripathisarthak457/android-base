"""
What a project with UI components says about itself holds for the features it was generated with: the startup
profile names its own classes, and its docs, KDocs, manifest comments and overlay never send a reader to a screen or a
tool the project left out.
"""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.core.copy import is_verbatim
from genkit.targets import get
from tests import test_docs

ANDROID = get("android")
#: A name other than the template's own, so everything the projects say about their own classes has to be renamed.
APP_NAME = "Acme Ui"
PACKAGE = "com.acme.ui"
PKG = PACKAGE.replace(".", "/")

SHAPES: dict[str, dict] = {
    "everything": {"features": sorted(ANDROID.manifest.preset_features("everything"))},
    # Without `performance`, which owns the harness: the presets in both architectures.
    **{
        f"{preset}-{architecture}-without-performance": {
            "features": sorted(ANDROID.manifest.preset_features(preset) - {"performance"}),
            "architecture": architecture,
        }
        for preset, architecture in (
            ("lean", "compact"), ("standard", "modular"), ("everything", "compact"), ("everything", "modular"),
        )
    },
    # Push with each mix of the two screens that ask for its permission, and without UI components.
    "push-alone": {"features": ["push"]},
    "push-onboarding": {"features": ["onboarding", "push"]},
    "push-settings": {"features": ["push", "settings"]},
    "push-no-ui": {"features": ["onboarding", "push", "settings"], "ui_components": False},
}

#: What a project with push says asks for the permission to post notifications, by whether onboarding and settings
#: ask for it: in the push passage of docs/ARCHITECTURE.md, in rememberNotificationPermission's KDoc and in the
#: manifest's comment on the permission.
PUSH_WORDS = {
    (True, True): (
        "The permission is asked at the end of onboarding and offered again in settings.",
        "asked for in context: onboarding asks once at its end, and settings offers it again.",
        "Asked for in context on Android 13 and later: at the end of onboarding, and in settings.",
    ),
    (True, False): (
        "The permission is asked at the end of onboarding.",
        "asked for in context: onboarding asks once at its end.",
        "Asked for in context on Android 13 and later: at the end of onboarding.",
    ),
    (False, True): (
        "The permission is asked from the notifications row in settings.",
        "asked for in context: the notifications row in settings asks.",
        "Asked for in context on Android 13 and later: from the notifications row in settings.",
    ),
    (False, False): (
        "Nothing asks for the permission to post notifications yet",
        "Nothing in this project calls this yet",
        "Nothing asks for it yet",
    ),
}


#: Where a reader is sent to the harness `performance` owns.
HARNESS = re.compile(r"tools/perf|tools/adaptive|perf\.sh|gates\.toml")
#: Files that still send a reader to the harness in a project without `performance`, each outside this lane's files
#: and asked of the integrator in handoff/NOUI-FINAL.md's cross-lane requests. A file leaves the list when its text is
#: fenced, and the test fails while a listed file no longer names the harness.
HARNESS_PENDING = {
    f"app/src/dev/kotlin/{PKG}/perf/DevLaunchOptions.kt",
    f"app/src/dev/kotlin/{PKG}/perf/ShellLog.kt",
    f"app/src/main/kotlin/{PKG}/ui/AppRoot.kt",
    f"catalog/src/main/kotlin/{PKG}/catalog/FrameTimes.kt",
    f"catalog/src/main/kotlin/{PKG}/catalog/sections/sheets/CatalogSheets.kt",
    f"core/perfhud/src/test/kotlin/{PKG}/core/perfhud/recording/PerformanceRecorderTest.kt",
}


def generate(directory: Path, name: str, **fields) -> Path:
    raw = {"schema": 2, "platform": "android", "app_name": APP_NAME, "package_name": PACKAGE, **fields}
    project = directory / name
    pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
    return project


def flat(text: str) -> str:
    return " ".join(text.split())


def asks_in(project: Path, folder: str) -> bool:
    """Whether main code under `folder` calls rememberNotificationPermission, outside the file that declares it."""
    root = project / folder
    return root.is_dir() and any(
        re.search(r"\brememberNotificationPermission\s*[({]", text) and "fun rememberNotificationPermission(" not in text
        for path in root.rglob("*.kt") if "/src/main/" in path.as_posix()
        for text in [path.read_text(encoding="utf-8")]
    )


def push_words(project: Path) -> tuple[str, str, str]:
    """The push passage of the architecture doc, rememberNotificationPermission's KDoc and the permission's comment."""
    doc = flat(test_docs.prose((project / "docs/ARCHITECTURE.md").read_text(encoding="utf-8")))
    source = (project / f"core/notification/src/main/kotlin/{PKG}/core/notification/permission/NotificationPermission.kt")
    kdoc = re.search(r"/\*\*((?:(?!\*/).)*)\*/\s*@Composable\s*fun rememberNotificationPermission",
                     source.read_text(encoding="utf-8"), re.DOTALL)
    manifest = re.search(r"<!--((?:(?!-->).)*)-->\s*<uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\"",
                         (project / "core/notification/src/main/AndroidManifest.xml").read_text(encoding="utf-8"), re.DOTALL)
    passage = doc[doc.index("**Notifications.**"):doc.index("**Connectivity.**")]
    return passage, flat(re.sub(r"(?m)^\s*\*", " ", kdoc.group(1))), flat(manifest.group(1))


_temp: tempfile.TemporaryDirectory | None = None
_projects: dict[str, Path] = {}


def setUpModule():
    global _temp
    _temp = tempfile.TemporaryDirectory()
    _projects.update({name: generate(Path(_temp.name), name, **fields) for name, fields in SHAPES.items()})


def tearDownModule():
    _temp.cleanup()


class ProfileTest(unittest.TestCase):
    """The checked-in startup profiles of a UI project, generated under a name of its own as users do."""

    def test_the_profiles_name_only_classes_the_project_has(self):
        from tests.android_profile import classes_of, kept

        project = _projects["everything"]
        known = classes_of(project)
        profiles = {
            module: (project / module / "src/main/baseline-prof.txt").read_text(encoding="utf-8").splitlines()
            for module in ("app", "core/navigation", "core/designsystem", "core/ui")
        }
        for module, profile in profiles.items():
            with self.subTest(module=module):
                self.assertTrue(profile)
                self.assertEqual([], [line for line in profile if not kept(line, known, PKG)][:10])
        # The cold start runs through the application class and the Hilt graph built for it.
        for name in ("AcmeUiApplication", "Hilt_AcmeUiApplication", "DaggerAcmeUiApplication_HiltComponents_SingletonC"):
            with self.subTest(name=name):
                self.assertTrue(any(f"L{PKG}/{name};" in line for line in profiles["app"]))
        with self.subTest(name="injectAcmeUiApplication"):
            self.assertTrue(any(f"->injectAcmeUiApplication(L{PKG}/AcmeUiApplication;)" in line for line in profiles["app"]))


class PushPermissionTest(unittest.TestCase):
    """Who asks for the permission to post notifications, in every project with push, as its words say it."""

    # The docs' own claims check (what asks, what opens), held to each push shape here.
    pages = test_docs.GeneratedDocsTest.pages
    test_what_the_docs_say_asks_or_opens_is_what_the_code_does = (
        test_docs.GeneratedDocsTest.test_what_the_docs_say_asks_or_opens_is_what_the_code_does
    )

    @classmethod
    def setUpClass(cls):
        cls.projects = {name: project for name, project in _projects.items() if (project / "core/notification").is_dir()}
        cls.asking = {
            name: (asks_in(project, "feature/onboarding"), asks_in(project, "feature/settings"))
            for name, project in cls.projects.items()
        }

    def test_every_mix_of_asking_screens_is_held(self):
        self.assertEqual(set(PUSH_WORDS), set(self.asking.values()))
        self.assertEqual((False, False), self.asking["push-no-ui"])

    def test_the_doc_the_kdoc_and_the_manifest_name_only_the_screens_that_ask(self):
        places = ("docs/ARCHITECTURE.md", "rememberNotificationPermission's KDoc", "the manifest's comment")
        for name, project in self.projects.items():
            asking = self.asking[name]
            for index, text in enumerate(push_words(project)):
                with self.subTest(case=name, place=places[index]):
                    self.assertIn(PUSH_WORDS[asking][index], text)
                    self.assertEqual([], [words[index] for key, words in PUSH_WORDS.items()
                                          if key != asking and words[index] in text])


class HarnessAdviceTest(unittest.TestCase):
    """`performance` owns the harness, so a UI project without it has none, and nothing in it sends a reader there."""

    @classmethod
    def setUpClass(cls):
        cls.without = {name: project for name, project in _projects.items() if name.endswith("-without-performance")}

    def test_nothing_sends_a_reader_to_the_harness_the_project_left_out(self):
        self.assertTrue(self.without)
        for name, project in self.without.items():
            with self.subTest(case=name):
                self.assertFalse((project / "tools/perf").exists())
                named: dict[str, list[str]] = {}
                for path in sorted(project.rglob("*")):
                    relative = path.relative_to(project).as_posix()
                    if not path.is_file() or is_verbatim(relative, ANDROID.manifest.verbatim) \
                            or path.suffix.lower() in ANDROID.manifest.binary_suffixes:
                        continue
                    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                        if HARNESS.search(line):
                            named.setdefault(relative, []).append(f"{relative}:{number}")
                self.assertEqual([], [line for path, lines in named.items() if path not in HARNESS_PENDING for line in lines])
                self.assertEqual(set(), {path for path in HARNESS_PENDING if (project / path).is_file()} - set(named))

    def test_the_overlay_advises_measurements_the_project_can_take(self):
        strings = "core/perfhud/src/main/res/values/donottranslate.xml"
        with_overlay = {name: project for name, project in self.without.items() if (project / strings).is_file()}
        self.assertTrue(with_overlay)
        for name, project in with_overlay.items():
            text = (project / strings).read_text(encoding="utf-8")
            with self.subTest(case=name):
                self.assertIn("StartupTimingMetric", text)
                self.assertIn("adb shell am start -W", text)
        # Where the project has the harness, the overlay still sends the reader to it.
        self.assertIn("tools/perf/perf.sh coldstart", (_projects["everything"] / strings).read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
