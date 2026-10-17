#!/usr/bin/env python3
"""Runs the generator's unit tests and fails only on failures beyond the Windows host baseline.

Until package WIN merges, six tests fail on this machine with no change at all
(`logs/windows/generator-baseline.md`): CRLF from `Path.write_text`, the missing exec bit, an 8.3 short path
and the symlink privilege. Those, and only those, are allowed; any other failure or error fails the run.

    python gen_tests.py <generator dir>
"""

from __future__ import annotations

import re
import subprocess
import sys

# (test method, subtest text or None): exactly the baseline's six.
BASELINE = {
    ("test_every_case_matches_its_golden_tree", "(case='all')"),
    ("test_every_case_matches_its_golden_tree", "(case='none')"),
    ("test_every_case_matches_its_golden_tree", "(case='hostile')"),
    ("test_the_wrapper_script_is_executable_and_nothing_else_is", None),
    ("test_json_means_only_json_on_stdout", None),
    ("test_force_replaces_a_link_and_not_the_directory_it_points_at", None),
}
HEADER = re.compile(r"^(FAIL|ERROR): (\w+) \([\w.]+\)(?: (\(.*\)))?\s*$")


def main() -> int:
    generator = sys.argv[1] if len(sys.argv) > 1 else "."
    result = subprocess.run(
        [sys.executable, "-I", "-m", "unittest", "discover", "-s", "tests", "-t", "."],
        cwd=generator,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    sys.stdout.write(result.stdout)
    sys.stdout.write(result.stderr)

    seen = set()
    for line in result.stderr.splitlines():
        match = HEADER.match(line)
        if match:
            seen.add((match.group(2), match.group(3)))
    unexpected = sorted(seen - BASELINE, key=str)
    summary = [line for line in result.stderr.splitlines() if line.startswith(("Ran ", "OK", "FAILED"))]
    print("GENERATOR TESTS:", " | ".join(summary))
    print(f"GENERATOR TESTS: {len(seen & BASELINE)} of the {len(BASELINE)} baseline failures seen")
    if unexpected:
        for name, subtest in unexpected:
            print(f"GENERATOR TESTS: beyond the baseline: {name} {subtest or ''}".rstrip())
        return 1
    if result.returncode != 0 and not seen:
        print("GENERATOR TESTS: unittest failed without a reported test failure")
        return 1
    print("GENERATOR TESTS OK (no failure beyond the Windows baseline)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
