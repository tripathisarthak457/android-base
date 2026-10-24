"""
The docs every generated Android project carries (CLAUDE.md, AGENTS.md, README.md and docs/*.md) stay true to the
project they ship in, whatever features, architecture, languages and UI option it was generated with.

Per generated project: CLAUDE.md imports the three detailed docs and each import resolves; AGENTS.md is CLAUDE.md with
each import as a link; every backticked module is in settings.gradle.kts and every backticked path exists; every rule
id cited is a rule the project has, and DESIGN-SYSTEM.md's rule index lists exactly those rules; every code name the
template or its variants declare and the docs cite is in the project; every relative link reaches its file and
heading; every skill named is vendored; every doc the code sends a reader to exists; no word of the workshop that
built the template; each doc within its line budget.

Against the raw template, which holds both sides of every block: every code name the docs cite is in the template's
code, a variant's code, or one of the short lists below of library and example names.
"""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get

ANDROID = get("android")
TEMPLATE = ANDROID.template_dir
VARIANTS = Path(__file__).resolve().parents[1] / "variants"
DOCS = ("CLAUDE.md", "AGENTS.md", "README.md", "docs/ARCHITECTURE.md", "docs/PERFORMANCE.md", "docs/DESIGN-SYSTEM.md")
IMPORTED = ("docs/ARCHITECTURE.md", "docs/PERFORMANCE.md", "docs/DESIGN-SYSTEM.md")
#: The most lines each doc may have in a generated project. CLAUDE.md and its three imports load into every agent
#: session, so they are kept detailed but bounded.
BUDGETS = {"CLAUDE.md": 120, "docs/ARCHITECTURE.md": 600, "docs/PERFORMANCE.md": 700, "docs/DESIGN-SYSTEM.md": 420}
CODE_SUFFIXES = (".kt", ".kts", ".xml", ".toml", ".pro")
#: Top-level folders of a project: a backticked token starting with one and holding a slash is a path that must exist.
PROJECT_DIRS = (
    ".agents", ".claude", "app", "benchmark", "build-logic", "catalog", "config", "core", "data", "docs", "domain",
    "feature", "gradle", "tools",
)
#: Words of the workshop that builds the template, and private names, which a project's docs never contain.
WORKSHOP_WORDS = re.compile(r"overhaul|staging-wp|\bWP\d\d\b|REQUIREMENTS|periflow|vitfleet|jorwen", re.IGNORECASE)
#: Platform and library names the docs cite. A project need not spell them out, and the template may declare one of
#: its own with the same name.
LIBRARY_NAMES = frozenset({
    "AndroidViewModel", "BasicText", "ColorProducer", "Crossfade", "DrawFrame", "DrawModifierNode", "FlexBox",
    "Hyphens", "ImmutableList", "LayoutModifierNode", "LazyLayoutCacheWindow", "LineBreak", "MediaQuery",
    "MutableState", "NavigationSuiteScaffold", "OnBackInvokedCallback", "RenderEffect", "SavedStateHandleSaveableApi",
    "SceneStrategy", "Simple", "StartupTimingMetric", "TextAutoSize", "TextFieldState", "TotalTime", "animateItem",
    "animateTo", "bindApplication", "computedStateOf", "currentValueOf", "doFrame", "drawWithCache",
    "drawWithContent", "getMemoryInfo", "initialVelocity", "onAttach", "onTrimMemory", "popTransitionSpec",
    "predictivePopTransitionSpec", "savedStateHandle", "snapTo", "transitionSpec", "withFrameNanos",
})
#: Names the docs make up to show the shape of something.
EXAMPLE_NAMES = frozenset({
    "OrdersScreen", "XColors", "XDefaults", "XScreen", "XState", "YourKey", "YourRoute", "isLoading",
    "rememberXState",
})
#: Names a page still cites that the code does not have. Each leaves when its paragraph is rewritten; empty now.
STALE_NAMES: dict[str, set[str]] = {}
#: Backticked hyphenated tokens that are neither rule ids, skills nor folders: Android tools' own words.
OTHER_HYPHENATED = frozenset({"speed-profile"})
#: Projects generated for the per-project checks: the presets at both ends, both architectures, every language, and
#: the same without UI components.
LANGUAGES = ["de", "es", "fr", "hi", "pt-BR"]
CASES = {
    "ui-none": ("none", "compact", (), True),
    "ui-lean": ("lean", "compact", (), True),
    "ui-standard": ("standard", "compact", (), True),
    "ui-everything": ("everything", "compact", (), True),
    "ui-everything-modular": ("everything", "modular", (), True),
    "ui-everything-languages": ("everything", "compact", LANGUAGES, True),
    "noui-lean": ("lean", "compact", (), False),
    "noui-standard": ("standard", "compact", (), False),
    "noui-everything": ("everything", "compact", (), False),
    "noui-everything-modular": ("everything", "modular", (), False),
}


