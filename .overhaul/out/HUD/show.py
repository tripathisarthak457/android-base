"""The phone demo of the performance overlay (REQUIREMENTS 40, "First, and visible"): screenshots and a recording.

  bin/locked adb python -I out/HUD/show.py --release <devRelease apk> --debug <devDebug apk> --out perf/HUD/show

1. devRelease, compiled with its profile as Play would, launched with recording on: a recording of the app in use
   with the pill beside the DEV badge, the pill tapped, and the Performance tab scrolled through; a screenshot of
   the pill and of each part of the tab; the report exported from the panel and pulled.
2. devDebug: the tab's warning that a debug build's numbers are pessimistic.
Changes no device setting. Leaves the devRelease installed, so the overlay can be tried on the phone.
"""

import argparse
import json
import os
import re
import subprocess
import time

PACKAGE = "com.example.hud.everything.dev"
# Named in full: a debug build also has LeakCanary's launcher activity, which makes resolving one ambiguous.
MAIN = f"{PACKAGE}/com.example.hud.everything.MainActivity"
PILL = "Live frame rate and janky frames. Opens the Performance tab."
TABS = ("Feed", "Search", "Profile", "Settings", "Home")
RECORD_SECONDS = 80
DEVICE_VIDEO = "/sdcard/hud-demo.mp4"


def adb(serial, *args, timeout=120):
    done = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=timeout,
                          encoding="utf-8", errors="replace")
    return done.stdout + done.stderr


def shell(serial, command, timeout=120):
    return adb(serial, "shell", command, timeout=timeout)


def ui_nodes(serial):
    shell(serial, "uiautomator dump /sdcard/hud_show_ui.xml >/dev/null 2>&1")
    xml = shell(serial, "cat /sdcard/hud_show_ui.xml")
    shell(serial, "rm -f /sdcard/hud_show_ui.xml")
    nodes = []
    for match in re.finditer(r"<node ([^>]*)>", xml):
        attrs = dict(re.findall(r'(\S+?)="([^"]*)"', match.group(1)))
        bounds = list(map(int, re.findall(r"\d+", attrs.get("bounds", ""))))
        if len(bounds) == 4:
            attrs["center"] = ((bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2)
            attrs["box"] = tuple(bounds)
            nodes.append(attrs)
    return nodes


def find(nodes, text):
    for node in nodes:
        if text in (node.get("text"), node.get("content-desc")):
            return node
    return None


def tap(serial, node):
    x, y = node["center"]
    shell(serial, f"input tap {x} {y}")


def screenshot(serial, path):
    with open(path, "wb") as f:
        subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], stdout=f, timeout=60, check=True)


def install(serial, apk):
    adb(serial, "uninstall", PACKAGE)
    print(adb(serial, "install", "-r", "-t", "-d", apk, timeout=300).strip())


def launch(serial, hud):
    shell(serial, f"am force-stop {PACKAGE}")
    time.sleep(0.8)
    print(shell(serial, f"am start -W -n {MAIN} --es perf.hud {hud}").strip())
    time.sleep(4.0)


def use_app(serial):
    """Scrolls, visits every tab, and pushes and pops a row: something for each graph and screen to show."""
    for _ in range(4):
        shell(serial, "input swipe 540 1800 540 700 140")
        time.sleep(0.9)
        shell(serial, "input swipe 540 700 540 1800 140")
        time.sleep(0.9)
    nodes = ui_nodes(serial)
    for label in TABS:
        tab = find(nodes, label)
        if tab:
            tap(serial, tab)
            time.sleep(1.2)
            shell(serial, "input swipe 540 1700 540 900 160")
            time.sleep(1.0)
    nodes = ui_nodes(serial)
    rows = [n for n in nodes if n.get("clickable") == "true" and n["box"][1] > 600 and n["box"][3] < 1900
            and n["box"][2] - n["box"][0] > 650]
    if rows:
        for _ in range(2):
            tap(serial, rows[0])
            time.sleep(1.4)
            shell(serial, "input keyevent 4")
            time.sleep(1.4)


