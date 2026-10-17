"""The generator's unit tests on this Windows machine, judged against the host baseline
(logs/windows/generator-baseline.md): it passes when the only failures are those six, so a new failure is never
hidden behind them. Run from generator/: python3 <this file>. Retire it once package WIN has merged."""
import re
import subprocess
import sys

# The baseline's failures by test name, with how many subtests or entries each may account for.
BASELINE = {
    "test_every_case_matches_its_golden_tree": 3,
    "test_the_wrapper_script_is_executable_and_nothing_else_is": 1,
    "test_json_means_only_json_on_stdout": 1,
    "test_force_replaces_a_link_and_not_the_directory_it_points_at": 1,
}

result = subprocess.run([sys.executable, "-m", "unittest", "discover", "-s", "tests", "-t", "."],
                        capture_output=True, text=True, errors="replace")
output = result.stdout + result.stderr
print(output[-4000:])
seen = {}
unexpected = []
for line in output.splitlines():
    match = re.match(r"^(FAIL|ERROR): (\w+)", line)
    if not match:
        continue
    name = match.group(2)
    seen[name] = seen.get(name, 0) + 1
    if name not in BASELINE or seen[name] > BASELINE[name]:
        unexpected.append(line)
ran = re.search(r"Ran (\d+) tests", output)
if not ran:
    sys.exit("GEN-TESTS: the suite did not finish")
if unexpected:
    print("GEN-TESTS: failures beyond the Windows baseline:\n" + "\n".join(unexpected))
    sys.exit(1)
print(f"GEN-TESTS OK: {ran.group(1)} tests, failures only from the Windows baseline: {seen}")
