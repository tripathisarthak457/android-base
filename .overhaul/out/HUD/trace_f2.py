"""One system trace of the overlay shown, to name what makes about 1 % of scroll frames take ~2 ms more main-
thread time in some launches (with the overlay hidden too, less often). Changes no device setting.

  bin/locked adb python -I out/HUD/trace_f2.py --package com.example.hud.standard.dev --hud on --out perf/HUD/trace-f2/shown
"""

import argparse
import subprocess
import time


def adb(serial, *args, timeout=180):
    done = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=timeout,
                          encoding="utf-8", errors="replace")
    return done.stdout + done.stderr


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="9a7c74f4")
    parser.add_argument("--package", required=True)
    parser.add_argument("--code", default="com.example.hud.standard")
    parser.add_argument("--hud", default="on")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    s, pkg = args.serial, args.package
    device_file = "/data/misc/perfetto-traces/hud-f2.pftrace"
    adb(s, "shell", f"am force-stop {pkg}")
    time.sleep(1)
    adb(s, "shell", f"am start -W -n {pkg}/{args.code}.MainActivity --es perf.hud {args.hud}")
    time.sleep(4)
    tracer = subprocess.Popen(
        ["adb", "-s", s, "shell", f"perfetto -o {device_file} -t 36s -b 96mb "
         f"sched freq idle gfx view dalvik binder_driver am wm -a {pkg}"],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    time.sleep(3)
    for _ in range(2):
        for _ in range(6):
            adb(s, "shell", "input swipe 540 1800 540 720 140")
            time.sleep(1.0)
        for _ in range(6):
            adb(s, "shell", "input swipe 540 720 540 1800 140")
            time.sleep(1.0)
    print(tracer.communicate(timeout=120)[0][-2000:])
    print(adb(s, "pull", device_file, f"{args.out}.pftrace"))
    adb(s, "shell", f"rm {device_file}")


if __name__ == "__main__":
    main()
