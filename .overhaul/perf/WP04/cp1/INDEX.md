# WP04 checkpoint 1 package

Recorded 2026-10-09 09:26–09:30 IST on the Redmi Note 10 Pro (9a7c74f4, 120 Hz) by `out/WP04/phone-pending.sh`
(log `out/WP04/logs/phone-pending.log`), from the phase 2 tree `out/WP04/p2` (`devBenchmarkRelease`, built at
`311052d`, i.e. before `d56ad13`). Each folder has the `screenrecord` video, a frame strip and `frames.json`
(frame times, per-frame difference, moving and near-blank frame counts). No recording has a near-blank frame.

| Folder | What | Moving frames | Extra |
|---|---|---|---|
| `push_0.24/`, `push_0.30/`, `push_0.36/` | push from the sample list at screen response 0.24 / 0.30 / 0.36 s | 37 / 47 / 52 | `perf.screenResponse=<r>` |
| `pop_0.24/`, `pop_0.30/`, `pop_0.36/` | Back-key pop at the same responses | 34 / 39 / 47 | same |
| `tab_switch/` | tab switches (instant, before the tab reveal of MOTION-PLAN M24) | 57 | — |
| `gesture_pop/` | edge-swipe pop (Navigation 3's seeked slide, before the Gmail card of SPEC) | 78 | — |
| `rapid_back/` | three Back presses at depth 4 | 62 | — |
| `bar_labels_selected/`, `bar_labels_never/` | tab switch with labels on the selected tab only, and with no labels | 53 / 51 | `perf.barLabels=…` |
| `bar_minimize/` | scroll down then up with the bar minimizing | 311 | `perf.barMinimize=OnScrollDown` |

What the strips show: the incoming screen slides over the covered one with the parallax and the drawn scrim, the bar
stays in place at every depth, no frame is blank and no frame shows two screens at partial opacity. The rapid-Back
strip shows the first pop, a pause while that slide settles, then one coalesced slide from depth 3 to the root.

## Screen spring candidates (critically damped, 1080 px travel)

| Response | Stiffness | 50 % of travel | 90 % | 99 % | Settled to 1 px | Travel done at 100 ms |
|---|---|---|---|---|---|---|
| 0.24 s | 685 | 64 ms | 149 ms | 254 ms | 356 ms | 74 % |
| 0.30 s | 439 | 80 ms | 186 ms | 317 ms | 445 ms | 62 % |
| 0.344 s ("iOS-exact", MOTION-PLAN §1) | 334 | 92 ms | 213 ms | 363 ms | 510 ms | 55 % |
| 0.36 s | 305 | 96 ms | 223 ms | 380 ms | 534 ms | 52 % |

The Back gate releases counted presses at 99 % of travel.

## Solid-gate table and spike outcome

`perf/WP04/phase2/summary.md` (3 runs, same APK). The warm-pop spike (A3.10) adopted option A (`KeepUnderScene`),
but the decision rule fails on the phone: pop first frame 15.0 ms median / 36.9 ms max against 8 / 16 ms. The cause
(research/SMOOTHNESS-PLAN S1): the scene is keyed by the top entry, so every pop moves the covered entry between
scenes and detaches it. Phase 3 replaces it with a persistent stack scene (`StackScene`); its numbers are in the
WP04 handoff.

## Recordings added after the phase 3 changes

Listed in the WP04 handoff under Measurements: `perf/WP04/tab-switch/` (REQUIREMENTS 28),
`perf/WP04/predictive-back/` (REQUIREMENTS 24) and `perf/WP04/back-during-push/` (REQUIREMENTS 32).

## Not recordable

- A sheet over a tab root and a cover over a pushed screen in the app: no template feature registers a sheet or
  cover destination yet (WP03's `SheetScene` and a sample sheet). The catalog's Navigation section shows a cover in
  its framed `AppShell` demo.
