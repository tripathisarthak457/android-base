"""Prints each test case of the JUnit XML files given, passed or with its failure message."""
import glob
import html
import re
import sys

for pattern in sys.argv[1:]:
    for path in sorted(glob.glob(pattern)):
        text = open(path, encoding="utf-8").read()
        for match in re.finditer(r'<testcase name="([^"]+)"[^>]*?(?:/>|>(.*?)</testcase>)', text, re.S):
            failure = re.search(r'<failure message="([^"]*)"', match.group(2) or "")
            name = html.unescape(match.group(1))
            if failure:
                print(f"FAIL {name} :: {html.unescape(failure.group(1))[:200]}")
            else:
                print(f"ok   {name}")
