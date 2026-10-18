"""
The consistency matrix's checks on hand-made modules, where every answer is known, and what its runner
hands its worker processes.
"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from tests.consistency import __main__ as runner
from tests.consistency.checks import _hollow, _manifests, _resources, _translations, _unused_resources
from tests.consistency.project import Project

FILES = {
    "app/src/main/AndroidManifest.xml": """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:icon="@drawable/ic_used" android:networkSecurityConfig="@xml/network_security_config">
        <meta-data android:name="preloaded_fonts" android:resource="@array/preloaded_fonts" />
    </application>
</manifest>
""",
    "app/src/main/kotlin/x/Screen.kt": """package x
// R.string.only_in_a_comment
fun title() = R.string.used
fun other() = FeatureR.plurals.items
""",
    "app/src/main/res/values/strings.xml": """<resources>
    <string name="used">Used</string>
    <string name="only_in_a_comment">Commented</string>
    <!-- <string name="ignored">A comment, not a resource</string> -->
    <string name="unused">Unused</string>
    <plurals name="items"><item quantity="other">%d items</item></plurals>
</resources>
""",
    "app/src/main/res/values-fr/strings.xml": """<resources>
    <string name="used">Utilisé</string>
    <string name="unused">@string/translated_reference_only</string>
</resources>
""",
    "app/src/main/res/values/arrays.xml": """<resources>
    <array name="preloaded_fonts"><item>@font/body</item></array>
    <array name="certs"><item>@array/certs_dev</item></array>
    <string-array name="certs_dev"><item>abc</item></string-array>
    <string-array name="translated_reference_only"><item>abc</item></string-array>
</resources>
""",
    "app/src/main/res/values/themes.xml": """<resources>
    <style name="Theme.X"><item name="android:windowBackground">@color/window</item></style>
    <color name="window">@color/brand</color>
    <color name="brand">#FF0000</color>
    <color name="orphan">#00FF00</color>
</resources>
""",
    "app/src/main/res/font/body.xml": "<font-family />\n",
    "app/src/main/res/drawable/ic_used.xml": "<vector />\n",
    "app/src/main/res/drawable-night/ic_unused.xml": "<vector />\n",
    "app/src/main/res/xml/network_security_config.xml": "<network-security-config />\n",
    "app/src/debug/res/values/strings.xml": "<resources><string name=\"debug_only\">Debug</string></resources>\n",
}


class UnusedResourcesTest(unittest.TestCase):

    def test_reports_what_nothing_reaches_and_follows_references_between_values(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for relative, text in FILES.items():
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8")
            issues = _unused_resources(root, Project(root))
        self.assertEqual([
            "UNUSED-RES app/src/main/res/drawable-night/ic_unused.xml: drawable/ic_unused",
            # Only the parent array names it, and nothing names the parent.
            "UNUSED-RES app/src/main/res/values/arrays.xml: array/certs",
            "UNUSED-RES app/src/main/res/values/arrays.xml: array/certs_dev",
            # Named by the translation of a string nothing uses.
            "UNUSED-RES app/src/main/res/values/arrays.xml: array/translated_reference_only",
            "UNUSED-RES app/src/main/res/values/strings.xml: string/only_in_a_comment",
            "UNUSED-RES app/src/main/res/values/strings.xml: string/unused",
            "UNUSED-RES app/src/main/res/values/themes.xml: color/orphan",
        ], issues)


class IssuePathTest(unittest.TestCase):

    def test_issue_lines_name_files_with_forward_slashes_on_every_host(self):
        # known_issues.toml is matched against these lines, and the runner swaps the package's path
        # in them for {pkg}: both are written with "/".
        files = {
            "app/src/main/AndroidManifest.xml": """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application><activity android:name="x.Missing" /></application>
</manifest>
""",
            "app/src/main/res/values/broken.xml": "<resources>\n",
            "app/src/main/res/values/strings.xml": '<resources><string name="a">A</string></resources>\n',
            "app/src/main/res/values-fr/strings.xml": "<resources />\n",
            "app/src/main/res/xml/backup.xml": '<full-backup-content label="@string/missing" />\n',
            "app/src/main/res/raw/empty.txt": "",
        }
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for relative, text in files.items():
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8")
            project = Project(root)
            issues = _manifests(root, project, "x") + _resources(root, project) + _translations(root)
            issues += _hollow(root, project)
        self.assertEqual([
            "MANIFEST app/src/main/AndroidManifest.xml",
            "XML app/src/main/res/values/broken.xml",
            "RES app/src/main/res/xml/backup.xml",
            "TRANSLATION app/src/main/res/values-fr/strings.xml",
            "EMPTY-FILE app/src/main/res/raw/empty.txt",
        ], [issue.split(":", 1)[0] for issue in issues])


class RunnerTest(unittest.TestCase):

    def test_the_pool_is_handed_a_function_its_workers_can_import(self):
        # `python -m tests.consistency` runs __main__.py as "__main__", a module that workers started
        # with spawn or forkserver never import, so a function defined there cannot be unpickled in one.
        self.assertNotEqual(runner.__name__, runner.run_one.__module__)


if __name__ == "__main__":
    unittest.main()
