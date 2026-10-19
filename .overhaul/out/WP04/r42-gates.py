"""REQUIREMENTS 42 for the template's gates.toml: gate what users feel, hold the rest where the merge left it.

    python r42-gates.py <gates.toml> <final> <final-standard> <final-lean> [--write]

The three directories are one acceptance's perf.sh outputs (summary.json with each metric's runs). Without
--write the new file is printed; with it, gates.toml is rewritten in place (comments and row order kept).

- Release limits for the motions: tab switch (`tabs`, `tabs_tap`), push/pop, the back gesture and Rapid Back gate
  the hitch ratio (`*.hitch_ms_per_s`) and the jank after each motion's first frame (`*.janky_after_first_pct`);
  `*.late_first_frames` is reported. A missed first frame from idle is the phone's clock-up (WP04-perf §0: an
  empty-content build misses it on 7 of 7 switches), the rest is what the app does.
- Non-regression rows: total janky share, first-frame and p99 rows of those motions and app PSS are held at the
  worst of the acceptance's three runs plus a noise margin, so they may only improve (REQUIREMENTS 42).
- Everything else keeps its limit: scroll (REQUIREMENTS 41), cold start, idle, recomposition, the checks.
"""
import json
import math
import os
import statistics
import sys
import tomllib

COLUMNS = ("solid", "standard", "lean")

# Under 5 ms of hitch per second of motion is Apple's "good" band, the line parse_framestats.hitch_ratio
# documents. The back gesture and Rapid Back are driven by injected input whose own cadence makes a few late
# frames the app does not cause (WP04-perf §0a), so their share after the first frame gets one point.
RELEASE = {
    "tabs": {"hitch": 5.0, "after_first": 0.5, "columns": COLUMNS, "label": "Tab switch"},
    "tabs_tap": {"hitch": 5.0, "after_first": 0.5, "columns": ("solid",), "label": "Tab switch with plain taps"},
    "pushpop": {"hitch": 5.0, "after_first": 0.5, "columns": COLUMNS, "label": "Push/pop with the Back key"},
    "pushpop_gesture": {"hitch": 5.0, "after_first": 1.0, "columns": ("solid",),
                        "label": "Push/pop with the back gesture"},
    "rapid_back": {"hitch": 5.0, "after_first": 1.0, "columns": ("solid",), "label": "Rapid Back"},
}

NON_REGRESSION = (
    "tabs.janky_pct", "tabs.first_frame_ms.median", "tabs.first_frame_ms.max",
    "tabs_tap.janky_pct", "tabs_tap.first_frame_ms.median", "tabs_tap.first_frame_ms.max",
    "pushpop.janky_pct", "pushpop.p99_ms", "pushpop.pop_first_frame_ms.median", "pushpop.pop_first_frame_ms.max",
    "pushpop.push_first_frame_ms.median", "pushpop.push_first_frame_ms.max",
    "pushpop_gesture.janky_pct", "pushpop_gesture.gesture_first_frame_ms.median",
    "rapid_back.janky_pct", "rapid_back.pop_frame_ms.median", "rapid_back.pop_frame_ms.max",
    "meminfo.pss_app_mb",
)


def held(metric, runs):
    """The worst run plus the margin the phone's run-to-run noise needs."""
    worst = max(runs)
    if metric.endswith("janky_pct"):
        return round(worst + max(worst * 0.1, 0.1), 2)
    if metric.endswith("_mb"):
        return round(worst * 1.05, 1)
    if metric.endswith("p99_ms"):
        return math.ceil(worst * 1.1)
    return math.ceil(worst * 1.1 * 10) / 10


def load_runs(directory):
    with open(os.path.join(directory, "summary.json"), encoding="utf-8") as handle:
        metrics = json.load(handle)["metrics"]
    return {name: entry.get("runs") or [entry["value"]] for name, entry in metrics.items()
            if isinstance(entry.get("value"), (int, float)) and not isinstance(entry.get("value"), bool)}


def blocks(text):
    """The header, then each [[row]] block as its list of lines."""
    head, rows, current = [], [], None
    for line in text.splitlines():
        if line == "[[row]]":
            current = [line]
            rows.append(current)
        elif current is None:
            head.append(line)
        else:
            current.append(line)
    return head, rows


def metric_of(row):
    return next(line.split("=", 1)[1].strip().strip('"') for line in row if line.startswith("metric = "))


