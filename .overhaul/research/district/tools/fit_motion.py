"""Fits a motion curve to an edge track: is it a spring or a fixed-duration tween, how long, how much overshoot, and did
it keep the finger's speed?

Standard library only. Input is edge_track.py's CSV (columns t and pos; t,y also works). Several CSVs are several
trials of one scene, compared at the end.

    python -I fit_motion.py present.csv                         # a tap: motion starts from rest
    python -I fit_motion.py fling_*.csv --swipe-ms 120          # scripted flings: the finger lifts ~120 ms in
    python -I fit_motion.py drag.csv --release 0.412 --json

What it reports, from the release (or the start of motion) onwards:
  - travel, the times to 50, 90 and 99 percent of it, and the time to settle within 1 dp;
  - overshoot past the resting position (dp and percent of travel) and how many times it crosses it;
  - the speed just before the release, and the carry: how far the surface moved in the two frames after the lift
    against how far the finger's speed would have taken it (about 1: the speed was kept; near 0: the motion
    restarted from rest, as a FastOutSlowIn tween or a zero-velocity settle does);
  - the best spring (damping ratio, stiffness, and the same as iOS response and template bounce) and the best tween
    (easing, duration), each with its RMS error; a clearly smaller error names the family.
One curve rarely separates a critically damped spring from a quintic ease-out (ViewDragHelper): their shapes are
nearly the same. Several trials at different release speeds do: a spring keeps its stiffness and starts at the
finger's speed, a fixed tween keeps its duration, and ViewDragHelper's duration shrinks as the release speed grows.

Positions are in recorded pixels; --px-per-dp converts (a 720 x 1600 recording of a 1080 x 2400 phone at density
2.75 has 1.8333 recorded px per dp).
"""

import argparse
import csv
import json
import math

EASINGS = {
    "linear": None,
    "FastOutSlowIn (0.4,0,0.2,1)": (0.4, 0.0, 0.2, 1.0),
    "LinearOutSlowIn (0,0,0.2,1)": (0.0, 0.0, 0.2, 1.0),
    "EmphasizedDecelerate (0.05,0.7,0.1,1)": (0.05, 0.7, 0.1, 1.0),
    "StandardDecelerate (0,0,0,1)": (0.0, 0.0, 0.0, 1.0),
    "iOS easeInOut (0.42,0,0.58,1)": (0.42, 0.0, 0.58, 1.0),
    "iOS easeOut (0,0,0.58,1)": (0.0, 0.0, 0.58, 1.0),
    "quintic ease-out (ViewDragHelper)": "quintic",
    "Decelerate (1-(1-u)^2)": "quad",
}


def bezier_lut(x1, y1, x2, y2, samples=2001):
    def coord(s, a, b):
        return 3 * a * s * (1 - s) ** 2 + 3 * b * s * s * (1 - s) + s ** 3

    lut = []
    for index in range(samples):
        u = index / (samples - 1)
        lo, hi = 0.0, 1.0
        for _ in range(40):
            mid = (lo + hi) / 2
            if coord(mid, x1, x2) < u:
                lo = mid
            else:
                hi = mid
        lut.append(coord((lo + hi) / 2, y1, y2))
    return lut


def easing_function(spec):
    if spec is None:
        return lambda u: u
    if spec == "quintic":
        return lambda u: 1 - (1 - u) ** 5
    if spec == "quad":
        return lambda u: 1 - (1 - u) ** 2
    lut = bezier_lut(*spec)
    last = len(lut) - 1

    def ease(u):
        position = u * last
        index = min(int(position), last - 1)
        fraction = position - index
        return lut[index] + (lut[index + 1] - lut[index]) * fraction

    return ease


def solve(matrix, vector):
    """Gaussian elimination with partial pivoting; None when singular."""
    size = len(vector)
    rows = [list(matrix[i]) + [vector[i]] for i in range(size)]
    for column in range(size):
        pivot = max(range(column, size), key=lambda r: abs(rows[r][column]))
        if abs(rows[pivot][column]) < 1e-12:
            return None
        rows[column], rows[pivot] = rows[pivot], rows[column]
        for r in range(size):
            if r != column:
                factor = rows[r][column] / rows[column][column]
                for c in range(column, size + 1):
                    rows[r][c] -= factor * rows[column][c]
    return [rows[i][size] / rows[i][i] for i in range(size)]


