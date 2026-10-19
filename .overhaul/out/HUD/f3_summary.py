"""Fix run 3's phone results as the handoff's tables, and perf/HUD/measurements.json updated with them.

  python -I out/HUD/f3_summary.py <head commit>

Besides the per-journey tables, it reads per launch: janky scroll frames (the overlay's cost shows there
first) and the main-thread p99 of scroll frames (SyncQueued - AnimationStart, as parse_framestats reads UI
time), which the review asked about.
"""

import glob
import json
import os
import re
import sys

P = "D:/projects/tools/.overhaul/perf/HUD"
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04/templates/android/tools/perf")
import parse_framestats  # noqa: E402
import parse_gfx  # noqa: E402


def load(path):
    path = os.path.join(P, path)
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def segments(directory, mode):
    """(counter, name, text) of every segment file of [mode], in the order they were taken."""
    found = []
    for path in glob.glob(os.path.join(P, directory, f"{mode}_*_*.txt")):
        match = re.match(rf"{mode}_(\d+)_(\w+)\.txt$", os.path.basename(path))
        with open(path, encoding="utf-8") as handle:
            found.append((int(match.group(1)), match.group(2), handle.read()))
    return sorted(found)


def per_launch(directory, mode):
    launches = []
    for _, name, text in segments(directory, mode):
        if name == "idle":
            launches.append({"janky_scroll": 0, "scroll_ui": []})
        elif name == "scroll" and launches:
            launches[-1]["janky_scroll"] += parse_gfx.parse(text)["janky"]
            launches[-1]["scroll_ui"] += [parse_framestats.ui_ms(r) for r in parse_framestats.load(text)]
    return launches


def screen_lines(directory):
    """How many frames each screen line counted, summed over the shown launches' PerfHud logs."""
    counted = {}
    for path in glob.glob(os.path.join(P, directory, "shown_*_perfhud.txt")):
        with open(path, encoding="utf-8") as handle:
            for match in re.finditer(r"PerfHud\s*: screen=(\S+) frames=(\d+)", handle.read()):
                counted[match.group(1)] = counted.get(match.group(1), 0) + int(match.group(2))
    return counted


def p99(values):
    values = sorted(values)
    return round(values[int(0.99 * (len(values) - 1))], 2) if values else None


def valid_total_means(directory):
    kinds = {"scroll": ("scroll",), "tabs": ("tab",), "pushpop": ("push", "pop"),
             "all": ("scroll", "tab", "push", "pop")}
    means = {}
    for mode in ("hidden", "shown"):
        texts = segments(directory, mode)
        for journey, names in kinds.items():
            rows = [row for _, name, text in texts if name in names for row in parse_framestats.load(text)]
            totals = [(r["FrameCompleted"] - r["IntendedVsync"]) / 1e6 for r in rows
                      if r["FrameCompleted"] > r["IntendedVsync"]]
            means[(mode, journey)] = sum(totals) / len(totals) if totals else 0.0
    return means


def table(result, directory):
    means = valid_total_means(directory)
    rows = ["| Journey | Frames hidden / shown | Janky % hidden → shown | Δ points | UI thread mean Δ | Frame total mean Δ |",
            "|---|---|---|---|---|---|"]
    for name, label in (("scroll", "Scroll"), ("tabs", "Tabs"), ("pushpop", "Push/pop"), ("all", "**All**")):
        hidden, shown = result["hidden"][name], result["shown"][name]
        delta = result["shown_minus_hidden"][name]
        rows.append(f"| {label} | {hidden['frames']} / {shown['frames']} | {hidden['janky_pct']:.2f} → "
                    f"{shown['janky_pct']:.2f} | {delta['janky_pp']:+.2f} | {delta['ui_mean_ms']:+.3f} ms | "
                    f"{means[('shown', name)] - means[('hidden', name)]:+.3f} ms |")
    launches = {mode: per_launch(directory, mode) for mode in ("hidden", "shown")}
    rows.append("")
    for mode in ("hidden", "shown"):
        janky = [launch["janky_scroll"] for launch in launches[mode]]
        everything = [ui for launch in launches[mode] for ui in launch["scroll_ui"]]
        rows.append(f"{mode}: janky scroll frames per launch {janky} (mean {sum(janky) / len(janky):.1f}); "
                    f"main-thread scroll p99 {p99(everything)} ms, per launch "
                    f"{[p99(launch['scroll_ui']) for launch in launches[mode]]}")
    rows.append(f"Idle frames in 5 s, hidden {result['hidden']['idle_frames']}, shown {result['shown']['idle_frames']}; "
                f"PerfHud window lines per launch, hidden {result['hidden']['perfhud_window_lines']}, "
                f"shown {result['shown']['perfhud_window_lines']}.")
    return "\n".join(rows), means, launches


def main():
    head = sys.argv[1]
    with open(os.path.join(P, "measurements.json"), encoding="utf-8") as handle:
        old = json.load(handle)
    new = {"head": head, "device": old.get("device"), "method": old.get("method"), "limits": old.get("limits")}
    for name, spec in (
        ("standard", "standard preset less auth/onboarding (out/HUD/specs/hud-standard.json): no analytics, so "
                     "the overlay's own JankStats; hidden has no frame listener at all"),
        ("everything", "everything preset less auth/onboarding/googlesignin (out/HUD/specs/hud-everything.json): "
                       "analytics' JankStats shared; the overlay keeps each frame's screen itself and puts no "
                       "state on the window, so analytics keys every frame idle whether or not it records"),
    ):
        result = load(f"overhead-{name}-f3/result.json")
        if not result:
            continue
        text, means, launches = table(result, f"overhead-{name}-f3")
        print(f"## {name}\n\n{text}\n")
        print(f"Frames per screen, summed over the shown launches' screen lines: {screen_lines(f'overhead-{name}-f3')}\n")
        delta = result["shown_minus_hidden"]
        new[f"fix3_{name}"] = {
            "build": f"{spec}; devRelease arm64 from {head}, installed fresh, profile installed, compiled speed-profile",
            "shown_minus_hidden": delta,
            "frame_total_mean_valid_ms": {f"{mode}_{journey}": round(value, 3) for (mode, journey), value in means.items()},
            "janky_scroll_per_launch": {mode: [l["janky_scroll"] for l in launches[mode]] for mode in launches},
            "ui_scroll_p99_ms": {mode: p99([u for l in launches[mode] for u in l["scroll_ui"]]) for mode in launches},
            "hidden_all": result["hidden"]["all"],
            "shown_all": result["shown"]["all"],
            "idle_frames_5s": {"hidden": result["hidden"]["idle_frames"], "shown": result["shown"]["idle_frames"]},
            "per_journey": {mode: {j: result[mode][j] for j in ("scroll", "tabs", "pushpop")}
                            for mode in ("hidden", "shown")},
            "verdict": "pass" if delta["all"]["janky_pp"] <= 0.3 and (delta["all"]["ui_mean_ms"] or 0) < 0.5 else "fail",
        }
    for check, file in (("fix3_listener", "checks-f3/checks-listener.json"), ("fix3_adb", "checks-f3/checks-adb.json")):
        value = load(file)
        if value:
            new[check] = value
            print(f"## {check}\n", json.dumps(value, indent=1)[:2500])
    for key, value in old.items():
        if key not in new and key not in ("head",):
            new[key] = value
    with open(os.path.join(P, "measurements.json"), "w", encoding="utf-8") as handle:
        json.dump(new, handle, indent=1)
        handle.write("\n")


if __name__ == "__main__":
    main()
