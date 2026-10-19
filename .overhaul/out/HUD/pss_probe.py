"""Does reading the app's PSS while it scrolls make frames miss their deadline? (Run 3 open question.)

The overlay samples PSS with Debug.getMemoryInfo every 15 s while recording, which walks every mapping
of the process. `dumpsys meminfo <package>` makes the app run the same call on one of its binder threads,
so with the overlay hidden (no sampler at all) this compares scroll flings alone with flings while the
app reads its PSS, using the same gfxinfo reset/dump per segment as overhead.py.

  bin/locked adb python -I out/HUD/pss_probe.py --out perf/HUD/pss-probe [--rounds 4]
"""

import argparse
import json
import os
import subprocess
import sys
import time

sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04/templates/android/tools/perf")
import parse_gfx  # noqa: E402

PACKAGE = "com.example.hud.everything.dev"
MAIN = f"{PACKAGE}/com.example.hud.everything.MainActivity"
FLING_UP = "input swipe 540 1800 540 720 140"
FLING_DOWN = "input swipe 540 720 540 1800 140"


def shell(serial, command, timeout=120):
    done = subprocess.run(["adb", "-s", serial, "shell", command], capture_output=True, text=True,
                          timeout=timeout, encoding="utf-8", errors="replace")
    return done.stdout


def segment(serial, out, name, command, settle=1.2):
    shell(serial, f"dumpsys gfxinfo {PACKAGE} reset >/dev/null; {command}")
    time.sleep(settle)
    raw = shell(serial, f"dumpsys gfxinfo {PACKAGE} framestats")
    with open(os.path.join(out, name + ".txt"), "w", encoding="utf-8") as f:
        f.write(raw)
    return parse_gfx.parse(raw)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="9a7c74f4")
    parser.add_argument("--rounds", type=int, default=4)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    s = args.serial
    shell(s, f"am force-stop {PACKAGE}")
    time.sleep(0.8)
    shell(s, f"am start -W -n {MAIN} --es perf.hud off")
    time.sleep(5.0)
    totals = {"alone": [0, 0], "with_pss": [0, 0]}
    timings = []
    counter = 0
    for round_index in range(args.rounds):
        for mode in (("alone", "with_pss") if round_index % 2 == 0 else ("with_pss", "alone")):
            for fling in (FLING_UP, FLING_DOWN) * 4:
                counter += 1
                if mode == "with_pss":
                    # `input` takes about 0.3 s to start, so the reads land while the fling is drawn, as a
                    # sampler tick can; two reads per fling, the first one timed.
                    command = f"{fling} & sleep 0.3; t0=$(date +%s%N); dumpsys meminfo {PACKAGE} >/dev/null; " \
                              f"t1=$(date +%s%N); dumpsys meminfo {PACKAGE} >/dev/null; " \
                              f"echo $(( (t1 - t0) / 1000000 )) > /data/local/tmp/hud_pss_ms; wait"
                else:
                    command = fling
                gfx = segment(s, args.out, f"{counter:03d}_{mode}", command)
                totals[mode][0] += gfx["frames"]
                totals[mode][1] += gfx["janky"]
                if mode == "with_pss":
                    timings.append(int(shell(s, "cat /data/local/tmp/hud_pss_ms").strip() or -1))
            print(f"round {round_index + 1} {mode}: {totals[mode]}", flush=True)
    shell(s, "rm -f /data/local/tmp/hud_pss_ms")
    result = {
        mode: {"frames": f, "janky": j, "janky_pct": round(100.0 * j / f, 2) if f else None}
        for mode, (f, j) in totals.items()
    }
    result["meminfo_ms"] = {"each": timings, "median": sorted(timings)[len(timings) // 2] if timings else None}
    with open(os.path.join(args.out, "result.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
