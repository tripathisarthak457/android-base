"""
K12 on the phone: at a sheet's medium detent the end of its content can be reached.

    sheet_probe.py --serial 9a7c74f4 --package com.example.wp03.cp1.catalog --out DIR

Run it under the adb lock, with the phone awake and unlocked (with_phone.sh). It opens the catalog's
Filters sheet (Medium and Large, 40 rows and a pinned Apply footer), switches its list to scroll
before the sheet grows, scrolls to the end at Medium, and checks that the last row sits fully above
the footer and that the sheet is still at Medium. Screenshots go to DIR; the verdict is printed and
written to DIR/k12.txt.
"""

import argparse
import importlib.util
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("cp1_record", os.path.join(HERE, "cp1_record.py"))
cp1 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cp1)

LAST_ROW = "Filter 40"
SCROLLS_FIRST = "List scrolls before the sheet grows"


def shot(device, out, name):
    png = subprocess.run(["adb", "-s", device.serial, "exec-out", "screencap -p"], capture_output=True, check=True).stdout
    with open(os.path.join(out, f"{name}.png"), "wb") as handle:
        handle.write(png)


def find(nodes, text):
    """A node by its text, whatever case the design style draws its labels in (Editorial: upper)."""
    return next((box for key, box in nodes.items() if key.casefold() == text.casefold()), None)


def tap(device, box):
    x, y = cp1.centre(box)
    device.shell(f"input tap {x} {y}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    device = cp1.Device(args.serial)
    cp1.require_unlocked(device)
    cp1.launch(device, args.package, "Sheets")
    trigger, _ = cp1.reveal(device, "catalog_sheet_filters")
    tap(device, trigger)
    time.sleep(2.0)
    shot(device, args.out, "filters-medium")
    nodes = device.nodes()
    grabber = nodes.get("sheet_grabber")
    toggle = find(nodes, SCROLLS_FIRST)
    if grabber is None or toggle is None:
        raise SystemExit(f"k12: the sheet did not open at Medium (grabber {grabber}, toggle {toggle})")
    tap(device, toggle)
    time.sleep(0.8)
    width, height = device.size()
    x = width // 2
    for _ in range(12):
        device.shell(f"input swipe {x} {int(height * 0.85)} {x} {int(height * 0.62)} 400")
        time.sleep(0.5)
    time.sleep(1.0)
    shot(device, args.out, "filters-medium-end")
    nodes = device.nodes()
    last = find(nodes, LAST_ROW)
    apply = find(nodes, "Apply")
    still = nodes.get("sheet_grabber")
    lines = [
        f"grabber before {grabber}, after {still}",
        f"last row {last}, Apply {apply}, window {width}x{height}",
    ]
    ok = last is not None and apply is not None and last[3] <= apply[1] and still is not None and abs(still[1] - grabber[1]) <= 4
    lines.append("K12 OK: the last row is above the footer at Medium" if ok else "K12 FAILED")
    with open(os.path.join(args.out, "k12.txt"), "w") as handle:
        handle.write("\n".join(lines) + "\n")
    print("\n".join(lines))
    device.shell("input keyevent 4")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