def prose(text: str) -> str:
    """`text` without its fenced code blocks."""
    return re.sub(r"^(```|~~~).*?^\1[^\n]*$", "", text, flags=re.MULTILINE | re.DOTALL)


def backticked(text: str) -> list[str]:
    return re.findall(r"`([^`\n]+)`", text)


def code_files(root: Path) -> list[Path]:
    return [
        path for path in root.rglob("*")
        if path.suffix in CODE_SUFFIXES and path.is_file() and not {"build", ".agents", ".gradle"} & set(path.parts)
    ]


def sources(root: Path) -> list[str]:
    return [path.read_text(encoding="utf-8", errors="replace") for path in code_files(root)]


def words(root: Path) -> set[str]:
    """Every identifier in the project's code, build scripts and resources, and the code files' names."""
    found = {word for text in sources(root) for word in re.findall(r"[A-Za-z_][A-Za-z0-9_]*", text)}
    return found | {path.stem for path in code_files(root)}


def declared(root: Path) -> set[str]:
    """The names the code itself declares, Gradle tasks included: its own API, not the libraries'."""
    names: set[str] = set()
    for text in sources(root):
        names.update(re.findall(r"\b(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)", text))
        names.update(re.findall(r'\bregister(?:<[^>]*>)?\(\s*"(\w+)"', text))
    return names


def cited_names(text: str) -> set[str]:
    """Identifiers in backticks that read as code: CamelCase or camelCase, four letters or more."""
    names: set[str] = set()
    for token in backticked(text):
        # A JVM flag and a placeholder inside a name (`<Feature>Navigation.kt`, `verify<Variant>…`) are not names.
        token = re.sub(r"<[A-Za-z]\w*>(?=\w)|(?<=\w)<[A-Za-z]\w*>", " ", re.sub(r"(?<![\w-])-D[\w.]+", "", token))
        for word in re.findall(r"[A-Za-z_][A-Za-z0-9_]*", token):
            if len(word) >= 4 and re.search(r"[a-z]", word) and re.search(r"[A-Z]", word[1:] if word[0].islower() else word):
                names.add(word)
    return names


def variant_task(name: str) -> bool:
    """A build variant, or a task named after one: devBenchmarkRelease, assembleDevDebug."""
    return re.search(r"(?:[dD]ev|[sS]taging|[pP]rod|[pP]laystore)(?:Debug|Release|Benchmark)", name) is not None


def rule_ids(root: Path) -> set[str]:
    """Every rule id the build logic declares, the Compose compiler's check (not a source pattern) included."""
    ids: set[str] = set()
    for path in (root / "build-logic").rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        ids.update(re.findall(r'\bid = "([a-z0-9-]+)"', text))
        ids.update(re.findall(r'\bCOMPOSE_COMPILER = "([a-z0-9-]+)"', text))
    return ids


def active_rule_ids(root: Path) -> set[str]:
    """The ids of the rules `CodeRules.ALL` runs: those of each rule object it adds up."""
    guard = next(root.glob("build-logic/convention/src/main/kotlin/**/guard"))
    line = re.search(r"val ALL: List<Rule> = (.+)", (guard / "CodeRules.kt").read_text(encoding="utf-8")).group(1)
    ids: set[str] = set()
    for owner in re.findall(r"(\w+)\.ALL", line):
        ids.update(re.findall(r'\bid = "([a-z0-9-]+)"', (guard / f"{owner}.kt").read_text(encoding="utf-8")))
    return ids


def included_modules(root: Path) -> set[str]:
    return set(re.findall(r'^\s*include\("(:[\w:-]+)"\)', (root / "settings.gradle.kts").read_text(encoding="utf-8"), re.MULTILINE))


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


