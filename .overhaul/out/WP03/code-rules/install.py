"""Installs the sheet and scroll code rules into a copy of WP02's build-logic: the rules, their fixtures and cases.

    python3 install.py <templates/android of the copy>
"""
import pathlib
import re
import shutil
import sys

here = pathlib.Path(__file__).parent
root = pathlib.Path(sys.argv[1]) / "build-logic/convention/src"
main = root / "main/kotlin/com/base/app/buildlogic/guard"
test = root / "test/kotlin/com/base/app/buildlogic/guard"

for name in ("SheetRules.kt", "ScrollRules.kt"):
    shutil.copy(here / name, main / name)
for name in ("SheetRuleCasesTest.kt", "ScrollRuleCasesTest.kt"):
    shutil.copy(here / name, test / name)

rules = main / "CodeRules.kt"
text = rules.read_text()
if "SheetRules.ALL" not in text:
    text = text.replace("+ AdaptiveRules.ALL", "+ AdaptiveRules.ALL + SheetRules.ALL + ScrollRules.ALL", 1)
    assert "ScrollRules.ALL" in text, "CodeRules.ALL changed shape"
    rules.write_text(text, newline="\n")

# WP02's own fixtures and cases use scroll containers to prove other rules. They name the fling, as
# every container must, so each still proves only its own rule (and keeps its line numbers).
FLING = "flingBehavior = AppScrollDefaults.flingBehavior()"
CONTAINER = r"(?<![\w.])(LazyColumn|LazyRow|LazyVerticalGrid)"


def name_fling(source: str) -> str:
    source = re.sub(CONTAINER + r" \{", lambda m: f"{m.group(1)}({FLING}) {{", source)
    source = re.sub(CONTAINER + r"\((?!flingBehavior)", lambda m: f"{m.group(1)}({FLING}, ", source)
    return source.replace(".verticalScroll(rememberScrollState())", f".verticalScroll(rememberScrollState(), {FLING})")


for name in ("CodeRulesTest.kt", "CodeRuleCasesTest.kt"):
    path = test / name
    text = path.read_text()
    if FLING not in text:
        path.write_text(name_fling(text), newline="\n")

tests = test / "CodeRulesTest.kt"
text = tests.read_text()
if '"sheet-ime-padding"' not in text:
    fixtures = (here / "CodeRulesTest-fixtures.kt").read_text()
    body, const = fixtures.split("        // And beside FEATURE")
    const_line = next(line for line in const.splitlines() if "const val SHEET" in line)
    body = "\n".join(line for line in body.splitlines() if not line.strip().startswith("// Sheet rules"))
    marker = "    )\n\n    @Test\n    fun `every rule fires on its fixture, on the right line`()"
    assert marker in text, "CodeRulesTest changed shape"
    text = text.replace(marker, body.rstrip() + "\n" + marker, 1)
    anchor = '        const val DESIGN_SYSTEM = "core/designsystem/src/main/kotlin/A.kt"\n'
    assert anchor in text, "CodeRulesTest constants changed"
    text = text.replace(anchor, anchor + const_line + "\n", 1)
    tests.write_text(text, newline="\n")
print("installed")
