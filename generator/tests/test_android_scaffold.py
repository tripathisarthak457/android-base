"""Scaffolded feature modules: the STANDARDS-PLAN 2.1 shape, on the M1 APIs, ready for the code rules."""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get
from genkit.targets.android import scaffold

ANDROID = get("android")
PKG = "com/acme/field"


def spec(**fields):
    raw = {"schema": 2, "platform": "android", "app_name": "Acme Field", "package_name": "com.acme.field", **fields}
    return pipeline.read_spec(raw, ANDROID)[0].spec


class RegionsTest(unittest.TestCase):

    def test_each_module_contributes_its_includes_and_an_app_dependency(self):
        regions = scaffold.regions(spec(feature_modules=["orders", "wallet"]))
        self.assertEqual(['include(":data:orders")\n', 'include(":data:wallet")\n'], regions["data-modules"])
        self.assertEqual(['include(":feature:orders")\n', 'include(":feature:wallet")\n'], regions["feature-modules"])
        self.assertEqual(
            ['    implementation(project(":feature:orders"))\n', '    implementation(project(":feature:wallet"))\n'],
            regions["app-feature-dependencies"],
        )

    def test_home_is_added_only_when_nothing_else_gives_a_tab(self):
        self.assertEqual(("home",), scaffold.modules(spec(features=["network"])))
        self.assertEqual((), scaffold.modules(spec(features=["settings"])))
        self.assertEqual(("orders",), scaffold.modules(spec(feature_modules=["orders"])))
        self.assertEqual([], scaffold.notes(spec(feature_modules=["orders"])))


class GeneratedModuleTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.online = Path(cls.temp.name) / "online"
        cls.offline = Path(cls.temp.name) / "offline"
        scaffold.write_feature_modules(cls.online, spec(features=["network", "sample"], feature_modules=["orders"]))
        scaffold.write_feature_modules(cls.offline, spec(features=["settings"], feature_modules=["orders"]))

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_the_feature_has_api_navigation_list_and_detail_screens_and_tests(self):
        feature = self.online / "feature/orders/src"
        expected = [
            f"main/kotlin/{PKG}/feature/orders/api/OrdersKeys.kt",
            f"main/kotlin/{PKG}/feature/orders/navigation/OrdersNavigation.kt",
            *(f"main/kotlin/{PKG}/feature/orders/{screen}/Orders{Screen}{part}.kt"
              for screen, Screen in (("list", "List"), ("detail", "Detail"))
              for part in ("Contract", "ViewModel", "Route", "Screen")),
            "main/res/values/strings.xml",
            f"test/kotlin/{PKG}/feature/orders/list/OrdersListViewModelTest.kt",
            f"test/kotlin/{PKG}/feature/orders/OrdersRecompositionTest.kt",
        ]
        for relative in expected:
            with self.subTest(file=relative):
                self.assertTrue((feature / relative).is_file())

    def test_the_data_module_decodes_a_real_answer_when_it_has_a_network(self):
        data = self.online / "data/orders/src"
        self.assertTrue((data / f"main/kotlin/{PKG}/data/orders/remote/OrdersDto.kt").is_file())
        self.assertTrue((data / f"test/kotlin/{PKG}/data/orders/remote/OrdersDtoTest.kt").is_file())
        self.assertTrue((data / "test/resources/orders.json").is_file())
        self.assertIn("NetworkClient", (data / f"main/kotlin/{PKG}/data/orders/DefaultOrdersRepository.kt").read_text())

    def test_the_fake_repository_is_shared_through_the_data_modules_test_fixtures(self):
        for project in (self.online, self.offline):
            with self.subTest(project=project.name):
                fake = project / f"data/orders/src/testFixtures/kotlin/{PKG}/data/orders/api/FakeOrdersRepository.kt"
                self.assertIn("class FakeOrdersRepository(", fake.read_text())
                self.assertIn(
                    'testImplementation(testFixtures(project(":data:orders")))',
                    (project / "feature/orders/build.gradle.kts").read_text(),
                )
                test = project / f"feature/orders/src/test/kotlin/{PKG}/feature/orders/list/OrdersListViewModelTest.kt"
                self.assertIn("import com.acme.field.data.orders.api.FakeOrdersRepository", test.read_text())
                self.assertNotIn("class FakeOrdersRepository", test.read_text())

    def test_without_a_network_the_repository_has_no_remote_and_says_so(self):
        data = self.offline / "data/orders"
        self.assertFalse((data / f"src/main/kotlin/{PKG}/data/orders/remote").exists())
        self.assertNotIn(":core:network", (data / "build.gradle.kts").read_text())
        self.assertIn("no network", (data / f"src/main/kotlin/{PKG}/data/orders/DefaultOrdersRepository.kt").read_text())

    def test_the_keys_and_models_are_the_public_surface_and_everything_else_is_internal(self):
        for path in self.online.rglob("src/main/kotlin/**/*.kt"):
            text = path.read_text(encoding="utf-8")
            public = re.findall(r"^(?:data |sealed |abstract )?(?:class|interface|object|fun) (\w+)", text, re.MULTILINE)
            with self.subTest(file=path.name):
                if "/api/" not in path.as_posix():
                    self.assertEqual([], public)

    def test_every_file_is_filled_in_its_package_and_sorted(self):
        for path in [*self.online.rglob("*.kt"), *self.offline.rglob("*.kt")]:
            text = path.read_text(encoding="utf-8")
            with self.subTest(file=path.name):
                self.assertIsNone(re.search(r"__[A-Z_]+__", text))
                package = re.search(r"^package ([\w.]+)$", text, re.MULTILINE).group(1)
                self.assertTrue(path.parent.as_posix().endswith(package.replace(".", "/")))
                imports = re.findall(r"^import ([\w.]+)$", text, re.MULTILINE)
                self.assertEqual(imports, sorted(imports))

    def test_the_screens_use_the_mvi_and_navigation_apis_of_m1(self):
        list_screen = (self.online / f"feature/orders/src/main/kotlin/{PKG}/feature/orders/list/OrdersListViewModel.kt").read_text()
        self.assertIn("import com.acme.field.core.mvi.MviViewModel", list_screen)
        self.assertIn("override fun handleEvent", list_screen)
        self.assertIn("launchLatest(", list_screen)
        navigation = (self.online / f"feature/orders/src/main/kotlin/{PKG}/feature/orders/navigation/OrdersNavigation.kt").read_text()
        self.assertIn("entry<OrdersListKey>(pane = NavPane.List)", navigation)
        self.assertIn("entry<OrdersDetailKey>(pane = NavPane.Detail)", navigation)

    def test_scrolling_content_clears_the_chrome_and_placeholders_take_the_rows_height(self):
        screens = self.online / f"feature/orders/src/main/kotlin/{PKG}/feature/orders"
        list_screen = (screens / "list/OrdersListScreen.kt").read_text()
        detail_screen = (screens / "detail/OrdersDetailScreen.kt").read_text()
        # The content list and the loading list both, so the rows land where the placeholders were.
        self.assertEqual(1, list_screen.count("contentPadding = chromeContentPadding("))
        self.assertEqual(1, list_screen.count(".padding(chromeContentPadding("))
        self.assertIn(".padding(chromeContentPadding(", detail_screen)
        # Each bone is sized by a transparent line in the style of the text that replaces it.
        self.assertEqual(2, list_screen.count("style = AppTheme.typography.headline"))
        self.assertEqual(2, detail_screen.count("style = AppTheme.typography.title2"))
        for screen in (list_screen, detail_screen):
            self.assertIn("color = Color.Transparent", screen)
            self.assertIn(
                "AppSkeleton(modifier = Modifier.matchParentSize(), shape = AppTheme.shapes.xs)", screen
            )
        self.assertNotIn("AppSkeletonListItem", list_screen)

    def test_the_screens_follow_the_compose_performance_audit(self):
        screens = self.online / f"feature/orders/src/main/kotlin/{PKG}/feature/orders"
        list_screen = (screens / "list/OrdersListScreen.kt").read_text()
        loading = list_screen[list_screen.index("private fun LoadingList()"):]
        loading = loading[:loading.index("\n}\n")]
        # Six fixed rows that never scroll: no lazy list, one node for TalkBack.
        self.assertNotIn("LazyColumn", loading)
        self.assertIn("repeat(SKELETON_ROWS)", loading)
        self.assertIn("clearAndSetSemantics { contentDescription = loading }", loading)
        # Every lazy list names its items' content type, as well as their key.
        self.assertIn("contentType = { ROW }", list_screen)
        for route in ("list/OrdersListRoute.kt", "detail/OrdersDetailRoute.kt"):
            with self.subTest(route=route):
                text = (screens / route).read_text()
                # Sending an event suspends nothing, so it takes no coroutine.
                self.assertIn("SideEffect(Unit) { viewModel.onEvent(", text)
                self.assertNotIn("LaunchedEffect", text)

    def test_the_code_fits_the_formatting_features_line_length_at_every_module_name_length(self):
        letters = "abcdefghijklmnopqrstuvwxyzabcd"
        names = [letters[:length] for length in range(1, len(letters) + 1)]
        with tempfile.TemporaryDirectory() as temp:
            for features in (["network", "sample"], ["settings"]):
                # A spec scaffolds at most 12 modules.
                for start in range(0, len(names), 10):
                    project = Path(temp) / f"{features[0]}-{start}"
                    parsed = spec(features=features, feature_modules=names[start:start + 10])
                    scaffold.write_feature_modules(project, parsed)
                    for path in [*project.rglob("*.kt"), *project.rglob("*.kts")]:
                        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                            if line.startswith(("import ", "package ")):
                                continue
                            with self.subTest(file=path.name, line=number):
                                self.assertLessEqual(len(line), scaffold.MAX_LINE_LENGTH, line)


