#!/usr/bin/env python3
"""Checks on a device that the devtools badge keeps clear of titles and Back (QA-44).

Installs the APK, clears the app and launches it into the shell. On the root screen the badge must
not overlap the large title. It then opens the first row of the list, where the badge must not
overlap the back button, and taps the back button's centre, which must pop back to the root. Each
state is saved as a screenshot and a `uiautomator dump` under `--out`.

With `--top-chrome` (audit C-8) it also throws and drags the badge to the top edge: on the root, where
it must stay clear of the whole large-title header (title, subtitle and search field), and on the
pushed screen, where it must stay clear of the bar (Back's touch target and the inline title). After
Back pops, the badge left below the pushed screen's shorter bar must be clear of the root's header again.

Run under the adb lock: `locked adb python3 badge_qa_check.py --serial ... --package ... --apk ... --out ...`.
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

BADGE_ID = "devtools_badge"
ROOT_TITLE = "Samples"
BACK_DESCRIPTION = "Navigate back"
# Long enough for the badge's settle spring to come to rest after a swipe.
SETTLE_SECONDS = 1.5
DUMP_ON_DEVICE = "/sdcard/wp06_qa.xml"
SHOT_ON_DEVICE = "/sdcard/wp06_qa.png"

Bounds = tuple[int, int, int, int]


def adb(serial: str, *args: str) -> str:
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=180)
    if result.returncode != 0:
        raise SystemExit(f"adb {' '.join(args)} failed ({result.returncode}): {result.stderr.strip()}")
    return result.stdout


def capture(serial: str, out: Path, name: str) -> ElementTree.Element:
    adb(serial, "shell", "uiautomator", "dump", DUMP_ON_DEVICE)
    adb(serial, "pull", DUMP_ON_DEVICE, str(out / f"{name}.xml"))
    adb(serial, "shell", "screencap", "-p", SHOT_ON_DEVICE)
    adb(serial, "pull", SHOT_ON_DEVICE, str(out / f"{name}.png"))
    return ElementTree.fromstring((out / f"{name}.xml").read_text(encoding="utf-8"))


def bounds_of(node: ElementTree.Element) -> Bounds:
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
    return left, top, right, bottom


def find(root: ElementTree.Element, predicate) -> Bounds | None:
    for node in root.iter("node"):
        if predicate(node):
            return bounds_of(node)
    return None


def badge(root: ElementTree.Element) -> Bounds | None:
    return find(root, lambda n: n.get("resource-id", "").endswith(BADGE_ID))


def overlaps(a: Bounds, b: Bounds) -> bool:
    return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]


def centre(b: Bounds) -> tuple[int, int]:
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def union(*boxes: Bounds) -> Bounds:
    return min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes)


def swipe(serial: str, start: tuple[int, int], end: tuple[int, int], millis: int) -> None:
    adb(serial, "shell", "input", "swipe", str(start[0]), str(start[1]), str(max(end[0], 0)), str(max(end[1], 0)), str(millis))
    time.sleep(SETTLE_SECONDS)


def throw_up_to_start(serial: str, tag: Bounds) -> None:
    """A fast flick towards the top-left: the badge settles on the start edge at its top bound."""
    x, y = centre(tag)
    swipe(serial, (x, y), (x - 600, 0), 120)


def drag_up_to_end(serial: str, tag: Bounds, width: int) -> None:
    """A slow drag past the top-right corner: the badge stops at its top bound on the end edge."""
    x, y = centre(tag)
    swipe(serial, (x, y), (width - 1, 0), 1200)


def root_header(root: ElementTree.Element, title: Bounds, content: tuple[int, int]) -> Bounds | None:
    """The large title down to the bottom of the search field, across the content's width.

    The search field's own node is narrower than the box drawn around it (its icon sits outside the
    text field), so its band is widened to the content's edges, which the list rows share.
    """
    parents = {child: parent for parent in root.iter() for child in parent}
    for node in root.iter("node"):
        if "EditText" not in node.get("class", ""):
            continue
        field = bounds_of(node)
        # The box drawn around the text field, when it is the field's parent rather than the content.
        box = parents.get(node)
        if box is not None and box.tag == "node":
            outer = bounds_of(box)
            if content[0] <= outer[0] and outer[2] <= content[1]:
                field = union(field, outer)
        return union(title, (content[0], field[1], content[1], field[3]))
    return None


def pushed_bar(root: ElementTree.Element) -> Bounds | None:
    """Back's touch target and the inline title beside it."""
    parents = {child: parent for parent in root.iter() for child in parent}
    for node in root.iter("node"):
        if node.get("content-desc") != BACK_DESCRIPTION:
            continue
        target = node
        while target is not None and target.get("clickable") != "true":
            target = parents.get(target)
        back = bounds_of(target if target is not None else node)
        texts = [bounds_of(n) for n in root.iter("node") if n.get("text") and bounds_of(n)[1] < back[3] and bounds_of(n)[3] > back[1]]
        return union(back, *texts)
    return None


