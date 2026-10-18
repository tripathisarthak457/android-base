"""
For each catch in a catch_* recording (scenarios.sh): after the throw into the end, how far the content moved the
flick's way and how far it moved back against the flick, in the 2 s that follow. Reads NAME.track.tsv (track.py).

    python -I catch_summary.py DIR...

The throw is the run towards the end of 500 px or more (200 on the carousel, which is pulled past its last card).
Everything after it, until the next throw or 2 s, is the band coming back, the catch and the flick, all of which should
move the flick's way only.
"""

import glob
import os
import sys

WINDOW_S = 2.0
THROW_PX = {"list": 500.0, "carousel": 200.0}


def rows(path):
    with open(path, encoding="utf-8") as handle:
        next(handle)
        for line in handle:
            _, t, step, _, _ = line.split("\t")
            yield float(t), float(step)


def throws(track, way, least):
    """Each run of frames moving `way` (+1 or -1) adding up to `least` px or more: its end time."""
    out, run, end = [], 0.0, None
    for t, step in track:
        if step * way > 1.0:
            run += abs(step)
            end = t
        elif abs(step) > 1.0 or (end is not None and t - end > 0.05):
            if run >= least:
                out.append(end)
            run, end = 0.0, None
    if run >= least:
        out.append(end)
    return out


def main():
    for directory in sys.argv[1:]:
        print(f"## {os.path.basename(os.path.normpath(directory))}")
        for path in sorted(glob.glob(os.path.join(directory, "*catch*.track.tsv"))):
            name = os.path.basename(path)[: -len(".track.tsv")]
            # Into the top is down (+), into the bottom up (-); into the carousel's end is left (-), right (+) in RTL.
            way = {"list_catch_top": 1, "list_catch_bottom": -1, "carousel_catch_end": -1}[name.replace("_rtl", "")]
            if name == "carousel_catch_end_rtl":
                way = 1
            track = list(rows(path))
            ends = throws(track, way, THROW_PX[name.split("_")[0]])
            for index, start in enumerate(ends):
                stop = min([start + WINDOW_S] + [e for e in ends if e > start])
                window = [step for t, step in track if start < t <= stop]
                with_flick = sum(-s * way for s in window if -s * way > 0)
                against = sum(s * way for s in window if s * way > 0)
                print(f"  {name:24s} catch {index + 1} (throw ends {start:6.2f} s): "
                      f"flick's way {with_flick:6.0f} px, against it {against:5.0f} px")


if __name__ == "__main__":
    main()
