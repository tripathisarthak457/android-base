"""
What a project with UI components says about itself holds for the features it was generated with: the startup
profile names its own classes, and its docs, KDocs, manifest comments and overlay never send a reader to a screen or a
tool the project left out.
"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get

ANDROID = get("android")
#: A name other than the template's own, so everything the projects say about their own classes has to be renamed.
APP_NAME = "Acme Ui"
PACKAGE = "com.acme.ui"
PKG = PACKAGE.replace(".", "/")

SHAPES: dict[str, dict] = {
    "everything": {"features": sorted(ANDROID.manifest.preset_features("everything"))},
}


def generate(directory: Path, name: str, **fields) -> Path:
    raw = {"schema": 2, "platform": "android", "app_name": APP_NAME, "package_name": PACKAGE, **fields}
    project = directory / name
    pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
    return project


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


if __name__ == "__main__":
    unittest.main()
