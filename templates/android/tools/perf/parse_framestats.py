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


def stages(rows, count=3):
    """Where each of the first [count] frames spent its time after the UI thread, in milliseconds.

    `issue` is the render thread turning the display list into GPU commands, `gpu` the GPU's own work
    on them, and `dequeue` the wait for a free buffer: a non-zero dequeue means the GPU or the display
    was still holding the buffers of earlier frames.
    """

    def span(row, start, end):
        if not row.get(start) or not row.get(end) or row[end] < row[start]:
            return None
        return round((row[end] - row[start]) / _NS_PER_MS, 2)

    return [
        {
            "ui": round(ui_ms(row), 2),
            "issue": span(row, "IssueDrawCommandsStart", "SwapBuffers"),
            "gpu": span(row, "CommandSubmissionCompleted", "GpuCompleted"),
            "dequeue": round(row.get("DequeueBufferDuration", 0) / _NS_PER_MS, 2),
        }
        for row in rows[:count]
    ]


def missed_deadline(row):
    """Whether the frame finished after the deadline FrameTimeline gave it, which is what gfxinfo
    counts as janky. A frame that starts from idle gets the shortest deadline (13.67 ms after its
    vsync on a 120 Hz Redmi); frames queued behind it get later ones."""
    deadline = row.get("FrameDeadline")
    if not deadline:
        return False
    return max(row["FrameCompleted"], row.get("GpuCompleted", 0)) > deadline


def first_frame_missed(rows, rendered, janky):
    """Whether a transition's first frame missed its deadline, from its framestats [rows] and the
    gfxinfo counts of the same segment: [rendered] frames, [janky] of them janky.

    gfxinfo can count a first frame that framestats holds no row for. The first row is then a later
    frame of the same motion: its deadline is further out than the one a frame starting from idle
    gets, and a janky frame gfxinfo counts beyond the rows' own is that unlisted first frame.
    """
    if not rows:
        return False
    first = rows[0]
    if len(rows) >= rendered or not _queued_behind_earlier_frame(first):
        return missed_deadline(first)
    return janky > sum(1 for row in rows if missed_deadline(row))


def _queued_behind_earlier_frame(row):
    # A frame that starts from idle is given the workload target as its deadline; one queued behind
    # a frame still on its way to the display is given a later one.
    deadline, target = row.get("FrameDeadline"), row.get("WorkloadTarget")
    if not deadline or not target:
        return False
    return deadline - row["IntendedVsync"] > target + _interval(row) // 2


def late_first_frames(dumps):
    """How many transitions missed the deadline on their first frame, of how many had one."""
    firsts = [rows[0] for rows in dumps if rows]
    return sum(1 for row in firsts if missed_deadline(row)), len(firsts)


# A gap this long between two presented frames is the end of one burst of motion and the start of
# another (a list settling, then a late image), not a frame the display waited for.
_MAX_HITCH_INTERVALS = 12


def hitch_time(rows):
    """(hitch ms, time in motion ms) from the display's own present times.

    A frame presented later than one vsync after the frame before it is a hitch, and the time past
    that vsync is what it cost, which is what the user sees whatever gfxinfo's deadline says. Rows
    the display never reported (present time 0) are left out, and so are the idle gaps between
    bursts of motion.
    """
    presented = sorted(row["DisplayPresentTime"] for row in rows if row.get("DisplayPresentTime", 0) > 0)
    if len(presented) < 2:
        return 0.0, 0.0
    interval = _interval(rows[0])
    hitch = motion = 0.0
    for earlier, later in zip(presented, presented[1:]):
        gap = later - earlier
        if gap > _MAX_HITCH_INTERVALS * interval:
            continue
        motion += gap
        if gap > 1.5 * interval:
            hitch += gap - interval
    return hitch / _NS_PER_MS, motion / _NS_PER_MS


def hitch_ratio(dumps):
    """Hitch time per second of motion over several dumps (Apple's hitch ratio; good is under 5)."""
    hitch = span = 0.0
    for rows in dumps:
        h, s = hitch_time(rows)
        hitch += h
        span += s
    return round(1000 * hitch / span, 2) if span else None


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("usage: parse_framestats.py <framestats dump>...")
    for path in sys.argv[1:]:
        with open(path, errors="ignore") as handle:
            frames = load(handle.read())
        print(path, json.dumps({"first": first_frame(frames), "max_ui_ms": max_ui_ms(frames)}))
