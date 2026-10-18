"""Phone checks of the performance overlay beyond its cost (overhead.py measures that).

1. Hidden adds no frame listener: on a build without analytics, JankStats is the overlay's alone,
   and its thread ("FrameMetricsAggregator", shown as "FrameMetricsAgg") exists only once a
   JankStats has listened to a window. Launched hidden, the process must not have it; launched
   shown, it must.
2. The adb path: switch recording on through the DUMP-guarded receiver, use the app, export, and
   pull the report the broadcast names.
3. The log: the PerfHud lines that recording wrote.

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
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=timeout,
                          encoding="utf-8", errors="replace").stdout


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
    result = {}
    for mode in ("off", "on"):
        launch(serial, package, code, mode)
        scroll(serial)
        pid, names = threads(serial, package)
        result[mode] = {
            "pid": pid,
            "frame_metrics_thread": "FrameMetricsAgg" in names,
            "threads": len(names),
            "names": names,
        }
    result["hidden_adds_no_listener"] = (
        not result["off"]["frame_metrics_thread"] and result["on"]["frame_metrics_thread"]
    )
    return result


def adb_path_check(serial, package, code, out):
    receiver = f"{package}/{code}{RECEIVER}"
    launch(serial, package, code)
    shell(serial, "logcat -c")
    # The receiver is not exported: adb reaches it as the app's own uid, through run-as.
    as_app = f"run-as {package} am broadcast -n {receiver}"
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
        "record_broadcast": record.strip().splitlines()[-1:] if record.strip() else [],
        "export_broadcast": export.strip().splitlines()[-1:] if export.strip() else [],
        "pulled": pulled,
        "perfhud_lines": len([line for line in log.splitlines() if "PerfHud" in line]),
        "record_off_broadcast": off.strip().splitlines()[-1:] if off.strip() else [],
        "without_run_as": refused.strip().splitlines()[-1:] if refused.strip() else [],
        "without_run_as_delivered": 'data="' in refused,
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
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    result = {}
    if not args.adb_only:
        result["listener_standard"] = listener_check(args.serial, args.standard, args.standard_code)
        result["listener_everything"] = listener_check(args.serial, args.everything, args.everything_code)
    if not args.listener_only:
        result["adb_path"] = adb_path_check(args.serial, args.everything, args.everything_code, args.out)
    name = "checks-listener.json" if args.listener_only else "checks-adb.json" if args.adb_only else "checks.json"
    with open(os.path.join(args.out, name), "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