class FitTest(unittest.TestCase):

    def block(self, *layouts: str) -> str:
        return "__FIT__\n" + "__ELSE__\n".join(layouts) + "__END_FIT__\n"

    def test_the_first_layout_whose_lines_all_fit_is_kept(self):
        wide = "x" * (scaffold.MAX_LINE_LENGTH + 1)
        text = "before\n" + self.block(f"{wide}\n", "one\nline\n", "two\n") + "after\n"
        self.assertEqual("before\none\nline\nafter\n", scaffold._fit(text, "S.kt"))

    def test_a_line_of_exactly_the_limit_fits(self):
        exact = "x" * scaffold.MAX_LINE_LENGTH
        self.assertEqual(f"{exact}\n", scaffold._fit(self.block(f"{exact}\n", "short\n"), "S.kt"))

    def test_when_nothing_fits_the_last_layout_is_kept(self):
        wide = "x" * (scaffold.MAX_LINE_LENGTH + 1)
        self.assertEqual(f"{wide}b\n", scaffold._fit(self.block(f"{wide}a\n", f"{wide}b\n"), "S.kt"))

    def test_unbalanced_markers_are_refused(self):
        for text in ("__FIT__\na\n", "a\n__END_FIT__\n", "__ELSE__\n", "__FIT__\n__FIT__\na\n__END_FIT__\n"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                scaffold._fit(text, "S.kt")


class GeneratedProjectTest(unittest.TestCase):

    def test_screens_without_the_preview_annotation_join_the_code_rule_baseline(self):
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp) / "p"
            pipeline.build(spec(features=["network"], feature_modules=["orders"]), ANDROID, project, zip_output=False, commit="t")
            baseline = (project / "config/code-rules-baseline.txt").read_text(encoding="utf-8")
            screens = sorted(project.glob("feature/orders/src/main/kotlin/**/*Screen.kt"))
            self.assertEqual(2, len(screens))
            annotated = all("@AppScreenPreviews" in path.read_text(encoding="utf-8") for path in screens)
            for path in screens:
                entry = f"{path.relative_to(project).as_posix()} adaptive-preview 1"
                self.assertEqual(not annotated, entry in baseline, entry)

    def test_a_modular_project_translates_the_modules_it_split(self):
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp) / "p"
            parsed = spec(architecture="modular", features=["network"], feature_modules=["orders"], languages=["de"])
            pipeline.build(parsed, ANDROID, project, zip_output=False, commit="t")
            self.assertTrue((project / "feature/orders/impl/src/main/res/values-de/strings.xml").is_file())


if __name__ == "__main__":
    unittest.main()
