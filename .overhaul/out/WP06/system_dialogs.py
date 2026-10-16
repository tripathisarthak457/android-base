"""Answers a system process's "isn't responding" dialog that covers the app during a device check.

A cold-booted emulator on a loaded laptop can show "System UI isn't responding" over the app for a while after
boot (accept7 and accept9 on emulator-5554). That dialog belongs to the system, not to the app under test, so the
checks answer it with Wait and look again. A dialog for the app under test is left on screen: the check fails on it.
"""

from __future__ import annotations

import re
import subprocess
import xml.etree.ElementTree as ElementTree

WAIT_BUTTON = "android:id/aerr_wait"


def answer_system_anr(serial: str, root: ElementTree.Element, package: str) -> bool:
    """Taps Wait on a not-responding dialog shown for any process but [package]; True when it did."""
    for node in root.iter("node"):
        if node.get("resource-id") != WAIT_BUTTON:
            continue
        windows = subprocess.run(
            ["adb", "-s", serial, "shell", "dumpsys", "window"], capture_output=True, text=True, timeout=60,
        ).stdout
        focused = re.search(r"mCurrentFocus=.*Not Responding: ([\w.:]+)", windows)
        process = focused.group(1) if focused else "a system process"
        if process.split(":")[0] == package:
            return False
        left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
        subprocess.run(
            ["adb", "-s", serial, "shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2)],
            check=True, capture_output=True, timeout=60,
        )
        print(f"Answered Wait to {process}'s not-responding dialog.")
        return True
    return False
