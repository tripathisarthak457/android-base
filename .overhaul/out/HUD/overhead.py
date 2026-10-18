"""What the performance overlay costs the app, on the phone.

Alternating launches of one release-like build with the overlay hidden and shown (the launch extra
`perf.hud=off|on`), each running the same journey, measured the way tools/perf/perf.sh measures:
`dumpsys gfxinfo` reset before and dumped after each step, parsed with WP04's parse_gfx and
parse_framestats. The order alternates (hidden, shown, shown, hidden, ...) so drift in temperature
or background load lands on both sides.

Run under the adb lock:
  bin/locked adb python -I out/HUD/overhead.py --package com.example.hud.everything.dev --rounds 4 --out perf/HUD/x
"""

import argparse
import json
import os
import re
import statistics
import subprocess
import sys
import time

PERF = "D:/projects/tools/.overhaul/wt/WP04/templates/android/tools/perf"
sys.path.insert(0, PERF)
import parse_framestats  # noqa: E402
import parse_gfx  # noqa: E402



def adb(serial, *args, timeout=120):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=timeout,
                            encoding="utf-8", errors="replace")
    return result.stdout


def shell(serial, command, timeout=120):
    return adb(serial, "shell", command, timeout=timeout)


def ui_nodes(serial):
    shell(serial, "uiautomator dump /sdcard/hud_ui.xml >/dev/null 2>&1")
    xml = shell(serial, "cat /sdcard/hud_ui.xml")
    nodes = []
    for match in re.finditer(r"<node ([^>]*)>", xml):
        attrs = dict(re.findall(r'(\S+?)="([^"]*)"', match.group(1)))
        bounds = re.findall(r"\d+", attrs.get("bounds", ""))
        if len(bounds) == 4:
            x1, y1, x2, y2 = map(int, bounds)
            attrs["center"] = ((x1 + x2) // 2, (y1 + y2) // 2)
            attrs["box"] = (x1, y1, x2, y2)
            nodes.append(attrs)
    return nodes


def find_text(nodes, text):
    for node in nodes:
        if node.get("text") == text or node.get("content-desc") == text:
            return node
    return None


class Journey:
    def __init__(self, serial, package, out):
        self.serial = serial
        self.package = package
        self.out = out
        self.counter = 0
        resolved = shell(serial, f"cmd package resolve-activity --brief -a android.intent.action.MAIN "
                                 f"-c android.intent.category.LAUNCHER {package}").strip().splitlines()
        self.component = resolved[-1].strip()
        if "/" not in self.component or "ResolverActivity" in self.component:
            raise SystemExit(f"{package}: no single launcher activity ({self.component}); measure a release build")

    def launch(self, hud):
        shell(self.serial, f"am force-stop {self.package}")
        time.sleep(0.8)
        shell(self.serial, "logcat -c")
        shell(self.serial, f"am start -W -n {self.component} --es perf.hud {hud}")
        time.sleep(5.0)

    def segment(self, mode, name, command, settle):
        self.counter += 1
        shell(self.serial, f"dumpsys gfxinfo {self.package} reset >/dev/null" + (f"; {command}" if command else ""))
        time.sleep(settle)
        raw = shell(self.serial, f"dumpsys gfxinfo {self.package} framestats")
        with open(os.path.join(self.out, f"{mode}_{self.counter:04d}_{name}.txt"), "w", encoding="utf-8") as f:
            f.write(raw)
        return parse_gfx.parse(raw), parse_framestats.load(raw)

    def run(self, mode, sink):
        width, height = 1080, 2400
        x, low, high = width // 2, int(height * 0.75), int(height * 0.30)
        gfx, _ = self.segment(mode, "idle", "", 5.0)
        sink["idle_frames"].append(gfx["frames"])
        for _ in range(6):
            sink["scroll"].append(self.segment(mode, "scroll", f"input swipe {x} {low} {x} {high} 140", 1.0))
        for _ in range(6):
            sink["scroll"].append(self.segment(mode, "scroll", f"input swipe {x} {high} {x} {low} 140", 1.0))
        nodes = ui_nodes(self.serial)
        tabs = [find_text(nodes, label) for label in ("Feed", "Search", "Profile", "Settings", "Home")]
        for tab in tabs:
            if tab is None:
                continue
            tx, ty = tab["center"]
            sink["tabs"].append(self.segment(mode, "tab", f"input tap {tx} {ty}", 1.0))
        # Push the first row of the list on Home, then Back.
        nodes = ui_nodes(self.serial)
        rows = [n for n in nodes if n.get("clickable") == "true" and n["box"][1] > height * 0.25
                and n["box"][3] < height * 0.8 and (n["box"][2] - n["box"][0]) > width * 0.6]
        if rows:
            rx, ry = rows[0]["center"]
            for _ in range(4):
                sink["pushpop"].append(self.segment(mode, "push", f"input tap {rx} {ry}", 1.2))
                sink["pushpop"].append(self.segment(mode, "pop", "input keyevent 4", 1.2))
        log = shell(self.serial, "logcat -d -s PerfHud:I")
        sink["perfhud_lines"].append(len([l for l in log.splitlines() if "PerfHud" in l and "window=" in l]))


def summarize(segments):
    dumps = [gfx for gfx, _ in segments]
    rows = [row for _, frames in segments for row in frames]
    combined = parse_gfx.combine(dumps) if dumps else {}
    ui = [parse_framestats.ui_ms(row) for row in rows]
    total = [(row["FrameCompleted"] - row["IntendedVsync"]) / 1e6 for row in rows]
    return {
        "frames": combined.get("frames", 0),
        "janky": combined.get("janky", 0),
        "janky_pct": combined.get("janky_pct", 0.0),
        "p50_ms": combined.get("p50_ms"),
        "p90_ms": combined.get("p90_ms"),
        "p99_ms": combined.get("p99_ms"),
        "gpu_p90_ms": combined.get("gpu_p90_ms"),
        "framestats_rows": len(rows),
        "ui_mean_ms": round(statistics.mean(ui), 3) if ui else None,
        "ui_p90_ms": round(sorted(ui)[int(0.9 * (len(ui) - 1))], 3) if ui else None,
        "total_mean_ms": round(statistics.mean(total), 3) if total else None,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--package", required=True)
    parser.add_argument("--serial", default="9a7c74f4")
    parser.add_argument("--rounds", type=int, default=4)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    journey = Journey(args.serial, args.package, args.out)
    sinks = {mode: {"idle_frames": [], "scroll": [], "tabs": [], "pushpop": [], "perfhud_lines": []}
             for mode in ("hidden", "shown")}
    order = []
    for round_index in range(args.rounds):
        pair = ("hidden", "shown") if round_index % 2 == 0 else ("shown", "hidden")
        for mode in pair:
            order.append(mode)
            journey.launch("off" if mode == "hidden" else "on")
            journey.run(mode, sinks[mode])
            print(f"round {round_index + 1} {mode} done", flush=True)
    result = {"order": order, "package": args.package}
    for mode, sink in sinks.items():
        result[mode] = {
            "idle_frames": sink["idle_frames"],
            "perfhud_window_lines": sink["perfhud_lines"],
            "scroll": summarize(sink["scroll"]),
            "tabs": summarize(sink["tabs"]),
            "pushpop": summarize(sink["pushpop"]),
            "all": summarize(sink["scroll"] + sink["tabs"] + sink["pushpop"]),
        }
    delta = {}
    for journey_name in ("scroll", "tabs", "pushpop", "all"):
        hidden, shown = result["hidden"][journey_name], result["shown"][journey_name]
        delta[journey_name] = {
            "janky_pp": round(shown["janky_pct"] - hidden["janky_pct"], 2),
            "ui_mean_ms": None if hidden["ui_mean_ms"] is None or shown["ui_mean_ms"] is None
            else round(shown["ui_mean_ms"] - hidden["ui_mean_ms"], 3),
            "total_mean_ms": None if hidden["total_mean_ms"] is None or shown["total_mean_ms"] is None
            else round(shown["total_mean_ms"] - hidden["total_mean_ms"], 3),
        }
    result["shown_minus_hidden"] = delta
    with open(os.path.join(args.out, "result.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    print(json.dumps(result["shown_minus_hidden"], indent=2))
    print(json.dumps({m: result[m]["all"] for m in ("hidden", "shown")}, indent=2))


if __name__ == "__main__":
    main()