def open_tab(serial):
    pill = find(ui_nodes(serial), PILL)
    if pill is None:
        raise SystemExit("the pill is not on screen")
    tap(serial, pill)
    time.sleep(2.0)
    return pill


def release_demo(serial, apk, out):
    install(serial, apk)
    adb(serial, "shell", "am", "broadcast", "-a", "androidx.profileinstaller.action.INSTALL_PROFILE",
        "-n", f"{PACKAGE}/androidx.profileinstaller.ProfileInstallReceiver")
    shell(serial, f"am force-stop {PACKAGE}")
    print(shell(serial, f"cmd package compile -f -m speed-profile {PACKAGE}", timeout=300).strip())
    launch(serial, "on")

    recorder = subprocess.Popen(["adb", "-s", serial, "shell", "screenrecord", "--size", "720x1600",
                                 "--time-limit", str(RECORD_SECONDS), DEVICE_VIDEO])
    started = time.monotonic()
    time.sleep(1.5)
    use_app(serial)
    shell(serial, "input swipe 540 1800 540 700 140")
    time.sleep(0.25)
    screenshot(serial, os.path.join(out, "1-pill-after-fling.png"))
    time.sleep(1.5)
    screenshot(serial, os.path.join(out, "2-pill-idle.png"))

    pill = open_tab(serial)
    shots = []
    for part in range(1, 9):
        name = f"3-tab-{part}.png"
        screenshot(serial, os.path.join(out, name))
        shots.append(name)
        shell(serial, "input swipe 540 1750 540 950 400")
        time.sleep(1.2)

    export = find(ui_nodes(serial), "Export report")
    for _ in range(12):
        if export:
            break
        shell(serial, "input swipe 540 950 540 1750 300")
        time.sleep(0.8)
        export = find(ui_nodes(serial), "Export report")
    exported = None
    if export:
        tap(serial, export)
        time.sleep(2.5)
        screenshot(serial, os.path.join(out, "4-exported.png"))
        for node in ui_nodes(serial):
            match = re.search(r"Saved (\S+\.json)", node.get("text", ""))
            if match:
                exported = match.group(1)

    left = RECORD_SECONDS + 3 - (time.monotonic() - started)
    if left > 0:
        time.sleep(left)
    recorder.wait(timeout=60)
    time.sleep(1.0)
    adb(serial, "pull", DEVICE_VIDEO, os.path.join(out, "demo-devRelease.mp4"), timeout=300)
    shell(serial, f"rm -f {DEVICE_VIDEO}")

    pulled = []
    if exported:
        for path in (exported, exported[:-len(".json")] + ".md"):
            target = os.path.join(out, os.path.basename(path))
            adb(serial, "pull", path, target, timeout=120)
            if os.path.exists(target):
                pulled.append(os.path.basename(path))
    log = shell(serial, "logcat -d -s PerfHud:I")
    with open(os.path.join(out, "perfhud-logcat-devRelease.txt"), "w", encoding="utf-8") as f:
        f.write(log)
    return {"pill_bounds": pill["box"], "tab_shots": shots, "exported": exported, "pulled": pulled}


def debug_demo(serial, apk, out):
    install(serial, apk)
    launch(serial, "on")
    use_app(serial)
    screenshot(serial, os.path.join(out, "5-debug-pill.png"))
    open_tab(serial)
    screenshot(serial, os.path.join(out, "6-debug-tab.png"))
    warning = find(ui_nodes(serial), "Debug build: these numbers are pessimistic")
    return {"debug_warning_on_screen": warning is not None}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="9a7c74f4")
    parser.add_argument("--release", required=True)
    parser.add_argument("--debug", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--steps", default="debug,release")
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    steps = args.steps.split(",")
    result = {}
    if "debug" in steps:
        shell(args.serial, "logcat -c")
        result["debug"] = debug_demo(args.serial, args.debug, args.out)
    if "release" in steps:
        shell(args.serial, "logcat -c")
        result["release"] = release_demo(args.serial, args.release, args.out)
    with open(os.path.join(args.out, "show.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
