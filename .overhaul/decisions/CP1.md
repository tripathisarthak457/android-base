# Checkpoint 1: motion and press feel

Decided 2026-10-09 by the decision agent standing in for the user at CP1 (MASTER-PLAN B.5). The user asked not to be
asked about it (PROGRESS.md, "Decisions made on your behalf"). Every value below is a token, so changing one is cheap.
"How to override" at the end lists the recording to watch for each value.

Inputs:

- **Phone recordings** (Redmi 9a7c74f4, 720 × 1600 at about 120 fps, WP04's phase-2 build, 09:26–09:30):
  `perf/WP04/cp1/`.
- **Phone numbers**: `perf/WP04/phase2/summary.md`.
- **Emulator recordings** of press, switch, segmented, sheet and toast: `perf/WP03/cp1/`. These show the shape of
  each motion only.
- **Plans and requirements**: `handoff/WP03.md`, `handoff/WP04.md`, `research/MOTION-PLAN.md`,
  `research/ios-motion.md`, `research/ios-motion-catalogue.md` §1, `research/SMOOTHNESS-PLAN.md` (S1, S3),
  `research/predictive-back/SPEC.md` §4–5, and REQUIREMENTS 1, 2, 19–24 and 28.

**Method.** I opened every strip. I also measured the moving screen's edge on every frame with `cp1_edge.py` (in
this folder). It reports the edge of the white top bar on row 75, as a fraction of travel, and checks it against the
critically damped curve of each response. Numbers in this file come from that tool or from `frames.json` times. "f12"
means `frames/f_0012.png` of the named recording.

> **Note: `perf/WP04/cp1/INDEX.md` is out of date.** It still says the phone recordings are pending. They were made
> at 09:26–09:30 and are the ones used here.

---

## Decisions at a glance

| Item | Decision | Was |
|---|---|---|
| `screen` (push, pop, covers, back-card commit) | **0.344 s, bounce 0** (stiffness 333, ζ 1), divided by the style's speed | 0.24 s |
| `navigation.parallax` | **0.333** | 0.30 |
| Edge-shadow alpha | **0.04** (width stays 8 dp) | 0.12 |
| Scrim | keep 0.10 light / 0.24 dark | — |
| `press` | keep: 0.16 s, bounce 0 | — |
| `release` | keep: 0.32 s, bounce = style (0.15 Standard) | — |
| Press amounts | keep: scale 0.96 / 0.98 large / 0.90 compact; dim 0.45; highlight 7 % light / 9 % dark, 220 ms fade | — |
| `selection` | **retired into `thumb`: 0.28 s, bounce 0** (stiffness 504) for switch and segmented thumbs, page dots, tab reveal and bar minimize | 0.30 s, 0.6 × bounce |
| `sheet` | keep 0.34 s, bounce 0 | — |
| `dialog` (new) | 0.275 s, bounce 0, ÷ speed | `sheet` |
| Bar labels default | **Always** (`Selected` and `Never` stay as options) | Always |
| Tab switch content | **nudge and unveil, incoming tab only**: 12 dp toward the travel, veil α 0.6 → 0, on `thumb` | instant swap |
| Tab indicator | two edges: lead 0.22 s / bounce 0.15 (style), trail 0.32 s / 0; drawn width ≤ 1.5 slots; squash 0.06 | one position, 0.30 s / 0.09 |
| Tab icon bounce | **down then up**: 0.84 on `spring(1, 1500)`, then back on `spring(0.45, 500)` (peak ≈ 1.03) | none |
| Bar minimize | **triggered by direction** after 10 dp, animated on `thumb`; not scrubbed | scrubbed by scroll |
| BackGate release (Back key/button) | keep at **99 % of travel**, which is 363 ms at 0.344 s | 254 ms at 0.24 s |
| Gesture commit | **never held behind the seek's settle** (SPEC §4: `gate.hold` + `GesturePop`, no transition) | returns to rest, then pops |
| Pop rows (warm-pop spike fell back) | **not relaxed**: ≤ 8 / ≤ 16 ms stays the release gate. More work is scheduled (§7) | — |