def rule_index(design_system: str) -> set[str]:
    """The rule ids in the first column of DESIGN-SYSTEM.md's rule index."""
    section = re.search(r"^## The rule index\n(.*?)(?=^## |\Z)", design_system, re.MULTILINE | re.DOTALL)
    assert section is not None, "DESIGN-SYSTEM.md has no rule index"
    return {
        token for row in re.findall(r"^\| (`[^|]+`) \|", section.group(1), re.MULTILINE)
        for token in backticked(row)
    }


def mirrored(claude: str) -> str:
    """AGENTS.md as CLAUDE.md should make it: each @-import a link, for agents that do not read imports."""
    return re.sub(r"(?<![\w`])@(docs/[\w.-]+\.md)", r"[\1](\1)", claude)


class TemplateDocsTest(unittest.TestCase):
    """The raw template's docs, which hold every feature's lines and both sides of the UI option."""

    def test_every_name_the_docs_cite_exists_somewhere(self):
        known = words(TEMPLATE) | (words(VARIANTS) if VARIANTS.is_dir() else set()) | LIBRARY_NAMES | EXAMPLE_NAMES
        cited_total = 0
        for page in DOCS:
            names = cited_names(prose((TEMPLATE / page).read_text(encoding="utf-8")))
            cited_total += len(names)
            unknown = {name for name in names if name not in known and not variant_task(name)}
            with self.subTest(page=page):
                self.assertEqual(STALE_NAMES.get(page, set()), unknown)
        self.assertGreater(cited_total, 300)

    def test_agents_md_mirrors_claude_md(self):
        claude = (TEMPLATE / "CLAUDE.md").read_text(encoding="utf-8")
        self.assertEqual(mirrored(claude), (TEMPLATE / "AGENTS.md").read_text(encoding="utf-8"))

    def test_the_design_system_doc_carries_both_templates(self):
        text = (TEMPLATE / "docs/DESIGN-SYSTEM.md").read_text(encoding="utf-8")
        for tag in ("kotlin component-template", "kotlin component-test-template"):
            with self.subTest(tag=tag):
                self.assertRegex(text, rf"(?m)^```{tag}$")


