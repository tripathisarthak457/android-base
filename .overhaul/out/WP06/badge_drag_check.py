#!/usr/bin/env python3
"""Checks on a device that the devtools badge follows a drag.

Installs the APK, clears the app, launches it, finds the badge (resource id `devtools_badge`) in a
`uiautomator dump`, swipes from its centre by (-500, -800) px over 600 ms, dumps again and exits 1
unless the centre moved at least 80 % of the swipe on both axes. A badge stopped by the screen's
edge counts as having moved: the badge clamps to the window, and after the release it settles on
the side it was thrown towards, so on the horizontal axis it usually travels further than the
finger did.

Run under the adb lock: `locked adb python3 badge_drag_check.py --serial ... --package ... --apk ... --out ...`.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

from system_dialogs import answer_system_anr

SWIPE = (-500, -800)
SWIPE_MILLIS = 600
REQUIRED_FRACTION = 0.8
#: A badge whose edge is this close to the screen's edge was stopped by it.
EDGE_PX = 160
BADGE_ID = "devtools_badge"
DUMP_ON_DEVICE = "/sdcard/wp06_badge.xml"


def adb(serial: str, *args: str) -> str:
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=180)
    if result.returncode != 0:
        raise SystemExit(f"adb {' '.join(args)} failed ({result.returncode}): {result.stderr.strip()}")
    return result.stdout


def dump(serial: str, saved: Path) -> str:
    adb(serial, "shell", "uiautomator", "dump", DUMP_ON_DEVICE)
    adb(serial, "pull", DUMP_ON_DEVICE, str(saved))
    return saved.read_text(encoding="utf-8")


def badge_bounds(hierarchy: str) -> tuple[int, int, int, int] | None:
    for node in ElementTree.fromstring(hierarchy).iter("node"):
        if node.get("resource-id", "").endswith(BADGE_ID):
            left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
            return left, top, right, bottom
    return None


def screen_size(serial: str) -> tuple[int, int]:
    sizes = re.findall(r"(\d+)x(\d+)", adb(serial, "shell", "wm", "size"))
    width, height = sizes[-1]
    return int(width), int(height)


def launcher_component(serial: str, package: str) -> str:
    lines = adb(serial, "shell", "cmd", "package", "resolve-activity", "--brief", package).strip().splitlines()
    component = lines[-1].strip()
    if "/" not in component:
        raise SystemExit(f"No launcher activity for {package}: {lines}")
    return component


def wait_for_badge(
    serial: str, package: str, out: Path, seconds: float = 45.0,
) -> tuple[str, tuple[int, int, int, int]]:
    deadline = time.monotonic() + seconds
    while True:
        hierarchy = dump(serial, out / "before.xml")
        bounds = badge_bounds(hierarchy)
        if bounds is not None:
            return hierarchy, bounds
        if answer_system_anr(serial, ElementTree.fromstring(hierarchy), package):
            continue
        if time.monotonic() > deadline:
            raise SystemExit(f"The badge ({BADGE_ID}) never appeared; the last dump is {out / 'before.xml'}.")
        time.sleep(1.0)


def moved_enough(before: float, after: float, swipe: int, low_edge: float, high_edge: float) -> bool:
    travelled = after - before
    if abs(travelled) >= REQUIRED_FRACTION * abs(swipe) and (travelled < 0) == (swipe < 0):
        return True
    reached_edge = after <= low_edge if swipe < 0 else after >= high_edge
    return reached_edge


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True, help="the application id, flavour suffix included")
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)

    adb(args.serial, "install", "-r", "-d", str(args.apk))
    adb(args.serial, "shell", "pm", "clear", args.package)
    component = launcher_component(args.serial, args.package)
    adb(
        args.serial, "shell", "am", "start", "-W", "-n", component,
        "--ez", "perf.fakeSession", "true", "--ez", "perf.skipOnboarding", "true",
    )

    width, height = screen_size(args.serial)
    _, before = wait_for_badge(args.serial, args.package, args.out)
    adb(args.serial, "shell", "screencap", "-p", "/sdcard/wp06_badge_before.png")
    adb(args.serial, "pull", "/sdcard/wp06_badge_before.png", str(args.out / "before.png"))

    start_x = (before[0] + before[2]) // 2
    start_y = (before[1] + before[3]) // 2
    adb(
        args.serial, "shell", "input", "swipe",
        str(start_x), str(start_y), str(start_x + SWIPE[0]), str(start_y + SWIPE[1]), str(SWIPE_MILLIS),
    )
    # The release starts a settle to the nearer edge; the dump must see where it came to rest.
    time.sleep(1.5)

    after = badge_bounds(dump(args.serial, args.out / "after.xml"))
    adb(args.serial, "shell", "screencap", "-p", "/sdcard/wp06_badge_after.png")
    adb(args.serial, "pull", "/sdcard/wp06_badge_after.png", str(args.out / "after.png"))
    if after is None:
        print(f"FAIL: the badge is gone after the swipe; see {args.out / 'after.xml'}")
        return 1

    end_x = (after[0] + after[2]) / 2
    end_y = (after[1] + after[3]) / 2
    half_w = (after[2] - after[0]) / 2
    half_h = (after[3] - after[1]) / 2
    x_ok = moved_enough(start_x, end_x, SWIPE[0], half_w + EDGE_PX, width - half_w - EDGE_PX)
    y_ok = moved_enough(start_y, end_y, SWIPE[1], half_h + EDGE_PX, height - half_h - EDGE_PX)

    result = {
        "screen": [width, height],
        "swipe": {"from": [start_x, start_y], "by": list(SWIPE), "millis": SWIPE_MILLIS},
        "before": list(before),
        "after": list(after),
        "moved": [round(end_x - start_x), round(end_y - start_y)],
        "x_ok": x_ok,
        "y_ok": y_ok,
    }
    (args.out / "result.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result))
    if not (x_ok and y_ok):
        print("FAIL: the badge did not follow the drag.")
        return 1
    print("BADGE DRAG OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