Rules that hold for every value above:

- No bounce on anything that moves a full screen or the bar: `screen`, `sheet`, `thumb`, the tab reveal, minimize
  and the back-card commit all have ζ = 1.
- Bounce stays only where it is small and tactile: the release of a 4 % press dip, the indicator's leading edge,
  the icon's up-leg and `emphasis`.

---

## 1. Screen response: 0.344 s ("iOS-exact")

**Value.**

- `springs.screen` = response **0.344 s**, bounce 0, `bySpeed: true`. In Compose that is `spring(1f, 333f)`.
- Under the other styles: Calm 0.395 s, Snappy 0.299 s.
- Parallax **0.333**; edge-shadow alpha **0.04**; scrim unchanged.
- Reduce motion is unchanged: a 150 ms fade.

**How each candidate moves** (critically damped, computed):

| Response | Travel at 100 ms | At 200 ms | 99 % of travel (BackGate release) | Settled to 1 px of 1080 |
|---|---|---|---|---|
| 0.24 s | 74 % | 97 % | 254 ms | 356 ms |
| 0.30 s | 62 % | 92 % | 317 ms | 445 ms |
| **0.344 s** | **55 %** | **88 %** | **363 ms** | **510 ms** |
| 0.36 s | 52 % | 86 % | 380 ms | 534 ms |

**What the phone recordings show.** Each row is the first frame that moves. "Clock lead" is how far ahead of that
frame the spring's clock already is.

