#!/usr/bin/env python3
"""
The phone half of the shimmer verification (REPORT.md section 5.4), in one adb lock hold:

    bin/locked adb python3 qa/shimmer/after/scripts/session.py 9a7c74f4 [step ...]

Records the states EVIDENCE.md recorded (the catalog's Skeletons group, the sample list's cold start
with its data cleared, light, dark, font scale 1.5 and landscape) on the release builds of
wt/WP03-skel, measures the catalog's Loading page and Skeletons group for 10 s with dumpsys gfxinfo
(FrameTimeline janky frames, framestats), checks that idle skeletons draw no frames, and measures the
pre-fix catalog's Skeletons group the same way. Every setting it changes (night mode, font scale,
rotation, stay awake, animator duration scale, airplane mode) is read first and put back however the
session ends; the final state is compared with the initial one. It stops at once on a locked phone.
"""

import hashlib
import json
import os
import re
import subprocess
import sys
import time

S = sys.argv[1]
ONLY = set(sys.argv[2:])
OV = "/home/crazyhusir/Documents/projects/tools/.overhaul"
OUT = f"{OV}/qa/shimmer/after"
for sub in ("rec", "shots", "gfx", "device", "ui"):
    os.makedirs(f"{OUT}/{sub}", exist_ok=True)

CAT = "com.example.wp03skel.cp1.catalog"
CAT_OLD = "com.example.wp03.cp1.catalog"
PTR = "com.example.wp03skel.ptr.dev"
PTR_ACTIVITY = f"{PTR}/com.example.wp03skel.ptr.MainActivity"
APK_CAT = f"{OV}/out/WP03-skel/cp1/catalog/build/outputs/apk/release/catalog-release.apk"
APK_PTR = f"{OV}/out/WP03-skel/ptr/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk"
APK_CAT_OLD = f"{OV}/out/WP03/cp1/catalog/build/outputs/apk/release/catalog-release.apk"

LOG = open(f"{OUT}/session.log", "a")


def say(message):
    line = f"{time.strftime('%H:%M:%S')} {message}"
    print(line, flush=True)
    LOG.write(line + "\n")
    LOG.flush()


def adb(*args, timeout=120):
    return subprocess.run(["adb", "-s", S, *args], capture_output=True, text=True, timeout=timeout)


def sh(command, timeout=120):
    return adb("shell", command, timeout=timeout).stdout.replace("\r", "")


def setting(namespace, key):
    return sh(f"settings get {namespace} {key}").strip()


def put(namespace, key, value):
    if value == "null":
        sh(f"settings delete {namespace} {key}")
    else:
        sh(f"settings put {namespace} {key} {value}")


SETTINGS = [
    ("system", "font_scale"),
    ("system", "accelerometer_rotation"),
    ("system", "user_rotation"),
    ("global", "stay_on_while_plugged_in"),
    ("global", "airplane_mode_on"),
    ("global", "window_animation_scale"),
    ("global", "transition_animation_scale"),
    ("global", "animator_duration_scale"),
]


def read_state():
    state = {f"{ns}/{key}": setting(ns, key) for ns, key in SETTINGS}
    state["night"] = sh("cmd uimode night").strip().replace("Night mode: ", "")
    state["wm size"] = sh("wm size").strip()
    state["wm density"] = sh("wm density").strip()
    return state


def restore(initial):
    sh(f"cmd uimode night {initial['night']}")
    for ns, key in SETTINGS:
        if key == "airplane_mode_on":
            continue
        if setting(ns, key) != initial[f"{ns}/{key}"]:
            put(ns, key, initial[f"{ns}/{key}"])
    if setting("global", "airplane_mode_on") != initial["global/airplane_mode_on"]:
        sh("cmd connectivity airplane-mode " + ("enable" if initial["global/airplane_mode_on"] == "1" else "disable"))
        time.sleep(3)
    final = read_state()
    with open(f"{OUT}/device/final-state.txt", "w") as handle:
        json.dump(final, handle, indent=1)
    differences = {k: (initial[k], final[k]) for k in initial if initial[k] != final[k]}
    say(f"restored; differences from the initial state: {differences or 'none'}")


