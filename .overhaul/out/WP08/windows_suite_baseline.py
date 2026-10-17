"""Passes a generator suite log whose only failures are the Windows host baseline.

Until package WIN merges, six tests fail on Windows with no change at all
(logs/windows/generator-baseline.md). A failure outside that set, or a log in which the suite did
not run to its summary, fails WP08's acceptance.

Usage: python -I windows_suite_baseline.py <unittest log>
"""
import re
import sys

BASELINE = {
    "ERROR: test_force_replaces_a_link_and_not_the_directory_it_points_at "
    "(tests.test_cli.CliTest.test_force_replaces_a_link_and_not_the_directory_it_points_at)",
    "FAIL: test_json_means_only_json_on_stdout (tests.test_cli.CliTest.test_json_means_only_json_on_stdout)",
    *(
        "FAIL: test_every_case_matches_its_golden_tree "
        f"(tests.test_golden.GoldenTest.test_every_case_matches_its_golden_tree) (case='{case}')"
        for case in ("all", "none", "hostile")
    ),
    "FAIL: test_the_wrapper_script_is_executable_and_nothing_else_is "
    "(tests.test_golden.GoldenTest.test_the_wrapper_script_is_executable_and_nothing_else_is)",
}

log = open(sys.argv[1], encoding="utf-8", errors="replace").read()
if not re.search(r"(?m)^Ran \d+ tests? in ", log):
    sys.exit("the suite did not finish: no 'Ran N tests' line")
failed = set(re.findall(r"(?m)^(?:FAIL|ERROR): .+$", log))
beyond = sorted(failed - BASELINE)
if beyond:
    sys.exit("failures beyond the Windows baseline:\n" + "\n".join(beyond))
print(f"suite: {len(failed)} failures, all in the Windows baseline of {len(BASELINE)}")
