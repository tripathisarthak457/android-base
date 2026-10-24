"""
The performance guide and the agent rules every generated Android project carries
(`docs/PERFORMANCE.md` and the "Smooth and light" section of `AGENTS.md`): each rule id, lint id,
gate row, perf scenario, spring, path, name and link they cite exists in the project they ship in,
whatever features it was generated with. Every code name the template's other pages cite exists too.
The other way round: a guide the project's code sends a reader to, such as the build's message when a
code rule fails, is in the project.
"""

from __future__ import annotations

import re
import tempfile
import tomllib
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get

ANDROID = get("android")
TEMPLATE = ANDROID.template_dir
GUIDE = "docs/PERFORMANCE.md"
BUILD_LOGIC = TEMPLATE / "build-logic/convention/src/main/kotlin"
CODE_SUFFIXES = (".kt", ".kts", ".xml")
#: Top-level folders of a project: a backticked token starting with one is a path that must exist.
PROJECT_DIRS = ("app", "benchmark", "build-logic", "catalog", "config", "core", "data", "docs", "feature", "gradle", "tools")
#: Words the overhaul's own notes use and a project's docs never should, and the `{…}` a measurement fills in.
WORKSPACE_WORDS = re.compile(r"overhaul|staging-wp|\bWP\d\d\b|REQUIREMENTS", re.IGNORECASE)
PLACEHOLDER = re.compile(r"\{[a-z][a-z ]*\}")
#: Platform and library names the docs cite that the template's code never spells out.
LIBRARY_NAMES = frozenset({
    # Compose, and the collections the stability config makes unnecessary.
    "Crossfade", "Hyphens", "LineBreak", "Simple", "LazyLayoutCacheWindow", "fitInside", "toImageBitmap",
    "computedStateOf", "ImmutableList",
    # The framework, and the slices its traces show.
    "PixelCopy", "RenderEffect", "OnBackPressedDispatcher", "onTrimMemory", "bindApplication", "performResume",
    "doFrame", "DrawFrame", "CircularRRectOp",
    # Coroutines, CameraX and the activity result contracts.
    "SharedFlow", "ImageCapture", "VideoCapture", "CaptureVideo",
})
#: Names the docs make up to show the shape of something.
EXAMPLE_NAMES = frozenset({"ListKey", "YourKey", "YourRoute", "isLoading"})
#: Names a page still cites that the code does not have. Each leaves when its paragraph is rewritten, so
#: the sets only shrink; a name misspelt or removed anywhere else fails.
STALE_NAMES = {
    "README.md": {"entryPoint"},
    "docs/ARCHITECTURE.md": {"AppIndication", "AppNavHost", "DeepLinkResolver", "Paged", "debugEnabled", "surfaceRaised"},
}


def prose(text: str) -> str:
    """`text` without its fenced code blocks."""
    return re.sub(r"^(```|~~~).*?^\1[^\n]*$", "", text, flags=re.MULTILINE | re.DOTALL)


def smooth_section(agents: str) -> str:
    match = re.search(r"^## (?:\d+\. )?Smooth and light\n(.*?)(?=^## |\Z)", agents, re.MULTILINE | re.DOTALL)
    return match.group(1) if match else ""


def backticked(text: str) -> list[str]:
    return re.findall(r"`([^`\n]+)`", text)


def enforcement(guide: str, rules: str) -> list[str]:
    """What the docs say enforces each rule: the guide's "Enforced by." paragraphs, and each AGENTS line's tail."""
    cited = re.findall(r"^\*\*Enforced by\.\*\*(.*?)(?=^\s*$)", prose(guide), re.MULTILINE | re.DOTALL)
    cited += [line.rsplit(" — ", 1)[1] for line in rules.splitlines() if line.startswith("- ") and " — " in line]
    # What follows a check in brackets says where it applies, not what it is.
    return [token for text in cited for token in backticked(re.sub(r"\([^()]*\)", "", text))]


def rule_ids() -> set[str]:
    return {
        rule for path in (BUILD_LOGIC.rglob("*.kt")) for rule in re.findall(r'\bid = "([a-z0-9-]+)"', path.read_text(encoding="utf-8"))
    }


def lint_ids() -> set[str]:
    android = BUILD_LOGIC / "com/base/app/buildlogic/android"
    return {
        lint
        for name in ("Compose.kt", "AndroidCommon.kt")
        for lint in re.findall(r'^\s*"([A-Z][A-Za-z]+)",', (android / name).read_text(encoding="utf-8"), re.MULTILINE)
    }


def gate_metrics() -> set[str]:
    with (TEMPLATE / "tools/perf/gates.toml").open("rb") as file:
        return {row["metric"] for row in tomllib.load(file)["row"]}


