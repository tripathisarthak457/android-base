"""Installs the sheet code rules into a copy of WP02's build-logic: SheetRules.kt, its fixtures and its cases.

    python3 install.py <templates/android of the copy>
"""
import pathlib
import shutil
import sys

here = pathlib.Path(__file__).parent
root = pathlib.Path(sys.argv[1]) / "build-logic/convention/src"
main = root / "main/kotlin/com/base/app/buildlogic/guard"
test = root / "test/kotlin/com/base/app/buildlogic/guard"

shutil.copy(here / "SheetRules.kt", main / "SheetRules.kt")
shutil.copy(here / "SheetRuleCasesTest.kt", test / "SheetRuleCasesTest.kt")

rules = main / "CodeRules.kt"
text = rules.read_text()
if "SheetRules.ALL" not in text:
    text = text.replace("+ AdaptiveRules.ALL", "+ AdaptiveRules.ALL + SheetRules.ALL", 1)
    assert "SheetRules.ALL" in text, "CodeRules.ALL changed shape"
    rules.write_text(text)

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
    tests.write_text(text)
print("installed")
