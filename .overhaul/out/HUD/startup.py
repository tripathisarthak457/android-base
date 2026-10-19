"""What the overlay costs a cold start while hidden: devRelease of this branch against the same spec from
wp/WP06 (no perfhud), both installed fresh and compiled with their own profile as perf.sh does, launched with
no extra (as a developer would) and so hidden by default. The order is base, HUD, base, HUD, so drift
lands on both and the HUD build is what stays installed.

  bin/locked adb python -I out/HUD/startup.py --hud <apk> --base <apk> --out perf/HUD/startup-f1 [--launches 15]
"""

import argparse
import json
import os
import re
import statistics
import subprocess
import time

PACKAGE = "com.example.hud.everything.dev"


def adb(serial, *args, timeout=180):
    done = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=timeout,
                          encoding="utf-8", errors="replace")
    return done.stdout + done.stderr


def install(serial, apk):
    adb(serial, "uninstall", PACKAGE)
    print(adb(serial, "install", "-r", "-t", "-d", apk).strip(), flush=True)
    adb(serial, "shell", "am", "broadcast", "-a", "androidx.profileinstaller.action.INSTALL_PROFILE",
        "-n", f"{PACKAGE}/androidx.profileinstaller.ProfileInstallReceiver")
    adb(serial, "shell", "am", "force-stop", PACKAGE)
    adb(serial, "shell", "cmd", "package", "compile", "-f", "-m", "speed-profile", PACKAGE)
    resolved = adb(serial, "shell", "cmd", "package", "resolve-activity", "--brief", "-a",
                   "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", PACKAGE)
    return resolved.strip().splitlines()[-1].strip()


def cold_starts(serial, component, launches):
    times = []
    for _ in range(launches):
        adb(serial, "shell", "am", "force-stop", PACKAGE)
        time.sleep(1.5)
        out = adb(serial, "shell", "am", "start", "-W", "-S", "-n", component)
        match = re.search(r"TotalTime:\s*(\d+)", out)
        if match:
            times.append(int(match.group(1)))
        time.sleep(2.0)
    return times


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="9a7c74f4")
    parser.add_argument("--hud", required=True)
    parser.add_argument("--base", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--launches", type=int, default=15)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    runs = {"hud": [], "base": []}
    for label, apk in (("base", args.base), ("hud", args.hud), ("base", args.base), ("hud", args.hud)):
        component = install(args.serial, apk)
        times = cold_starts(args.serial, component, args.launches)
        runs[label].append(times)
        print(label, times, flush=True)
    result = {"package": PACKAGE, "launches_per_install": args.launches, "runs": runs}
    for label, sets in runs.items():
        every = [t for times in sets for t in times]
        result[label] = {"median_ms": statistics.median(every), "mean_ms": round(statistics.mean(every), 1),
                         "count": len(every)}
    result["hud_minus_base_median_ms"] = result["hud"]["median_ms"] - result["base"]["median_ms"]
    with open(os.path.join(args.out, "result.json"), "w", encoding="utf-8") as handle:
        json.dump(result, handle, indent=2)
    print(json.dumps({k: v for k, v in result.items() if k != "runs"}, indent=2))


if __name__ == "__main__":
    main()
