"""The tabs a spec gives the app: their order, icons, start destination and the More warning."""

from __future__ import annotations

import unittest

from genkit.core import pipeline
from genkit.targets import get
from genkit.targets.android import tabs

ANDROID = get("android")


def spec(**fields):
    raw = {"schema": 2, "platform": "android", "app_name": "Acme Field", "package_name": "com.acme.field", **fields}
    return pipeline.read_spec(raw, ANDROID)[0]


class OrderTest(unittest.TestCase):

    def test_home_then_modules_then_the_template_tabs_with_settings_last(self):
        parsed = spec(features=["sample", "paging", "search", "profile", "settings"], feature_modules=["orders"])
        self.assertEqual(["Home", "Orders", "Feed", "Search", "Profile", "Settings"], tabs.labels(parsed.spec))

    def test_each_module_tab_is_labelled_by_its_own_title_string(self):
        regions = tabs.regions(spec(feature_modules=["orders", "order_history"]).spec)
        self.assertIn("key = OrdersListKey", regions["shell-tabs"][0])
        # Translating the module translates its tab.
        self.assertIn("label = OrderHistoryR.string.order_history_title", regions["shell-tabs"][1])

    def test_the_reference_feature_supplies_its_own_start(self):
        self.assertEqual([], tabs.regions(spec(features=["sample"], feature_modules=["orders"]).spec)["start-destination"])

    def test_without_home_the_first_module_starts_and_its_key_comes_from_its_api(self):
        regions = tabs.regions(spec(features=["paging"], feature_modules=["orders", "wallet"]).spec)
        self.assertEqual(["        ?: OrdersListKey\n"], regions["start-destination"])
        self.assertEqual(
            [
                "import com.acme.field.feature.orders.api.OrdersListKey\n",
                "import com.acme.field.feature.orders.R as OrdersR\n",
                "import com.acme.field.feature.wallet.api.WalletListKey\n",
                "import com.acme.field.feature.wallet.R as WalletR\n",
            ],
            regions["start-destination-import"],
        )

    def test_without_modules_the_first_template_tab_starts(self):
        for features, key in ((["paging", "search"], "FeedKey"), (["search", "settings"], "SearchKey"), (["settings"], "SettingsKey")):
            with self.subTest(features=features):
                self.assertEqual([f"        ?: {key}\n"], tabs.regions(spec(features=features).spec)["start-destination"])

    def test_with_no_tab_source_a_home_module_is_the_tab_and_the_start(self):
        parsed = spec(features=["network"])
        regions = tabs.regions(parsed.spec)
        self.assertEqual(["Home"], tabs.labels(parsed.spec))
        self.assertEqual(["        ?: HomeListKey\n"], regions["start-destination"])
        self.assertTrue(any("home" in warning for warning in ANDROID.validate(parsed.spec)))


class MoreTest(unittest.TestCase):

    def test_five_tabs_fit_the_bar_without_a_warning(self):
        parsed = spec(features=["sample", "search", "profile", "settings"], feature_modules=["orders"])
        self.assertEqual([], tabs.warnings(parsed.spec))

    def test_past_five_the_warning_names_what_moves_under_more(self):
        parsed = spec(features=["sample", "paging", "search", "profile", "settings"], feature_modules=["orders", "wallet"])
        (warning,) = tabs.warnings(parsed.spec)
        self.assertIn("7 tabs", warning)
        self.assertIn("Home, Orders, Wallet, Feed and More", warning)
        self.assertIn("holds Search, Profile, Settings", warning)
        self.assertIn("rail shows them all", warning)
        # The headless JSON and --dry-run carry the target's warnings.
        self.assertIn(warning, pipeline.plan(parsed, ANDROID, ANDROID.validate(parsed.spec))["warnings"])

    def test_past_seven_the_rail_has_a_more_tab_too(self):
        parsed = spec(
            features=["sample", "paging", "search", "profile", "settings"], feature_modules=["orders", "wallet", "trips"]
        )
        (warning,) = tabs.warnings(parsed.spec)
        self.assertIn("8 tabs", warning)
        self.assertIn("rail shows 6 and More, and a large window's sidebar shows them all", warning)


class IconTest(unittest.TestCase):

    GLYPHS = frozenset({
        "Home", "ListView", "Search", "User", "Settings", "Grid", "Tag", "Bookmark", "Star", "Folder", "Package",
        "PackageFilled", "CreditCard", "MapPin", "Message", "MessageFilled", "Globe", "Heart", "Calendar",
    })

    def test_a_name_picks_its_glyph_and_a_filled_selected_form_when_there_is_one(self):
        chosen = tabs.icons(("orders", "wallet", "team_chat"), set(), self.GLYPHS)
        self.assertEqual(("Package", "PackageFilled"), chosen["orders"])
        self.assertEqual(("CreditCard", "CreditCard"), chosen["wallet"])
        self.assertEqual(("Message", "MessageFilled"), chosen["team_chat"])

    def test_no_two_tabs_share_a_glyph(self):
        names = ("orders", "parcels", "reports", "stats", "notes", "misc")
        chosen = tabs.icons(names, {"Home", "Search", "Grid"}, self.GLYPHS)
        glyphs = [icon for icon, _ in chosen.values()]
        self.assertEqual(len(glyphs), len(set(glyphs)))
        self.assertFalse({"Home", "Search", "Grid"} & set(glyphs))

    def test_the_generated_tab_names_its_glyphs(self):
        regions = tabs.regions(spec(features=["sample"], feature_modules=["orders", "wallet"]).spec)
        self.assertIn("icon = AppIcons.Package", regions["shell-tabs"][0])
        self.assertIn("selectedIcon = AppIcons.", regions["shell-tabs"][0])
        self.assertIn("icon = AppIcons.CreditCard", regions["shell-tabs"][1])

    def test_the_glyphs_come_from_the_design_system_the_template_ships(self):
        self.assertTrue({"Home", "Grid", "Package", "CreditCard"} <= tabs.available_glyphs())



class NoUiComponentsTest(unittest.TestCase):
    """A project without UI components has no shell: no tabs, no warning, nothing in the tab regions."""

    def test_no_tabs_and_no_warning(self):
        noui = spec(features=["sample", "settings", "search", "profile", "paging"], feature_modules=["a", "b", "c"], ui_components=False).spec
        self.assertEqual([], tabs.labels(noui))
        self.assertEqual([], tabs.warnings(noui))
        self.assertEqual({"shell-tabs": [], "start-destination-import": [], "start-destination": []}, tabs.regions(noui))

if __name__ == "__main__":
    unittest.main()
