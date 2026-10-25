"""
The consistency matrix's checks on hand-made modules, where every answer is known, and its runner's
hand-off to worker processes and clean-up.
"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tests.consistency import __main__ as runner
from tests.consistency import worker
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


class VendoredFilesTest(unittest.TestCase):
    """Google's skills ship Kotlin, Gradle and resource samples of their own: none of it is the project's."""

    FILES = {
        "app/src/main/kotlin/x/Screen.kt": "package x\nfun title() = R.string.used\n",
        "app/build.gradle.kts": "plugins { }\n",
        "app/src/main/res/values/strings.xml": '<resources><string name="used">Used</string></resources>\n',
        ".agents/skills/glance/references/Widget.kt": "package com.upstream\nimport nowhere.Missing\nfun widget() = 1\n",
        ".agents/skills/glance/references/build.gradle.kts": "plugins { }\n",
        ".agents/skills/glance/references/res/xml/widget_info.xml": '<appwidget-provider previewImage="@drawable/sample_preview" />\n',
        ".agents/skills/glance/references/src/main/res/values/strings.xml": '<resources><string name="sample">S</string></resources>\n',
        ".agents/skills/glance/references/src/main/res/values-fr/strings.xml": "<resources />\n",
        ".agents/skills/glance/references/empty.md": "",
    }

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        for relative, text in self.FILES.items():
            path = self.root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")

    def test_they_are_not_read_as_the_projects_sources(self):
        project = Project(self.root)
        self.assertEqual(["app/src/main/kotlin/x/Screen.kt"], [project.relative(path) for path in project.kotlin])
        self.assertEqual(["app/build.gradle.kts"], [project.relative(path) for path in project.gradle])
        self.assertNotIn("com.upstream", project.declared)

    def test_no_check_reports_them(self):
        project = Project(self.root)
        issues = _resources(self.root, project) + _translations(self.root) + _hollow(self.root, project)
        self.assertEqual([], [issue for issue in issues if ".agents" in issue])


class RunnerTest(unittest.TestCase):

    def test_the_pool_is_handed_a_function_its_workers_can_import(self):
        # `python -m tests.consistency` runs __main__.py as "__main__", a module that workers started
        # with spawn or forkserver never import, so a function defined there cannot be unpickled in one.
        self.assertNotEqual(runner.__name__, runner.run_one.__module__)

    def test_the_quick_run_adds_a_component_with_and_without_ui_components(self):
        quick = runner.combinations(quick=True)
        added = {name: raw for name, raw in quick.items() if runner.COMPONENTS.get(name)}
        self.assertEqual({True, False}, {raw.get("ui_components", True) for raw in added.values()})

    def test_a_component_add_component_refuses_is_an_issue_of_its_run(self):
        raw = {"schema": 2, "platform": "android", "app_name": "Consistency", "package_name": worker.PACKAGE}
        with tempfile.TemporaryDirectory() as temp:
            name, issues = worker.run_one("refused", raw, temp, keep=False, components=("Button",))
        self.assertEqual("refused", name)
        self.assertEqual(1, len([issue for issue in issues if issue.startswith("ADD_COMPONENT Button")]), issues)

    def test_a_component_added_without_ui_components_is_the_developers_not_the_options(self):
        component = "core/designsystem/src/main/kotlin/x/core/designsystem/component"
        added = frozenset({f"{component}/tag/AppTag.kt"})
        issues = [
            f"NOUI-OWNED {component} is still there",
            f"NOUI-REACH {component}/tag/AppTag.kt:1: package x.core.designsystem.component.tag",
            "NOUI-REACH feature/a/src/main/kotlin/x/A.kt:3: import x.core.designsystem.component.tag.AppTag",
            "NOUI-OWNED core/navigation/shell is still there",
        ]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / component / "tag").mkdir(parents=True)
            (root / component / "tag/AppTag.kt").write_text("package x\n", encoding="utf-8")
            (root / "core/navigation/shell").mkdir(parents=True)
            self.assertEqual(issues[2:], worker.without_added(issues, root, added))

    def test_a_failed_run_leaves_nothing_in_the_temporary_folder(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp, "consistency-run")
            directory.mkdir()
            with (
                mock.patch.object(runner, "combinations", return_value={}),
                mock.patch.object(runner.tempfile, "mkdtemp", return_value=str(directory)),
                mock.patch.object(runner, "reference", side_effect=RuntimeError("no worker started")),
                self.assertRaisesRegex(RuntimeError, "no worker started"),
            ):
                runner.main(["--quick"])
            self.assertFalse(directory.exists())


class NoUiRunsTest(unittest.TestCase):
    """The matrix covers projects without UI components in its quick run and its full one."""

    def test_the_quick_run_has_every_preset_in_both_architectures_and_the_module_extremes(self):
        runs = runner.combinations(quick=True)
        noui = {name for name in runs if name.startswith("noui-")}
        expected = {
            f"noui-preset-{preset}-{architecture}"
            for preset in ("lean", "standard", "everything")
            for architecture in ("compact", "modular")
        } | {"noui-everything-modules", "noui-none-modules", "noui-component-everything"}
        self.assertEqual(expected, noui)
        self.assertTrue(all(runs[name]["ui_components"] is False for name in noui))
        self.assertTrue(all("ui_components" not in raw for name, raw in runs.items() if name not in noui))

    def test_the_full_run_adds_each_feature_alone_seeded_subsets_and_hostile_names(self):
        runs = runner.combinations(quick=False)
        disabled = set(runner.get("android").manifest.options["ui_components"].disables)
        alone = {name.removeprefix("noui-only-") for name in runs if name.startswith("noui-only-")}
        self.assertFalse(alone & disabled)
        self.assertEqual({feature.key for feature in runner.get("android").manifest.selectable} - disabled, alone)
        self.assertEqual(10, sum(name.startswith("noui-random-") for name in runs))
        self.assertEqual(3, sum(name.startswith("noui-hostile-") for name in runs))


class NoUiChecksTest(unittest.TestCase):
    """What the matrix reports about a project without UI components."""

    def test_reaching_the_components_or_the_shell_is_reported(self):
        from tests.consistency.checks import NO_UI_FORBIDDEN

        for line in (
            "import com.acme.core.designsystem.component.button.AppButton",
            "import com.acme.core.designsystem.foundation.interaction.pressable",
            "val state: ShellState = rememberShellState()",
            "StackHost(stack, registry)",
        ):
            self.assertTrue(NO_UI_FORBIDDEN.search(line), line)
        for line in (
            "import com.acme.core.designsystem.theme.AppTheme",
            "import com.acme.core.designsystem.foundation.adaptive.SizeClass",
            "import com.acme.core.devtools.kit.AppText",
        ):
            self.assertFalse(NO_UI_FORBIDDEN.search(line), line)


if __name__ == "__main__":
    unittest.main()