def least_squares(bases, values):
    """Coefficients and RMS of values ≈ Σ coefficient · basis."""
    count = len(bases)
    normal = [[sum(a * b for a, b in zip(bases[i], bases[j])) for j in range(count)] for i in range(count)]
    right = [sum(a * v for a, v in zip(bases[i], values)) for i in range(count)]
    coefficients = solve(normal, right)
    if coefficients is None:
        return None, float("inf")
    error = 0.0
    for index, value in enumerate(values):
        model = sum(coefficients[k] * bases[k][index] for k in range(count))
        error += (model - value) ** 2
    return coefficients, math.sqrt(error / len(values))


def spring_bases(times, zeta, omega):
    if abs(zeta - 1.0) < 1e-9:
        decay = [math.exp(-omega * t) for t in times]
        return [decay, [t * d for t, d in zip(times, decay)]]
    if zeta < 1.0:
        damped = omega * math.sqrt(1 - zeta * zeta)
        decay = [math.exp(-zeta * omega * t) for t in times]
        return [[d * math.cos(damped * t) for t, d in zip(times, decay)],
                [d * math.sin(damped * t) for t, d in zip(times, decay)]]
    root = math.sqrt(zeta * zeta - 1)
    fast, slow = omega * (zeta + root), omega * (zeta - root)
    return [[math.exp(-slow * t) for t in times], [math.exp(-fast * t) for t in times]]


def spring_velocity0(zeta, omega, a, b):
    """d/dt of the transient at t = 0 for the bases above."""
    if abs(zeta - 1.0) < 1e-9:
        return b - omega * a
    if zeta < 1.0:
        return -zeta * omega * a + omega * math.sqrt(1 - zeta * zeta) * b
    root = math.sqrt(zeta * zeta - 1)
    return -omega * (zeta - root) * a - omega * (zeta + root) * b


def fit_spring(times, values):
    ones = [1.0] * len(times)

    def attempt(zeta, omega):
        coefficients, rms = least_squares([ones] + spring_bases(times, zeta, omega), values)
        return rms, coefficients

    best = (float("inf"), None, None, None)
    zetas = [round(0.30 + 0.02 * i, 4) for i in range(86)]
    if 1.0 not in zetas:
        zetas.append(1.0)
    for zeta in zetas:
        omega = 4.0
        while omega <= 100.0:
            rms, coefficients = attempt(zeta, omega)
            if rms < best[0]:
                best = (rms, zeta, omega, coefficients)
            omega += 0.5
    _, zeta0, omega0, _ = best
    for i in range(-10, 11):
        zeta = zeta0 + 0.002 * i
        if zeta <= 0.05:
            continue
        for j in range(-10, 11):
            omega = omega0 + 0.05 * j
            if omega <= 0.5:
                continue
            rms, coefficients = attempt(zeta, omega)
            if rms < best[0]:
                best = (rms, zeta, omega, coefficients)
    rms, zeta, omega, (rest, a, b) = best
    return {
        "rms_px": rms,
        "damping_ratio": zeta,
        "omega": omega,
        "stiffness": omega * omega,
        "ios_response_s": 2 * math.pi / omega,
        "bounce_template": 1 - zeta,
        "rest_px": rest,
        "velocity0_px_s": spring_velocity0(zeta, omega, a, b),
    }


def fit_tween(times, values):
    best = {"rms_px": float("inf")}
    for name, spec in EASINGS.items():
        ease = easing_function(spec)
        for delay_step in range(0, 7):
            delay = delay_step * 0.008333
            duration = 0.060
            while duration <= 0.900:
                progress = [ease(min(1.0, max(0.0, (t - delay) / duration))) for t in times]
                coefficients, rms = least_squares([[1 - p for p in progress], progress], values)
                if coefficients is not None and rms < best["rms_px"]:
                    best = {"rms_px": rms, "easing": name, "duration_s": duration, "delay_s": delay,
                            "from_px": coefficients[0], "rest_px": coefficients[1]}
                duration += 0.005
    return best