| Recording | First moving frame | Travel shown on it | Clock lead |
|---|---|---|---|
| `push_0.24` | f3, 17 ms | **37.5 %** (≈ 405 px at 1080) | 23–34 ms, held through the slide (f3–f28) |
| `push_0.30` | f4 (431 ms; this run's tap landed late) | 4.6 %, then 9.7, 15.3, 21 % | ≈ 0: a clean start |
| `push_0.36` | f3, 20 ms | **16.1 %** (≈ 174 px) | 15–23 ms |
| `pop_0.24` / `pop_0.30` / `pop_0.36` | f3–f4 | 2.1 % / 9.2 % / 1.1 % | ≤ 0 / 9–13 ms for 6 frames, then ±5 / ≤ 0 |

The 0.344 curve is never more than 2.5 % of travel from the 0.36 curve, so `push_0.36` and `pop_0.36` show what
0.344 will look like.

**Reasons.**

1. **It matches iOS.** UIKit's push is critically damped with ω₀ 18.26. That fits Flutter's iOS page curve with an
   RMS error of 0.0018 (catalogue §1.3), and it is the "UIKit push ≈ 0.35 s" the brief asks for. It has no bounce.
2. **0.24 is part of what the user calls "pushy".** It covers 74 % of the distance in 100 ms and 97 % by 200 ms, so
   the new screen is shoved in.
3. **The jump on the first frame makes it worse.** The spring's clock starts before the heavy composing frame is
   shown (SMOOTHNESS S3). So the first frame users see is already 23–34 ms into the spring:
   - at 0.24 that is a 405 px jump in one frame;
   - at 0.36 the same lost time shows only 16 %;
   - the warm start in `push_0.30` (f4–f7: 4.6, 9.7, 15.3, 21 %) shows what the fixed start looks like.
4. **0.30 is the only real alternative.** It shortens the BackGate hold by just 46 ms. Against that, it runs up to
   7.4 % of travel ahead of the UIKit curve, and its pops started with a late first frame (`pop_0.30` f3–f8).

**Costs accepted, and when to fall back.**

- **Back presses are held longer.** The gate's hold grows from 254 to 363 ms (§6).
- **Transitions settle later.** A full settle takes 510 ms instead of 356.
- **The GPU draws two screens for longer.** Push/pop already runs at GPU p50 10 ms and p90 12 ms per frame against a
  budget of 8.33 ms; today it is pipelined, and janky frames come mostly from the composing frame. So the longer
  transition is a risk, not a measured problem.
- **Fallback to 0.30.** After S1, S3 and S5 land, measure 0.344 and 0.24 on the same APK:
  `perf.sh pushpop --extra perf.screenResponse=<s>`. If 0.344 is worse by more than 0.3 percentage points of janky or
  1 ms of GPU p90, set `screen` to 0.30 and record the reason here.

**Conditions this value depends on (WP04 acceptance).**

1. **S3, the clock after composition.**
   - In a `record.sh push` strip at 0.344, the first moving frame shows **≤ 4 % of travel**. The spring is at 1.0 %
     one frame in and 3.8 % two frames in. Today 0.36 shows 16 % and 0.24 shows 37.5 %.
   - Check with `cp1_edge.py <dir> push --resp 0.344`. The lead must be within one frame (≤ 9 ms).
2. **S3.2, no content swap inside the slide** (WP02-M3 seeds the detail screen; WP06 adds `peek`).
   - In `push_0.36`, the detail screen swaps from skeleton to content at f3 → f5 (20–38 ms), mid-slide.
   - In `push_0.24`, it swaps at f26–f30 (210–243 ms), just after the slide.
   - Either way it reads as a glitch.

---

## 2. Press, release, selection and the other component springs

| Token | Decision | Why |
|---|---|---|
| `press` 0.16 s, bounce 0 | keep | 90 % of the dip in about 100 ms, close to iOS's 120 ms highlight. It only shows after the tap timeout, so a scroll that starts on a control does not flash it. |
| `release` 0.32 s, bounce = style | keep | Under Standard (ζ 0.85) the overshoot is 0.6 % of the 4 % dip: about 0.03 dp on a 120 dp button. It reads as a soft ease-out, not a bounce, which matches Apple's "0.15 is not felt as bouncy". Under Bouncy (ζ 0.65) the bounce becomes visible, which is that style's purpose. |
| Scale 0.96 / 0.98 (≥ 80 dp) / 0.90 (≤ 56 dp); `Dim` 0.45; `Highlight` 7 % / 9 %, 220 ms fade | keep | The user asked for subtle, spring-based feedback and "not a grey flash". The old flash was a 9 % overlay on every touchable. The row highlight is visible in `press_card_row` (f25) but stays below the old flash. iOS's cell highlight (≈ 18 %) is the override if rows feel unacknowledged. |
| `selection` → **`thumb`** 0.28 s, bounce 0 | change | This is iOS's segmented-thumb spring (k 503.55). The old 0.30 s / 0.09 overshoots by 0.1 %, which nobody can see, so this costs no feel. It is 20 ms quicker and joins one token family with the tab reveal and minimize. Bounce belongs on release, not on indicators (catalogue §5). WP03 decides whether `selection` is deleted or kept as an alias. |
| `sheet` 0.34 s, bounce 0 | keep | It matches the iOS page curve within 0.004 s. |
| `dialog` (new) 0.275 s, bounce 0, ÷ speed | add | iOS alert/action-sheet spring k 522. It is used for alerts, action sheets, menus and popovers. |
| `layout`, `emphasis`, `interactive`, `bounceBack`, `overscrollRelease`, `fling`/`flingFast` | as MOTION-PLAN §1 | Confirmed, not changed. |

**Caveat.** The WP03 recordings were made on the emulator, with 40–90 ms between captured frames. They show how far
each motion goes, not how smooth it is.

- In phase B, WP03 re-records on the phone: `$L adb $O/scripts/cp1_session.sh 9a7c74f4 $P/cp1-phone`.
- Change a value from that recording only if it shows a defect:
  - a press that flashes when a scroll starts on the control;
  - a scale that clips its neighbours;
  - a thumb that lags the finger after release.

---

## 3. Tab bar labels: `Always` by default

**Reasons.**

1. **iOS labels every tab bar item** (HIG). Labels are what makes five glyphs readable.
2. **`Selected` makes the bar jump on every switch** (`bar_labels_selected`, f1–f24):
   - On the switch frame (f2, 0 ms), the new tab's label appears under an icon that is not yet highlighted, while
     the pill is still on the old tab. The pill reaches it about 200 ms later.
   - The icons jump vertically by about 7 dp, because the labelled cell's glyph moves up and the old one recentres.
3. **`Never` drops the labels**, which keeps them only for TalkBack (`bar_labels_never`). It shows the same
   wash-out of the glyphs while the pill passes (f8–f16).

`Selected` and `Never` remain spec options. If 5 labels do not fit (large font), they ellipsize at the pill width
minus 20 dp (QA-03). More than 5 tabs become 4 + More (requirement 18; not a CP1 item).

---

## 4. Tab switch effect (requirement 28, MOTION-PLAN M24)

What exists now (`tab_switch`, f2–f28): the content swaps on the tap frame, and one blob-shaped pill slides on the
old selection spring. While the pill crosses them, the labels and glyphs wash out (QA-04).

**Content: "nudge and unveil", on the incoming tab only.**

- `translationX` = **12 dp** × the direction of travel → 0.
- A veil in `colors.background` at **α 0.6** → 0.
- Both run on `thumb`: 66 % done at 100 ms, 94 % at 200 ms.
- The outgoing tab disappears at once: no crossfade, no double exposure, no offscreen alpha.
- RTL flips the direction.
- A programmatic or deep-link switch gets no reveal; so does reduce motion.
- **Fallback:** if the veil pushes `tabs` GPU p90 above 8 ms or janky above 1 %, drop the veil and keep the nudge,
  which costs nothing.
- **Not QA-05's veil from α 1.** Starting fully opaque puts a blank, background-coloured frame on screen at the
  switch, which reads as a flash. The perf tooling's blank-frame check would flag it too.

**Indicator.**

- Two edges in slot units:
  - the leading edge on `indicatorLead`: 0.22 s, bounce = style, so ζ 0.85 under Standard and 1 under Calm;
  - the trailing edge on `indicatorTrail`: 0.32 s, bounce 0.
- The drawn width is clamped to 1.5 slots, and the height squashes by `1 − 0.06 · (stretch − 1)`.
- Pill geometry (QA-03): 44 dp tall in the 64 dp capsule (10 dp vertical inset). Width is
  `clamp(label + 20 dp, 52 dp, min(slot − 8 dp, 72 dp))`.
- Labels are drawn masked in two layers: no per-tab colour lerp, and the copy carries `clearAndSetSemantics`.
- Filled glyphs when selected.
- The existing `Select` haptic stays.

**Icon bounce on select.**

- The glyph scales down to **0.84** on `spring(1, 1500)`, then back on `spring(0.45, 500)`. The overshoot is 20 % of
  the dip, so it peaks at about 1.03. Most of it is over in about 300 ms, and it settles by about 500 ms.
- Under Calm the up-leg is ζ 1 (no overshoot). Reduce motion and programmatic switches get no bounce.
- Why down-then-up rather than QA-05's 1 → 1.12 → 1:
  - it is SF Symbols' bounce;
  - it reads as the icon being pressed, which suits a bar whose items have no press feedback of their own;
  - it never grows the glyph past the 44 dp pill.

**Seen in passing.**

- The pill visibly starts moving 20–35 ms after the content swaps (f49 → f51–f53 on Feed → Search; f2 → f4 on
  Home → Feed), because the selection spring starts from rest. The two-edge indicator starts on the same frame as
  the reveal, so the bar and the content move together.
- Profile's cold first visit draws its title alone, and the body appears about 50 ms later (`tab_switch` f100 →
  f104–f105). That pop-in belongs with S6/S7 (first use paid while idle, precomposed tab roots).
