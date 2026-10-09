"""The modular split: a pure function of a compact project's tree."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from genkit.targets.android import modes

PKG = "com/acme/field"


def write(root: Path, relative: str, text: str) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def compact_project(root: Path) -> Path:
    """Two features and two data domains; trips opens an orders screen through a shared key."""
    write(root, "app/build.gradle.kts", """plugins {
    id("com.acme.field.android.application")
}

android {
    namespace = "com.acme.field"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":feature:orders"))
    implementation(project(":feature:trips"))
    implementation(libs.androidx.activity.compose)
}
""")
    write(root, "settings.gradle.kts", """include(":app")
include(":core:navigation")
include(":data:orders")
include(":data:trips")
include(":feature:orders")
include(":feature:trips")
""")
    for name in ("orders", "trips"):
        write(root, f"data/{name}/build.gradle.kts", f"""plugins {{
    id("com.acme.field.android.data")
}}

android {{
    namespace = "com.acme.field.data.{name}"
}}

dependencies {{
    implementation(project(":core:network"))
}}
""")
        write(root, f"data/{name}/src/main/kotlin/{PKG}/data/{name}/api/Repository.kt",
              f"package com.acme.field.data.{name}.api\n\ninterface Repository\n")
        write(root, f"data/{name}/src/main/kotlin/{PKG}/data/{name}/DefaultRepository.kt",
              f"package com.acme.field.data.{name}\n\ninternal class DefaultRepository\n")
        write(root, f"feature/{name}/build.gradle.kts", f"""plugins {{
    id("com.acme.field.android.feature")
}}

android {{
    namespace = "com.acme.field.feature.{name}"
}}

dependencies {{
    implementation(projects.data.{name})
}}
""")
        write(root, f"feature/{name}/src/main/kotlin/{PKG}/feature/{name}/api/Keys.kt",
              f"package com.acme.field.feature.{name}.api\n\nimport kotlinx.serialization.Serializable\n")
        write(root, f"feature/{name}/src/main/res/values/strings.xml", "<resources />\n")
    write(root, "data/orders/build.gradle.kts", (root / "data/orders/build.gradle.kts").read_text().replace(
        'implementation(project(":core:network"))', 'implementation(project(":core:network"))\n    api(libs.androidx.paging.common)'))
    write(root, f"core/navigation/src/main/kotlin/{PKG}/feature/orders/api/OrderDetailKey.kt",
          "package com.acme.field.feature.orders.api\n\ndata class OrderDetailKey(val id: String)\n")
    write(root, f"feature/trips/src/main/kotlin/{PKG}/feature/trips/TripScreen.kt",
          "package com.acme.field.feature.trips\n\nimport com.acme.field.feature.orders.api.OrderDetailKey\n")
    return root


class SplitTest(unittest.TestCase):

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.project = compact_project(Path(self.directory.name))
        self.notes = modes.split(self.project)

    def tearDown(self):
        self.directory.cleanup()

    def text(self, relative: str) -> str:
        return (self.project / relative).read_text(encoding="utf-8")

    def test_the_api_package_becomes_its_own_module_and_the_rest_moves_to_impl(self):
        self.assertTrue((self.project / f"feature/orders/api/src/main/kotlin/{PKG}/feature/orders/api/Keys.kt").is_file())
        self.assertTrue((self.project / f"data/orders/impl/src/main/kotlin/{PKG}/data/orders/DefaultRepository.kt").is_file())
        self.assertTrue((self.project / "feature/orders/impl/src/main/res/values/strings.xml").is_file())
        self.assertFalse((self.project / f"feature/orders/impl/src/main/kotlin/{PKG}/feature/orders/api").exists())
        self.assertFalse((self.project / "feature/orders/build.gradle.kts").exists())

    def test_sources_keep_their_text(self):
        self.assertEqual(
            "package com.acme.field.feature.trips\n\nimport com.acme.field.feature.orders.api.OrderDetailKey\n",
            self.text(f"feature/trips/impl/src/main/kotlin/{PKG}/feature/trips/TripScreen.kt"),
        )

    def test_api_build_files_are_pure_kotlin_and_carry_the_public_libraries(self):
        feature_api = self.text("feature/orders/api/build.gradle.kts")
        self.assertIn('id("com.acme.field.jvm.library")', feature_api)
        self.assertIn("api(libs.kotlinx.serialization.core)", feature_api)
        data_api = self.text("data/orders/api/build.gradle.kts")
        self.assertIn('api(project(":core:common"))', data_api)
        self.assertIn("api(libs.androidx.paging.common)", data_api)
        self.assertNotIn("paging", self.text("data/trips/api/build.gradle.kts"))

    def test_impl_exposes_its_api_and_sees_other_modules_through_theirs(self):
        impl = self.text("feature/trips/impl/build.gradle.kts")
        self.assertIn('api(project(":feature:trips:api"))', impl)
        self.assertIn("implementation(projects.data.trips.api)", impl)
        self.assertIn('api(project(":data:orders:api"))', self.text("data/orders/impl/build.gradle.kts"))

    def test_a_shared_key_moves_to_its_owner_and_the_importer_depends_on_it(self):
        moved = self.project / f"feature/orders/api/src/main/kotlin/{PKG}/feature/orders/api/OrderDetailKey.kt"
        self.assertTrue(moved.is_file())
        self.assertFalse((self.project / f"core/navigation/src/main/kotlin/{PKG}/feature").exists())
        self.assertIn('implementation(project(":feature:orders:api"))', self.text("feature/trips/impl/build.gradle.kts"))
        self.assertNotIn(":feature:trips:api", self.text("feature/orders/impl/build.gradle.kts"))
        self.assertIn("orders", " ".join(self.notes))

    def test_the_app_wires_every_impl_and_settings_include_both_halves(self):
        app = self.text("app/build.gradle.kts")
        for path in (":feature:orders:impl", ":feature:trips:impl", ":data:orders:impl", ":data:trips:impl"):
            self.assertIn(f'implementation(project("{path}"))', app)
        self.assertIn('implementation(project(":core:ui"))', app)
        settings = self.text("settings.gradle.kts")
        for path in (":data:orders:api", ":data:orders:impl", ":feature:trips:api", ":feature:trips:impl"):
            self.assertIn(f'include("{path}")', settings)
        self.assertNotIn('include(":feature:orders")', settings)

    def test_a_split_project_is_refused_a_second_time(self):
        with self.assertRaises(modes.SplitError):
            modes.split(self.project)


class RefusalTest(unittest.TestCase):

    def test_a_shared_key_without_a_feature_module_is_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            project = compact_project(Path(directory))
            write(project, f"core/navigation/src/main/kotlin/{PKG}/feature/billing/api/BillingKey.kt", "package x\n")
            with self.assertRaisesRegex(modes.SplitError, "billing"):
                modes.split(project)

    def test_a_directory_without_an_app_namespace_is_not_a_project(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(modes.SplitError):
                modes.split(Path(directory))


if __name__ == "__main__":
    unittest.main()