class GeneratedDocsTest(unittest.TestCase):
    """The docs as each case's project receives them."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        manifest = ANDROID.manifest
        cls.projects: dict[str, Path] = {}
        for name, (preset, architecture, languages, ui) in CASES.items():
            features = manifest.preset_features(preset) if preset != "none" else frozenset()
            raw = {
                "schema": 2, "platform": "android", "app_name": "Docs Check", "package_name": "com.example.docs",
                "architecture": architecture, "features": sorted(features), "languages": list(languages),
            }
            if not ui:
                raw["ui_components"] = False
            project = Path(cls.temp.name) / name
            pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
            cls.projects[name] = project
        cls.template_names = declared(TEMPLATE) | (declared(VARIANTS) if VARIANTS.is_dir() else set())

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def pages(self, project: Path) -> dict[str, str]:
        return {page: (project / page).read_text(encoding="utf-8") for page in DOCS}

    def test_claude_md_imports_the_three_docs_and_each_exists(self):
        for name, project in self.projects.items():
            claude = (project / "CLAUDE.md").read_text(encoding="utf-8")
            imports = re.findall(r"(?<![\w`])@([\w./-]+\.md)", prose(claude))
            with self.subTest(case=name):
                self.assertEqual(sorted(IMPORTED), sorted(imports))
                for page in imports:
                    self.assertTrue((project / page).is_file(), page)

    def test_agents_md_mirrors_claude_md(self):
        for name, project in self.projects.items():
            with self.subTest(case=name):
                claude = (project / "CLAUDE.md").read_text(encoding="utf-8")
                self.assertEqual(mirrored(claude), (project / "AGENTS.md").read_text(encoding="utf-8"))

    def test_each_doc_keeps_to_its_budget(self):
        for name, project in self.projects.items():
            for page, limit in BUDGETS.items():
                with self.subTest(case=name, page=page):
                    self.assertLessEqual(len((project / page).read_text(encoding="utf-8").splitlines()), limit)

    def test_every_module_the_docs_name_is_in_the_build(self):
        for name, project in self.projects.items():
            modules = included_modules(project)
            for page, text in self.pages(project).items():
                for token in backticked(prose(text)):
                    if not re.fullmatch(r":[a-z][\w-]*(?::[\w-]+)+", token):
                        continue
                    with self.subTest(case=name, page=page, module=token):
                        # A modular project includes `:feature:auth:impl`; `:feature:auth` names the pair.
                        self.assertTrue(any(module == token or module.startswith(token + ":") for module in modules))

    def test_every_path_the_docs_name_is_in_the_project(self):
        for name, project in self.projects.items():
            for page, text in self.pages(project).items():
                for token in backticked(prose(text)):
                    path = token.split()[0].rstrip(".,:;")
                    if path.split("/")[0] not in PROJECT_DIRS or "/" not in path:
                        continue
                    pattern = re.sub(r"<[^>]+>", "*", path.replace("…", "**").replace("...", "**"))
                    with self.subTest(case=name, page=page, path=path):
                        self.assertTrue(any(project.glob(pattern)) if "*" in pattern else (project / path).exists(), path)

    def test_every_rule_the_docs_cite_is_a_rule_of_the_project(self):
        for name, project in self.projects.items():
            rules = rule_ids(project)
            skills = {path.name for path in (project / ".agents/skills").iterdir() if path.is_dir()}
            for page, text in self.pages(project).items():
                for token in backticked(prose(text)):
                    if not re.fullmatch(r"[a-z][a-z0-9]*(?:-[a-z0-9]+)+", token) or token in skills | OTHER_HYPHENATED | set(PROJECT_DIRS):
                        continue
                    with self.subTest(case=name, page=page, token=token):
                        self.assertIn(token, rules)

    def test_the_rule_index_lists_exactly_the_rules_the_project_runs(self):
        for name, project in self.projects.items():
            index = rule_index((project / "docs/DESIGN-SYSTEM.md").read_text(encoding="utf-8"))
            with self.subTest(case=name):
                self.assertEqual(active_rule_ids(project), index)

    def test_no_name_the_docs_cite_was_left_out_of_the_project(self):
        for name, project in self.projects.items():
            present = words(project)
            for page, text in self.pages(project).items():
                missing = {
                    word for word in cited_names(prose(text)) - LIBRARY_NAMES - EXAMPLE_NAMES
                    if word in self.template_names and word not in present
                }
                with self.subTest(case=name, page=page):
                    self.assertEqual(set(), missing)

    def test_every_link_reaches_its_file_and_heading(self):
        for name, project in self.projects.items():
            for page in [*DOCS, *(path.relative_to(project).as_posix() for path in (project / "docs").glob("*.md"))]:
                path = project / page
                for target in re.findall(r"\]\(([^)\s]+)\)", prose(path.read_text(encoding="utf-8"))):
                    if re.match(r"[a-z]+:", target):
                        continue
                    file, _, anchor = target.partition("#")
                    linked = (path.parent / file) if file else path
                    with self.subTest(case=name, page=page, link=target):
                        self.assertTrue(linked.is_file(), f"{page} links {file}, which is not in the project")
                        if anchor and linked.suffix == ".md":
                            self.assertIn(anchor, anchors(linked.read_text(encoding="utf-8")))

    def test_every_skill_the_docs_name_is_vendored(self):
        for name, project in self.projects.items():
            for page, text in self.pages(project).items():
                for skill in re.findall(r"`([a-z0-9-]+)`(?:,? (?:and )?`[a-z0-9-]+`)* skills?\b", prose(text)):
                    with self.subTest(case=name, page=page, skill=skill):
                        self.assertTrue((project / ".agents/skills" / skill / "SKILL.md").is_file())

    def test_every_doc_the_code_points_to_is_in_the_project(self):
        for name, project in self.projects.items():
            pages = {page for text in sources(project) for page in re.findall(r"\bdocs/[\w-]+\.md\b", text)}
            for page in sorted(pages):
                with self.subTest(case=name, page=page):
                    self.assertTrue((project / page).is_file(), f"the code sends readers to {page}, not in the project")

    def test_the_docs_speak_to_the_project_not_the_workshop(self):
        for name, project in self.projects.items():
            for page in [*DOCS, ".agents/skills/README.md"]:
                with self.subTest(case=name, page=page):
                    self.assertEqual([], WORKSHOP_WORDS.findall((project / page).read_text(encoding="utf-8")))

    def test_the_design_system_doc_carries_both_templates(self):
        for name, project in self.projects.items():
            text = (project / "docs/DESIGN-SYSTEM.md").read_text(encoding="utf-8")
            for tag in ("kotlin component-template", "kotlin component-test-template"):
                with self.subTest(case=name, tag=tag):
                    self.assertRegex(text, rf"(?m)^```{tag}$")


if __name__ == "__main__":
    unittest.main()
