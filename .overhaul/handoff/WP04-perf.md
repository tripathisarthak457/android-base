# WP04-perf: deep smoothness (branch `wp/WP04-perf`, worktree `$OV/wt/WP04-perf`)

`OV=D:/projects/tools/.overhaul`, `O=$OV/out/WP04-perf` (builds and scripts),
`P=$OV/perf/WP04-perf` (phone results and traces). Started 2026-10-15 15:45 from `wp/WP04` at `3c9c403`.
Phone: Redmi Note 10 Pro `9a7c74f4` (Snapdragon 732G: 6 × A55 at 576–1708 MHz, 2 × A76 at 652–2304 MHz,
Adreno 618; 120 Hz; custom ROM, SDK 37, HWUI Skia GL). Trace tooling: the android-profiler skill's
`trace_processor` (Perfetto v57).

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
tap handler, `b7e8074` rounded-rect pill, `ac56e69` one background fill. Uncommitted until round 2's tests:
`shell/tabs/KeptTabs.kt` (the hidden tab's lifecycle drops the frame after the switch, not in it) and
`feature/sample/.../detail/*` (the related rows wait for `LocalNavigationSettled` before they compose, the
documented rule; they composed 185 composables 220 ms into every push).

## 3. Measurements

Pending: HEAD (`$O/everything`, built from `3c9c403`) against the fixed build (`$O/fixed`) and the floor build
(`$O/floor`: the fixed build with every entry's content removed, so a switch draws one fill, the bar and the
fade), all in one adb hold (`$O/measure.sh`).

## 4. Still open

- Everything in §3.

## 5. Scripts

- `$O/build.sh <preset>`: generate from the worktree and build `devBenchmarkRelease`.
- `$O/test-then-build.sh <name>`: the navigation tests that cover the fixes, then the build.
- `$O/floor-build.sh`: the floor experiment from `$O/fixed`.
- `$O/dvfs-trace.sh <out> [package]`: Perfetto capture (FrameTimeline, cpufreq, sched, the app's sections) of
  plain taps and 250 ms press-holds on the tabs from idle. GPU frequency is not readable on this phone
  (`/sys/class/kgsl` is denied to shell and traced_probes; `power/gpu_frequency` and `kgsl/*` produce nothing).
- `$O/measure.sh <name>...`: `perf.sh --runs 3` on each build, then a trace.

## 6. Queries

In the scratchpad (`/tmp/claude-1000/.../scratchpad/q*.sql`, `d*.sql`); the per-frame one joins
`Choreographer#doFrame` (UI), `DrawFrames` (RenderThread) and `waiting for GPU completion` (GPU) by vsync id and
reports the gap since the previous frame, so the first frame after idle is the row with a gap over 300 ms.

WP04: merge wp/WP04-perf into wp/WP04 (git merge wp/WP04-perf) at the start of your next run; the orchestrator verified it

## Orchestrator note (Oct 15, 20:50): Android skills v1.0.15 (REQUIREMENTS 38)

Google published new and updated skills, now installed under `~/.claude/plugins/marketplaces/android-skills/` (v1.0.15). Read the new **jetpack-compose/jetpack-compose-performance** SKILL.md and its references before your next change, along with the updated navigation-3, navigation-event, adaptive and android-profiler skills where they touch your area. The digest `research/ANDROID-SKILLS-DIGEST.md` is being updated with the rules that change what we build.
