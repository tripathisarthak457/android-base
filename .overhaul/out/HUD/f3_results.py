"""Unit-test and lint totals of a Gradle build, read from its result files (written after a given time).

  python -I out/HUD/f3_results.py <project dir> <earliest mtime, epoch seconds>

Fix run 3's acceptance log lost the template build's output (tee /dev/stderr truncates a redirected log in Git
Bash), so its results are read from what the build wrote instead.
"""
import glob
import os
import re
import sys

root, since = sys.argv[1], float(sys.argv[2])
tests = failures = stale = 0
modules = {}
for path in glob.glob(os.path.join(root, "**/build/test-results/**/TEST-*.xml"), recursive=True):
    if os.path.getmtime(path) < since:
        stale += 1
        continue
    with open(path, encoding="utf-8") as handle:
        head = handle.read(4000)
    match = re.search(r'tests="(\d+)" skipped="(\d+)" failures="(\d+)" errors="(\d+)"', head)
    module = os.path.relpath(path, root).split(os.sep + "build" + os.sep)[0].replace(os.sep, ":")
    count, failed = int(match.group(1)), int(match.group(3)) + int(match.group(4))
    tests += count
    failures += failed
    total = modules.setdefault(module, [0, 0])
    total[0] += count
    total[1] += failed
for module, (count, failed) in sorted(modules.items()):
    print(f"  {module}: {count} tests, {failed} failed")
print(f"tests: {tests} run after the build started, {failures} failed ({stale} result files older, not counted)")
lint_errors = lint_warnings = 0
for path in glob.glob(os.path.join(root, "**/build/reports/lint-results-*.xml"), recursive=True):
    if os.path.getmtime(path) < since:
        continue
    with open(path, encoding="utf-8") as handle:
        text = handle.read()
    errors = len(re.findall(r'severity="(?:Error|Fatal)"', text))
    warnings = len(re.findall(r'severity="Warning"', text))
    lint_errors += errors
    lint_warnings += warnings
    if errors or warnings:
        print(f"  lint {os.path.relpath(path, root)}: {errors} errors, {warnings} warnings")
print(f"lint: {lint_errors} errors, {lint_warnings} warnings")
