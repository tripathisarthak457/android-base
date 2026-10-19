"""fit_motion.py (research/district/tools) with Telegram's own curves added to the tweens it tries.

Standard library only. Same arguments and output as fit_motion.py:

    python -I tg_fit.py tracks/T1_r1.csv tracks/T1_r2.csv tracks/T1_r3.csv
    python -I tg_fit.py tracks/T3_150_r1.csv --swipe-ms 150 --json

The curves are the values Telegram's source states (research/telegram/SOURCES.md §13), written here as numbers only:
DecelerateInterpolator(1.5) is 1 - (1 - u)^3, AccelerateDecelerate is (1 - cos(pi u)) / 2, and the rest are cubic
Bezier control points. A track that a Telegram curve fits clearly better than every other tween and spring, at the
duration the source gives (within one 8.3 ms frame), "matches the source".
"""

import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "district", "tools"))
import fit_motion as base  # noqa: E402  (the District tool, read-only)

TELEGRAM_EASINGS = {
    "Telegram Decelerate(1.5) 1-(1-u)^3": "cubic",
    "Telegram AccelerateDecelerate (framework default)": "acceldecel",
    "Telegram DEFAULT (0.25,0.1,0.25,1)": (0.25, 0.1, 0.25, 1.0),
    "Telegram EASE_OUT_QUINT (0.23,1,0.32,1)": (0.23, 1.0, 0.32, 1.0),
    "Telegram chat curve (0.19919,0.01064,0.27921,0.91025)": (0.19919, 0.01064, 0.27921, 0.91025),
}

_district_easing = base.easing_function


def easing_function(spec):
    if spec == "cubic":
        return lambda u: 1 - (1 - u) ** 3
    if spec == "acceldecel":
        return lambda u: (1 - math.cos(math.pi * u)) / 2
    return _district_easing(spec)


base.EASINGS.update(TELEGRAM_EASINGS)
base.easing_function = easing_function

if __name__ == "__main__":
    base.main()
