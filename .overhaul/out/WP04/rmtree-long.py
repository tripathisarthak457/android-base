"""Removes generated projects before create_project.py --force runs on this Windows host.

The generator's --force deletes the old output with shutil.rmtree, which fails on Gradle build outputs whose paths
pass MAX_PATH (WinError 3 and 145: Kotlin's inlined lambda classes and AGP's transform folders reach 330 characters).
Through the extended-length prefix the same delete reaches them. Retire it once package WIN fixes --force.
    python3 rmtree-long.py <dir>...
"""
import os
import shutil
import sys

BACKSLASH = chr(92)
for target in sys.argv[1:]:
    path = BACKSLASH * 2 + "?" + BACKSLASH + os.path.abspath(target)
    if os.path.exists(path):
        shutil.rmtree(path)
        print("removed", target)
