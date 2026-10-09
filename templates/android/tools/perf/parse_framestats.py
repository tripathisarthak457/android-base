"""Per-frame timings from the PROFILEDATA blocks of `dumpsys gfxinfo <package> framestats`.

A transition is measured by resetting gfxinfo, triggering it, and dumping framestats: the first row
is then the transition's first frame. Its UI-thread time (animation, composition, layout and draw,
`AnimationStart` to `SyncQueued`) is what a cold composition costs, and the vsyncs skipped after it
are what the user sees as a hitch.

Rows with flags set (window visibility changes, render-thread-only animation frames, skipped frames)
are left out: they carry no UI-thread work to measure.
"""

import json
import sys

_NS_PER_MS = 1_000_000


def load(text):
    """Every regular frame of every window in the dump, oldest first."""
    rows = []
    header = None
    inside = False
    for line in text.splitlines():
        if line.startswith("---PROFILEDATA---"):
            inside = not inside
            header = None
            continue
        if not inside:
            continue
        fields = line.strip().rstrip(",").split(",")
        if fields[0] == "Flags":
            header = fields
            continue
        if header is None or len(fields) < len(header):
            continue
        row = dict(zip(header, (int(value) for value in fields[: len(header)])))
        if row["Flags"] == 0:
            rows.append(row)
    rows.sort(key=lambda row: row["IntendedVsync"])
    return rows


def ui_ms(row):
    return (row["SyncQueued"] - row["AnimationStart"]) / _NS_PER_MS


def phases(row):
    """Where one frame's time went, in milliseconds."""

    def span(start, end):
        return round((row[end] - row[start]) / _NS_PER_MS, 2)

    return {
        "animation_and_composition": span("AnimationStart", "PerformTraversalsStart"),
        "layout_and_draw": span("PerformTraversalsStart", "SyncQueued"),
        "render_thread": span("SyncStart", "FrameCompleted"),
        "total": span("IntendedVsync", "FrameCompleted"),
    }


def _interval(row):
    # FrameInterval is the display's vsync period for that frame (8.33 ms at 120 Hz).
    return row.get("FrameInterval") or 16_666_667


def first_frame(rows):
    """The first frame of a transition and the vsyncs skipped right after it, or None."""
    if not rows:
        return None
    first = rows[0]
    skipped = 0
    for earlier, later in zip(rows[:2], rows[1:3]):
        gap = later["IntendedVsync"] - earlier["IntendedVsync"]
        skipped += max(0, round(gap / _interval(earlier)) - 1)
    return {
        "ui_ms": round(ui_ms(first), 2),
        "second_ui_ms": round(ui_ms(rows[1]), 2) if len(rows) > 1 else None,
        "dropped_vsyncs": skipped,
        "frames": len(rows),
        "phases": phases(first),
    }


def opening_frame(rows, window=3):
    """The slowest of a tap's first [window] frames: the one that composes what the tap opened.

    A tap's first frame can be its press feedback letting go, with the new screen composed in the
    frame after; a Back key has no such frame, so a pop is measured with first_frame().
    """
    return max(rows[:window], key=ui_ms) if rows else None


def max_ui_ms(rows):
    """The slowest UI-thread frame in the dump: where a second transition started mid-dump."""
    return round(max(ui_ms(row) for row in rows), 2) if rows else None


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("usage: parse_framestats.py <framestats dump>...")
    for path in sys.argv[1:]:
        with open(path, errors="ignore") as handle:
            frames = load(handle.read())
        print(path, json.dumps({"first": first_frame(frames), "max_ui_ms": max_ui_ms(frames)}))