- Frame strips list only the frames whose whole-screen difference passes a threshold, and the pill alone stays under
  it. To judge bar motion, crop the bar from `frames/` rather than reading the strip.

**Acceptance.**

- `perf.sh tabs tabs_tap tab1`: janky ≤ 1 %, visited first frame ≤ 8 / ≤ 16 ms, GPU p90 ≤ 8 ms.
- RECOMPOSITION "Tab switch" budgets.
- Recordings into `perf/WP04/tab-switch/`: light and dark, 1× and 2× font, 4 and 5 tabs, RTL, and a cold first
  visit.

---

## 5. Bar minimize: triggered, not scrubbed

`bar_minimize` shows the problem:

- The collapse takes two frames: the full bar at f1, half collapsed at f2 (0 ms), a round button at f3 (13 ms).
- The expansion is the same: f157 → f158, 17 ms.
- At a normal fling speed, the collapse distance passes in one or two frames, so a scrubbed morph reads as a jump.

**Decision.**

- When the scroll direction flips past **10 dp** of hysteresis, the bar animates between expanded and minimized on
  **`thumb`**.
- An animation running in the other direction is reversed with its velocity kept, never snapped.
- The bar expands on a scroll up past 10 dp, on a tab tap, or at the top.
- The collapse progress is still read only in layout and draw, with `@FrequentlyChangingValue` on `collapse`.
- The `scroll_minimize` gate is unchanged: ≤ 0.5 % janky, GPU p90 ≤ 7 ms. It measured 0.3 % and 6 ms.