def scenarios() -> set[str]:
    source = (TEMPLATE / "tools/perf/perf.py").read_text(encoding="utf-8")
    block = re.search(r"^SCENARIOS = \[(.*?)\]", source, re.MULTILINE | re.DOTALL).group(1)
    return set(re.findall(r'"([a-z0-9_]+)"', block))


def standard_springs() -> dict[str, tuple[float, float]]:
    tokens = (TEMPLATE / "core/designsystem/src/main/kotlin/com/base/app/core/designsystem/theme/tokens/DesignTokens.kt")
    block = re.search(r"val standard = MotionStyle\((.*?)\n\s*\)", tokens.read_text(encoding="utf-8"), re.DOTALL).group(1)
    return {name: (float(r), float(b)) for name, r, b in re.findall(r"(\w+) = Spring\(([-\d.]+)f, ([-\d.]+)f\)", block)}


def sources(root: Path) -> list[str]:
    return [
        path.read_text(encoding="utf-8", errors="replace")
        for path in root.rglob("*")
        if path.suffix in CODE_SUFFIXES and path.is_file() and not {"build", ".agents"} & set(path.parts)
    ]


def words(root: Path) -> set[str]:
    """Every identifier in the project's code, build scripts and resources."""
    return {word for text in sources(root) for word in re.findall(r"[A-Za-z_][A-Za-z0-9_]*", text)}


def declared(root: Path) -> set[str]:
    """The names the project itself declares, Gradle tasks included: its own API, not the libraries'."""
    names: set[str] = set()
    for text in sources(root):
        names.update(re.findall(r"\b(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)", text))
        names.update(re.findall(r'\bregister(?:<[^>]*>)?\(\s*"(\w+)"', text))
        names.update(piece for pieces in re.findall(r'"(\w*)\$\{[^}]*\}(\w+)"', text) for piece in pieces if piece)
    return names


def cited_names(text: str) -> set[str]:
    """Identifiers in backticks that read as code: CamelCase or camelCase, four letters or more."""
    names: set[str] = set()
    for token in backticked(text):
        # A JVM flag (`-Dorg.gradle…`) and a placeholder inside a name (`<Feature>Navigation.kt`) are not names.
        token = re.sub(r"<[A-Z]\w*>(?=\w)", " ", re.sub(r"(?<![\w-])-D[\w.]+", "", token))
        for word in re.findall(r"[A-Za-z_][A-Za-z0-9_]*", token):
            if len(word) >= 4 and re.search(r"[a-z]", word) and re.search(r"[A-Z]", word[1:] if word[0].islower() else word):
                names.add(word)
    return names


def unknown_names(cited: set[str]) -> set[str]:
    """The names in `cited` that are none of the template's identifiers, code files, variants' tasks or listed names."""
    config = (BUILD_LOGIC / "com/base/app/buildlogic/config/AppConfig.kt").read_text(encoding="utf-8")
    flavors = "|".join(f"[{name[0]}{name[0].upper()}]{name[1:]}" for name in re.findall(r'flavorName = "(\w+)"', config))
    # A variant's name, or a task named after one: devBenchmarkRelease, assembleDevDebug.
    variant = re.compile(rf"(?:{flavors})(?:Debug|Release|Benchmark)")
    files = {
        path.stem for path in TEMPLATE.rglob("*") if path.suffix in CODE_SUFFIXES and "build" not in path.relative_to(TEMPLATE).parts
    }
    known = words(TEMPLATE) | files | LIBRARY_NAMES | EXAMPLE_NAMES
    return {name for name in cited if name not in known and not variant.search(name)}


def list_items(markdown: str) -> list[str]:
    """Each list item of `markdown`'s prose, its lines joined."""
    items: list[list[str]] = []
    current: list[str] | None = None
    for line in prose(markdown).splitlines():
        stripped = line.strip()
        if re.match(r"(?:- |\d+\. )", stripped):
            current = [stripped]
            items.append(current)
        elif current is not None and stripped and not stripped.startswith(("<!--", "#", "|")):
            current.append(stripped)
        else:
            current = None
    return [" ".join(item) for item in items]


def rows_over_limit(guide: str) -> list[str]:
    """The rows of the guide's "Where the template stands" whose last column is not "within"."""
    section = re.search(r"^### Where the template stands\n(.*?)(?=^#|^---)", guide, re.MULTILINE | re.DOTALL).group(1)
    rows = [row.strip("|").split("|") for row in re.findall(r"^\|.*\|$", section, re.MULTILINE)[2:]]
    return [cells[0].strip() for cells in rows if cells[-1].strip() != "within"]