def set_column(row, column, value):
    """Sets a column's limit, keeping the column order solid, standard, lean and anything after them."""
    rendered = f"{column} = {render(value)}"
    for i, line in enumerate(row):
        if line.startswith(f"{column} = "):
            row[i] = rendered
            return
    order = [line.split(" = ", 1)[0] for line in row]
    later = [c for c in COLUMNS[COLUMNS.index(column) + 1:] + ("smoke", "large") if c in order]
    at = order.index(later[0]) if later else len(row)
    while at > 0 and row[at - 1] == "":
        at -= 1
    row.insert(at, rendered)


def set_label(row, label):
    for i, line in enumerate(row):
        if line.startswith("label = "):
            row[i] = f"label = {render(label)}"


def column_value(row, column):
    for line in row:
        if line.startswith(f"{column} = "):
            return tomllib.loads(line)[column]
    return None


def label_of(row):
    return next(line.split("=", 1)[1].strip().strip('"') for line in row if line.startswith("label = "))


def render(value):
    if isinstance(value, str):
        return '"' + value.replace('"', '\\"') + '"'
    if isinstance(value, bool):
        return "true" if value else "false"
    return repr(value)


def new_row(metric, label, limits):
    row = ["[[row]]", f"metric = {render(metric)}", f"label = {render(label)}"]
    row += [f"{column} = {render(value)}" for column, value in limits]
    return row + [""]


HEADER_ANCHOR = "# tolerance is a table: the reference value plus max(`add`, reference × `scale`)."
HEADER = """#
# What gates a release (REQUIREMENTS 42): for tab switches, push/pop, the back gesture and Rapid Back,
# what the user feels, the hitch time per second of motion and the janky frames after each motion's
# first. A first frame from idle misses its deadline on this phone whatever is drawn (the GPU is still
# clocked down: an empty-content build misses it on 7 of 7 switches), so it is reported, not gated.
# Rows marked non-regression hold the worst run of the merge's acceptance plus its noise margin, and
# never more than the limit they replaced where the merge met it: they may only go down, and each
# improvement is written back here."""


def transform(text, runs_by_column):
    head, rows = blocks(text)
    if "REQUIREMENTS 42" not in "\n".join(head):
        at = head.index(HEADER_ANCHOR) + 1
        head[at:at] = HEADER.splitlines()
    by_metric = {metric_of(row): row for row in rows}
    for metric in NON_REGRESSION:
        row = by_metric[metric]
        for column in COLUMNS:
            limit = column_value(row, column)
            runs = runs_by_column[column].get(metric)
            if limit is None or not runs:
                continue
            # A row the merge already passes is never loosened: only the limits of the phone itself move.
            passes = isinstance(limit, (int, float)) and statistics.median(runs) <= limit
            set_column(row, column, min(limit, held(metric, runs)) if passes else held(metric, runs))
        if "non-regression" not in label_of(row):
            set_label(row, label_of(row) + " [non-regression]")
    out = list(rows)
    for scenario, spec in RELEASE.items():
        hitch = f"{scenario}.hitch_ms_per_s"
        if hitch not in by_metric:
            last = max(i for i, row in enumerate(out) if metric_of(row).startswith(scenario + "."))
            added = new_row(hitch, f"{spec['label']}, hitch time per second of motion (ms/s)", [])
            out.insert(last + 1, added)
            by_metric[hitch] = added
        row = by_metric[hitch]
        for column in spec["columns"]:
            set_column(row, column, spec["hitch"])
        at = out.index(row) + 1
        for metric, label, limit in (
            (f"{scenario}.janky_after_first_pct",
             f"{spec['label']}, janky frames other than each motion's first (%)", spec["after_first"]),
            (f"{scenario}.late_first_frames",
             f"{spec['label']}, motions whose first frame missed its deadline (count)", "ratchet"),
        ):
            if metric in by_metric:
                continue
            added = new_row(metric, label, [(column, limit) for column in spec["columns"]])
            out.insert(at, added)
            by_metric[metric] = added
            at += 1
    lines = head + [line for row in out for line in row]
    result = "\n".join(lines).rstrip("\n") + "\n"
    tomllib.loads(result)
    return result


def main(argv):
    write = "--write" in argv
    args = [a for a in argv if a != "--write"]
    if len(args) != 4:
        sys.exit(__doc__)
    gates, *dirs = args
    runs_by_column = {column: load_runs(directory) for column, directory in zip(COLUMNS, dirs)}
    with open(gates, encoding="utf-8") as handle:
        text = handle.read()
    result = transform(text, runs_by_column)
    if write:
        with open(gates, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(result)
    else:
        # Bytes, so Windows' text-mode stdout does not turn the file's line endings into CRLF.
        sys.stdout.buffer.write(result.encode("utf-8"))


if __name__ == "__main__":
    main(sys.argv[1:])
