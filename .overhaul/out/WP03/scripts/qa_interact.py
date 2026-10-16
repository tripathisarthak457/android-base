#!/usr/bin/env python3
"""
Interaction checks on the catalog: opens each overlay and stateful control and screenshots the
result, so states a static page cannot show (an alert, a menu, a swiped row) get looked at.

    qa_interact.py --serial emulator-5554 --package com.example.wp03.cp1.catalog --out DIR

Run it under the adb lock. Elements are found by their text through UiAutomator, as a person would
find them.
"""

import argparse
import importlib.util
import os
import sys
import time

spec = importlib.util.spec_from_file_location("cp1", os.path.join(os.path.dirname(__file__), "cp1_record.py"))
cp1 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cp1)


def shot(device, out, name):
    path = os.path.join(out, f"{name}.png")
    with open(path, "wb") as handle:
        handle.write(cp1.subprocess.run(["adb", "-s", device.serial, "exec-out", "screencap -p"],
                                        capture_output=True, check=True).stdout)
    print(f"{name}: {path}")


def find(device, label):
    """The last element on the page whose text starts with [label], scrolled fully on screen."""
    width, height = device.size()
    for _ in range(10):
        found = None
        for text, box in all_nodes(device):
            if text == label or any(text.startswith(label + separator) for separator in (",", " ", "&#10;")):
                found = box
        limit = height - int(height * 0.12)
        if found and found[3] <= limit and found[1] >= int(height * 0.15):
            return found
        distance = (found[3] - limit + 40) if found and found[3] > limit else int(height * 0.4)
        distance = max(120, min(distance, int(height * 0.55)))
        x = width // 2
        device.shell(f"input swipe {x} {int(height * 0.8)} {x} {int(height * 0.8) - distance} 1200")
        time.sleep(1.0)
    raise SystemExit(f"qa: could not find {label!r}")


def all_nodes(device):
    """(text, bounds) for every node with text or a description, in document order."""
    device.shell("uiautomator dump /sdcard/qa_ui.xml >/dev/null 2>&1")
    xml = device.shell("cat /sdcard/qa_ui.xml")
    result = []
    for node in cp1.re.finditer(r"<node [^>]*>", xml):
        text = node.group(0)
        bounds = cp1.re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', text)
        if not bounds:
            continue
        box = tuple(int(v) for v in bounds.groups())
        for attribute in ("text", "content-desc"):
            value = cp1.re.search(rf'{attribute}="([^"]*)"', text)
            if value and value.group(1):
                result.append((value.group(1), box))
    return result


def run(device, out, package):
    width, height = device.size()

    def page(section):
        cp1.require_unlocked(device)
        cp1.launch(device, package, section)

    page("Overlays")
    device.shell(cp1.tap(find(device, "Alert")))
    time.sleep(1.0)
    shot(device, out, "alert")
    device.shell("input keyevent 4")
    time.sleep(1.0)

    device.shell(cp1.tap(find(device, "Share options")))
    time.sleep(1.0)
    shot(device, out, "action_sheet")
    device.shell("input keyevent 4")
    time.sleep(1.0)

    device.shell(cp1.hold(find(device, "Long-press me"), 800))
    time.sleep(0.8)
    shot(device, out, "menu")
    device.shell("input keyevent 4")
    time.sleep(1.0)

    device.shell(cp1.tap(find(device, "Large only")))
    time.sleep(1.2)
    shot(device, out, "sheet_large")
    device.shell("input keyevent 4")
    time.sleep(1.2)

    device.shell(cp1.tap(find(device, "Medium and large")))
    time.sleep(1.2)
    shot(device, out, "sheet_medium")
    device.shell("input keyevent 4")
    time.sleep(1.2)

    device.shell(cp1.tap(find(device, "HUD")))
    time.sleep(0.8)
    shot(device, out, "toast_hud")
    time.sleep(3.0)

    device.shell(cp1.tap(find(device, "Error")))
    time.sleep(0.8)
    shot(device, out, "toast_error")
    time.sleep(3.0)

    device.shell(cp1.tap(find(device, "Block the screen for 2 s")))
    time.sleep(0.6)
    shot(device, out, "loading_overlay")
    time.sleep(2.5)

    page("Lists")
    box = find(device, "Swipe me")
    y = (box[1] + box[3]) // 2
    device.shell(f"input swipe {int(width * 0.8)} {y} {int(width * 0.45)} {y} 400")
    time.sleep(1.0)
    shot(device, out, "swipe_trailing")
    device.shell(cp1.tap(box))
    time.sleep(1.0)
    device.shell(f"input swipe {int(width * 0.2)} {y} {int(width * 0.55)} {y} 400")
    time.sleep(1.0)
    shot(device, out, "swipe_leading")

    page("Lists")
    device.shell(cp1.tap(find(device, "Shipping details")))
    time.sleep(1.0)
    shot(device, out, "disclosure_open")

    page("Pickers")
    device.shell(cp1.tap(find(device, "Oct 12, 2026")))
    time.sleep(1.2)
    shot(device, out, "date_pill_open")

    page("Fields")
    device.shell(cp1.tap(find(device, "Select")))
    time.sleep(1.0)
    shot(device, out, "select_open")
    device.shell("input keyevent 4")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    run(cp1.Device(args.serial), args.out, args.package)
    print("QA INTERACT OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
