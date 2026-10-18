"""Compares a perf.sh summary with one column of gates.toml.

    check_gates.py --gate solid [--gates gates.toml] [--baseline reference/summary.json] summary.json

Every row that has a limit in the column is checked; a row whose metric the run did not measure
fails, so a gate cannot pass by skipping a scenario. The one exception is a scenario the summary
lists as inapplicable to the app, with its reason (a tab switch in an app with one tab): its rows
are reported as not applicable. "ratchet" rows are reported and never fail. Exits 1 when any row
fails.
"""

import argparse
import json
import os
import sys
import tomllib

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_GATES = os.path.join(HERE, "gates.toml")


def load_rows(path):
    with open(path, "rb") as handle:
        return tomllib.load(handle)["row"]


def columns(rows):
    return sorted({key for row in rows for key in row if key not in ("metric", "label")})


def _value(summary, metric):
    entry = summary.get("metrics", {}).get(metric)
    return None if entry is None else entry.get("value")


def evaluate(summary, rows, column, baseline=None):
    """One result per row of the column: dicts with label, value, limit and status."""
    results = []
    inapplicable = summary.get("inapplicable", {})
    for row in rows:
        if column not in row:
            continue
        limit = row[column]
        value = _value(summary, row["metric"])
        scenario = row["metric"].split(".")[0]
        result = {"metric": row["metric"], "label": row["label"], "value": value, "limit": limit}
        if limit == "ratchet":
            result["status"] = "report"
        elif value is None and scenario in inapplicable:
            result["status"] = f"not applicable ({inapplicable[scenario]})"
        elif value is None:
            result["status"] = "not measured"
        elif isinstance(limit, bool):
            result["status"] = "pass" if value is limit else "fail"
        elif isinstance(limit, dict):
            reference = _value(baseline or {}, row["metric"])
            if reference is None:
                result["status"] = "no reference"
            else:
                allowed = reference + max(limit.get("add", 0.0), reference * limit.get("scale", 0.0))
                result["limit"] = round(allowed, 2)
                result["status"] = "pass" if value <= allowed else "fail"
        else:
            result["status"] = "pass" if value <= limit else "fail"
        results.append(result)
    return results


def passed(results):
    return all(result["status"] in ("pass", "report") or result["status"].startswith("not applicable")
               for result in results)


def table(results, column):
    lines = [
        f"| Gate `{column}` | Value | Limit | Result |",
        "|---|---|---|---|",
    ]
    for result in results:
        lines.append(
            f"| {result['label']} | {_show(result['value'])} | {_show(result['limit'])} | {result['status']} |"
        )
    return "\n".join(lines)


def _show(value):
    if value is None:
        return "—"
    if isinstance(value, bool):
        return "yes" if value else "no"
    if isinstance(value, float):
        return f"{value:g}"
    return str(value)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("summary")
    parser.add_argument("--gate", required=True)
    parser.add_argument("--gates", default=DEFAULT_GATES)
    parser.add_argument("--baseline")
    args = parser.parse_args(argv)

    rows = load_rows(args.gates)
    if args.gate not in columns(rows):
        sys.exit(f"no column '{args.gate}' in {args.gates}; columns: {', '.join(columns(rows))}")
    with open(args.summary) as handle:
        summary = json.load(handle)
    baseline = None
    if args.baseline:
        with open(args.baseline) as handle:
            baseline = json.load(handle)
    results = evaluate(summary, rows, args.gate, baseline)
    print(table(results, args.gate))
    return 0 if passed(results) else 1


if __name__ == "__main__":
    sys.exit(main())
