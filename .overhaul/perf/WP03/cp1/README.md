# WP03 checkpoint 1: press feel, selection, sheet, toast

**Recorded on the emulator `periflow_api35` (1080x2400, 420 dpi), not on the Redmi.** The phone (9a7c74f4) was
attached but PIN-locked (`dumpsys trust`: `deviceLocked=1`), and an agent may not enter a PIN. These recordings show
the *shape* of each motion (how far, how long, whether it overshoots) correctly, because springs run on time, not on
frames; they say nothing about smoothness on the phone. To re-record on the phone once it is unlocked:
`$OV/bin/locked adb $OV/out/WP03/scripts/cp1_session.sh 9a7c74f4 $OV/perf/WP03/cp1-phone` (about 3 minutes).

Build: the catalog of a generated project (`$OV/out/WP03/cp1`, package `com.example.wp03.cp1`, spec
`$OV/out/WP03/specs/cp1.json`), `:catalog:assembleRelease` (R8, signed with the debug key), speed-compiled, design
style Utility, motion style **Standard**, light theme. Page: the catalog's "Press and motion" section
(`--es section Feel`). Script: `$OV/out/WP03/scripts/cp1_record.py` (taps found by test tag, frames and strips made
with WP04's `perf.py`).

Each folder has the video (`<name>.mp4`), every frame (`frames/`), `frames.json` (frame times, moving frames, crop),
a full-screen strip of the moving frames (`<name>_strip.png`) and a **cropped strip at 2x around the control**
(`<name>_crop_strip.png`, the one to look at).

| Folder | What happens | Feedback / spring |
|---|---|---|
| `press_buttons` | Filled, Tinted, Plain: each held 450 ms, then tapped | Filled, Tinted: `Scale` to 0.96 on `press`, back on `release`; Plain: `Dim` to alpha 0.45, fades back over 200 ms |
| `press_card_row` | A clickable card and a list row: held 450 ms, then tapped | Card: `Scale` to 0.98 (anything 80 dp tall or more); row: `Highlight` (on at once, 220 ms fade out) |
| `switch` | Tap off, tap on, drag on to off, drag off to on | Thumb on `selection`; the thumb widens by 7 dp on `press` while held or dragged; `ToggleOn`/`ToggleOff` haptics |
| `segmented` | Tap Month, tap Day, drag Day to Year, drag Year to Day | Thumb on `selection`, follows the finger while dragged, `Selection` haptic on change |
| `sheet` | Open (rises to medium), drag to large (past it into the rubber band), drag back to medium, drag down to close | `sheet` spring, velocity carried from the finger, rubber band 120 px past large |
| `toast` | "Saved to your library" with Undo, 4.5 s on screen | Slides up by its own height on `sheet` with a 200 ms fade in, rests 12 dp above the bottom chrome, leaves down on `sheet` with a 120 ms fade |

## Spring values used (Standard motion style)

iOS `Spring(duration: response, bounce:)` mapped with mass 1: stiffness = (2π / response)², dampingRatio = 1 - bounce.

| Token | Response (s) | Bounce | Stiffness | Damping ratio | Used for |
|---|---|---|---|---|---|
| `press` | 0.16 | 0 | 1542 | 1.00 | finger down (scale to 0.96 / 0.98 / 0.90) |
| `release` | 0.32 | 0.15 | 386 | 0.85 | finger up; a tap quicker than the press delay first finishes the dip (80 ms) |
| `selection` | 0.30 | 0.09 | 439 | 0.91 | switch thumb, segment thumb, tab indicator, page dots |
| `screen` | 0.24 | 0 | 685 | 1.00 | push, pop (provisional; WP04 records 0.24 / 0.30 / 0.36) |
| `sheet` | 0.34 | 0 | 342 | 1.00 | sheet detents and dismiss, toasts, menus, alerts |
| `layout` | 0.36 | 0 | 305 | 1.00 | list placement, expand and collapse |
| `emphasis` | 0.50 | 0.15 | 158 | 0.85 | pop-ins, badges, the HUD |

Other press values: scale 0.96 (buttons, chips), 0.98 (cards and anything 80 dp tall or more), 0.90 (controls 56 dp
or smaller); dim alpha 0.45; highlight fade-out 220 ms; the press shows after the tap timeout (100 ms), so a scroll
that starts on a row does not flash it. Haptics are silent on plain taps.

Other motion styles (bounce, speed): Bouncy (0.35, 1.0): release damping 0.65, selection 0.79. Calm (0, 0.87): no
overshoot anywhere, screen 0.276 s, sheet 0.391 s. Snappy (0.10, 1.15): release 0.90, screen 0.209 s, sheet 0.296 s.

## What to decide

1. **Press**: is 0.96 with a 0.15 release overshoot the right feel for buttons, or should the release be flatter
   (Calm-like, bounce 0) or quicker (`press` 0.12 s)?
2. **Selection**: 0.30 s with bounce 0.09 for the switch and segment thumbs.
3. **Sheet**: 0.34 s, no bounce, for detents and dismiss.
4. **Row highlight**: the highlight colour is `contentPrimary` ink at 7 % (light) / white at 9 % (dark); iOS's cell
   highlight is stronger (about 18 %). Keep it subtle or match iOS?

## Bugs these recordings found (fixed on `wp/WP03` before this set was made)

- Rows never highlighted (`69837f4`): the highlight node was delegated from a node that draws itself, and Compose
  skips such delegates. Test: `RowHighlightTest`.
- The segmented control's thumb was invisible in scrolling pages (`abe0f1c`). Test: `SegmentedControlTest`.
- Toasts flew to the top of the window (`8eae2c0`). Test: `OverlayHostTest` "a toast rests just above…".
