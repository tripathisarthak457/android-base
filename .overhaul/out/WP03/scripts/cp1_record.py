#!/usr/bin/env python3
"""
Checkpoint 1 recordings from the catalog's Feel page: press states, the switch, the segmented
control, a sheet between detents, and the toast.

    cp1_record.py --serial emulator-5554 --package com.example.wp03.cp1.catalog --out $P/cp1 [scenario...]

Run it under the adb lock. Elements are found by their test tag (the catalog exposes tags as
resource ids), so the taps follow the layout of whatever device it runs on. Frames and strips come
from WP04's perf.py (imported read-only), so they match the rest of CP1's material.
"""

import argparse
import importlib.util
import json
import os
import re
import subprocess
import sys
import time

PERF = "/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP04/templates/android/tools/perf/perf.py"
spec = importlib.util.spec_from_file_location("perf", PERF)
perf = importlib.util.module_from_spec(spec)
sys.path.insert(0, os.path.dirname(PERF))
spec.loader.exec_module(perf)


class Device:
    def __init__(self, serial):
        self.serial = serial

    def adb(self, *args, check=True):
        return subprocess.run(["adb", "-s", self.serial, *args], capture_output=True, text=True, check=check).stdout

    def shell(self, command):
        return self.adb("shell", command)

    def size(self):
        match = re.search(r"(\d+)x(\d+)", self.shell("wm size").splitlines()[-1])
        return int(match.group(1)), int(match.group(2))

    def nodes(self):
        """Every node with a resource id or text: {key: (left, top, right, bottom)}."""
        # A failed dump leaves the last one in place, whose bounds belong to another screen.
        self.shell("rm -f /sdcard/cp1_ui.xml; uiautomator dump /sdcard/cp1_ui.xml >/dev/null 2>&1")
        xml = self.shell("cat /sdcard/cp1_ui.xml 2>/dev/null")
        if "<hierarchy" not in xml:
            raise SystemExit("cp1: uiautomator could not dump the screen (it never went idle?)")
        found = {}
        for node in re.finditer(r"<node [^>]*>", xml):
            text = node.group(0)
            bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', text)
            if not bounds:
                continue
            box = tuple(int(v) for v in bounds.groups())
            for attribute in ("resource-id", "text", "content-desc"):
                value = re.search(rf'{attribute}="([^"]*)"', text)
                if value and value.group(1):
                    found.setdefault(value.group(1), box)
        return found


def require_unlocked(device):
    """Taps and swipes on a lock screen land on its keypad, so nothing runs while it shows."""
    if "isKeyguardShowing=true" in device.shell("dumpsys window"):
        raise SystemExit("cp1: the device is locked; unlock it and run again")


def centre(box):
    return (box[0] + box[2]) // 2, (box[1] + box[3]) // 2


def launch(device, package, section):
    device.shell(f"am force-stop {package}")
    activity = device.shell(
        f"cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER {package}"
    ).strip().splitlines()[-1].strip()
    device.shell(f"am start -W -n {activity} --es section {section}")
    time.sleep(2.5)


def reveal(device, tag, keep_below=0):
    """Scrolls slowly (no fling) until the tagged element sits fully on screen with room below it."""
    width, height = device.size()
    for _ in range(8):
        nodes = device.nodes()
        box = nodes.get(tag)
        limit = height - int(height * 0.12) - keep_below
        if box and box[3] <= limit and box[1] >= int(height * 0.15):
            return box, nodes
        distance = (box[3] - limit + 40) if box else int(height * 0.4)
        distance = max(120, min(distance, int(height * 0.55)))
        x = width // 2
        start = int(height * 0.8)
        device.shell(f"input swipe {x} {start} {x} {start - distance} 1200")
        time.sleep(1.0)
    raise SystemExit(f"cp1: could not bring {tag} on screen")


def union(*boxes, margin=40):
    return (min(b[0] for b in boxes) - margin, min(b[1] for b in boxes) - margin,
            max(b[2] for b in boxes) + margin, max(b[3] for b in boxes) + margin)


def crop_strip(video, frames_dir, crop, path, columns=10, limit=120):
    """The frames in which the cropped region changes (two either side), cropped and tiled at 2x."""
    x, y, w, h = crop
    region = f"crop={w}:{h}:{x}:{y}"
    differences = perf._ffmpeg_values(video, f"{region},tblend=all_mode=difference,signalstats", "YAVG")
    moving = [index + 2 for index, value in enumerate(differences) if value > 0.2]
    if not moving:
        return 0
    chosen, last = [], -10
    for n in moving:
        for m in range(max(1, n - 2), n + 3):
            if m > last and os.path.exists(os.path.join(frames_dir, f"f_{m:04d}.png")):
                chosen.append(m)
                last = m
    chosen = chosen[:limit]
    listing = os.path.join(frames_dir, "crop.txt")
    with open(listing, "w") as handle:
        handle.writelines(f"file '{os.path.join(frames_dir, f'f_{n:04d}.png')}'\n" for n in chosen)
    rows = (len(chosen) + columns - 1) // columns
    subprocess.run(
        ["ffmpeg", "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", listing, "-vf",
         f"{region},scale=iw*2:-1,tile={columns}x{rows}:padding=6:color=black", "-frames:v", "1", path],
        check=True,
    )
    return len(chosen)


