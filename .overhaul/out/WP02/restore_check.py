#!/usr/bin/env python3
"""
Process-death restore on a minified build: open a sample detail, leave the app, kill its process,
come back, and check that the detail (its typed key, with the id) is what the app restores, and
that Back then pops to the list. Run under the adb lock: locked adb python3 restore_check.py <apk>.
"""
import json, re, subprocess, sys, time, xml.etree.ElementTree as ET
from pathlib import Path

SERIAL = "9a7c74f4"
PKG = "com.example.wp02.restore.dev"  # the dev flavour adds .dev
OUT = Path("/home/crazyhusir/Documents/projects/tools/.overhaul/perf/WP02/restore")
ROW_TITLE = "qui est esse"  # jsonplaceholder post 2
DETAIL_MARK = "#2"

def adb(*args, check=True):
    return subprocess.run(["adb", "-s", SERIAL, *args], capture_output=True, text=True, check=check).stdout

def shell(cmd): return adb("shell", cmd, check=False)

def dump():
    shell("uiautomator dump /sdcard/wp02-ui.xml >/dev/null")
    xml = adb("exec-out", "cat /sdcard/wp02-ui.xml", check=False)
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return []
    nodes = []
    for n in root.iter("node"):
        text = n.get("text") or n.get("content-desc") or ""
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
        if text and m:
            x1, y1, x2, y2 = map(int, m.groups())
            nodes.append((text, ((x1 + x2) // 2, (y1 + y2) // 2)))
    return nodes

def texts(nodes): return [t for t, _ in nodes]

def wait_for(predicate, seconds):
    end = time.time() + seconds
    nodes = []
    while time.time() < end:
        nodes = dump()
        if predicate(texts(nodes)):
            return True, nodes
        time.sleep(0.7)
    return False, nodes

def shot(name):
    (OUT / name).write_bytes(subprocess.run(["adb", "-s", SERIAL, "exec-out", "screencap", "-p"], capture_output=True).stdout)

def pid(): return shell(f"pidof {PKG}").strip()

def main(apk):
    OUT.mkdir(parents=True, exist_ok=True)
    result = {"apk": apk, "device": shell("getprop ro.product.model").strip()}
    adb("install", "-r", "-t", apk)
    shell(f"am force-stop {PKG}")
    shell(f"pm clear {PKG}")
    adb("logcat", "-c", check=False)
    shell(f"monkey -p {PKG} -c android.intent.category.LAUNCHER 1")

    ok, nodes = wait_for(lambda t: any(x.startswith(ROW_TITLE) for x in t), 20)
    result["list_loaded"] = ok
    if not ok:
        shot("0-list-missing.png"); result["texts"] = texts(nodes); return result
    row = next(c for t, c in nodes if t.startswith(ROW_TITLE))
    shell(f"input tap {row[0]} {row[1]}")
    ok, nodes = wait_for(lambda t: DETAIL_MARK in t, 10)
    result["detail_opened"] = ok
    shot("1-detail-before.png")
    before = texts(nodes)

    shell("input keyevent KEYCODE_HOME")
    time.sleep(1.5)
    old = pid()
    shell(f"am kill {PKG}")
    time.sleep(1.0)
    result["killed"] = bool(old) and pid() == ""

    shell(f"monkey -p {PKG} -c android.intent.category.LAUNCHER 1")
    ok, nodes = wait_for(lambda t: DETAIL_MARK in t, 10)
    result["restored_detail"] = ok
    result["new_pid_differs"] = pid() not in ("", old)
    shot("2-detail-after-restore.png")
    result["texts_before"] = before[:12]
    result["texts_after"] = texts(nodes)[:12]

    shell("input keyevent 4")
    ok, nodes = wait_for(lambda t: DETAIL_MARK not in t and any(x.startswith(ROW_TITLE) for x in t), 8)
    result["back_pops_to_list"] = ok
    shot("3-list-after-back.png")
    result["crash_log"] = adb("logcat", "-d", "-b", "crash", check=False).strip()[:2000]
    result["passed"] = all(result.get(k) for k in ("list_loaded", "detail_opened", "killed", "restored_detail", "new_pid_differs", "back_pops_to_list")) and not result["crash_log"]
    shell(f"am force-stop {PKG}")
    return result

if __name__ == "__main__":
    r = main(sys.argv[1])
    (OUT / "restore.json").write_text(json.dumps(r, indent=2))
    print(json.dumps({k: v for k, v in r.items() if not k.startswith("texts")}, indent=2))
    print("RESTORE", "PASS" if r.get("passed") else "FAIL")