def fit_decay(times, values):
    """A fling's coast: y = rest + A·e^(−λt). iOS's normal rate keeps 0.998 of the speed per ms (λ ≈ 2.0/s)."""
    ones = [1.0] * len(times)
    best = (float("inf"), None, None)
    rate = 0.1
    while rate <= 40.0:
        coefficients, rms = least_squares([ones, [math.exp(-rate * t) for t in times]], values)
        if coefficients is not None and rms < best[0]:
            best = (rms, rate, coefficients)
        rate += 0.05
    rms, rate, (rest, amplitude) = best
    return {
        "rms_px": rms,
        "rate_per_s": rate,
        "kept_per_ms": math.exp(-rate / 1000.0),
        "rest_px": rest,
        "coast_px": -amplitude,
        "velocity0_px_s": -rate * amplitude,
    }


def slope(points):
    if len(points) < 2:
        return float("nan")
    n = len(points)
    mean_t = sum(t for t, _ in points) / n
    mean_y = sum(y for _, y in points) / n
    denominator = sum((t - mean_t) ** 2 for t, _ in points)
    if denominator == 0:
        return float("nan")
    return sum((t - mean_t) * (y - mean_y) for t, y in points) / denominator


def line_fit(points):
    """Slope and intercept of a least-squares line through (t, y) points."""
    k = slope(points)
    n = len(points)
    return k, sum(y for _, y in points) / n - k * sum(t for t, _ in points) / n


def progress_from_rest(zeta, omega, at):
    """Share of the travel a spring started from rest has covered at time `at` (numerical, any damping)."""
    x, v, dt, t = 1.0, 0.0, 0.0001, 0.0
    while t < at:
        acceleration = -omega * omega * x - 2 * zeta * omega * v
        v += acceleration * dt
        x += v * dt
        t += dt
    return 1 - x


def position_at(rows, at):
    """The track linearly interpolated at time `at`."""
    for (t0, y0), (t1, y1) in zip(rows, rows[1:]):
        if t0 <= at <= t1:
            return y0 if t1 == t0 else y0 + (y1 - y0) * (at - t0) / (t1 - t0)
    return rows[-1][1] if at > rows[-1][0] else rows[0][1]


def load(path):
    rows = []
    with open(path, newline="") as handle:
        for record in csv.DictReader(handle):
            position = record.get("pos", record.get("y", ""))
            if position in ("", None):
                continue
            rows.append((float(record["t"]), float(position)))
    return rows


def find_release(rows, motion_start, swipe_ms, tolerance):
    """
    The finger's lift in a scripted swipe. `input swipe` moves at a constant speed, so while the finger is down a
    surface that tracks it moves along a straight line; the lift is where the track leaves that line. Searched within
    25 ms of motion start + the swipe's duration. Returns the release time and the drag speed in px/s.
    """
    estimate = motion_start + swipe_ms / 1000.0
    drag = [(t, y) for t, y in rows if motion_start + 0.012 <= t <= estimate - 0.010]
    if len(drag) < 3:
        return estimate, float("nan")
    k, c = line_fit(drag)
    release = estimate
    for t, y in rows:
        if t < estimate - 0.025:
            continue
        if t > estimate + 0.025:
            break
        if abs(y - (k * t + c)) > tolerance:
            release = t - 0.004
            break
        release = t
    return release, k