This replaces M9's "scrub, then settle with velocity". iOS 26's `tabBarMinimizeBehavior` is a state change, not a
fraction.

---

## 6. BackGate hold window

**Back key, back button, 3-button navigation and `EDGE_NONE`.**

- Presses are counted while the screens move. They are applied as one coalesced pop once the running transition
  reaches **99 % of travel**. At 0.344 s that is **363 ms** after the transition starts.
- Why not earlier: Navigation 3 starts the coalesced pop from rest. Whatever the first pop has not finished becomes a
  jump, on the arriving screen (parallax 1/3) and on the leaving screen:

  | Released at | Jump on the arriving screen | Leaving sliver that vanishes |
  |---|---|---|
  | 99 % | 3.6 px | 11 px |
  | 97 % | 11 px | 32 px |
  | 90 % | 36 px | 108 px |

  At 99 % neither can be seen.
- The `rapid_back` recording (0.24 s build):
  - The burst lands on the root, with no blank or double-exposed frame.
  - The first pop leaves a 3 % sliver at f24 (195 ms).
  - The intermediate screen then sits still for about 80 ms (f26–f35, 212–294 ms) before the coalesced pop starts
    at f36 (303 ms).
  - At 0.344, expect about the same pause: 97 % → 99 % takes 70 ms, plus the cold first frame.
  - Three presses from depth 4 settle at the root in about 0.9 s, against 0.6 s at 0.24 s. Accepted: the user's
    complaint was about glitches, not speed.
- **Lower the threshold only on `StackScene`.** Once S1 carries `slide` and its velocity from one change to the
  next, the threshold may drop, or M4's retarget variant may replace coalescing. That needs `rapid_back` and
  `nav_stress` green, and a frame strip with no snap.

**Back gesture.** A gesture's own commit must never wait for the seek to settle. `gesture_pop` shows what happens
today:

1. f7–f33 (58–282 ms): the screen follows the finger to 48 %.
2. f34–f52 (289–447 ms): after the release, it slides back to rest.
3. f52–f57: it stays there for 40 ms.
4. f58–f60 (496–511 ms): it pops from rest. The first frame jumps 13.5 %, and f60 jumps another 16 %.

That back-and-forth is exactly the "exit/back glitch" of requirement 1. The SPEC §4–5 design replaces it:

- the Gmail card;
- `gate.hold { card.commit(); pop(1) }`;
- `StackChange.GesturePop` mapped to no transition, with Back presses during the commit counted.

**Acceptance:** in a `gesture_pop` recording of a commit, the card never moves back toward rest after release, and
the list is the top screen within 120 ms of release. Recordings go into `perf/WP04/predictive-back/`, compared with
`research/predictive-back/gmail/key_frames.png`.

---

## 7. Pop rows: the warm-pop spike fell back; the rows are not relaxed

**Spike outcome (A3.10).** Option A (`KeepUnderScene`) was adopted from the source reading. On the phone it misses
the decision rule:

| Row | Measured (median of 3 runs) | Per-run values | Rule |
|---|---|---|---|
| Pop first frame | **15.0 / 36.9 ms** | medians 33.8, 14.9, 15.0 | ≤ 8 / ≤ 16 ms |
| Coalesced rapid-Back pop, slowest frame | **23.6 / 34.6 ms** | — | ≤ 8 / ≤ 16 ms |

- PSS at depth 4 was not measured on its own; the whole journey reached 93.8 MB against a gate of 70.
- Option B was not attempted inside the timebox.
- So the fallback applies, and the user's CP1 decision is needed: relax the pop rows, or schedule more work.

**Decision: do not relax them.** Requirement 19 makes the §2 gates the floor, and the target is no dropped first
frames. Scheduled, in SMOOTHNESS §4 order:

1. **S0: re-measure at HEAD** into `perf/WP04/phase2b/`. The phase-2 APK (08:13) predates `d56ad13` (08:54), which
   removed a hidden full-screen draw on every pop frame, so today's pop numbers are stale.
2. **S2:** the app and library baseline profiles. The merged profile has 0 app rules.
3. **S1:** `StackScene`, which is option B of A3.10. It parks the entries down to depth 3, so a pop and a coalesced
   pop both land on an entry that is already laid out. **S3**'s clock-after-composition comes with it.
4. **All four A3.10 fallback items, not a subset:** cheaper root rows, `LazyLayoutCacheWindow`, first-frame-light,
   and profiles.

**Until then** (so the merge order does not stall; R26):

- WP04 may merge with the pop rows and the coalesced-pop row as **non-regression ratchets at its phase-2b values**.
  The orchestrator writes them into `gates.toml` as it does for other ratchet rows.
- **The ≤ 8 / ≤ 16 ms rows gate the release** (WP13 / CP3).
- If they still fail after step 4, the orchestrator decides within the working session (B.5 rule 6), with a trace of
  the pop's first frame. It records any relaxation in PROGRESS.md under "Decisions made on your behalf", with the
  numbers, so the user sees it.

---

## 8. Hand-off

**Who takes what.**

- **WP03 phase B** writes the values into `tokens/motion.json`. The proposed shape is below; the schema is WP03's
  call.
- **WP04** takes `screen`, the parallax, the edge shadow, M24 (§4), M9 (§5), the BackGate rules (§6) and the pop-row
  plan (§7).
- **Unblocked:** WP02-M3, WP05 and WP12's motion docs.
- **WP12** documents the reasons in this file: the "pushy" first-frame jump, no bounce on full-screen moves, and why
  the gate releases at 99 %.