def wait_for(serial: str, package: str, out: Path, name: str, predicate, seconds: float = 45.0):
    deadline = time.monotonic() + seconds
    while True:
        root = capture(serial, out, name)
        found = predicate(root)
        if found:
            return root, found
        if answer_system_anr(serial, root, package):
            continue
        if time.monotonic() > deadline:
            raise SystemExit(f"FAIL: timed out waiting for {name}; see {out / (name + '.xml')}")
        time.sleep(1.0)


def first_row(root: ElementTree.Element, title: Bounds, avoid: Bounds, height: int) -> Bounds | None:
    for node in root.iter("node"):
        if node.get("clickable") != "true":
            continue
        # The list's search field sits between the title and the rows.
        if any("EditText" in child.get("class", "") for child in node.iter("node")):
            continue
        b = bounds_of(node)
        wide = (b[2] - b[0]) > 400
        below_title = b[1] > title[3]
        above_bar = b[3] < height * 0.8
        if wide and below_title and above_bar and not overlaps(b, avoid):
            return b
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--top-chrome", action="store_true", help="also throw and drag the badge to the top edge")
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)

    adb(args.serial, "install", "-r", "-d", str(args.apk))
    adb(args.serial, "shell", "pm", "clear", args.package)
    component = adb(args.serial, "shell", "cmd", "package", "resolve-activity", "--brief", args.package).strip().splitlines()[-1]
    adb(
        args.serial, "shell", "am", "start", "-W", "-n", component.strip(),
        "--ez", "perf.fakeSession", "true", "--ez", "perf.skipOnboarding", "true",
    )
    width, height = map(int, re.findall(r"(\d+)x(\d+)", adb(args.serial, "shell", "wm", "size"))[-1])

    def root_ready(root):
        title = find(root, lambda n: n.get("text") == ROOT_TITLE)
        tag = badge(root)
        if title is None or tag is None:
            return None
        row = first_row(root, title, tag, height)
        return (title, tag, row) if row else None

    _, (title, root_badge, row) = wait_for(args.serial, args.package, args.out, "root", root_ready)
    title_clear = not overlaps(title, root_badge)
    content = (row[0], row[2])
    top = {}

    def header_check(name: str) -> bool:
        root = capture(args.serial, args.out, name)
        tag, header = badge(root), root_header(root, title, content)
        if tag is None or header is None:
            raise SystemExit(f"FAIL: no badge or no search field on {name}; see {args.out / (name + '.xml')}")
        clear = not overlaps(tag, header)
        top[name] = {"header": list(header), "badge": list(tag), "clear": clear}
        return clear

    if args.top_chrome:
        throw_up_to_start(args.serial, root_badge)
        header_check("root-throw")
        drag_up_to_end(args.serial, tuple(top["root-throw"]["badge"]), width)
        header_check("root-drag")
        # The badge has moved; tap a row it does not cover.
        latest = capture(args.serial, args.out, "root-before-push")
        row = first_row(latest, title, badge(latest), height)
        if row is None:
            raise SystemExit(f"FAIL: no list row clear of the badge; see {args.out / 'root-before-push.xml'}")

    adb(args.serial, "shell", "input", "tap", *map(str, centre(row)))

    def pushed_ready(root):
        back = find(root, lambda n: n.get("content-desc") == BACK_DESCRIPTION)
        tag = badge(root)
        return (back, tag) if back and tag else None

    _, (back, pushed_badge) = wait_for(args.serial, args.package, args.out, "pushed", pushed_ready)
    back_clear = not overlaps(back, pushed_badge)

    if args.top_chrome:
        throw_up_to_start(args.serial, pushed_badge)
        thrown = capture(args.serial, args.out, "pushed-throw")
        tag, bar = badge(thrown), pushed_bar(thrown)
        if tag is None or bar is None:
            raise SystemExit(f"FAIL: no badge or no Back on pushed-throw; see {args.out / 'pushed-throw.xml'}")
        top["pushed-throw"] = {"bar": list(bar), "badge": list(tag), "clear": not overlaps(tag, bar)}

    adb(args.serial, "shell", "input", "tap", *map(str, centre(back)))
    time.sleep(SETTLE_SECONDS)
    after = capture(args.serial, args.out, "popped")
    popped = find(after, lambda n: n.get("content-desc") == BACK_DESCRIPTION) is None and \
        find(after, lambda n: n.get("text") == ROOT_TITLE) is not None
    if args.top_chrome and popped:
        header_check("popped-reclamped")

    result = {
        "root": {"title": list(title), "badge": list(root_badge), "clear": title_clear},
        "pushed": {"back": list(back), "badge": list(pushed_badge), "clear": back_clear},
        "back_tap_popped": popped,
    }
    if args.top_chrome:
        result["top_chrome"] = top
    top_clear = all(check["clear"] for check in top.values()) and (not args.top_chrome or "popped-reclamped" in top)
    (args.out / "qa.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result))
    if not (title_clear and back_clear and popped and top_clear):
        print("FAIL: the badge overlaps a title, the search field or Back, or Back did not pop.")
        return 1
    print("BADGE QA OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