def md5(path):
    return hashlib.md5(open(path, "rb").read()).hexdigest()


def ui_nodes():
    sh("rm -f /sdcard/qv_ui.xml; uiautomator dump /sdcard/qv_ui.xml >/dev/null 2>&1", timeout=60)
    xml = sh("cat /sdcard/qv_ui.xml")
    nodes = []
    for match in re.finditer(r"<node [^>]*>", xml):
        node = match.group(0)
        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        text = re.search(r' text="([^"]*)"', node)
        desc = re.search(r'content-desc="([^"]*)"', node)
        if bounds:
            nodes.append(((text.group(1) if text else ""), (desc.group(1) if desc else ""),
                          tuple(int(v) for v in bounds.groups())))
    return nodes, xml


def find(label):
    nodes, _ = ui_nodes()
    for text, desc, box in nodes:
        if text == label or desc == label:
            return box
    return None


def tap(label):
    box = find(label)
    if not box:
        say(f"could not find {label!r}")
        return False
    sh(f"input tap {(box[0] + box[2]) // 2} {(box[1] + box[3]) // 2}")
    return True


def scroll_heading_to(label, target_y, fallback_swipes=()):
    """Scrolls slowly until [label]'s top is near target_y (device px). Returns the swipes made."""
    swipes = []
    for _ in range(6):
        box = find(label)
        if box is None:
            if fallback_swipes:
                say(f"{label!r} not in the UI dump (never idle?): replaying {list(fallback_swipes)}")
                for distance in fallback_swipes:
                    sh(f"input swipe 540 1900 540 {1900 - distance} 1500")
                    time.sleep(1.2)
                return list(fallback_swipes)
            sh("input swipe 540 1900 540 1100 1500")
            swipes.append(800)
            time.sleep(1.2)
            continue
        error = box[1] - target_y
        if abs(error) < 40:
            return swipes
        distance = max(-900, min(900, error))
        start = 1900 if distance > 0 else 600
        sh(f"input swipe 540 {start} 540 {start - distance} 1500")
        swipes.append(distance)
        time.sleep(1.2)
    return swipes


def record(name, seconds, action=None, size="720x1600"):
    remote = f"/sdcard/qv_{name}.mp4"
    process = subprocess.Popen(["adb", "-s", S, "shell",
                                f"screenrecord --size {size} --bit-rate 20000000 --time-limit {seconds} {remote}"])
    time.sleep(0.8)
    started = time.time()
    if action:
        action()
    process.wait(timeout=seconds + 30)
    time.sleep(0.6)
    pulled = adb("pull", remote, f"{OUT}/rec/{name}.mp4")
    sh(f"rm -f {remote}")
    say(f"recorded rec/{name}.mp4 ({seconds} s, action at +0.8 s after start, {pulled.returncode=})")
    return started


def screencap(name):
    with open(f"{OUT}/shots/{name}.png", "wb") as handle:
        handle.write(subprocess.run(["adb", "-s", S, "exec-out", "screencap -p"], capture_output=True).stdout)


def burst(name, count):
    """Screencaps one after another (about 0.3-0.5 s apart): lossless frames at many sweep phases."""
    times = []
    for index in range(count):
        times.append(time.time())
        screencap(f"{name}-{index:02d}")
    with open(f"{OUT}/shots/{name}-times.txt", "w") as handle:
        handle.write("\n".join(f"{t - times[0]:.3f}" for t in times))


def gfx(package, name, seconds):
    sh(f"dumpsys gfxinfo {package} reset")
    time.sleep(seconds)
    text = sh(f"dumpsys gfxinfo {package} framestats")
    with open(f"{OUT}/gfx/{name}.txt", "w") as handle:
        handle.write(text)
    frames = re.search(r"Total frames rendered: (\d+)", text)
    janky = re.search(r"Janky frames: (\d+) \(([\d.]+)%\)", text)
    gpu90 = re.search(r"\n90th gpu percentile: (\d+)ms", text)
    say(f"gfx {name}: {seconds} s, frames {frames.group(1) if frames else '?'}, janky "
        f"{janky.group(0) if janky else '?'}, gpu p90 {gpu90.group(1) if gpu90 else '?'} ms")


