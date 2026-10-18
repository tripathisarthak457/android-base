"""Phone checks of the performance overlay beyond its cost (overhead.py measures that).

1. Hidden adds no frame listener: on a build without analytics, JankStats is the overlay's alone,
   and its thread ("FrameMetricsAggregator", shown as "FrameMetricsAgg") exists only once a
   JankStats has listened to a window. Launched hidden, the process must not have it; launched
   shown, it must.
2. The adb path: switch recording on through the unexported receiver (adb reaches it through run-as on a
   debuggable build), use the app, export, and pull the report the broadcast names; without run-as the
   system must refuse it.
3. The log: the PerfHud lines that recording wrote.
4. The prod release (--prod-before/--prod-after): launched with perf.hud=on, no badge or pill on screen, no
   PerfHud line, and no thread the same build from wp/WP06 did not have.

  bin/locked adb python -I out/HUD/device_checks.py --standard <pkg> --everything <pkg> --out perf/HUD/checks
"""

import argparse
import json
import os
import re
import subprocess
import time

RECEIVER = ".core.perfhud.report.ReportReceiver"
ACTIONS = ".core.perfhud.action."


def run(serial, *args, timeout=120):
    """stdout, then stderr: run-as and am say why they refused on stderr."""
    done = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=timeout,
                          encoding="utf-8", errors="replace")
    return done.stdout + done.stderr


def shell(serial, command, timeout=120):
    return run(serial, "shell", command, timeout=timeout)


def launcher(serial, package):
    """The launcher activity, as package/class: the class is in the code's package, not the app id's."""
    output = shell(serial, f"cmd package resolve-activity --brief -a android.intent.action.MAIN "
                           f"-c android.intent.category.LAUNCHER {package}").strip().splitlines()
    return output[-1].strip()




def launch(serial, package, code, hud=None):
    shell(serial, f"am force-stop {package}")
    time.sleep(0.8)
    extra = f" --es perf.hud {hud}" if hud else ""
    # Named in full: a debug build also has LeakCanary's launcher activity, which makes resolving one ambiguous.
    shell(serial, f"am start -W -n {package}/{code}.MainActivity{extra}")
    time.sleep(4.0)


def scroll(serial, times=4):
    for _ in range(times):
        shell(serial, "input swipe 540 1800 540 700 140")
        time.sleep(0.7)
        shell(serial, "input swipe 540 700 540 1800 140")
        time.sleep(0.7)


def threads(serial, package):
    """The process's thread names: the last column of `ps -T`, which the kernel cuts to 15 characters."""
    pid = shell(serial, f"pidof {package}").strip()
    listing = shell(serial, f"ps -T -p {pid}") if pid else ""
    rows = [line.split(None, 9) for line in listing.splitlines()[1:]]
    return pid, [row[-1].strip() for row in rows if len(row) == 10]


def listener_check(serial, package, code):
    """Launched hidden and then shown: JankStats' thread, the PerfHud lines and the threads that differ."""
    result = {}
    for mode in ("off", "on"):
        shell(serial, "logcat -c")
        launch(serial, package, code, mode)
        scroll(serial)
        time.sleep(6.0)
        pid, names = threads(serial, package)
        log = shell(serial, "logcat -d -s PerfHud:I")
        result[mode] = {
            "pid": pid,
            "frame_metrics_thread": "FrameMetricsAgg" in names,
            "threads": len(names),
            "thread_names": normalised(names),
            "perfhud_lines": len([line for line in log.splitlines() if "PerfHud" in line]),
        }
    result["threads_only_on"] = sorted(set(result["on"]["thread_names"]) - set(result["off"]["thread_names"]))
    result["threads_only_off"] = sorted(set(result["off"]["thread_names"]) - set(result["on"]["thread_names"]))
    return result


def adb_path_check(serial, package, code, out):
    receiver = f"{package}/{code}{RECEIVER}"
    launch(serial, package, code)
    shell(serial, "logcat -c")
    # The receiver is not exported: adb reaches it as the app's own uid, through run-as.
    as_app = f"run-as {package} am broadcast --user 0 -n {receiver}"
    record = shell(serial, f"{as_app} -a {code}{ACTIONS}RECORD --ez recording true")
    time.sleep(1.0)
    scroll(serial, times=6)
    time.sleep(6.0)
    export = shell(serial, f"{as_app} -a {code}{ACTIONS}EXPORT", timeout=60)
    # Without run-as the system must refuse it: nothing but the app itself reaches the receiver.
    refused = shell(serial, f"am broadcast -n {receiver} -a {code}{ACTIONS}EXPORT", timeout=60)
    match = re.search(r'data="([^"]+)"', export)
    pulled = []
    if match:
        json_path = match.group(1)
        for path in (json_path, json_path[: -len(".json")] + ".md"):
            target = os.path.join(out, os.path.basename(path))
            run(serial, "pull", path, target)
            pulled.append(target if os.path.exists(target) else None)
    log = shell(serial, "logcat -d -s PerfHud:I")
    with open(os.path.join(out, "perfhud-logcat.txt"), "w", encoding="utf-8") as f:
        f.write(log)
    off = shell(serial, f"{as_app} -a {code}{ACTIONS}RECORD --ez recording false")
    return {
        "record_broadcast": record.strip().splitlines()[-3:] if record.strip() else [],
        "export_broadcast": export.strip().splitlines()[-3:] if export.strip() else [],
        "pulled": pulled,
        "perfhud_lines": len([line for line in log.splitlines() if "PerfHud" in line]),
        "record_off_broadcast": off.strip().splitlines()[-3:] if off.strip() else [],
        "without_run_as": refused.strip().splitlines()[-3:] if refused.strip() else [],
        "without_run_as_delivered": 'data="' in refused,
    }