def record(device, out, name, actions, seconds, focus=None):
    remote = "/sdcard/cp1_record.mp4"
    device.shell(f"rm -f {remote}")
    recorder = subprocess.Popen(
        ["adb", "-s", device.serial, "shell",
         f"screenrecord --time-limit {seconds} --size 720x1600 --bit-rate 16000000 {remote}"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    time.sleep(1.2)
    for step in actions:
        if isinstance(step, (int, float)):
            time.sleep(step)
        else:
            device.shell(step)
    recorder.wait(timeout=seconds + 30)
    time.sleep(1.0)
    directory = os.path.join(out, name)
    os.makedirs(directory, exist_ok=True)
    video = os.path.join(directory, f"{name}.mp4")
    device.adb("pull", remote, video)
    device.shell(f"rm -f {remote}")
    report = perf.extract_frames(video, os.path.join(directory, "frames"))
    strip = os.path.join(directory, f"{name}_strip.png")
    perf.make_strip(os.path.join(directory, "frames"), report, strip)
    if focus:
        # The recording is 720 px wide whatever the panel, so device pixels scale down to it.
        width, _ = device.size()
        scale = 720 / width
        left, top, right, bottom = (max(0, int(v * scale)) for v in focus)
        report["crop"] = [left, top, right - left, bottom - top]
        tiles = crop_strip(video, os.path.join(directory, "frames"), report["crop"],
                           os.path.join(directory, f"{name}_crop_strip.png"))
        report["crop_frames"] = tiles
    with open(os.path.join(directory, "frames.json"), "w") as handle:
        json.dump(report, handle, indent=2)
    print(f"{name}: {video} ({len(report['moving'])} moving frames)")


def hold(box, millis):
    x, y = centre(box)
    return f"input swipe {x} {y} {x} {y} {millis}"


def tap(box):
    x, y = centre(box)
    return f"input tap {x} {y}"


def press_buttons(device, out):
    box, nodes = reveal(device, "cp1_plain")
    filled, tinted, plain = nodes["cp1_filled"], nodes["cp1_tinted"], nodes["cp1_plain"]
    record(device, out, "press_buttons", [
        hold(filled, 450), 0.6, tap(filled), 0.8,
        hold(tinted, 450), 0.6, tap(tinted), 0.8,
        hold(plain, 450), 0.6, tap(plain),
    ], seconds=9, focus=union(filled, tinted, plain))


def press_card_row(device, out):
    box, nodes = reveal(device, "cp1_row")
    card, row = nodes["cp1_card"], nodes["cp1_row"]
    record(device, out, "press_card_row", [
        hold(card, 450), 0.7, tap(card), 0.9,
        hold(row, 450), 0.7, tap(row),
    ], seconds=7, focus=union(card, row))


def switch(device, out):
    box, _ = reveal(device, "cp1_switch")
    left, top, right, bottom = box
    y = (top + bottom) // 2
    width = right - left
    record(device, out, "switch", [
        tap(box), 1.0, tap(box), 1.0,
        # Drag the thumb across and back, slowly enough to see it follow the finger. Each drag
        # starts on the thumb's inner half: a touch that starts within the system's back-gesture
        # strip at the screen edge is a Back, not a drag.
        f"input swipe {right - int(width * 0.3)} {y} {left + int(width * 0.1)} {y} 500", 1.0,
        f"input swipe {left + int(width * 0.53)} {y} {right + int(width * 0.3)} {y} 500",
    ], seconds=7, focus=union(box, margin=60))


def segmented(device, out):
    box, _ = reveal(device, "cp1_segmented")
    left, top, right, bottom = box
    y = (top + bottom) // 2
    quarter = (right - left) // 4
    first, third, last = left + quarter // 2, left + quarter * 2 + quarter // 2, right - quarter // 2
    record(device, out, "segmented", [
        f"input tap {third} {y}", 1.0, f"input tap {first} {y}", 1.0,
        f"input swipe {first} {y} {last} {y} 700", 1.0,
        f"input swipe {last} {y} {first} {y} 700",
    ], seconds=8, focus=union(box))


def sheet(device, out):
    box, _ = reveal(device, "cp1_sheet_open")
    width, height = device.size()
    # The grabber's position depends on the detent, so the sheet is opened and measured first.
    device.shell(tap(box))
    time.sleep(1.5)
    grabber = device.nodes().get("sheet_grabber")
    if grabber is None:
        raise SystemExit("cp1: the sheet grabber was not found")
    gx, gy = centre(grabber)
    medium_y = gy
    large_y = int(height * 0.08)
    device.shell("input keyevent 4")
    time.sleep(1.5)
    record(device, out, "sheet", [
        tap(box), 1.4,
        f"input swipe {gx} {medium_y + 10} {gx} {large_y} 450", 1.4,
        f"input swipe {gx} {large_y + 30} {gx} {medium_y + 10} 450", 1.4,
        f"input swipe {gx} {medium_y + 10} {gx} {height - 60} 350",
    ], seconds=10)


def toast(device, out):
    box, _ = reveal(device, "cp1_toast")
    width, height = device.size()
    record(device, out, "toast", [tap(box)], seconds=8, focus=(0, int(height * 0.6), width, height))


SCENARIOS = {
    "press_buttons": press_buttons,
    "press_card_row": press_card_row,
    "switch": switch,
    "segmented": segmented,
    "sheet": sheet,
    "toast": toast,
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("scenarios", nargs="*", default=list(SCENARIOS))
    args = parser.parse_args()
    device = Device(args.serial)
    for name in args.scenarios:
        require_unlocked(device)
        launch(device, args.package, "Feel")
        require_unlocked(device)
        SCENARIOS[name](device, args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