def cpu_ticks(package):
    pid = sh(f"pidof {package}").strip()
    if not pid:
        return None
    stat = sh(f"cat /proc/{pid}/stat").split()
    return int(stat[13]) + int(stat[14]) if len(stat) > 15 else None


def start_catalog(package, *extras):
    sh(f"am force-stop {package}")
    sh(f"am start -W -n {package}/.CatalogActivity " + " ".join(extras))
    time.sleep(2.5)


def step(name):
    return not ONLY or name in ONLY


def cold_start(name, seconds=5, size="720x1600"):
    sh(f"am force-stop {PTR}")
    sh(f"pm clear {PTR}")
    time.sleep(1)
    record(name, seconds, lambda: sh(f"am start -n {PTR_ACTIVITY}"), size=size)
    path = f"{OUT}/rec/{name}.mp4"
    if size != "720x1600" and (not os.path.exists(path) or os.path.getsize(path) < 50_000):
        say(f"{name}: nothing usable at {size}; again at 720x1600")
        sh(f"am force-stop {PTR}")
        sh(f"pm clear {PTR}")
        time.sleep(1)
        record(name, seconds, lambda: sh(f"am start -n {PTR_ACTIVITY}"))
    time.sleep(1.5)
    screencap(f"{name}-rest")


def main():
    initial = read_state()
    with open(f"{OUT}/device/initial-state.txt", "w") as handle:
        json.dump(initial, handle, indent=1)
    say(f"initial state: {initial}")
    try:
        sh("input keyevent KEYCODE_WAKEUP")
        time.sleep(1)
        if "deviceLocked=1" in sh("dumpsys trust"):
            say("PHONE LOCKED: nothing measured")
            return 3
        put("global", "stay_on_while_plugged_in", "7")
        put("system", "accelerometer_rotation", "0")
        put("system", "user_rotation", "0")
        sh("cmd uimode night no")
        put("system", "font_scale", "1.0")
        time.sleep(1)

        if step("install"):
            for apk in (APK_CAT, APK_PTR):
                say(f"install {os.path.basename(apk)} ({md5(apk)}): {adb('install', '-r', apk, timeout=300).stdout.strip()[-60:]}")
            installed = sh(f"pm path {CAT_OLD}").strip().replace("package:", "")
            if installed:
                adb("pull", installed, f"{OUT}/device/installed-{CAT_OLD}.apk")
                same = md5(f"{OUT}/device/installed-{CAT_OLD}.apk") == md5(APK_CAT_OLD)
                say(f"{CAT_OLD} installed copy {'is' if same else 'is NOT'} the Oct 12 13:30 build")
                if not same:
                    say(f"install the Oct 12 build: {adb('install', '-r', APK_CAT_OLD, timeout=300).stdout.strip()[-60:]}")
            else:
                say(f"install the Oct 12 build: {adb('install', '-r', APK_CAT_OLD, timeout=300).stdout.strip()[-60:]}")

        # 1. The catalog's Loading page: 12 redacted rows under a large title.
        if step("loading"):
            for theme in ("light", "dark"):
                extras = ["--es section Loading"] + (["--es theme dark"] if theme == "dark" else [])
                start_catalog(CAT, *extras)
                gfx(CAT, f"cat-loading-{theme}-10s", 10)
                record(f"cat-loading-{theme}", 7)
                burst(f"cat-loading-{theme}", 14)
                if theme == "light":
                    nodes, xml = ui_nodes()
                    with open(f"{OUT}/ui/cat-loading-light.xml", "w") as handle:
                        handle.write(xml)
            # Lifting the redaction in place: no row may move, and the clock must stop.
            start_catalog(CAT, "--es section Loading")
            box = find("Load")
            say(f"Load button at {box}")
            if box:
                record("cat-loading-light-load", 5,
                       lambda: (time.sleep(1.6), sh(f"input tap {(box[0] + box[2]) // 2} {(box[1] + box[3]) // 2}")))
                time.sleep(1)
                gfx(CAT, "cat-loaded-light-idle-5s", 5)
                screencap("cat-loaded-light")
            # Right to left: the band must enter at the start (right) edge.
            start_catalog(CAT, "--es section Loading", "--ez rtl true")
            record("cat-loading-light-rtl", 5)

        # 2. Idle: motion reduced, and the page covered.
        if step("idle"):
            start_catalog(CAT, "--es section Loading", "--ez reduceMotion true")
            gfx(CAT, "cat-loading-reduce-motion-idle-5s", 5)
            burst("cat-loading-reduce-motion", 3)
            start_catalog(CAT, "--es section Loading")
            sh("input keyevent KEYCODE_HOME")
            time.sleep(2)
            before = cpu_ticks(CAT)
            gfx(CAT, "cat-loading-covered-idle-5s", 5)
            after = cpu_ticks(CAT)
            say(f"covered: process CPU ticks over 5 s: {None if before is None or after is None else after - before}")
            # The system's "Remove animations" setting, which the app follows (the catalog overrides it).
            put("global", "animator_duration_scale", "0")
            time.sleep(1)
            cold_start("ptr-cold-light-reduce-motion", 4)
            put("global", "animator_duration_scale", initial["global/animator_duration_scale"])
            time.sleep(1)

        # 3. The Skeletons group on the Feedback page, as EVIDENCE.md recorded it, after and before.
        if step("feedback"):
            swipes = None
            for theme in ("light", "dark"):
                extras = ["--es section Feedback"] + (["--es theme dark"] if theme == "dark" else [])
                start_catalog(CAT, *extras)
                made = scroll_heading_to("Skeletons", 700)
                swipes = swipes or made
                time.sleep(1.5)
                screencap(f"cat-skel-{theme}")
                record(f"cat-skel-{theme}", 6)
                if theme == "light":
                    gfx(CAT, "cat-skel-light-10s", 10)
            start_catalog(CAT_OLD, "--es section Feedback")
            scroll_heading_to("Skeletons", 700, fallback_swipes=swipes or ())
            time.sleep(1.5)
            screencap("old-cat-skel-light")
            gfx(CAT_OLD, "old-cat-skel-light-10s", 10)
            sh(f"am force-stop {CAT_OLD}")

        # 4. Cold start of the sample list with its data cleared.
        if step("cold"):
            for run in ("", "-2"):
                cold_start(f"ptr-cold-light{run}")
            sh("cmd uimode night yes")
            time.sleep(2)
            for run in ("", "-2"):
                cold_start(f"ptr-cold-dark{run}")
            sh("cmd uimode night no")
            time.sleep(2)
            put("system", "font_scale", "1.5")
            time.sleep(1.5)
            cold_start("ptr-cold-font150-light")
            put("system", "font_scale", initial["system/font_scale"] if initial["system/font_scale"] != "null" else "1.0")
            time.sleep(1.5)
            put("system", "user_rotation", "1")
            time.sleep(2)
            cold_start("ptr-cold-landscape-light", size="1600x720")
            put("system", "user_rotation", "0")
            time.sleep(2)

        # 5. The routed bottom strip (K3, old shell) during a pull, and a state view at 1.5x offline (K7).
        if step("routed"):
            sh(f"am force-stop {PTR}")
            sh(f"am start -W -n {PTR_ACTIVITY}")
            time.sleep(4)
            screencap("ptr-light-loaded")
            record("ptr-light-pull", 4, lambda: sh("input swipe 540 960 540 1760 1400"))
            time.sleep(2)
            put("system", "font_scale", "1.5")
            sh("cmd connectivity airplane-mode enable")
            time.sleep(3)
            sh(f"am force-stop {PTR}")
            sh(f"pm clear {PTR}")
            sh(f"am start -W -n {PTR_ACTIVITY}")
            time.sleep(5)
            screencap("ptr-offline-font150")
            sh("cmd connectivity airplane-mode disable")
            put("system", "font_scale", initial["system/font_scale"] if initial["system/font_scale"] != "null" else "1.0")
            time.sleep(4)
        return 0
    finally:
        restore(initial)


if __name__ == "__main__":
    sys.exit(main())