def release_refusal(serial, package, code, apk):
    """On devRelease (not debuggable) neither route reaches the receiver: run-as refuses, and the
    receiver is not exported."""
    run(serial, "uninstall", package)
    run(serial, "install", "-r", "-t", "-d", apk, timeout=300)
    receiver = f"{package}/{code}{RECEIVER}"
    launch(serial, package, code)
    via_run_as = shell(serial, f"run-as {package} am broadcast --user 0 -n {receiver} -a {code}{ACTIONS}EXPORT")
    direct = shell(serial, f"am broadcast -n {receiver} -a {code}{ACTIONS}EXPORT")
    return {
        "run_as": via_run_as.strip().splitlines()[-3:],
        "direct": direct.strip().splitlines()[-3:],
        "reached": 'data="' in via_run_as or 'data="' in direct,
    }


def normalised(names):
    """Thread names without the numbers pools and binder threads carry, so two runs compare."""
    return sorted({re.sub(r"[-_:#]?\d+", "#", name) for name in names})


def ui_dump(serial):
    shell(serial, "uiautomator dump /sdcard/hud_prod_ui.xml >/dev/null 2>&1")
    return shell(serial, "cat /sdcard/hud_prod_ui.xml")


def prod_run(serial, package, code, apk, out, label):
    run(serial, "uninstall", package)
    run(serial, "install", "-r", "-t", "-d", apk, timeout=300)
    launch(serial, package, code, hud="on")
    scroll(serial)
    time.sleep(2.0)
    pid, names = threads(serial, package)
    xml = ui_dump(serial)
    with open(os.path.join(out, f"prod-{label}-ui.xml"), "w", encoding="utf-8") as f:
        f.write(xml)
    with open(os.path.join(out, f"prod-{label}.png"), "wb") as f:
        subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], stdout=f, timeout=60)
    log = shell(serial, "logcat -d -s PerfHud:I")
    return {
        "pid": pid,
        "threads": len(names),
        "thread_names": normalised(names),
        "raw_thread_names": sorted(names),
        "badge_or_pill_on_screen": bool(re.search(r"devtools_badge|fps|janky|PROD", xml)),
        "perfhud_log_lines": len([line for line in log.splitlines() if "PerfHud" in line]),
    }


def prod_check(serial, package, code, base_apk, hud_apk, out):
    """The prod release before this package (wp/WP06) and after it: no badge, no pill, no thread more."""
    shell(serial, "logcat -c")
    before = prod_run(serial, package, code, base_apk, out, "before")
    shell(serial, "logcat -c")
    after = prod_run(serial, package, code, hud_apk, out, "after")
    run(serial, "uninstall", package)
    return {
        "before": before,
        "after": after,
        "threads_only_after": sorted(set(after["thread_names"]) - set(before["thread_names"])),
        "threads_only_before": sorted(set(before["thread_names"]) - set(after["thread_names"])),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="9a7c74f4")
    parser.add_argument("--standard", required=True)
    parser.add_argument("--standard-code", default="com.example.hud.standard")
    parser.add_argument("--everything", required=True)
    parser.add_argument("--everything-code", default="com.example.hud.everything")
    parser.add_argument("--out", required=True)
    parser.add_argument("--listener-only", action="store_true")
    parser.add_argument("--adb-only", action="store_true")
    parser.add_argument("--release-apk", help="the devRelease APK, for the adb path's refusal")
    parser.add_argument("--prod-before", help="the prod release APK built from wp/WP06")
    parser.add_argument("--prod-after", help="the prod release APK built from wp/HUD")
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    result = {}
    if args.prod_before and args.prod_after:
        result["prod_release"] = prod_check(args.serial, args.everything_code, args.everything_code,
                                            args.prod_before, args.prod_after, args.out)
        with open(os.path.join(args.out, "checks-prod.json"), "w", encoding="utf-8") as f:
            json.dump(result, f, indent=2)
        print(json.dumps(result, indent=2))
        return
    if not args.adb_only:
        result["listener_standard"] = listener_check(args.serial, args.standard, args.standard_code)
        result["listener_everything"] = listener_check(args.serial, args.everything, args.everything_code)
    if not args.listener_only:
        result["adb_path"] = adb_path_check(args.serial, args.everything, args.everything_code, args.out)
        if args.release_apk:
            result["adb_path_release"] = release_refusal(args.serial, args.everything, args.everything_code,
                                                         args.release_apk)
    name = "checks-listener.json" if args.listener_only else "checks-adb.json" if args.adb_only else "checks.json"
    with open(os.path.join(args.out, name), "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
