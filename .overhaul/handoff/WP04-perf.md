# WP04-perf: deep smoothness (branch `wp/WP04-perf`, worktree `$OV/wt/WP04-perf`)

- **Orchestrator note (Oct 18, 14:00).** New REQUIREMENTS 41 (iOS-like scrolling by default): this branch owns the scroll jank (≤ 0.5% at 120 Hz, aim ~0%) and the first-fling hitch after a cold start or `pm clear`; WP03 owns the physics defaults. Read item 41.


- **Orchestrator note (Oct 18, 13:40).** After the 10:05 stop, `out/WP04-perf/logs/phase10-win.log` ended `PHASE10 DONE` (10:12: MEASURE DONE, STARTUP-AB-TRACE DONE) and `phase11-win.log` ended `PHASE11 DONE` (10:39; the bar-minimizing scroll row is still not measured). The `cardclip` tab measurement (`measure.win.sh cardclip`, `TAG=-t1`, tabs and tabs_tap) has only partial output under `perf/WP04-perf/cardclip-t1/` from 10:14; check whether it finished before relying on it.


`OV=D:/projects/tools/.overhaul`, `O=$OV/out/WP04-perf` (builds and scripts),
`P=$OV/perf/WP04-perf` (phone results and traces). Started 2026-10-15 15:45 from `wp/WP04` at `3c9c403`.
Phone: Redmi Note 10 Pro `9a7c74f4` (Snapdragon 732G: 6 × A55 at 576–1708 MHz, 2 × A76 at 652–2304 MHz,
Adreno 618; 120 Hz; custom ROM, SDK 37, HWUI Skia GL). Trace tooling: the android-profiler skill's
`trace_processor` (Perfetto v57).

## 0. Decision for the perf escalation (Oct 18; the user's call, `gates.toml` unchanged)

The floor build (every screen's content removed: a switch draws one fill, the bar and the fade) misses
FrameTimeline's 13.67 ms deadline on **7 of 7** tab switches from idle (first frame vsync-to-GPU-done median
18.2 ms, max 22.0 ms; GPU alone 8.3 ms median at the idle clock). The fixed build is 23.8 ms median, head (KeptTabs) 21.2 ms
and this branch's tip 19.2 ms: **1 ms above the floor**. Like for like (main thread on a big core) the real screens add
about 1 ms of UI time and 0.3–0.5 ms of GPU to what the empty ones cost.
Everything after a switch's first frame is on time: `tabs` and `tabs_tap` have **0.00 %** janky frames other than
the first, and a hitch ratio of **0 ms/s** (median of every build; 0.78 ms/s in one run each of head and tip). So "first frame within one vsync" and
"`tabs.janky_pct` ≤ 1 %" cannot be met on this phone's idle clocks by any app (`tabs.janky_pct` is exactly one frame
per switch, 1.07–1.15 % on every build, the floor's included).

The same holds for push/pop and Rapid Back (`$O/latestage.py` over `$P/tip/run2`): a pop's first frame takes 2.0–2.4 ms
of UI and 1.6–5.2 ms of RenderThread, then 9.4–11.4 ms of GPU against 5.5 ms for the same pop's steady frames, so
it ends 14–20 ms after its vsync; the GPU needs about three frames to clock up (frame 2: 7.3–12.6 ms). Rapid Back's
late frames are its first two (frame 0 GPU 9–12 ms, frame 1 RenderThread 6–10 ms); every later frame ends 10–13 ms
after its vsync. WP04's one-fill experiment already showed that drawing less does not shorten these frames
(`exp-onefill`: first-frame GPU 10.6 → 10.7 ms with a full-screen fill removed). In the nav capture
(`$O/sql/rtslow.sql` on `$P/tip-nav`) the RenderThread ran every frame under 5 ms on a big core at **652–662 MHz,
the big cluster's floor**, and 76 % of its frames over 8 ms on a little core: the governor keeps the clocks down for
this load, and HWUI's own ADPF hints do not move them.

