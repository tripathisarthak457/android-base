# Replicates LiquidLensState.animate()/step()/travel for a tap, on Compose's critically damped
# SpringSimulation (whole-millisecond steps, the sub-millisecond rest carried to the next frame).
# Run 11: travel = min(glass(energy), lift); each frame lift = min(glass(energy), lift + rise), with
# rise = min(distance moved / LIFT, frame time / LIFT_MS). Run 10's saturated lift is kept for comparison.
import math
K = (2 * math.pi / 0.28) ** 2          # thumb: response 0.28 s, bounce 0
W = math.sqrt(K)
SLOT, STILL, SHOWN, FULL, LIFT, LIFT_MS = 0.002, 0.002 * 60, 0.01, 6.0, 0.2, 32
PERIOD = 8_333_333

def spring(x, v, goal, ms):
    t = ms / 1000.0
    a = x - goal
    b = v + W * a
    e = math.exp(-W * t)
    return goal + (a + b * t) * e, (a + b * t) * e * -W + b * e

def eased(a):
    a = min(1.0, max(0.0, a))
    return a * a * (3 - 2 * a)

class Lens:
    def __init__(self, slot, run10=False):
        self.x, self.v, self.goal, self.run10 = float(slot), 0.0, float(slot), run10
        self.lift, self.origin, self.departure = 0.0, float(slot), 0.0
    def glass(self):
        return eased(math.sqrt(self.v ** 2 + K * (self.goal - self.x) ** 2) / FULL)
    def travel(self):
        if self.run10:
            return self.glass() * eased(self.departure / LIFT)
        return min(self.glass(), self.lift)
    def step(self, ms):
        frm = self.x
        nx, nv = spring(self.x, self.v, self.goal, ms)
        if abs(self.goal - nx) < SLOT and abs(nv) < STILL: self.x, self.v = self.goal, 0.0
        else: self.x, self.v = nx, nv
        if self.run10:
            if self.glass() < SHOWN: self.origin, self.departure = self.x, 0.0
            elif self.departure < LIFT: self.departure = max(self.departure, abs(self.x - self.origin))
        else:
            rise = min(abs(self.x - frm) / LIFT, ms / LIFT_MS)
            self.lift = min(self.glass(), self.lift + rise)
    def at_rest(self):
        return self.x == self.goal and self.v == 0

def drive(lens, taps):
    """taps: {frame: slot}. Per frame drawn while the lens moves: (frame, travel before the frame's
    selection, travel right after it, travel after the step, x)."""
    out, last, unspent, n = [], None, 0, 0
    while True:
        before = lens.travel()
        if n in taps: lens.goal = float(taps[n])
        after = lens.travel()
        if lens.at_rest():
            if n >= max(taps): return out
            last, unspent, n = None, 0, n + 1
            continue
        now = n * PERIOD
        if last is not None: unspent += now - last
        ms = unspent // 1_000_000; unspent -= ms * 1_000_000
        lens.step(ms)
        out.append((n, before, after, lens.travel(), lens.x))
        last = now; n += 1

def ms(frame): return frame * PERIOD / 1e6
def largest_rise(seq): return max(b - a for a, b in zip(seq, seq[1:]))

def describe(slots):
    f = drive(Lens(0), {0: slots})
    t = [0.0] + [r[3] for r in f]
    full = [r for r in f if r[3] >= 0.99]
    shown = [r for r in f if r[3] >= SHOWN]
    hand = next(r for r in f if r[3] < SHOWN and r[0] > shown[0][0])
    return (f"{slots} slot(s): lift " + " ".join(f"{r[3]:.2f}" for r in f[1:7]) +
            f"; full (>=0.99) {ms(full[0][0]):.0f}-{ms(full[-1][0]):.0f} ms; samples "
            f"{ms(shown[0][0]):.0f}-{ms(shown[-1][0]):.0f} ms, hands over at {ms(hand[0]):.0f} ms with "
            f"{slots - hand[4]:.4f} slot left; settles {ms(f[-1][0]):.0f} ms; largest rise {largest_rise(t):.3f}")

for slots in (1, 2, 4):
    print(describe(slots))

base = drive(Lens(0), {0: 1})
reference = largest_rise([0.0] + [r[3] for r in base])
shown = [r for r in base if r[3] >= SHOWN]
handover = next(r[0] for r in base if r[3] < SHOWN and r[0] > shown[0][0])
print(f"a second selection after a tap to the next tab, at every frame up to its hand-over (frame {handover}):")
for run10 in (False, True):
    label = "run 10 (saturated lift)" if run10 else "run 11"
    for second in (2, 0):
        worst, falls, pops = 0.0, 0, []
        for n in range(1, handover + 1):
            f = drive(Lens(0, run10), {0: 1, n: second})
            sel = next((r for r in f if r[0] == n), None)
            if sel is None: continue        # sent back to the tab it has not yet left
            seq = [sel[1], sel[2]] + [r[3] for r in f if r[0] >= n]
            rise = largest_rise(seq)
            worst = max(worst, rise)
            if sel[2] < sel[1] - 1e-9: falls += 1
            if rise > reference + 1e-6: pops.append(f"{ms(n):.0f} ms {sel[1]:.2f}->{max(seq):.2f}")
        print(f"  {label}, then to {second}: largest rise {worst:.3f} (a tap from rest: {reference:.3f}); "
              f"falls at the selection {falls}; faster than from rest at {len(pops)} frames"
              + (f", e.g. {', '.join(pops[:4])}" if pops else ""))
for n in (18, 20, 22, 24, 26, 28, 36):
    f = drive(Lens(0), {0: 1, n: 2})
    print(f"  run 11, a tap to the next tab at {ms(n):.0f} ms, then the next: " +
          " ".join(f"{r[3]:.2f}" for r in f if r[0] >= n and r[0] < n + 6))
for (a, b) in ((2, 0), (1, 0), (2, 1)):
    worst, falls, blinks = 0.0, 0, 0
    for rev in range(1, 40):
        f = drive(Lens(0), {0: a, rev: b})
        sel = next((r for r in f if r[0] == rev), None)
        if sel is None: continue
        back = [sel[2]] + [r[3] for r in f if r[0] >= rev]
        worst = max(worst, largest_rise([sel[1]] + back))
        if sel[2] < sel[1] - 1e-9: falls += 1
        peak = back.index(max(back))
        if not all(y <= x for x, y in zip(back[peak:], back[peak + 1:])): blinks += 1
    print(f"run 11, reversal {a}->{b} at every frame: largest rise {worst:.3f}, falls {falls}, second dips {blinks}")
print(f"K={K:.1f} sqrt(K)={W:.2f}")
