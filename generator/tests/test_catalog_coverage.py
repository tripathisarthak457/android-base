"""
The catalog against the design system it shows: every public component and every token on a page.

    py -m unittest tests.test_catalog_coverage

A hand-kept catalog falls behind as soon as someone adds a component without a page for it, and
the projects generated from this template showed exactly that. These tests name what is missing.
"""

from __future__ import annotations

import re
import unittest
from pathlib import Path

from genkit.targets.android.target import TEMPLATE_DIR

DESIGN_SYSTEM = TEMPLATE_DIR / "core" / "designsystem" / "src" / "main" / "kotlin"
CATALOG = TEMPLATE_DIR / "catalog" / "src" / "main" / "kotlin"

# `@Composable`, any further annotations, then a top-level `fun` whose name starts with `App`,
# optionally generic and optionally an extension (`fun BoxScope.AppX(`).
COMPOSABLE = re.compile(
    r"@Composable\s+(?:@[\w.]+(?:\([^)]*\))?\s+)*"
    r"(?P<visibility>(?:public|internal|private)\s+)?"
    r"fun\s+(?:<[^>]+>\s+)?(?:[\w.]+(?:<[^>]*>)?\.)?(?P<name>App\w+)\s*\("
)

# Each token class, and the names the catalog reads it through (`AppTheme.colors` bound to `colors`).
TOKENS = {
    "AppColors": ("theme/AppColors.kt", ("colors",)),
    "AppTypography": ("theme/AppTypography.kt", ("typography", "type")),
    "AppSpacing": ("theme/AppMetrics.kt", ("spacing",)),
    "AppShapes": ("theme/AppMetrics.kt", ("shapes",)),
    "AppSizes": ("theme/AppMetrics.kt", ("sizes",)),
    "AppElevation": ("theme/AppMetrics.kt", ("elevation",)),
}


def kotlin_sources(root: Path) -> dict[Path, str]:
    return {path: path.read_text(encoding="utf-8") for path in sorted(root.rglob("*.kt"))}


def not_for_apps(source: str, start: int) -> bool:
    """Whether the declaration starting at [start] is deprecated or a shell seam: a catalog shows what an app uses."""
    preamble = source[max(source.rfind("\n\n", 0, start), source.rfind("}\n", 0, start)) + 1:start]
    return "@Deprecated(" in preamble or "@ShellSeamApi" in preamble


def public_components() -> dict[str, Path]:
    """Every public, current `@Composable fun App…` an app calls, with the file that declares it."""
    found: dict[str, Path] = {}
    for path, source in kotlin_sources(DESIGN_SYSTEM).items():
        for match in COMPOSABLE.finditer(source):
            if match.group("visibility") and match.group("visibility").strip() != "public":
                continue
            if not_for_apps(source, match.start()):
                continue
            found.setdefault(match.group("name"), path)
    return found


def token_fields(class_name: str, relative: str) -> list[str]:
    """The token properties of a token data class, in declaration order: flags such as `isLight` are not tokens."""
    source = (DESIGN_SYSTEM / "com" / "base" / "app" / "core" / "designsystem" / relative).read_text(encoding="utf-8")
    header = re.search(rf"\bclass\s+{class_name}\s*\(", source)
    if header is None:
        raise AssertionError(f"{class_name} is not declared in {relative}")
    depth, start = 1, header.end()
    end = start
    while depth:
        char = source[end]
        depth += {"(": 1, ")": -1}.get(char, 0)
        end += 1
    fields = re.findall(r"^\s*val\s+(\w+)\s*:\s*([\w.]+)", source[start:end - 1], flags=re.MULTILINE)
    return [name for name, kind in fields if kind != "Boolean"]


class CatalogCoverageTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.catalog = "\n".join(kotlin_sources(CATALOG).values())

    def test_the_patterns_still_find_the_design_system(self):
        # Guards the guard: a rename that left these finding nothing would pass every test below.
        self.assertGreater(len(public_components()), 40)
        for class_name, (relative, _) in TOKENS.items():
            self.assertTrue(token_fields(class_name, relative), class_name)

    def test_every_public_component_is_shown_in_the_catalog(self):
        missing = sorted(
            f"{name} ({path.relative_to(TEMPLATE_DIR).as_posix()})"
            for name, path in public_components().items()
            if not re.search(rf"\b{name}\s*[({{]", self.catalog)
        )
        self.assertEqual([], missing, "show these on a catalog page, in the section their package belongs to")

    def test_every_token_is_shown_in_the_catalog(self):
        for class_name, (relative, accessors) in TOKENS.items():
            readers = "|".join(accessors)
            missing = [
                field for field in token_fields(class_name, relative)
                if not re.search(rf"\b(?:{readers})\.{field}\b", self.catalog)
            ]
            self.assertEqual([], missing, f"{class_name} fields no catalog page shows (add them to the Foundations section)")


if __name__ == "__main__":
    unittest.main()