```json
{
  "springs": {
    "press":          { "response": 0.16,  "bounceScale": 0, "bySpeed": false },
    "release":        { "response": 0.32,  "bounceScale": 1, "bySpeed": false },
    "thumb":          { "response": 0.28,  "bounceScale": 0, "bySpeed": false },
    "indicatorLead":  { "response": 0.22,  "bounceScale": 1, "bySpeed": false },
    "indicatorTrail": { "response": 0.32,  "bounceScale": 0, "bySpeed": false },
    "screen":         { "response": 0.344, "bounceScale": 0, "bySpeed": true },
    "sheet":          { "response": 0.34,  "bounceScale": 0, "bySpeed": true },
    "dialog":         { "response": 0.275, "bounceScale": 0, "bySpeed": true },
    "layout":         { "response": 0.36,  "bounceScale": 0, "bySpeed": true },
    "emphasis":       { "response": 0.5,   "bounceScale": 1, "bySpeed": false }
  },
  "press": { "scale": 0.96, "scaleLarge": 0.98, "scaleIcon": 0.9, "dimAlpha": 0.45,
             "highlightAlpha": { "light": 0.07, "dark": 0.09 }, "highlightFadeOutMs": 220 },
  "navigation": { "parallax": 0.333, "scrimAlpha": { "light": 0.1, "dark": 0.24 },
                  "edgeShadow": 8, "edgeShadowAlpha": 0.04, "backGateRelease": 0.99 },
  "tabBar": {
    "labels": "Always",
    "reveal": { "shiftDp": 12, "veilAlpha": 0.6, "spring": "thumb" },
    "indicator": { "heightDp": 44, "maxStretchSlots": 1.5, "squash": 0.06 },
    "iconBounce": { "downScale": 0.84, "down": { "dampingRatio": 1, "stiffness": 1500 },
                    "up": { "dampingRatio": 0.45, "stiffness": 500, "calmDampingRatio": 1 } },
    "minimize": { "trigger": "direction", "hysteresisDp": 10, "spring": "thumb" }
  }
}
```

`interactive`, `bounceBack`, `overscrollRelease` and `fling`/`flingFast` keep their MOTION-PLAN §1 values.
`selection` is removed or kept as an alias of `thumb`.

---

## How to override

Change the token, rebuild, then re-record. To re-record a push or pop on the phone at a candidate response:

```
$L adb tools/perf/record.sh <appId>.dev push <out> --extra perf.screenResponse=<s>
```

Use `pop` in place of `push` for a pop. Then run `cp1_edge.py <out> push --resp <s>` to see the first-frame jump and
the curve.

| To change | Watch (under `perf/`) | What to look for |
|---|---|---|
| `screen` | `WP04/cp1/push_0.30`, `push_0.36` (the 0.344 stand-in), `pop_0.30`, `pop_0.36`; `push_0.24` for the old value | how far the screen gets in the first frames; how long the arrival lingers |
| Parallax, edge shadow, scrim | the same push clips, f3–f15 | the dark line at the incoming edge (today 0.12) |
| `press`, `release`, scale, dim | `WP03/cp1/press_buttons/press_buttons_crop_strip.png`, `press_card_row` (emulator; the phone set lands in `WP03/cp1-phone/`) | the depth of the dip; any visible rebound |
| Row highlight strength | `WP03/cp1/press_card_row` f25 | too faint at 7 %? iOS is about 18 % |
| `thumb` (switch, segmented) | `WP03/cp1/switch`, `segmented` | thumb speed; whether you want overshoot (you would set bounceScale > 0) |
| Bar label mode | `WP04/cp1/tab_switch` (Always), `bar_labels_selected` f1–f24, `bar_labels_never` f1–f24 | the label jump and the icon shift with `Selected` |
| Tab switch effect, indicator, icon bounce | not built yet. Today's instant swap: `WP04/cp1/tab_switch` f1–f28. M24's recordings land in `WP04/tab-switch/` | veil strength (0.6), nudge (12 dp), stretch, bounce depth (0.84) |
| Bar minimize | `WP04/cp1/bar_minimize/custom.mp4`, f1–f3 and f156–f158 | today's two-frame snap |
| BackGate hold | `WP04/cp1/rapid_back`, f20–f36 | the pause at the intermediate screen |
| Gesture commit | `WP04/cp1/gesture_pop`, f27–f60 | the return to rest before the pop (the defect) |
| Pop rows | `WP04/phase2/summary.md`; next `WP04/phase2b/` | pop first frame median / max |