def anchors(markdown: str) -> set[str]:
    """The anchors GitHub gives `markdown`'s headings, plus every explicit `<a id>`."""
    found: set[str] = set(re.findall(r'<a id="([^"]+)"', markdown))
    seen: dict[str, int] = {}
    for heading in re.findall(r"^#{1,6} (.+)$", prose(markdown), re.MULTILINE):
        slug = re.sub(r"[^\w\- ]", "", heading.strip().lower()).replace(" ", "-")
        count = seen.get(slug, 0)
        seen[slug] = count + 1
        found.add(slug if count == 0 else f"{slug}-{count}")
    return found


class TemplateDocsTest(unittest.TestCase):
    """What the docs cite against the template, which has every feature on."""

    @classmethod
    def setUpClass(cls):
        cls.guide = (TEMPLATE / GUIDE).read_text(encoding="utf-8")
        cls.rules = smooth_section((TEMPLATE / "AGENTS.md").read_text(encoding="utf-8"))

    def test_agents_md_carries_the_rules_and_links_the_guide(self):
        self.assertIn("](docs/PERFORMANCE.md)", self.rules)
        self.assertGreater(len([line for line in self.rules.splitlines() if line.startswith("- ")]), 20)

    def test_every_check_the_docs_name_exists(self):
        rules, lints, known = rule_ids(), lint_ids(), words(TEMPLATE)
        scenes = {metric.split(".")[0] for metric in gate_metrics()}
        logic = "\n".join(path.read_text(encoding="utf-8") for path in (TEMPLATE / "build-logic").rglob("*.kt*"))
        tokens = enforcement(self.guide, self.rules)
        self.assertGreater(len(tokens), 40)
        for token in tokens:
            with self.subTest(token=token):
                if re.fullmatch(r"[a-z][a-z0-9_]*(?:-[a-z0-9]+)*", token):
                    # A code rule, or the scenario whose gate holds the rule.
                    self.assertIn(token, rules | scenes)
                elif re.fullmatch(r"[A-Z][a-z]+(?:[A-Z][a-z]*)+", token):
                    # A lint id, or a test or a type that holds the rule.
                    self.assertTrue(token in lints or token in known, token)
                elif re.fullmatch(r"(?:check|verify)\w*(?:<Variant>\w*)?", token):
                    self.assertIn(token.replace("<Variant>", "").removeprefix("verify"), logic)

    def test_every_gate_row_the_docs_name_exists(self):
        metrics = gate_metrics()
        scenes = {metric.split(".")[0] for metric in metrics}
        for token in backticked(prose(self.guide) + self.rules):
            match = re.match(r"(\*|[a-z][a-z0-9_]*)((?:\.[a-z0-9_]+)+)", token)
            # A gate row is named by its scenario, or by a metric some scenario has: a misspelt scenario too.
            if not match or (match.group(1) not in scenes | {"*"} and not any(m.endswith(match.group(2)) for m in metrics)):
                continue
            with self.subTest(token=token):
                name = match.group(0)
                if name.startswith("*."):
                    self.assertTrue(any(metric.endswith(name[1:]) for metric in metrics), name)
                else:
                    self.assertTrue(any(metric == name or metric.startswith(name + ".") for metric in metrics), name)

    def test_every_scenario_the_docs_run_exists(self):
        known = scenarios()
        listing = re.search(r"^\| Scenario \| What it measures \|\n(.*?)\n\n", self.guide, re.MULTILINE | re.DOTALL).group(1)
        table = re.findall(r"^\| (`[a-z0-9_]+`(?:, `[a-z0-9_]+`)*) \|", listing, re.MULTILINE)
        named = {token for row in table for token in backticked(row)}
        command = re.search(r"tools/perf/perf\.sh --package.*?\n```", self.guide, re.DOTALL).group(0)
        named |= {word for word in re.findall(r"(?<![-\w/.:])[a-z][a-z0-9_]+(?![\w/.])", command) if word != "solid"}
        self.assertGreater(len(named), 10)
        self.assertEqual(set(), named - known)

    def test_every_name_the_docs_cite_exists(self):
        pages = {
            page.relative_to(TEMPLATE).as_posix(): cited_names(prose(page.read_text(encoding="utf-8")))
            for page in [TEMPLATE / "AGENTS.md", TEMPLATE / "README.md", *sorted((TEMPLATE / "docs").glob("*.md"))]
        }
        cited = set().union(*pages.values())
        self.assertGreater(len(cited), 200)
        unknown = unknown_names(cited)
        for page, names in pages.items():
            with self.subTest(page=page):
                self.assertEqual(STALE_NAMES.get(page, set()), names & unknown)

    def test_being_done_excuses_only_the_rows_the_guide_lists_as_over(self):
        # `--gate solid` fails the run on any row over its limit, so while the guide lists one, a done
        # condition without that exception could never be met by any change. A row's max moves by up to
        # about a millisecond between runs of one build, so the exception holds to the listed values only
        # up to that spread, or an unchanged build could fail it.
        over = rows_over_limit(self.guide)
        agents = (TEMPLATE / "AGENTS.md").read_text(encoding="utf-8")
        conditions = [
            item for text in (agents, self.guide) for item in list_items(text)
            if "--gate solid`" in item and "--gate smoke" in item
        ]
        self.assertEqual(3, len(conditions), conditions)
        clauses = set()
        for item in conditions:
            plain = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", item)
            clause = re.search(
                r"--gate solid` fails on no limit but those Where the template stands .*? lists there"
                r" beyond the spread of one build's repeated runs, .*? passes and no other row",
                plain,
            )
            with self.subTest(condition=item[:60]):
                if over:
                    self.assertIsNotNone(clause, f"{over} are over their limits today")
                    clauses.add(clause.group(0))
                else:
                    self.assertIsNone(clause, "every row is within its limits: drop the exception")
        self.assertLessEqual(len(clauses), 1, clauses)

    def test_the_spring_table_is_the_standard_style(self):
        springs = standard_springs()
        rows = re.findall(
            r"^\| `(\w+)`(?: / `(\w+)`)? \| ([\d.]+) s(?: / ([\d.]+) s)? \| ([-\d.]+)(?: / ([-\d.]+))? \|",
            self.guide, re.MULTILINE,
        )
        self.assertGreater(len(rows), 8)
        for first, second, response, response2, bounce, bounce2 in rows:
            with self.subTest(spring=first):
                self.assertEqual((float(response), float(bounce)), springs[first])
                if second:
                    self.assertEqual((float(response2), float(bounce2)), springs[second])


