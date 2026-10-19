"""The handoff's cold-start paragraph from perf/HUD/startup-f1/result.json, printed for pasting.

  python -I out/HUD/f1_startup_block.py
"""

import json
import statistics

with open("D:/projects/tools/.overhaul/perf/HUD/startup-f1/result.json", encoding="utf-8") as handle:
    result = json.load(handle)

runs = result["runs"]
lines = []
for label in ("base", "hud"):
    medians = [statistics.median(times) for times in runs[label]]
    every = [t for times in runs[label] for t in times]
    lines.append(f"{label}: medians per install {', '.join(f'{m:.0f}' for m in medians)} ms; all {len(every)} "
                 f"launches median {statistics.median(every):.0f} ms, mean {statistics.mean(every):.1f} ms, "
                 f"min {min(every)} ms, max {max(every)} ms")
print("\n".join(lines))
print(f"hud - base median: {result['hud_minus_base_median_ms']:+.1f} ms")