**Recommendation:** gate what users see. For tab switches, push/pop and back: the hitch ratio
(`*.hitch_ms_per_s`) and `*.janky_after_first_pct` (the motion's own jank) as release limits, with
`*.late_first_frames` reported; keep the current `*.janky_pct`, first-frame and p99 rows as non-regression
ratchets at the values below (as WP04's `gates-merge.toml` already does for its merge). Numbers in §3.

### 0a. Every gap, where it stands (Oct 18)

Phase 2 and the old template from `STATUS.md` §4; "now" is this branch (`layer` = `7da2c84`'s tree, `$P/layer-p7`,
three runs; tab rows also `$P/layer-t1`, `layer-t2`), the same tools and phone.

| Gap | Old / phase 2 | Now | Target | Status |
|---|---|---|---|---|
| Tab switch, janky | 1.58 % / 1.90 % | 1.07–1.15 %, all of it the switch's first frame; after the first 0 %, hitch 0–0.79 ms/s | ~0 % | first frame: device limit (§0, the floor misses on 7 of 7); the motion itself meets it |
| Tab switch, first frame UI median / max | 19.0 / 45.3 ms | 4.1–4.2 / 9.4–10.4 ms | ≤ 8 / 8.3 ms | median met; max is the main thread on a little core (§3) |
| Push/pop, janky / p99 | 4.03 % / 34 ms; 2.34 % / 57 ms | 1.56 % / 26 ms; after the first 0 %, hitch 2.0 ms/s | ~0–0.5 % / ≤ 16 ms | p99 is the push's composing frame and the pop's first frame from idle: device limit (§0) |
| Pop, first frame UI median / max | 19.1 / 27.6; 15.0 / 36.9 ms | 2.3 / 5.1 ms | ≤ 8 / 16 ms | met |
| Rapid Back, janky | 5.32 % / 2.62 % | 0.92 %; after the first 0.31 % | ≤ 1 % | met |
| Back gesture, janky | 3.54 % / 2.82 % | 2.4–2.9 %; after the first 0.9–1.3 %, hitch 15–23 ms/s | ~0 % | open: RenderThread frames on a little core while the card follows the finger, and the commit fade's GPU time (§2d, §2h) |
| Scroll, janky / GPU p90 | 0.22 % / 6 ms; 0.15 % / 6 ms | 0.22–0.44 % / 6 ms | ≤ 0.5 % (aim ~0) / ≤ 7 ms | met; the janky frames left are GPU programs compiled on first use after `pm clear` (§2f, §2i) |
| Cold start | 336 ms (Oct 9) / 355 ms | interleaved with the old template on one day: old 393, before the fix 396, **with it 365 ms** (§2e) | below the old template | met: the Play Store checks start after the first frame (`9ed16d25`) |
| PSS | 109.8 / 93.8 MB | total 107 MB, app share 54 MB (WP04 `final`) | 70 MB total | the 70 MB total is below the window's own buffers on this phone (§2c) |

## 1. What the first frame of a transition is made of (measured)

Every gap below is the same frame: the first one after the app has been idle. Measured on the r16 trace
(`$OV/perf/WP04/r16-trace/run1/trace.perfetto-trace`, HEAD-1 build) and on `$P/dvfs/trace.perfetto-trace`
(the installed `338d26c` build, with cpufreq and scheduling; per-frame SQL in the scratchpad queries `q11`, `d4`,
`d6`, reproduced in §6).

| Stage of the first frame after idle | Steady frames of the same animation | First frame |
|---|---|---|
| UI thread (composition + layout + record) | 1.8 ms | 6–11 ms with the main thread on a big core at **652 MHz** (its floor), 32–34 ms on a little core at 1248 MHz |
| RenderThread | 2.7 ms | 3–10 ms |
| GPU (`waiting for GPU completion`) | 5.5 ms | 9–15 ms (the GPU clock is still at its idle level) |
| Total from vsync | 9.5 ms | 17–29 ms against FrameTimeline's **13.67 ms** deadline |

- The CPU and the GPU both start every transition at their lowest DVFS level. The touch-down boost on this ROM
  raises the little cluster from 576 to 1248 MHz and leaves the big cluster at 652 MHz; the main thread lands on
  either. No app API changes that (HWUI's ADPF hint session is active, `sendCpuLoadResetHint` is in every frame,
  and the clocks do not move).
- SurfaceFlinger tags these first frames `Non Animating / Unspecified Present` (not app jank), but HWUI's
  `FrameDeadline` check counts them, so gfxinfo's janky % is exactly one frame per transition: 1/87 = 1.15 % on
  `tabs`, the push/pop p99 and the gesture janky % are the same frames plus the frames queued behind them.
- Scroll: the late mid-fling frames (`App Deadline Missed`, 14–15 ms) are the ones where the scheduler moved the
  RenderThread to a little core for one frame (RT 7–8 ms instead of 3.1 ms for the same display list; `q13`).
  The GPU is 5 ms per scroll frame throughout.
- The pop's first frame composed the whole leaving screen (416 composable executions in `$OV/out/WP04/logs/inv-compose2.txt`):
  once the entry has left the stack it is not in any page, so `paneContexts` gave it `SinglePane`, and
  `LocalPaneContext` is a static local, so a new value composes the entire subtree. A push does not do this
  (the covered list keeps its `List` context).
- Every tab switch recomposed every cell of the bar: `onTap` in `AppTabBar` captured `selected`, so it was a
  new lambda per switch and every `TabCell`'s `selectable` modifier changed.
- The pill's two clip layers and its fill used a path of cubics (`addCapsule`), which Skia cannot clip
  analytically; as a rounded rect the same shape is an analytic clip.
- `AppScaffold` painted `colors.background` over `EntryFrame`'s on every frame of every screen: one full-screen
  fill (2.6 Mpx) per frame, about a third of the GPU time of a frame.

## 2. Fixes on `wp/WP04-perf`

| Fix | Files | Why |
|---|---|---|
| A leaving entry keeps its pane context | `stack/StackLayout.kt` (`rememberPaneContexts`) | the pop's first frame no longer composes the leaving screen |
| The bar's tap handler reads the selection through state | `component/navigation/AppTabBar.kt` | a switch recomposes the two cells that flip, not all ten |
| The pill is a rounded rect, not cubics | `component/navigation/TabCells.kt` | analytic clip and fill on every frame the pill moves |
| A screen's host tells it the background is painted | `foundation/layout/Chrome.kt` (`LocalPaintedBackground`), `stack/EntryFrame.kt`, `presentation/PresentationHost.kt`, `component/container/AppScaffold.kt` (`AppScaffoldDefaults.background()`) | one full-screen fill per frame instead of two |
| Tests | `recomposition/ShellRecompositionTest.kt` | a pop with a pane-reading screen composes nothing of it; a switch recomposes at most two cells |

Commits (34 navigation tests green, `$O/logs/test-then-build-fixed.log`): `6436b75` pane context, `1ec7462` bar
tap handler, `b7e8074` rounded-rect pill, `ac56e69` one background fill; merged into `wp/WP04` as `730f8e0`.
WP04 then committed `shell/tabs/KeptTabs.kt` (the hidden tab's lifecycle drops the frame after the switch, not
in it) as `fb866b3`. The `feature/sample/.../detail/*` change (the related rows wait for
`LocalNavigationSettled` before they compose, the documented rule; they composed 185 composables 220 ms into
every push) is WP02-M3's item 1, not this package's: it is saved as
`$O/sample-detail-related-after-settle.patch` (pointer in `handoff/WP02-M3.md` item 1) and the four files were
restored in this worktree on Oct 17.

Round 2 (Oct 18): `22a9ada` (the bar's back-card cutout as a rounded rect, §4a), then `wp/WP04` merged at `a256582`
(its Windows perf tools, `janky_after_first_pct`, re-recorded baseline profiles); `55e144d` (the page beneath the
back card drawn only around it, §2a); `wp/WP04` merged again (`0ec58cc`, then a fast-forward to `f60c0c7`: WIN's
generator fixes, a uiautomator dump that hangs is ended and retried, the y-follow swipe); `7da2c84` (each screen in a
layer under the slide's clip and scrim, §2b); `e2fdb08` (the dev launch hook checks for a stored session without
decrypting it, §2e).

| Fix (round 2) | Files | Why |
|---|---|---|
| The bar's back-card cutout is a rounded rect | `shell/BarAnchor.kt` | analytic clips around and inside the card on every gesture frame |
| The page beneath the back card is drawn only around it | `stack/StackScene.kt` (`drawAroundCard`), `stack/StackLayout.kt` (`widths`) | the opaque card hid 81–100 % of a page and a scrim drawn on every gesture frame |
| Each screen is a layer below the scene's draw | `stack/StackLayout.kt` (`sceneDraw`) | a slide frame no longer records both screens again; the RenderThread prepares no new display lists |
| The dev launch hook reads no token | `app/src/dev/.../perf/DevLaunchOptions.kt` | five Keystore binder calls on the main thread in every measured cold start, none in a user's |
| Tests | `back/BackCardDrawingTest.kt`, `stack/SlideRecordingTest.kt` | pixels around and under the card; no screen recorded on the frames a pop or a gesture moves it |

## 2a. Back gesture: the page beneath the card (done, `55e144d`)

- **Measured** (before and fixed builds, `$O/latestage.py` over `$P/<build>/run*/gesture_pop_*.txt`): while the card
  follows the finger every frame takes 7–14 ms of GPU and 2–3 ms of RenderThread (spikes of 7–12 ms when the
  RenderThread lands on a little core); UI is under 1.5 ms. GPU time over the 8.33 ms frame keeps three buffers
  queued, and a frame misses when a RenderThread spike meets a deadline that has reset to 13.7 ms (frames 14, 20,
  26 of a pop). That is the 0.94 % of janky frames after the first.
- **Cause:** under the card, `StackScene.drawOver` drew the whole page beneath and a full-screen scrim over it,
  then the opaque card covered 81–100 % of both: about 1.6 full-screen passes per frame drawn for nothing. The
  slide already clips the covered part of the lower page away; the card did not.
- **Fix (`stack/StackScene.kt`, commit `55e144d`):** while the card is opaque
  (not during the commit fade) the page beneath and its scrim are drawn only around the card, in up to four
  whole-pixel strips (scissors), with the card's corners left to the strips; the hole is the card's rectangle less
  its corner radius at the top and bottom, shrunk to whole pixels so no seam opens, sized from the subject's own
  laid-out width (`StackScene.widths`, written beside `lefts`), since a pane can be narrower than the host.
- **Checks:** 127 navigation unit tests green with it (`$O/logs/tests-nav-cardstrips.log`); a pixel test,
  `back/BackCardDrawingTest` (Robolectric native graphics), asserts that every pixel around the card, its rounded
  corners included, shows the page beneath under the scrim and none shows through the card; the phone A/B
  (`$O/phase3.win.sh`: `cardclip`, built from the worktree, against `tip` on `pushpop_gesture`, `pushpop` and
  `rapid_back`, plus `$O/rec-gesture.win.sh`, a recording of a slow swipe held at 45 %) ran at 02:59–03:12
  (`$O/logs/phase3-win.log`; the first attempt's run 3 hit a harness failure, so the numbers are
  `$P/cardclip-retry/summary.md`).
- **Result** (gesture frames after the first, all runs pooled, `$O/gesturestats.py $P/tip $P/cardclip-retry`):

  | Gesture frames | Tip `a256582` | Cardclip |
  |---|---|---|
  | GPU median / p90 / p99 (ms) | 6.03 / 8.98 / 12.45 | 5.68 / 8.52 / 11.68 |
  | RenderThread median / p90 / p99 (ms) | 2.35 / 5.76 / 10.88 | 2.16 / 4.78 / 8.61 |
  | UI median (ms) | 0.97 | 1.15 |
  | Late frames | 20 of 1081 (1.85 %) | 17 of 1094 (1.55 %) |
  | `pushpop_gesture` janky / after the first / hitch | 2.4 % / 0.8 % / 16.4 ms/s | 2.25 % / 0.66 % / 13.57 ms/s |

  The recording (`$P/cardclip-rec/gesture.mp4`, frames in `frames/`, crops in `sheet/`) shows the page beneath
  under the scrim in the card's rounded corners and along every edge, with no seam. Push/pop and rapid Back did not
  move (1.66 % / 1.07 % janky, within the runs' spread). The UI time rose by 0.18 ms: the strips' bounds read the
  card's pose, so the page beneath is recorded again on every frame of the gesture; §2b removes that cost.

## 2b. Every frame of a slide recorded both screens again (done, `7da2c84`)

- **Measured** (`$P/tip-nav/trace.perfetto-trace`, four push / Back / push / back-swipe cycles from idle,
  `$O/sql/mainslices.sql`): over 1010 frames the main thread spent 0.87 ms per frame in `Record View#draw()` and
  1.14 ms in `postAndWait` (waiting for the RenderThread to take the frame). In slide frames `postAndWait` is often
  3–10 ms with the main thread running only 10–40 % of the frame (`$O/sql/heavyframes.sql`): the slide is bound by
  the RenderThread and the GPU, and the UI thread waits on them.
- **Cause:** `StackScene.drawOver` reads `cover` (snapshot state) for every entry before it branches, and the strips
  of §2a read the card's pose. Both are read in the entry's draw node, and nothing put a layer between that node and
  the screen: `EntrySlot` is `graphicsLayer { pose }.drawWithContent { drawOver }` around `entry.Content()`, whose
  content draws straight into the same RenderNode. So every frame of a push, a pop or a back gesture recorded both
  screens' whole drawing again on the UI thread and handed the new display lists to the RenderThread, though only
  the clip, the scrim and the shadow over them changed.
- **Fix (commit `7da2c84`):** `stack/StackLayout.kt`, `sceneDraw()`: the screen goes
  in a layer of its own after the scene's draw (`.graphicsLayer()`), so a slide frame records the few operations
  around each screen and draws the screens from the display lists they already have. Both slots (entries and the
  empty detail pane) build the same chain, now in one function. Test `stack/SlideRecordingTest.kt`: a pop and a
  back gesture record neither screen on the frames they move it. Robolectric never draws the window between
  frames, so a first version that only counted the screens' `drawBehind` passed with and without the fix
  (`$O/logs/test-slide-mutation.log`); the test now draws the window into a `RenderNode` after every frame, as the
  renderer does (a layer whose content has not changed is drawn from its kept display list), and checks first that
  this path is live: the first draw records the top screen, a second draw with nothing changed records nothing.
  `$O/test-slide.win.sh` runs it with the fix and with the layer line removed (`$O/logs/test-slide2.log`): green
  with it; without it both cases fail, the page beneath recorded 32 times in 30 gesture frames and 120 times in one
  pop (`test-slide-mutation2.log`). 127 navigation unit tests green with the fix (`$O/logs/tests-nav-layer.log`).
- **Measured** (phase 7, `$O/phase7.win.sh`, `$O/logs/phase7-win.log`, 05:55–06:36: `cardclip` = `55e144d`'s tree,
  `layer` = it plus this fix, built from the worktree; three runs each, `$P/cardclip-p7`, `$P/layer-p7`; then a
  nav capture of each, `$P/<build>-nav`):

  | | Cardclip | Layer |
  |---|---|---|
  | Nav capture, main thread per frame: traversal / recording (ms) | 2.30 / 1.11 | 1.77 / 0.69 |
  | Nav capture, RenderThread `DrawFrames` mean (ms); frames 5–8 ms / ≥ 8 ms | 4.62; 166 / 85 | 4.46; 128 / 73 |
  | Gesture frames after the first, UI median / p90 (ms) | 1.12 / 1.63 | 0.78 / 1.37 |
  | Push/pop janky / after the first / p99 / hitch | 1.57 % / 0 / 25 / 3 ms/s | 1.56 % / 0 / 26 / 2.02 ms/s |
  | Rapid Back janky / after the first / slowest pop frame median | 0.76 % / 0.46 % / 5.5 ms | 0.92 % / 0.31 % / 5.18 ms |
  | Back gesture janky / after the first / late frames after the first | 2.43 % / 0.81 % / 16 | 2.67 % / 1.2 % / 25 |

  The main thread records 38 % less per frame and the RenderThread does not do more (its slow frames are fewer).
  The gesture's late frames rose in this A/B (8, 8 and 9 per run against 5, 8 and 3), all RenderThread spikes of
  6–11 ms, the kind the scheduler makes by putting the RenderThread on a little core; phase 8 (`$O/phase8.win.sh`)
  measures the gesture alone in both orders to tell the fix from drift before this is committed. Phase 8 never
  reached the phone (it was still waiting for the adb lock at 08:01 when the session that queued it ended; its log
  holds only the waits); its steps run again as phase 9 (`$O/phase9.win.sh`, `$O/logs/phase9-win.log`).
- **Phase 9** (`$O/phase9.win.sh`, 08:40–09:02): `pushpop_gesture` alone, cardclip then layer, then layer then
  cardclip, three runs each (`$P/<build>-g1`, `-g2`), read with `$O/abstats.py` (frames after each dump's first; a
  hitch is a present more than 1.5 vsyncs after the one before):

  | `pushpop_gesture`, three runs each | cardclip g1 | layer g1 | layer g2 | cardclip g2 |
  |---|---|---|---|---|
  | Pop half: late frames / RenderThread ≥ 6 ms / hitches | 18 / 92 / 32 | 18 / 86 / 32 | 20 / 104 / 27 | 17 / 60 / 38 |
  | Pop half: hitch (ms/s), UI median (ms) | 32.7, 1.19 | 30.0, 0.83 | 26.4, 0.87 | 35.4, 1.18 |
  | Push half: RenderThread ≥ 6 ms (of about 1127 frames), p90 (ms) | 206, 6.40 | **63, 2.73** | **68, 3.05** | 181, 6.33 |
  | perf.py janky / after the first / p99 / hitch (ms/s) | 2.67 / 1.07 / 26 / 19.2 | 2.94 / 1.34 / 25 / 20.5 | 2.53 / 0.93 / 28 / 15.0 | 2.55 / 0.94 / 26 / 23.2 |

  The gesture's own frames are the same with and without the layer: the spread between the two orders of one build
  (cardclip: 92 against 60 slow RenderThread frames) is wider than the difference between the builds, so phase 7's
  rise was drift. The push that precedes each gesture is where the layer shows on the RenderThread: without it every
  slide frame hands the RenderThread two new display lists to prepare, and a fifth to a sixth of the push's frames
  take 6 ms or more there; with it, 6 %. Phase 7's push and pop rows agree (RenderThread ≥ 6 ms, push 280 → 221,
  pop 29 → 13; rapid Back hitches 18 → 11). Committed as `7da2c84` after 130 navigation unit tests and
  `:core:navigation:lintDebug` passed with it (`$O/logs/tests-nav-layer2.log`).
- **Phase 11, tab switches** (`$O/phase11.win.sh`, 10:12–10:39, `$O/logs/phase11-win.log`): phase 7 had seen more slow
  RenderThread frames in `layer`'s tab switches, though a switch moves nothing the layer wraps. `tabs` and
  `tabs_tap` alone, cardclip then layer, then layer then cardclip, three runs each (`$P/cardclip-t1`, `layer-t1`,
  `layer-t2`, `cardclip-t2-retry`: cardclip's second order lost run 1 to a harness failure, `perf.sh exit 2`, and the
  retry finished; every other "exit 1" in the log is the `tabs.janky_pct` gate, as expected). `$O/abstats.py
  'tabs(_tap)?_\d+\.txt'`, frames after each dump's first:

  | Tab switches, three runs each | cardclip t1 | layer t1 | layer t2 | cardclip t2 |
  |---|---|---|---|---|
  | Frames / late / RenderThread ≥ 6 ms | 7739 / 1 / 188 | 7738 / 1 / 87 | 7739 / 1 / 126 | 7740 / 0 / 169 |
  | Display hitches (ms/s) | 3 (0.39) | 3 (0.39) | 3 (0.39) | 2 (0.26) |
  | UI median, RenderThread median / p90, GPU median / p90 (ms) | 0.99, 1.40 / 2.18, 4.80 / 6.97 | 1.01, 1.40 / 2.12, 4.80 / 6.65 | 1.01, 1.41 / 2.20, 4.78 / 6.82 | 1.01, 1.42 / 2.19, 4.74 / 6.83 |
  | perf.sh `tabs` janky / first frame median / max (ms) | 1.07 % / 4.14 / 9.99 | 1.15 % / 4.08 / 9.41 | 1.07 % / 4.09 / 9.66 | 1.07 % / 4.17 / 9.30 |

  The layer leaves tab switches as they were (fewer slow RenderThread frames in both orders, the same janky %, first
  frames and hitches); phase 7's difference was drift.

## 2d. What the display shows: where the remaining hitches are (Oct 18)

gfxinfo's janky % counts a frame that missed HWUI's deadline; the hitch ratio counts what the display shows (a
present more than 1.5 vsyncs after the one before; perf.py's `hitch_time`). `$O/hitchpos.py` lists every hitch of
every dump with its position in the transition and the frame's stages (UI, RenderThread, GPU); over `$P/layer-p7`
(three runs, the layer build):

| Scenario (frames) | Hitches | Where |
|---|---|---|
| Tab switches, `tabs` + `tabs_tap` (3912) | 2 | first three frames |
| Pop with the Back key (1536) | **0** | |
| Push (1532) | 8 | 5 in the first three frames (GPU 13–17 ms: the idle clock), 3 on frames 2–3 where the detail's post arrives a frame after the composing one (UI 12–16 ms: WP02-M3's item 1, the patch in §2) |
| Rapid Back (1962) | 11 | 9 in the first three frames |
| Back gesture, pop half (1102) | 40 | 33 while the card follows the finger, 7 in the commit fade |
| Scroll, root and minimizing (720) | 10 | one 34–43 ms RenderThread frame in every run of both scenarios, one 17 ms one in four of six (§2f) |

So a tab switch's 1.07 % janky is HWUI's count of a late first frame, which the display shows as the switch starting
one vsync later, never as a stutter inside it: no motion frame is held on screen. The same holds for the pops. The
back gesture is the one motion that still hitches mid-way:

- **While the card follows the finger** (frames 2–28): every hitch is a RenderThread frame of 6–12 ms
  (`$O/abstats.py`: 6.5–8.6 % of gesture frames reach 6 ms). On a big core the RenderThread spends 3.2–3.5 ms on a
  gesture frame (`$O/sql/rtframes.sql` + `$O/bursts.py` over the nav captures: sync 0.27, replaying the display
  lists into Skia 0.95–1.0, flush 1.05, swap 0.66 ms), less than on a push or a pop (3.8 ms); the slow frames are the
  same work on a little core (`$O/sql/rtlong.sql`: 7–12 ms running on CPUs 0–5). They come every 4 or 8 frames
  (frames 6, 14, 22 of one pop; 3, 7, 11, 15 of another), the rhythm of a feedback loop, not of content (every
  gesture frame draws the same things): the UI thread does 0.7 ms per gesture frame, so the frames HWUI reports to
  its ADPF session are short, which fits the boost lapsing, the RenderThread moving to a little core, the next frame
  overrunning and the boost returning. This phone's power HAL leaves no trace of its decisions (no hint-session or
  uclamp counters in the capture), so the loop is inferred from the rhythm, not seen.
- **In the commit fade** (frames 29–41: the card returns to full size while it fades over 100 ms): GPU 7.4–8.9 ms
  median per frame (max 12.9) against 5–6 ms while dragging and 4–4.5 ms once the list is alone. The card is
  translucent, so the page beneath, the scrim and the card are each drawn whole (§2a's strips need an opaque card):
  about three full-screen layers of fill, at the clock the gesture left the GPU at. Seven of the 40 hitches.
- **Overdraw, checked on the phone** (phase 9, `$O/overdraw.win.sh` → `$P/layer-overdraw/`: HWUI's overdraw view
  of `layer`, dark theme, `debug.hwui.overdraw` set and put back to `false`): the list's background is drawn once
  and its cards twice; the detail's body once, its top bar and cards twice; with the back card held at about 40 % the
  page beneath shows only around the card (its cards three times: background, card, scrim), the card's interior
  draws as the detail does, and only two bands the height of the card's corner radius, along its top and bottom
  edges, reach four or more (page beneath, scrim, card background, top bar or tab bar): about 6 % of the screen,
  the price of §2a's strips leaving the corners to the page beneath. No fill is drawn twice for nothing.

## 2h. Back gesture: what is left, and the one lever not pulled

- **What is left** (§2d, phases 7 and 9): the gesture's janky frames after the first (0.9–1.3 %) and its hitches are
  RenderThread frames of 6–12 ms while the card follows the finger (33 of 40 hitches) and GPU frames of 7.4–12.9 ms
  in the 100 ms commit fade (7 of 40). The same RenderThread work takes 3.2–3.5 ms on a big core; the slow frames are
  that work on a little core, in a rhythm of every 4 or 8 frames, not in any frame whose content differs. The UI
  thread does 0.7–0.9 ms per gesture frame and records neither screen (§2b).
- **Levers tried or ruled out:** drawing less under the card (§2a, done), recording less per frame (§2b, done),
  analytic clips (§4a, done), ADPF hints (HWUI already runs a hint session; the clocks do not move, §0), pinning the
  RenderThread to big cores (`sched_setaffinity` through JNI: core numbering differs per SoC and it fights the
  platform's own placement, below the AOSP bar).
- **The one app-side lever left, a decision for the user:** draw the card from a hardware layer while the gesture
  runs (`CompositingStrategy.Offscreen` on the subject's pose layer from the gesture's first frame to the end of the
  commit fade). The card's content would be rendered once into a texture, and each gesture frame would draw one
  textured quad under the card's clip instead of replaying the whole screen: the RenderThread's per-frame work would
  drop to roughly the page beneath plus one quad, which fits a little core's frame, and the commit fade would fade
  one texture instead of every draw of the screen. It costs a full-screen texture (about 10 MB of GPU memory) for
  the gesture's length, one extra full-screen pass on its first frame, and text drawn from a texture scaled to
  0.9–1.0 (filtered, as the system's own cross-activity back animation scales the closing window's buffer). A11.3 rule 1 forbids an
  offscreen layer on a whole screen, written against fades that re-render the layer every frame; this one would be
  rendered once per gesture unless the screen's own content animates under the card. Not built: it changes a
  project rule, which is the user's call.

## 2e. Cold start against the old template

- **Interleaved A/B** (phase 9, `$O/coldstart-ab.win.sh`, 15 rounds of one start each, the old template as the phone
  has it from Oct 9 and `layer`; `$P/coldstart-ab/base-vs-layer.tsv`): old template **382 ms** median, `layer`
  **403 ms**; `layer` is slower in 13 of 15 rounds, by 22 ms at the median round. Both are above the 336 / 355 ms of
  earlier sessions: the phone was warmer, which is why only a same-session A/B compares them.
- **Where a start's time goes** (`$P/tip-startup`, `$P/layer-startup`, `$P/base-startup`, three starts each as perf.sh
  makes them; `$O/sql/startstages.sql`, `startline.sql`, `window.sql`, `mainbinder2.sql` + `$O/binderphases.py`,
  `startstates.sql`): from the fork, `bindApplication` ends at about 205 ms (Firebase's eager components 63 ms of it:
  Crashlytics 41, sessions 13, the rest 9; androidx.startup 7.6), the activity is created, started and resumed by
  about 277 ms, the splash frame lays the window out, and the first frame with content runs from about 330 ms for
  77–98 ms: composition 13–17 ms, measure 31–39 ms (the tab's subcomposition 8.5, text layout about 16), recording
  7–15 ms, then the main thread waits for the RenderThread's first frame (12–33 ms: recording into Skia with the first
  glyphs, and nine GL programs loaded from HWUI's disk cache at 0.6–1.7 ms each).
- **Stage by stage, `layer` against the old template** (medians of three traced starts each): `bindApplication` 120 /
  124 ms, activity start 48 / 58, resume 38 / 28, first content frame at 344 / 348 ms lasting 89 / 91 ms (composition
  14 / 26, measure 37 / 22, recording 7 / 6.5, RenderThread 31 / 32), the main thread running 239 / 221 ms. Its
  synchronous binder calls take 91 ms per start against 75 (`binderphases.py`: 15 calls to system_server inside
  `bindApplication` against 10, two more in the activity's start, the same number in the resume but 6.6 ms longer).
  Three traced starts each differ by less than their own spread (traced, `layer` 428 ms median against 441), so the
  traces do not yet show where the A/B's 22 ms is; phase 10 alternates the two in one capture (`$O/startup-ab-trace.win.sh`,
  six of each, `$P/startab-base-layer`).
- **A dev-only Keystore read in every measured start (found; fixed for the next measurement):** between the splash
  frame and the first content frame both apps' main threads make five binder calls to `keystore2` (8–13 ms) after
  loading `OpenSSLAeadCipherAES$GCM`. In the new template that is `DevLaunchOptions.beforeStart` (the dev flavour's
  launch hook, which `StartupViewModel` waits for before the first frame): with `perf.fakeSession`, every launch read
  the access token to see whether a session existed, and reading it decrypts it through the Keystore, on the main
  thread. The phone's build of the old template does the same at the same moment, so the A/B above is even on it,
  but no user's start does it: release flavours bind no launch hook, and `StartupViewModel` reads only whether the
  token keys are present. `app/src/dev/.../perf/DevLaunchOptions.kt` now asks `tokens.isAuthenticated.first()` (the
  stored keys, no decryption) and saves the synthetic session only when none is stored (commit `e2fdb08`; the
  dev build compiles with it, `$O/logs/build-dl.log`). Phase 10 measures it: `dl` = `layer` plus this, perf.sh's
  cold start on both in both orders, and a capture of `dl` alternated with the old template.
- **Phase 10** (`$O/phase10.win.sh`, 09:49–10:12, `$O/logs/phase10-win.log`): perf.sh's cold start, `layer` then
  `dl`, then `dl` then `layer`, three runs each (`$P/layer-c1`, `dl-c1`, `dl-c2`, `layer-c2-retry`; `layer-c2` lost
  run 3 to a harness failure and was retried). Pooled over 36 starts each: `layer` **399 ms** median, `dl` **387 ms**
  (per order 394 / 388 and 399 / 382): the dev hook's Keystore read cost 12 ms of every measured start. The second
  capture the script planned (`startab-base-dl`) did not run (the log ends after `dl` was prepared); phase 12 replaces
  it.
- **The traced A/B** (`$P/startab-base-layer`: six cold starts of each, alternated in one capture;
  `$O/sql/startstages2.sql`, medians of six): `bindApplication` 111.5 / 113.1 ms (`layer` / old), activity start 55.3 /
  56.9, **resume 37.1 / 25.1** (`performResume` **15.9 / 7.6**; the main thread's binder calls inside the resume 19 /
  17, 22.7 / 15.5 ms), first content frame 92 / 84.3 (composition 13.2 / 24.2, measure 35.9 / 21.2: the work moves
  between the two, their sum is 49 / 45.4). Main thread running 237 / 223 ms, sleeping 147 / 128 ms
  (`startstates.sql`; per phase in `$O/sql/startphases.sql`). The resume is the one stage longer in every pair:
  `performResume` by 3.8–10.9 ms. After the same two `Settings` reads, the new activity loads classes (class-init
  and `ClassLinker` lock slices) and makes two more binder calls to system_server: the Play Store block of
  `MainActivity` (`repeatOnLifecycle(RESUMED)` on `Main.immediate` runs `AppUpdateManagerFactory.create(...)
  .requestUpdateFlow()`, which binds Play's service, and `ReviewPrompt.onLaunch` inside `performResume`). The rest of
  the resume (adding the window) also takes longer in system_server (`wm.addWindow` 4.2 against 3.0 ms in the first
  pair). The old template has no Play Store feature, app lock or theme follower.
- **A capture artifact, not app time:** in all four traced `layer` starts and three of the six old ones, the main thread
  sleeps 18–36 ms one to three milliseconds into `activityStart`, and the thread that wakes it is the app's own
  `TracingMuxer` (`$O/sql/waker.sql`: `sched_wakeup` events for the main thread at the end of each sleep). That is the
  in-process Perfetto client connecting to `traced` while a capture runs; untraced starts do not have it. Traced
  totals (`am start` 409–452 ms against perf.sh's 380–400) overstate both apps, unequally, so totals come only from
  untraced runs and traces are read stage by stage.
- **Phase 12** (`$O/phase12.win.sh`, 13:49–13:54, `$O/logs/phase12-win.log`):
  - Interleaved A/B, the old template against `dl` (HEAD `e2fdb08`'s tree), 15 rounds
    (`$P/coldstart-ab/base-vs-dl.tsv`): old **392 ms** median, `dl` **393 ms**; per round `dl` is slower in 8 of 15,
    by 7 ms at the median round (phase 9: `layer` 21 ms slower in 13 of 15). The old build still makes the dev
    hook's Keystore read and `dl` no longer does, so for a user (no dev hook in either) the new template is still
    about 10 ms slower, most of it the resume (`performResume` +8 ms).
  - CPU callstack samples of six alternated starts each (`$O/startup-ab-perf.win.sh` → `$P/startabperf-base-dl`,
    `traced_perf` at 500 Hz; `sql/startsamples.sql`, `sql/startstacks.sql`, `$O/startprofile.py` puts R8's names back
    from each build's `mapping.txt`). `traced_perf` runs on this user build for the profileable benchmark APKs, but
    it kept 58–67 samples per start for about 230 ms of main-thread running (half were dropped) and four per start
    in the activity's start: too few to put a 5 ms difference on code. Not used for any conclusion here.
  - Without `task_rename` events, a process forked late in a capture keeps the name `zygote64` in the process table:
    `startsamples.sql` finds each start's main thread as the one that ran its `bindApplication` (the earlier queries
    that match by name silently drop those starts: the two `[NULL]` rows above).
- **Fix (commit `9ed16d25`, `app/.../MainActivity.kt`):** the Play Store checks (`followPlayStore()`: the update
  flow and the review prompt, still under `repeatOnLifecycle(RESUMED)`) start from the effect that already waits for
  the first content frame, posted so they run in a message of their own. `:app:lintDevDebug` green
  (`$O/logs/lint-app-play.log`), the generator's 291 tests green, and a `none` preset generated without
  `playstore` drops the call with the function.
- **Phase 13** (`$O/phase13.win.sh`, 15:20–, `$O/logs/phase13-win.log`; `dlplay` = `dl` plus the fix, built as
  `com.example.wp04.dlplay.dev` by `PKG=… build-wt.win.sh` so the two alternate on the phone):
  - Interleaved, untraced, 15 rounds of the old template, `dl` and `dlplay` (`$P/coldstart-ab/base-dl-dlplay.tsv`):
    medians **393 / 396 / 365 ms**; without the five starts under 300 ms (a process `am force-stop` left warm;
    every build has one or two) 395 / 403 / 365.5. Round by round `dlplay` beats `dl` in 13 of 15 (by 25 ms at the
    median round) and the old template in 10 of 15 (by 15 ms).
  - Traced, five alternated pairs (`$P/startab-dl-dlplay`, `startstages2.sql`; `dl` / `dlplay`, medians):
    `performResume` 14.2 / 5.3 ms, the resume 42.9 / 24.2 ms (its binder calls 19 / 17, 25.4 / 16.5 ms: the window's
    own calls run slower while system_server serves the binding), the first content frame 83 / 76 ms, the start
    437 / 408 ms. The Play Store process (`com.android.vending`) ran 18.6–27.2 ms of CPU inside every `dl` start
    and none inside `dlplay`'s (`$O/sql/startvending.sql`): the update check woke it in the middle of the start.
  - perf.sh's rows, `dl` then `dlplay`, then `dlplay` then `dl`, three runs each (`$P/dl-c3`, `dlplay-c3`,
    `dlplay-c4`, `dl-c4`): cold start (speed-profile) **399 / 375** and **383 / 371 ms**, as installed **397 / 376**
    and **394 / 378 ms** (`dl` / `dlplay`). `dlplay` meets the release gate's 380 and 400 ms rows in both orders; `dl`
    missed the 380 ms row in the first.
  - So the new template now starts 15–28 ms faster than the old one on the same day (median round, medians), with
    the dev hook's Keystore read gone from it and still in the old build (about 10 ms; neither does it for a user):
    for a user's start the two are level or the new one slightly ahead.

## 2f. Scroll: shaders compiled on first use (after `pm clear`)

- **Measured** (phase 10, `$O/scroll-trace.win.sh` → `$P/layer-scroll/first.perfetto-trace` and
  `cached.perfetto-trace`: perf.py's scroll, six flings down the root list and six back up, right after `pm clear`
  and a launch, then the same flings again; `$O/sql/shadercomp.sql`): the first pass has four RenderThread frames with
  shader compiles, all `cache_miss` → `driver_compile_shader` → `driver_link_program`: one `CircularRRectOp` on the
  first fling down (28 ms, a 37 ms frame), and when the flings reach the top of the list a `FillRectOp`, three
  `CircleOp`s and a `TextureOp` in one frame (49 ms of compiles, a 52 ms frame), then two more `CircleOp` frames (16
  and 16 ms; 20 and 19 ms frames). The second pass compiles nothing; its slowest RenderThread frame is 12.8 ms.
- **Cause:** the frames at the top edge are `AppPullToRefresh`'s indicator (`core/designsystem/.../refresh/
  AppPullToRefresh.kt`, unchanged since the old template): its `onPostScroll` takes any source, so the leftover of a
  fling that reaches the top pulls the indicator out (a pill `AppSurface` with an elevation shadow, faded in with
  layer alpha, so drawn offscreen and then as a texture, around `AppCircularProgress`'s arcs), and nothing retracts
  it until the next scroll: the cached pass's first fling down still draws it (its `CircleOp`s start in that pass's
  first frames). Each of those draws is a GL program nothing earlier in a run uses. `pm clear` empties HWUI's shader
  cache, so every perf.py run meets them inside the scroll scenario: they are the 34–43 ms frame in every run of
  `scroll_root` and `scroll_minimize` and the 17 ms one in four of six (§2d). A user meets the compiles once per
  install or update (HWUI keeps compiled programs on disk; the second pass compiles nothing), but meets the
  indicator on every fling to the top.
- **Not changed here:** the indicator is WP03's, and `wp/WP03` has already rewritten it: its `onPreScroll` and
  `onPostScroll` act on `NestedScrollSource.UserInput` only (and the old one's coroutine launched per scroll event,
  A11.3's forbidden pattern, is gone), so after WP03 merges a fling no longer draws it and these compiles leave the
  scroll rows. Until then the steady scroll is clean: in the second pass no frame needs more than 12.8 ms of
  RenderThread, and `scroll_root` / `scroll_minimize` pass their `solid` rows (0.22–0.37 % janky against 0.5, GPU
  p90 6 against 7) with these frames included. After the merge, re-measure the scroll rows and check whether WP03's
  rubber-band edge has first-use programs of its own; if it does, perf.py's warm-up can pay one overscroll and report
  it as a first use, as it does for the first back swipe (`first_gesture`), which is the user's and WP04's call
  because it changes what the scroll rows measure.

## 2i. The first scroll after `pm clear` (REQUIREMENTS 41: no hitch on the first fling)

- **Measured** (phase 14, `$O/firstfling.win.sh` on `dl`, twice: `$P/firstfling-dl-1`, `-2`; read with
  `$O/ffcompile.py`): after `pm clear`, a launch and a settle, the finger goes down on the root list and moves it
  20 px at a time (`input motionevent`, each step's device uptime in `steps.txt`, the same boot clock as the
  capture). In both runs exactly one GPU program is compiled, a `CircularRRectOp` (25.3 / 25.2 ms, a 32 ms
  RenderThread frame), between the 60 and 80 px steps; nothing else compiles until the flings later reach the top
  edge (the pull-to-refresh programs of §2f) apart from one 1–2 ms `FillRectOp`. With touch slop (8 dp, 22 px)
  taken off, 60–80 px of finger is 38–58 px of scroll, and the first card's top edge sits 36–44 px below the list's
  top edge at rest (`0-rest.png`): **the program is the one a card needs once it is cut by the edge of the list.**
- **Cause:** `AppClickableSurface` (the old template's `foundation/AppSurface.kt`, unchanged on this branch)
  clips every card to its shape (`.clip(shape)`) and draws the card's fill and border inside that clip. While a card
  is wholly inside the list, the only clip that touches its fill is its own rounded outline, the same shape as the
  fill, which Skia can drop (Ganesh's pre-clip step reports a single rrect clip, and an rrect drawn inside an equal
  one needs no clip effect). Once the list's own clip cuts the card, two clip elements apply, the rounded outline
  becomes an analytic coverage effect on the fill, and that op-plus-effect is a GPU program no earlier frame used:
  compiled on the first such frame after an install, an update or `pm clear` (HWUI keeps it in its disk cache
  afterwards). This is the reading that fits the step the compile lands on in both runs; phase 15 tests it.
- **Not changed here, and why:** `wp/WP03` has rewritten the surface (`foundation/content/AppSurface.kt`): content
  is clipped only when a caller asks (`clipContent = false` by default, "a clip costs a layer"), and its clickable
  `AppCard` uses `pressable(feedback = Scale)` with no overlay. Its cards carry no rounded clip, so they should never
  need the program; the same rewrite stops a fling from pulling the refresh indicator out (§2f). Changing the old
  file here would only collide with that rewrite.
- **Phase 15 did not test it:** `wp/WP03`'s head (`c55a8fce`, built as `wp03` by `$O/build.win.sh`) has no
  `perf.fakeSession` launch hook, so after `pm clear` it stays on onboarding (`$P/firstfling-wp03-1/0-rest.png`) and
  its captures (`firstfling-wp03-*`, `wp03-scroll`) hold no list frames. WP03's rubber band is still unmeasured on
  first use; that needs the merged tree.
- **Phase 16** (`$O/phase16.win.sh`) tests the reading directly: `noclip` is HEAD's everything project with one
  change made in the generated copy only (`AppClickableSurface` draws the fill and border before `.clip(shape)`),
  `firstfling.win.sh` twice.

## 2c. PSS: which process the row measures, and the floor

- **The row measures a fresh process.** `perf.py`'s `SCENARIOS` order runs `idle`, `nav_stress`, `restore`, then
  `meminfo`. `restore` kills the app in the background and brings it back, so in a full run (WP04's `final`,
  `$OV/perf/WP04/final/run1/meminfo.txt`) `meminfo` reads a process that has drawn one restored screen. My
  measurements leave out `restore`, so they read the process that made the whole journey. Same tree (`7fa49d5`
  against this branch's `a256582`, which only adds `22a9ada`), same morning:

  | `dumpsys meminfo` (MB) | WP04 `final` (after `restore`) | `tip` (after the journey) |
  |---|---|---|
  | Native heap | 13.9 | 27.8 |
  | `.so mmap` private clean | 0.1 | 11.8 |
  | Java heap / code | 16.6 / 14.5 | 18.2 / 26.5 |
  | Graphics (EGL mtrack: the window's buffers) | 53.3 | 55.2 |
  | **App PSS (total less graphics) / total** | **54.9 / 108.2** | **82.2 / 137.2** |

  The 25–27 MB between them is native: a library mapped privately (11.8 MB) and native heap (+14 MB). A journey that
  starts after `pm clear` compiles every GPU program in the process, and the restored process found them in the
  shader cache the first one wrote; the Adreno compiler is a large library loaded on first use. `$O/mem-ab.win.sh`
  ran a shorter journey (every tab twice, three push/pops, three flings) twice on `layer`
  (`$P/layer-mem/meminfo-*.txt`): after `pm clear` app PSS 65.6 MB (native 17.0, `.so` 4.3), with the cache warm
  58.7 MB (native 14.1, `.so` 2.1). So the shader cache explains about 7 MB, **not** the 11.8 MB of private `.so`
  pages and the 14 MB of native heap in `tip`'s dump: those came from something else in `perf.sh`'s longer journey
  (gestures, rapid Back, the scroll flings, the restarts of `tab1`). Attributing them needs a per-mapping view
  (`showmap`, root), which this session did not take; it is open.
  I did not change `perf.py`: moving `meminfo` before `restore` changes what the release row measures, which is a
  gate decision (the user's and WP04's).
- **The 70 MB total cannot be met on this phone by any app.** Graphics is 53–55 MB in every dump of every build,
  the old template's included: EGL mtrack is 51.4 MB, the window's buffers at 1080×2400 and 120 Hz (WP04 Deviation
  20 measured the same). That leaves 15–17 MB for the Java heap, native heap, code and stacks together, less than the
  runtime alone takes in a fresh process (`final`: Java 16.6 + native 13.9 + code 14.5 MB). WP04 already gates the
  app's own share (`pss_app_mb` ≤ 48 MB in `solid`); the 70 MB line in `STATUS.md` §4 should read as that.

## 3. Measurements

Round 1's comparison never finished on Linux (`$O/logs/measure2.log`: the before build ran two of three runs,
then `perf.sh` exited 2 when uiautomator could not dump the screen; fixed and floor were never measured). Its
partial results were moved to `$P/*-linux-oct16`. On Windows (Oct 18) every build is generated again from its own
commit (`$O/build.win.sh`, a `git archive` of the commit under `$O/s/<commit>`, so the worktree is never switched)
and all four are measured in one adb hold (`$O/measure.win.sh`, `perf.sh --runs 3 --gate solid` with this
worktree's perf tooling, then `dvfs-trace.win.sh`), driven by `$O/pipeline.win.sh` (`$O/logs/pipeline-win.log`):

| Build | Commit | What it is |
|---|---|---|
| `everything` | `3c9c403` | before round 1 |
| `fixed` | `730f8e0` | round 1 merged into `wp/WP04` |
| `floor` | `730f8e0` | fixed with every entry's content removed: a switch draws one fill, the bar and the fade |
| `head` | `fb866b3` | `wp/WP04` with KeptTabs (the hidden tab's lifecycle drops the frame after the switch) |

Results (Oct 18, 00:23–01:40; `perf.sh` medians of three runs, `$P/<build>/summary.md`; janky "after the first"
computed from the same runs' gfxinfo dumps by `$O/afterfirst.py`, which applies perf.py's own
`janky_after_first_pct`):

| Row | Before `3c9c403` | Fixed `730f8e0` | Head `fb866b3` | Tip `a256582` | `solid` limit |
|---|---|---|---|---|---|
| Tab switch, janky (%) / after the first (%) | 1.07 / 0 | 1.15 / 0 | 1.07 / 0 | 1.07 / 0 | 1 |
| Tab switch, late first frames / switches | 14 / 15 | 15 / 15 | 14 / 15 | 14 / 15 |  |
| Tab switch, hitch (ms/s) | 0 | 0 | 0 | 0 | ratchet |
| Tab switch, first frame UI median / max (ms) | 4.29 / 11.13 | 4.7 / 10.94 | 4.04 / 9.82 | 3.95 / 9.41 | 8 / 16 |
| Tab switch, p99 / GPU p90 (ms) | 21 / 10 | 20 / 7 | 19 / 8 | 20 / 7 |  |
| Plain taps, janky (%) / after the first (%) | 1.15 / 0 | 1.15 / 0 | 1.15 / 0 | 1.07 / 0 | 1 |
| First visit to a tab, first frame median / max (ms) | 4.21 / 9.74 | 4.9 / 10.39 | 4.04 / 9.26 | 3.86 / 8.69 | ratchet |
| Push, composing frame median / max (ms) | 7.29 / 15.53 | 7.64 / 16.72 | 7.34 / 15.73 | 10.04 / 15.59 | ratchet |
| Pop, first frame median / max (ms) | 4.44 / 9.31 | 2.56 / 6.09 | 2.35 / 5.37 | 2.34 / 5.16 | 8 / 16 |
| Push/pop (Back key), janky (%) / after the first (%) / p99 (ms) | 1.78 / 0.2 / 30 | 1.56 / 0 / 28 | 1.67 / 0.1 / 29 | 1.57 / 0 / 29 | 2 / – / 25 |
| Push/pop, hitch (ms/s) / GPU p90 (ms) | 12.15 / 12 | 1 / 9 | 4.01 / 10 | 3.01 / 10 |  |
| Back gesture, janky (%) / after the first (%) / p99 (ms) | 3.06 / 1.23 / 30 | 2.55 / 0.94 / 28 | 2.54 / 0.93 / 27 | 2.4 / 0.8 / 26 | 2 / – / – |
| Back gesture, hitch (ms/s) / GPU p90 (ms) | 25.1 / 12 | 20.43 / 10 | 21.72 / 10 | 16.4 / 10 |  |
| Rapid Back, janky (%) / after the first (%) / p99 (ms) | 1.85 / 0.93 / 19 | 1.22 / 0.61 / 17 | 0.92 / 0.61 / 14 | 1.07 / 0.46 / 14 | 4 |
| Rapid Back, coalesced pop slowest frame median / max (ms) | 7.55 / 10.58 | 6.02 / 6.36 | 5.67 / 5.74 | 5.58 / 5.83 | 8 / 16 |
| Scroll root, janky (%) / GPU p90 (ms) | 0.98 / 6 | 0.81 / 6 | 0.52 / 6 | 0.22 / 6 | 0.5 / 7 |
| Scroll with minimize, janky (%) / GPU p90 (ms) | 0.88 / 7 | 0.96 / 6 | 0.37 / 6 | 0.37 / 6 | 0.5 / 7 |
| Recompositions per switch / push / pop | 1 / 5 / 2 | 1 / 5 / 2 | 1 / 5 / 2 | 1 / 5 / 2 | 2 / – / 2 |
| Cold start, speed-profile / as installed (ms) | 354 / 408 | 385 / 441 | 368 / 403 | 355 / 408 | 380 / 400 |
| PSS app / total (MB) | 79 / 133.7 | 84.7 / 140.1 | 80.5 / 135.6 | 82.2 / 137.2 | 48 / – |

`Tip` is this branch at `a256582` (`wp/WP04` at `7fa49d5`, with its re-recorded baseline profiles, plus `22a9ada`);
table from `$O/gap-table.py`. Two rows move between runs for a reason that is not the code: the push's composing
frame and the first visit's first frame are bimodal, 6–7 ms with the main thread on a big core and 12–16 ms on a
little one (`$O/latestage.py <run dir> 'push_\d+\.txt' all`), so a median of eight pushes flips with the scheduler (head's runs
6.4 / 10.2 / 7.3 ms, tip's 12.8 / 10.0 / 7.2 ms); and the scroll rows are RenderThread stalls only ("Slow issue draw
commands" is every janky scroll frame of every run; a 38 ms one in `fixed/run1/scroll_root_089.txt`, frame 30),
consistent with the shader compiles of the first stretch overscroll after `pm clear` that the r16 trace showed (not
traced again this round), so they vary from 3 to 17 frames per run.

The floor cannot run `perf.sh` (its start waits for a row of `primary_list`; `floor attempt 1/2: perf.sh exit 2`
in the log, as designed): it is judged on its trace.

First frame of a tab switch from idle, Perfetto (`$P/<build>-trace/trace.perfetto-trace`, plain and 250 ms
press-hold taps after a warm pass, `$O/firstframes.win.py` → `firstframes.txt`, slice breakdown
`$O/framebreak.win.py` → `framebreak.txt`); vsync to GPU done, against FrameTimeline's 13.67 ms:

| Build | UI median / max | RenderThread median / max | GPU median / max | Total median / max | Late |
|---|---|---|---|---|---|
| Before | 10.9 / 12.3 | 3.6 / 7.9 | 9.9 / 13.8 | 24.8 / 28.6 | 7 / 7 |
| Fixed | 10.9 / 14.5 | 2.5 / 8.1 | 8.6 / 12.3 | 23.8 / 31.4 | 6 / 6 |
| Head (KeptTabs) | 6.9 / 10.0 | 5.8 / 7.5 | 8.4 / 13.2 | 21.2 / 24.4 | 6 / 6 |
| Tip (`a256582`) | 6.1 / 10.0 | 4.2 / 9.1 | 8.8 / 14.3 | 19.2 / 27.7 | 7 / 7 |
| **Floor** | 9.2 / 10.1 | 2.2 / 5.7 | 8.3 / 10.5 | **18.2 / 22.0** | **7 / 7** |

- The UI time is decided by where the scheduler puts the main thread: with it on a big core (CPU 6 or 7, at its
  652 MHz floor) the fixed build's first frame is 5.2–6.5 ms of UI and the floor's 4.2–5.1 ms; on a little core
  both take 9–14 ms. Inside it (fixed, medians): `Recomposer:recompose` 3.7 ms (before 5.1), measure and layout
  1.6 ms (before 2.5), recording 0.9 ms (before 1.2), RenderThread sync 1.4 ms. The floor recomposes 4.6 ms for a
  switch with empty screens: that is the shell and the bar, the same in every app.
- The GPU part is the idle clock, not content: 8.3 ms for the floor's one fill, bar and fade, 8.6 ms for the fixed
  build's real screens; steady frames of the same switch take 3–6 ms.
- Fixed → head (KeptTabs, `fb866b3`: the hidden tab's lifecycle changes the frame after the switch): the first
  frame's recomposition falls from 3.7 to 2.3 ms and its UI time from 10.9 to 6.9 ms (median), total 21.2 ms, 3 ms
  above the floor; Rapid Back 1.22 → 0.92 % janky. Tip (the re-recorded profiles) is 19.2 ms, 1 ms above the floor,
  with six of its seven first frames on a big core (UI 5.4–6.7 ms). Head was measured with `wp/WP04`'s newer perf tools (merged at
  01:00), which report `janky_after_first_pct` themselves; the same function gives the before and fixed values.
- Before → fixed: round 1 took 1.4 ms off the first frame's recomposition, 0.9 ms off measure and layout, 1.1 ms
  off the RenderThread and 1.3 ms off the GPU (medians), and every push/pop and back row improved.

## 4. Still open

- `head` (`fb866b3`, KeptTabs) and `tip` (this branch at `a256582`: `wp/WP04`'s re-recorded baseline profiles and
  Windows perf tools, plus `22a9ada`) are being measured the same way (`$O/logs/pipeline-win.log`,
  `$O/logs/phase2-win.log`), then a push/pop/gesture capture of each build (`$O/nav-traces.win.sh` →
  `$P/<build>-nav`).
- Cold start and PSS regressed from before to fixed on this machine's builds (354 → 385 ms, 79 → 85 MB app PSS);
  `tip` carries the re-recorded profiles, so it decides whether this is the stale profile.
- The back gesture's own jank (0.94 % after the first frame, GPU 7–14 ms per frame while the card follows the
  finger): see §2a.

## 4a. jetpack-compose-performance audit (Oct 18)

All seven categories, references read first (`state-and-stability`, `layouts`, `lazy-layouts`, `animation`,
`effects-and-threading`, `graphics-performance`), over the shell (`AppShell`, `ShellBar`, `BottomEdgeFade`,
`BarAnchor`, `ScrolledUnder`, `WindowChromeWriter`), the stack (`StackHost`, `StackLayout`, `StackScene`,
`EntryFrame`), the tabs (`KeptTabs`, `TabReveal`, `AppTabBar`, `TabCells`, `AppTabBarState`) and the sample
screens (`SampleListScreen`, `SampleDetailScreen`, `SampleRow`).

- **Graphics (fixed):** `BarAnchor`'s back-card cutout was built from lines and cubic arcs (`setRoundedRect`), so
  the bar's two clips around and inside the card (`ClipOp.Difference` and `Intersect`) were general-path clips,
  masked on every frame of a back gesture over a screen that hides the bar, the same cost the pill had before
  `b7e8074`. It is now a rounded rect, clipped analytically, with the card's own corners (the card clips with
  `RoundedCornerShape`). Commit `22a9ada` (127 navigation unit tests green).
- **State (no finding):** every per-frame value (slide `cover`, card pose, pill edges, bounce, reveal progress,
  collapse, scroll edge) is read in a layer, draw or placement block; layout writes (`StackScene.width`, `lefts`,
  `TabMeasure.slots`) are read only in draw or placement, never in composition; `remember` keys are complete;
  `KeptTab.active` is a `derivedStateOf` over a value read by several tabs, so a switch recomposes the two that
  flip. `produceState` + `snapshotFlow` derivations: none (each `snapshotFlow` drives an effect: lifecycle caps,
  back enablement, chrome writes).
- **Layouts (no finding):** no `onGloballyPositioned`, no `BoxWithConstraints`; `KeptTabs` is a `SubcomposeLayout`
  by design (one composition per tab, warmed while idle); `onSizeChanged` in `AppTabBar` writes `settledHeight`,
  which only an effect reads.
- **Lazy (conflict, not changed):** `SampleListScreen`'s rows have a `key` and no `contentType`; the skill asks for
  one on every lazy item, the project's rule (A11.3 rule 13, WP02's `lazy-content-type`) only where a scope mixes
  kinds. The rows are one kind, and the list is WP02-M3's (its item 3).
- **Animation, effects, custom modifiers (no finding):** all motion is in `Modifier.Node`s or layer blocks; no
  `Modifier.composed`; no empty `DisposableEffect`; no `LaunchedEffect` doing synchronous work.

## 5. Scripts

Linux originals (Oct 16; kept as written): `$O/build.sh`, `test-then-build.sh`, `floor-build.sh`, `dvfs-trace.sh`,
`measure.sh`, `startup-trace.sh`. Windows (Oct 18), all run from `$O`, phone steps under `$L adb`:

- Builds: `build.win.sh <name>=<commit>...` (a `git archive` of each commit under `$O/s/<commit>`, floor included),
  `build-wt.win.sh <name>` (the worktree as it is, uncommitted changes included: the B side of an A/B).
- Phone: `measure.win.sh <name>...` (`perf.sh --runs 3 --gate solid` with this worktree's tooling; `SCENARIOS`,
  `TAG`, `NOTRACE`; a harness failure is retried once into `<name>-retry`), `coldstart-ab.win.sh` (cold starts of two
  packages interleaved), `startup-trace.win.sh` (three cold starts, traced), `startup-ab-trace.win.sh` (two packages'
  cold starts alternated in one capture), `dvfs-trace.win.sh` (tab switches from idle), `nav-trace.win.sh` /
  `nav-traces.win.sh` (push, Back, push, back swipe from idle), `scroll-trace.win.sh` (perf.py's flings after
  `pm clear`, then again with the shaders cached), `overdraw.win.sh` (HWUI's overdraw view; puts the property back),
  `rec-gesture.win.sh`, `mem-ab.win.sh`. Each `phase<N>.win.sh` is one session's builds and adb hold, logged in
  `$O/logs/phase<N>-win.log`.
- Reading results: `gap-table.py` (the §3 table), `afterfirst.py`, `abstats.py <regex> <perf dir>...` (late frames,
  RenderThread ≥ 6 ms, GPU over a vsync, display hitches), `hitchpos.py <run dir>... <regex>` (each hitch's position
  and stages), `latestage.py`, `gesturestats.py`, `firstframes.win.py`, `framebreak.win.py`, `bursts.py`
  (`rtframes.sql` output grouped into transitions), `binderphases.py` (`mainbinder2.sql` output by start phase).
- `tp.sh <query.sql> <trace>`: the android-profiler skill's trace_processor with `USERPROFILE=D:\.caches\perfetto`.

## 6. Queries

`$O/sql/*.sql` (PerfettoSQL; `{pkg}` and `{short}` are filled in with `sed` for another package):
`startstages.sql` (each cold start's stages), `startline.sql` and `window.sql` (a start's slices and thread states
by offset), `mainbinder.sql` / `mainbinder2.sql` (the main thread's binder calls, with the slices around them),
`startstates.sql`, `startup-slices.sql`, `bindapp.sql`, `firecls.sql`, `firstdraw*.sql`; `rtframes.sql` (each
RenderThread frame's stages and big-core share), `rtlong.sql` (RenderThread frames over 7 ms: running on a little or
big core, runnable, sleeping), `rtchildren.sql`, `rtslices.sql`, `rtslow.sql`, `burstfirst.sql`, `shadercomp.sql`
(shader compiles inside long frames); `mainslices.sql`, `heavyframes.sql`, `framedetail.sql`.

## Orchestrator note (Oct 15, 20:50): Android skills v1.0.15 (REQUIREMENTS 38)

Google published new and updated skills, now installed under `~/.claude/plugins/marketplaces/android-skills/` (v1.0.15). Read the new **jetpack-compose/jetpack-compose-performance** SKILL.md and its references before your next change, along with the updated navigation-3, navigation-event, adaptive and android-profiler skills where they touch your area. The digest `research/ANDROID-SKILLS-DIGEST.md` is being updated with the rules that change what we build.

WP04: merge wp/WP04-perf into wp/WP04 (git merge wp/WP04-perf) at the start of your next run; the orchestrator verified it