def analyse(path, args):
    rows = [(t, y) for t, y in load(path)
            if (args.start is None or t >= args.start) and (args.end is None or t <= args.end)]
    if len(rows) < 8:
        return {"track": path, "error": "fewer than 8 tracked frames in the window"}
    first_y = rows[0][1]
    moved = next(((t, y) for t, y in rows if abs(y - first_y) > args.move_px), None)
    if moved is None:
        return {"track": path, "error": "nothing moved"}
    at_rest = [t for t, _ in rows if t < moved[0]]
    motion_start = at_rest[-1] if at_rest else moved[0]
    v_before = float("nan")
    if args.release is not None:
        release = args.release
    elif args.swipe_ms:
        release, v_before = find_release(rows, motion_start, args.swipe_ms, args.tolerance_px)
    else:
        release = motion_start
    if v_before != v_before and (args.swipe_ms or args.release is not None):
        before = [(t, y) for t, y in rows if release - 0.045 <= t <= release]
        v_before = slope(before) if len(before) >= 3 else float("nan")
    after = [(t - release, y) for t, y in rows if t >= release]
    if len(after) < 6:
        return {"track": path, "error": "fewer than 6 frames after the release"}
    times = [t for t, _ in after]
    values = [y for _, y in after]

    spring = fit_spring(times, values)
    tween = fit_tween(times, values)
    rest = spring["rest_px"] if spring["rms_px"] <= tween["rms_px"] else tween["rest_px"]
    travel = rest - values[0]
    direction = 1.0 if travel >= 0 else -1.0

    def reached(share):
        limit = (1 - share) * abs(travel)
        for t, y in after:
            if abs(rest - y) <= limit:
                return t
        return float("nan")

    dp = args.px_per_dp
    settle = 0.0
    for t, y in after:
        if abs(rest - y) > dp:
            settle = t
    overshoot = max(0.0, max(direction * (y - rest) for y in values))
    crossings = 0
    sign = None
    for y in values:
        offset = direction * (y - rest)
        if abs(offset) < 0.5 * dp:
            continue
        current = offset > 0
        if sign is not None and current != sign:
            crossings += 1
        sign = current
    v_after_fit = spring["velocity0_px_s"]
    early = [(t, y) for t, y in after if t <= 0.025]
    v_after_data = slope(early) if len(early) >= 2 else float("nan")
    # How far the surface went in the first two 120 Hz frames after the lift, against how far the finger's speed
    # would have taken it: about 1 when the speed was kept, near 0 when the motion restarted from rest. Robust to a
    # few milliseconds of error in the release time, which the fitted initial velocity is not.
    window = 0.0167
    carry = float("nan")
    if v_before == v_before and abs(v_before) > 100:
        moved = position_at(rows, release + window) - position_at(rows, release)
        carry = moved / (v_before * window)
    family = ("spring" if spring["rms_px"] < 0.8 * tween["rms_px"]
              else "tween" if tween["rms_px"] < 0.8 * spring["rms_px"] else "ambiguous")
    zeta, omega = spring["damping_ratio"], spring["omega"]
    decay = fit_decay(times, values) if args.decay else None
    if decay and decay["rms_px"] < 0.8 * min(spring["rms_px"], tween["rms_px"]):
        family = "decay"
    return {
        "decay": decay,
        "px_per_dp": dp,
        "track": path,
        "release_s": release,
        "frames_after_release": len(after),
        "travel_dp": travel / dp,
        "t50_ms": reached(0.5) * 1000,
        "t90_ms": reached(0.9) * 1000,
        "t99_ms": reached(0.99) * 1000,
        "settle_within_1dp_ms": settle * 1000,
        "overshoot_dp": overshoot / dp,
        "overshoot_pct": 100 * overshoot / abs(travel) if travel else float("nan"),
        "crossings": crossings,
        "v_before_dp_s": v_before / dp,
        "v_after_fit_dp_s": v_after_fit / dp,
        "v_after_first25ms_dp_s": v_after_data / dp,
        "velocity_carry": carry,
        "family": family,
        "spring": {**spring, "share_from_rest_at_100_200_300ms":
                   [round(progress_from_rest(zeta, omega, s), 3) for s in (0.1, 0.2, 0.3)]},
        "tween": tween,
    }