class GeneratedDocsTest(unittest.TestCase):
    """The same docs in projects generated with the presets that bracket the catalogue."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        manifest = ANDROID.manifest
        cases = {
            "none": frozenset(),
            "lean": manifest.preset_features("lean"),
            "standard": manifest.preset_features("standard"),
            "everything": manifest.preset_features("everything"),
        }
        cls.projects = {}
        for name, features in cases.items():
            raw = {
                "schema": 2, "platform": "android", "app_name": "Docs Check", "package_name": "com.example.docs",
                "features": sorted(features),
            }
            project = Path(cls.temp.name) / name
            pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
            cls.projects[name] = project
        cls.template_names = declared(TEMPLATE)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def cited(self, project: Path) -> str:
        return prose((project / GUIDE).read_text(encoding="utf-8")) + smooth_section(
            (project / "AGENTS.md").read_text(encoding="utf-8")
        )

    def test_every_link_reaches_its_file_and_heading(self):
        for name, project in self.projects.items():
            for page in [project / "AGENTS.md", project / "README.md", *(project / "docs").glob("*.md")]:
                text = prose(page.read_text(encoding="utf-8"))
                for target in re.findall(r"\]\(([^)\s]+)\)", text):
                    if re.match(r"[a-z]+:", target):
                        continue
                    with self.subTest(preset=name, page=page.name, link=target):
                        file, _, anchor = target.partition("#")
                        linked = (page.parent / file) if file else page
                        self.assertTrue(linked.is_file(), f"{page.name} links {file}, which is not in the project")
                        if anchor and linked.suffix == ".md":
                            self.assertIn(anchor, anchors(linked.read_text(encoding="utf-8")))

    def test_every_path_the_rules_cite_is_in_the_project(self):
        for name, project in self.projects.items():
            for token in backticked(self.cited(project)):
                path = token.split()[0].rstrip(".,:;")
                if path.split("/")[0] not in PROJECT_DIRS or "/" not in path:
                    continue
                with self.subTest(preset=name, path=path):
                    pattern = path.replace("…", "**")
                    self.assertTrue(any(project.glob(pattern)) if "*" in pattern else (project / path).exists(), path)

    def test_no_name_the_rules_cite_was_left_out_of_the_project(self):
        for name, project in self.projects.items():
            present = words(project)
            missing = {word for word in cited_names(self.cited(project)) if word in self.template_names and word not in present}
            with self.subTest(preset=name):
                self.assertEqual(set(), missing)

    def test_every_guide_the_code_points_to_is_in_the_project(self):
        for name, project in self.projects.items():
            pages = {page for text in sources(project) for page in re.findall(r"\bdocs/[\w-]+\.md\b", text)}
            for page in sorted(pages):
                with self.subTest(preset=name, page=page):
                    self.assertTrue((project / page).is_file(), f"the code sends readers to {page}, not in the project")

    def test_the_docs_speak_to_the_project_not_the_workshop(self):
        for name, project in self.projects.items():
            for page in [project / "AGENTS.md", project / GUIDE]:
                text = prose(page.read_text(encoding="utf-8"))
                with self.subTest(preset=name, page=page.name):
                    self.assertEqual([], WORKSPACE_WORDS.findall(text))
                    self.assertEqual([], PLACEHOLDER.findall(text))


if __name__ == "__main__":
    unittest.main()
