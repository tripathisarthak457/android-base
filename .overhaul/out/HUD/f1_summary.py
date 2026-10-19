"""Fix run 1's phone results as the handoff's tables, and perf/HUD/measurements.json updated with them.

  python -I out/HUD/f1_summary.py <head commit>
"""

import json
import os
import sys

P = "D:/projects/tools/.overhaul/perf/HUD"


def load(path):
    path = os.path.join(P, path)
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def valid_total_means(directory):
    """Frame-total means per journey over completed frames only: result.json's counted in-flight rows."""
    import glob
    sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04/templates/android/tools/perf")
    import parse_framestats
    kinds = {"scroll": ("scroll",), "tabs": ("tab",), "pushpop": ("push", "pop"),
             "all": ("scroll", "tab", "push", "pop")}
    means = {}
    for mode in ("hidden", "shown"):
        for journey, names in kinds.items():
            rows = []
            for name in names:
                for path in glob.glob(os.path.join(P, directory, f"{mode}_*_{name}.txt")):
                    with open(path, encoding="utf-8") as handle:
                        rows += parse_framestats.load(handle.read())
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
    rows.append("")
    rows.append(f"Idle frames in 5 s, hidden {result['hidden']['idle_frames']}, shown {result['shown']['idle_frames']}; "
                f"PerfHud window lines per launch, hidden {result['hidden']['perfhud_window_lines']}, "
                f"shown {result['shown']['perfhud_window_lines']}.")
    return "\n".join(rows)


def main():
    head = sys.argv[1]
    standard = load("overhead-standard-f1/result.json")
    everything = load("overhead-everything-f1/result.json")
    startup = load("startup-f1/result.json")
    listener = load("checks-f1/checks-listener.json")
    for name, result in (("standard", standard), ("everything", everything)):
        if result:
            print(f"## {name}\n\n{table(result, f'overhead-{name}-f1')}\n")
    if startup:
        print("## startup\n", json.dumps({k: v for k, v in startup.items() if k != "runs"}, indent=1))
    if listener:
        print("## listener\n", json.dumps(listener, indent=1)[:3000])

    with open(os.path.join(P, "measurements.json"), encoding="utf-8") as handle:
        old = json.load(handle)
    if "run3_da30c7f" not in old:
        kept = {key: old.pop(key) for key in ("build", "shown_minus_hidden", "hidden_all", "shown_all",
                                               "idle_frames_5s", "per_journey", "verdict") if key in old}
        old["run3_da30c7f"] = kept
    method = old.get("method")
    new = {
        "head": head,
        "device": old.get("device"),
        "method": method,
        "limits": old.get("limits"),
    }
    for name, result, spec in (
        ("standard", standard, "standard preset less auth/onboarding (out/HUD/specs/hud-standard.json): no "
                               "analytics, so the overlay's own JankStats; hidden has no frame listener at all"),
        ("everything", everything, "everything preset less auth/onboarding/googlesignin "
                                   "(out/HUD/specs/hud-everything.json): analytics' JankStats shared"),
    ):
        if not result:
            continue
        delta = result["shown_minus_hidden"]
        means = valid_total_means(f"overhead-{name}-f1")
        new[f"fix1_{name}"] = {
            "note": "shown_minus_hidden.total_mean_ms counts frames still in flight when gfxinfo was dumped "
                    "(FrameCompleted 0); frame_total_mean_valid_ms leaves them out",
            "frame_total_mean_valid_ms": {f"{mode}_{journey}": round(value, 3)
                                          for (mode, journey), value in means.items()},
            "build": f"{spec}; devRelease arm64, installed fresh, profile installed, compiled speed-profile",
            "shown_minus_hidden": delta,
            "hidden_all": result["hidden"]["all"],
            "shown_all": result["shown"]["all"],
            "idle_frames_5s": {"hidden": result["hidden"]["idle_frames"], "shown": result["shown"]["idle_frames"]},
            "per_journey": {mode: {j: result[mode][j] for j in ("scroll", "tabs", "pushpop")}
                            for mode in ("hidden", "shown")},
            "verdict": "pass" if all(delta[j]["janky_pp"] <= 0.3 and (delta[j]["ui_mean_ms"] or 0) < 0.5
                                     for j in ("all",)) else "fail",
        }
    if startup:
        new["fix1_startup_hidden"] = {k: v for k, v in startup.items()}
    if listener:
        new["fix1_listener"] = listener
    for key, value in old.items():
        if key not in new and key not in ("build", "head"):
            new[key] = value
    with open(os.path.join(P, "measurements.json"), "w", encoding="utf-8") as handle:
        json.dump(new, handle, indent=1)
        handle.write("\n")


if __name__ == "__main__":
    main()