def report(result):
    if "error" in result:
        print(f"{result['track']}: {result['error']}")
        return
    spring, tween = result["spring"], result["tween"]
    print(f"{result['track']}: release at {result['release_s']:.3f} s, {result['frames_after_release']} frames after it; "
          f"travel {result['travel_dp']:.1f} dp")
    print(f"  t50 {result['t50_ms']:.0f} ms, t90 {result['t90_ms']:.0f} ms, t99 {result['t99_ms']:.0f} ms, "
          f"settled within 1 dp at {result['settle_within_1dp_ms']:.0f} ms")
    print(f"  overshoot {result['overshoot_dp']:.1f} dp ({result['overshoot_pct']:.1f} % of travel), "
          f"{result['crossings']} crossings")
    carry = result["velocity_carry"]
    # Meaningful for releases of about 800 dp/s and faster; slower ones move too little in two frames.
    verdict = "kept" if carry >= 0.6 else "dropped" if carry <= 0.35 else "unclear" if carry == carry else "n/a"
    print(f"  speed before the release {result['v_before_dp_s']:.0f} dp/s; first 25 ms after it "
          f"{result['v_after_first25ms_dp_s']:.0f} dp/s; carry {carry:.2f} ({verdict})")
    print(f"  spring: damping {spring['damping_ratio']:.3f}, stiffness {spring['stiffness']:.0f} "
          f"(omega {spring['omega']:.2f}/s), iOS response {spring['ios_response_s']:.3f} s, "
          f"template bounce {spring['bounce_template']:.3f}, RMS {spring['rms_px']:.2f} px; "
          f"from rest {spring['share_from_rest_at_100_200_300ms']} at 100/200/300 ms")
    print(f"  tween: {tween['easing']}, {tween['duration_s'] * 1000:.0f} ms after {tween['delay_s'] * 1000:.0f} ms, "
          f"RMS {tween['rms_px']:.2f} px")
    decay = result.get("decay")
    if decay:
        print(f"  coast: rate {decay['rate_per_s']:.2f}/s (keeps {decay['kept_per_ms']:.4f} of its speed per ms; "
              f"iOS normal 0.998), coast {decay['coast_px'] / result['px_per_dp']:.0f} dp from "
              f"{decay['velocity0_px_s'] / result['px_per_dp']:.0f} dp/s, RMS {decay['rms_px']:.2f} px")
    print(f"  family: {result['family']}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("tracks", nargs="+", help="one CSV per trial; several trials of one scene are compared")
    parser.add_argument("--from", dest="start", type=float, help="ignore samples before this time (s)")
    parser.add_argument("--to", dest="end", type=float, help="ignore samples after this time (s)")
    parser.add_argument("--move-px", type=float, default=2.0, help="movement that marks the start of motion")
    parser.add_argument("--release", type=float, help="time the finger lifted (s); default: the start of motion")
    parser.add_argument("--swipe-ms", type=float, help="a scripted swipe's duration: the lift is searched near "
                        "motion start + this, where the track leaves the drag's straight line")
    parser.add_argument("--tolerance-px", type=float, default=3.0, help="distance from the drag line that marks the lift")
    parser.add_argument("--px-per-dp", type=float, default=720 / 1080 * 2.75)
    parser.add_argument("--decay", action="store_true", help="also fit a fling's exponential coast (scroll tracks)")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    results = [analyse(path, args) for path in args.tracks]
    if args.json:
        print(json.dumps(results, indent=2))
        return
    for result in results:
        report(result)
    good = [r for r in results if "error" not in r]
    if len(good) > 1:
        print("\ntrials: release speed (dp/s) -> spring omega, tween duration, carry")
        for r in sorted(good, key=lambda r: abs(r["v_before_dp_s"]) if r["v_before_dp_s"] == r["v_before_dp_s"] else 0):
            print(f"  {r['v_before_dp_s']:8.0f} -> omega {r['spring']['omega']:6.2f}, "
                  f"tween {r['tween']['duration_s'] * 1000:4.0f} ms, carry {r['velocity_carry']:.2f}  ({r['track']})")


if __name__ == "__main__":
    main()
