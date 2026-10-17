"""Reads a `python -m unittest` log and fails only on failures beyond the Windows host baseline.

Until package WIN merges, six generator tests fail on this machine with no change at all
(`.overhaul/logs/windows/generator-baseline.md`). Anything else that fails or errors is ours.
    python beyond_baseline.py <log>
"""
import re
import sys

BASELINE = {
    "test_golden.GoldenTest.test_every_case_matches_its_golden_tree",
    "test_golden.GoldenTest.test_the_wrapper_script_is_executable_and_nothing_else_is",
    "test_cli.CliTest.test_json_means_only_json_on_stdout",
    "test_cli.CliTest.test_force_replaces_a_link_and_not_the_directory_it_points_at",
}

text = open(sys.argv[1], encoding="utf-8", errors="replace").read()
ran = re.search(r"^Ran (\d+) tests?", text, re.M)
if not ran:
    print("BASELINE CHECK: the suite did not finish")
    sys.exit(1)
found = re.findall(r"^(FAIL|ERROR): (\S+) \(([\w.]+)\)", text, re.M)
beyond = []
for kind, name, ident in found:
    ident = ident.removeprefix("tests.")
    if not ident.endswith(name):
        ident = f"{ident}.{name}"
    if ident not in BASELINE:
        beyond.append(f"{kind}: {ident}")
print(f"BASELINE CHECK: ran {ran.group(1)}, {len(found)} failing, {len(beyond)} beyond the Windows baseline")
for line in beyond:
    print("  " + line)
sys.exit(1 if beyond else 0)
