# HUD handoff (REQUIREMENTS 40: live performance overlay, log, export, score)

## Progress

- **Orchestrator note (Oct 19, 3:55, after the user's night run stopped at 03:30):** the run was stopped with this build still under `bin/locked`; it was left to finish. Build: acceptance builds `bin/locked gradle bash out/HUD/accept_builds.win.sh` (template build, everything and lean fixtures, standard check), started 03:19 by the HUD:run agent. Result: `BUILDS OK`. Log: `D:/projects/tools/.overhaul/out/HUD/logs/r2-acc.log`. The next run starts by reading this log instead of re-running blind.
Run 1, started 2026-10-18 ~14:30 IST (workflow `hud-first`, implement). Worktree `wt/HUD` on `wp/HUD`, created from
`wp/WP06` at `7b14c0d` (WP06 has not moved since).

- 14:40: read REQUIREMENTS 19/25/27/38/40, WP06 and WP04 handoffs, CODE-STANDARDS, the jetpack-compose-performance
  skill and its references (state-and-stability, graphics-performance, effects-and-threading), android-intent-security.
- 15:15: `b29b548` analytics (frame listener on FrameJankRecorder, state keys without a string per frame); `e45abd0`
  devtools seams (DevToolsPage, CurrentScreen, the root reporting the top destination); `895ec4b` `:core:perfhud`;
  `e0c29e6` the generator feature `perfhud` (spec, presets, fixture, catalogue, tests, docs). Template compile of every
  touched module, `:core:perfhud` (48), `:core:devtools` (6) and `:core:analytics` (5) unit tests green
  (`out/HUD/logs/t2.log`), `:app:compileProdReleaseKotlin` and `compileDevReleaseKotlin` green. Generator tests: 299 run,
  0 failures (4 skipped).
- 15:35: `a021190` the adb receiver is unexported and reached through `run-as` (see Decisions).
- Next: phone builds (`out/HUD/phone_builds.win.sh`), phone steps (`out/HUD/phone.win.sh`), template `build` with lint,
  generated presets' builds, the Compose audit write-up, Measurements.

Run 2, started 2026-10-19 01:40 IST (workflow `hud-first`, implement, "run"). Worktree clean at `e39f005`; `wp/WP06`
still `7b14c0d` (nothing to merge); merged: WIN, WP01, WP02-M1, WP09, WP10; phone 9a7c74f4 attached. WP04's agents
held both locks when this run began.

- 01:45: found what run 1 had not done: REQUIREMENTS 40's "Nothing in prod, not even idle cost" covers the whole dev
  tooling, and `app/build.gradle.kts` still wired `:core:devtools` with `implementation(...)` into every variant
  (WP06's handoff, Oct 18 14:40: "moving it to dev and debug variants is package HUD's job"). Done this run (see
  Decisions, "Dev tooling wiring"): `DevTooling` seam in `:core:ui`, `AppEnvironment` in `:core:common`, devtools'
  own Hilt module (network observer, the seam binding), `debugImplementation` + `"devImplementation"`, the
  `verify<Variant>CarriesNoDevTooling` guard with a ProjectBuilder test, generator ownership, tests and docs, and the
  consistency tool's Hilt scan taught `@BindsOptionalOf`.
- 01:50: `FrameIntake`'s comment said a frame spanning a navigation "carries both routes, the old one first". The
  JankStats 1.0.0 bytecode (`PerformanceMetricsState.addFrameState`) keeps only the newest value of a key per frame,
  so it carries the new route alone; the behaviour (the frame counts for the screen it opens) was right, the comment
  and the test's input were not. Both corrected; the loop now runs forward (the order no longer matters).

- 02:17: `logs/r2-t1.log` green: build-logic tests (with `DevToolingGuardTest`), devtools, perfhud, analytics and
  `:app` unit tests, `hiltJavaCompile` of devDebug and prodRelease, and the three release guards. Generator tests
  (`logs/r2-gen.log`): 300 run, 0 failures, 4 skipped. Consistency (`logs/r2-cons.log`, 151 projects): 92 new
  issues, all one: `DevToolsPanel.kt` imported `DevToolsPage` for a KDoc link only (run 1's); fixed.
- 02:05-02:30: preliminary overhead on run 1's devRelease (`perf/HUD/overhead-prelim-a021190/`, 4 rounds): shown
  vs hidden **+1.3 points of jank overall** (tabs +5.4, push/pop +3.6, scroll +0.15), UI thread +0.03 ms per frame.
  Cause, from the idle segments: shown, the app drew 20 frames in 5 idle seconds, all janky (framestats: about
  21 ms each, "slow issue draw commands", GPU 14 ms: a lone frame after idle on this phone misses its deadline).
  The pill's 4 Hz refresh loop wrote its numbers, and its own janky frame moved the janky share enough to change
  the shown tenths, so it redrew itself for ever, and every interaction ended with extra lone frames. Fixed by
  design rather than tuned: the pill holds no state and reads the frame log in its draw phase, and a new
  `Modifier.redrawDuringFrames` node re-records it at most every 250 ms from the window's pre-draw pass, which
  runs only inside a frame the app draws anyway; it never schedules a frame (see Decisions, "Pill refresh").

Run 3, started 2026-10-19 06:10 IST (workflow `hud-first`, implement, "run"; the user's 2-hour run). Worktree clean
at `3fe54d3`; `wp/WP06` had moved to `08c0d0a` (two session-token commits), so merged first; merged tags unchanged
(WIN, WP01, WP02-M1, WP09, WP10); phone attached; run 2's acceptance log read (`logs/r2-acc.log`: BUILDS OK).

- 06:12: `675662b` merge of `wp/WP06` (clean; nothing of HUD's touched). Generator tests after it
  (`logs/r3-gen.log`): 300 run, 0 failures, 4 skipped.
- 06:21-06:31: everything devRelease/prodRelease/devDebug rebuilt from the merge (`logs/r3-builds.log`), overhead
  re-measured (`perf/HUD/overhead-everything-r3/`, 6 rounds): overall +0.21 points, but scroll +0.30, with 8.8
  janky scroll frames per shown launch against 4.0 hidden and only one hidden launch reaching the shown range, so
  not noise. (`da30c7f`'s message says every shown launch was above every hidden one; one shown launch, 7, was below
  a hidden 8. The numbers here are the exact ones.)
- 06:31-06:33: demo capture (`out/HUD/show.py`) found two visual defects in the Perf tab (the frame-time graph's
  90 and 120 Hz labels overlapped each other and the bars; the histogram's "32 ms" ran past the edge): fixed in
  `e1873e1`. Its debug half launched the wrong activity (LeakCanary's launcher makes resolving ambiguous), fixed in
  the script.
- 06:40: `out/HUD/pss_probe.py` (`perf/HUD/pss-probe/`): overlay hidden, scroll flings alone vs with the app reading
  its PSS (`dumpsys meminfo`, the same `Debug.getMemoryInfo` call): 0.65 % vs 1.89 % janky, about one janky frame
  per read. The sampler took PSS every 15th second whatever the app was doing; `da30c7f` reads it at most every
  15 s and only after 250 ms without a frame (`PssSchedule`, tests; docs and caption say so).
- 06:53-07:24: acceptance on the final head (`logs/r3-builds.log`, after the "=== retry" line): everything phone
  APKs, template `build --continue` (16 min; `:core:perfhud` 52 unit tests, 0 failures, with `PssScheduleTest`
  and the new `FrameLogTest` case; lint; the three `verify<Variant>CarriesNoDevTooling` guards), the everything
  fixture `check assembleDevDebug`, lean `assembleDevDebug`: BUILDS OK. The standard preset regenerated from the
  final head and checked separately (`logs/r3-standard.log`, `check assembleDevDebug`, 07:43-07:48: green), since
  the acceptance script reuses run 2's copy.
- 06:59-07:15: overhead on the final head (`perf/HUD/overhead-everything-r3b/`): −0.03 points overall, scroll
  0.00, +0.05 ms UI thread per frame; demo capture again (`perf/HUD/show/`, debug notice shown). APK inspection of
  the final head's prodRelease (`checks/apk-inspection-r3b.json`): clean on all five looks.

Run 4, started 2026-10-19 about 08:15 IST (workflow `hud-first`, fix #1: a fresh-context review's 7 blocking
findings and 22 non-blocking ones). Worktree clean at `da30c7f`; `wp/WP06` unchanged (`08c0d0a`, already merged);
merged tags unchanged; WP04's `final-gates.win.sh` held the adb lock from 08:14 to 08:46.

- 08:32 `fcd24c6` (blocking 1, 2, 5, 6, 7): no score before the frames are graded; the percentiles graded on
  main-thread time; StrictMode examples structured and worded from resources; the report's network times through
  `perfhud_value_millis`; `collectAsStateWithLifecycle`; the dead `count` properties gone. `55aaacb` (blocking 4):
  `PerformanceRecorderTest` (9) and `FrameFeedTest` (4, frames reported through a real Robolectric window), which
  found the route left on a paused window; fixed there. Both new test classes fail against the old behaviour
  (checked by reverting the two guarded lines: 3 failures, `logs/f1-mutation.log`).
- 08:41 `0cea54c` the cheap non-blocking findings (GC per sampler run and `gc=` per window, the pill's idle width
  and debug colour, stable graph lambdas, one m:ss formatter, K-4 constants, comments). `logs/f1-builds.log`:
  template `:core:perfhud` 71 unit tests, `:core:analytics` 5, `:core:devtools` 6, all green, `:core:perfhud:lintDebug`
  clean, `:app` devRelease/prodRelease compile; the standard preset regenerated, its `:core:perfhud` 71 tests green
  (its `FrameFeedTest` on the no-analytics branch: the overlay's own JankStats) and its devRelease built; the
  everything devRelease and prodRelease built. The first attempt failed in lint on a Metaspace OOM of the daemon
  (`:core:ui:lintAnalyzeDebug`); the scripts now retry once on a fresh daemon. Generator tests (`logs/f1-gen.log`):
  300 run, 0 failures, 4 skipped.
- 08:46-09:08 phone (`logs/f1-phone.log`, `out/HUD/f1_phone.win.sh`, APKs from `0cea54c` copied to
  `out/HUD/apks-f1/`): the standard preset's overhead (blocking 3: +0.09 points, +0.089 ms per frame), the listener
  check on both presets (hidden adds none), the everything preset's overhead again (−0.05 points, +0.003 ms).
  APK inspection with the seam names: prodRelease clean on all five looks. Measurements below.
- 09:07 `e753d88`: the histogram drew full durations against one interval, the same mistake the review found in
  the score and the frame-time graph; it now draws main-thread time. The feed's and recorder's tests split into one
  behaviour each (T-3). `:core:perfhud` 76 tests green (`logs/f1-test2.log`). 09:09 `0596515`: the doc's measured
  numbers, the perf.hud-off rule, the score and graph changes.
- 09:08-09:11 `wp/WP06`'s everything devRelease built (`logs/f1-base.log`), 09:11-09:17 the hidden cold start
  against it (`logs/f1-startup.log`): +10 ms median, within noise. 09:07-09:10 generator tests on `e753d88`
  (`logs/f1-gen2.log`): 300 run, 0 failures, 4 skipped.
- 09:17-09:19 the release demo recaptured (`perf/HUD/show-f1/`, `logs/f1-show.log`, `out/HUD/show.py --steps
  release`, the `0cea54c` devRelease): the score card with "Main-thread time p99" and its fix, every metric graded
  (p90 0.42 of the budget, A; the screenrecord's p99 2.2, D; 61 / D in the export, schema 2), and the frame-time
  graph's faded full frames with the solid main-thread part, green past the 120 Hz line where the frame made its
  deadline. Its histogram screenshot predates `e753d88` (full durations); `show.py` from the final head redraws it.
- 09:12-09:26 consistency matrix on the final code (`logs/f1-cons.log`): 151 projects, 0 new issues, 1023 known.
  `python -m tests.consistency` dies on this machine before checking anything (Windows spawns its workers, which
  cannot find `run_one` on a `-m` main module); run it as `python -c "import sys; sys.argv = ['tests.consistency'];
  from tests.consistency.__main__ import main; sys.exit(main())"` from `generator/`.
- 09:19 `4405b78`: the doc states the hidden launch cost as measured (the recorder is built at launch).
- 09:11-09:37 acceptance on the final code (`logs/f1-accept.log`, `out/HUD/f1_accept.win.sh`): template `build
  --continue` (every variant, unit tests, lint, the three release guards) in 17 min; the everything fixture `check
  assembleDevDebug`; the standard preset regenerated and `check assembleDevDebug` (its `:core:perfhud` 76 tests on
  the no-analytics branch); lean `assembleDevDebug`: BUILDS OK, no Metaspace retry needed. The fixture's
  `:core:perfhud` ran the same 76 tests, all green. Worktree clean at `4405b78`.

## Design and decisions (recorded because the user is not to be asked)

- **No score before the frames (run 4).** `OptimisationScore.total` and `grade` are null until `JankyFrames` is
  graded (300 frames at a known refresh rate); the tab, the Markdown and the JSON (`score.total: null`) say "the
  score needs 300 frames of drawing". The build checks and StrictMode are always readable, so any rule weaker than
  "the frames are graded" scored an unused app (the reviewer's 100 / A before Android 15). The metric rows stay in
  the JSON, graded where they can be.
- **Percentiles on the main thread (run 4).** `UiThreadP90`/`UiThreadP99` (renamed from `FrameTimeP90/P99`; JSON
  `schema` 2) grade `FrameSummary.uiP90Millis`/`uiP99Millis`, JankStats' `frameDurationUiNanos`, against one refresh
  interval; limits unchanged (p90 A ≤ 0.6, p99 A ≤ 1.0 of the interval). Chosen over the overrun against the
  deadline because `JankyFrames` already grades the deadline, `gates.toml`'s frame times are UI-thread, and the
  main thread is what Compose code controls. Calibrated on run 3's own framestats (`perf/HUD/overhead-everything-r3b`,
  hidden, `SyncQueued − AnimationStart`): scroll p90/p99 1.12/1.80 ms (0.13/0.22 of 8.33 ms: A/A), the whole
  journey (scroll, tabs, push/pop) 1.71/8.81 ms (0.21/1.06: A/B, the p99 being screens composed for the first
  time), the screenrecord demo 3.16/20.6 ms (A/D). The total-duration grading gave the same smooth scroll D/D.
- **Frame-time graph (run 4).** Each bar is the full duration faded with the main-thread part solid; colour by
  missed deadline (red), main thread over the interval (amber), else green. The full duration stays visible
  because it is what a late frame is late by, but it no longer decides a colour. One more `FloatArray` in the ring
  (`uiMillis`, 32 KB at 8192 frames). The histogram (`e753d88`) draws `FrameLog`'s main-thread histogram against
  the same one-interval line, for the same reason: full durations past the line were on time.
- **StrictMode copy (run 4).** `StrictModeSummary.examples` is a list of `StrictModeExample(kind, frame?)`; the
  panel and the Markdown word it with `perfhud_strict_mode_example` / `_unknown_frame`. JSON examples are objects
  now (schema 2).
- **GC counted per sampler run (run 4).** `MemoryLog` sums the collector's counters between consecutive samples of
  one run (`startRun()` at each start of the sampler: recording on, or the app back in front), so collections with
  recording off or the app in the background no longer count; the log's `gc=` is per window. A screen line still
  sums every visit (documented rather than changed: the per-screen histograms are cumulative by design).
- **Route state off every window it was put on (run 4).** `FrameFeed` keeps a weak set of the windows it put
  `route=` on and removes it from each on stop; before, a stop while paused left it, and with analytics on the
  dev build's `frame_jank` keys kept `route=<name>`. While recording, dev-build analytics events carry `route=`
  keys by design (the state is the window's, shared).
- **Tests build a real `FrameJankRecorder` through reflection (run 4).** Its constructor is `internal` to
  `:core:analytics` (Hilt-only); the JVM sees it public, so `FrameFeeds.kt` calls it by reflection rather than
  widening the production API for a test. Frames are reported through Robolectric's `ShadowWindow` with
  `FrameMetricsBuilder` at a far-future vsync, so every state put before is seen and none removed before.
- **K-4 in perfhud's UI files (run 4).** Constants in `panel/**` and `overlay/**` are PascalCase, as in
  `:core:devtools`; the non-UI packages keep `UPPER_SNAKE_CASE`.
- **Pill (run 4).** Measured for its idle line too ("idle · 120 Hz" was wider than the box); debug builds draw it
  in `contentSecondary`, not the warning amber that "fair" also uses.
- **Accepted: `StateKeys` in prodRelease (run 4, recorded explicitly).** REQUIREMENTS 40 says the prod analytics
  stays as it is. `StateKeys` changes how the analytics key is built (once per change of states instead of once
  per frame), not what it is; its tests pin the keys. It is the one HUD change a prodRelease carries (R8 strips
  `frameListener`, which nothing sets there).

- **PSS only in a pause (run 3).** `PssSchedule`: at most every 15 s, and only when no frame has started for
  250 ms (`FrameLog.drewWithin`). 250 ms is under the Perf tab's 500 ms refresh, so an open tab still gets PSS
  between its redraws; an app that draws without pause gets its PSS late. Chosen over a rarer fixed interval,
  which would still land inside a scroll now and then, and over RSS from `/proc/self/statm` (cheap, but
  `gates.toml` budgets PSS less graphics, which only the smaps walk gives).
- **Graph labels (run 3).** The frame-time graph's budget labels sit in a gutter left of the plot, centred on
  their lines and pushed apart when they would touch (the 90 and 120 Hz lines are about 3 ms, 32 px, apart);
  drawn over the bars they were unreadable. The histogram clamps its last tick label inside the graph.

- **Pill refresh (run 2).** The pill never draws a frame of its own. `RedrawDuringFrames.kt` is a
  `Modifier.Node` (`DrawModifierNode` + `ViewTreeObserver.OnPreDrawListener`, registered on attach, removed on
  detach): in `onPreDraw` it calls `invalidateDraw()` when 250 ms have passed, and the layer is recorded again in
  that same frame. While the app is idle the pill keeps the numbers of its last drawing, which is what "frames
  per second over the last second of drawing" means; "idle" now shows only for too little drawing to judge. Cost
  while recording: one `nanoTime` and a compare per app frame on the main thread; nothing while not recording
  (the pill is not composed). The earlier design (a 4 Hz `LaunchedEffect` writing three `mutableIntStateOf`) was
  allocation-free and recomposition-free but not frame-free, and on this phone that is what matters.

- **Dev tooling wiring (run 2).** `:core:devtools` and `:core:perfhud` are `debugImplementation` and
  `"devImplementation"` dependencies of `:app`: devDebug, devRelease, devBenchmarkRelease, devNonMinifiedRelease,
  stagingDebug and prodDebug carry them; stagingRelease, prodRelease and playstoreRelease (and their benchmark
  variants) do not. REQUIREMENTS 40 says "dev-flavour builds and every debug build", so a staging *release* no
  longer shows the badge (WP06's `DevEnvironment.visible` showed it there); taken literally rather than adding
  `stagingImplementation`, because the user's words were "dev env and debug builds". `:app`'s main code reaches
  the tooling only through `DevTooling` (`:core:ui`, `core/ui/tooling/`: `onScreenChanged(name)` and
  `@Composable Overlay()`), injected as `Optional<DevTooling>` (`@BindsOptionalOf` next to the interface);
  `DevTools` implements it and `:core:devtools`' `DevToolsModule` binds it. Empty, `AppRoot` composes nothing and
  starts no effect for it (`if (devTooling != null)`, `devTooling?.Overlay()`). The build facts the badge and the
  Build tab show come from `AppEnvironment` (`:core:common`, pure Kotlin), which `:app` provides from its
  `BuildConfig` (`di/AppEnvironmentModule.kt`); nothing in a shippable build reads it, so R8 drops it. WP06's
  `DevEnvironment` is gone (its `label` is `DevTools.label`; `visible` was always true wherever the module now
  exists). The HttpExchange-to-NetworkExchange observer moved from `:app` into `:core:devtools`
  (`<opt:network>` dependency on `:core:network`). `DevTools` and `DevToolsOverlay` are internal now.
  Guard: `verify<Variant>CarriesNoDevTooling` (build-logic `DevToolingGuard.kt`, registered by the flavours
  plugin for every release outside the dev flavour) walks the variant's resolved runtime graph and fails `check`
  and `pre<Variant>Build` if `:core:devtools` or `:core:perfhud` is anywhere in it, directly or transitively.

- **Module `:core:perfhud`** (package `com.base.app.core.perfhud`), wired into `:app` with `debugImplementation` and
  `"devImplementation"` only: it is in devDebug, devRelease, devBenchmarkRelease, devNonMinifiedRelease, stagingDebug
  and prodDebug, and in no shippable release (prodRelease, playstoreRelease) nor stagingRelease. It plugs into
  `:core:devtools` through a Hilt `@IntoSet DevToolsPage` (a tab of the panel plus a line under the badge);
  `:core:devtools` declares the set with `@Multibinds`, so a build without the module has an empty set and no HUD code.
  `:app`'s main sources never name it (generator test).
- **Frames:** with `analytics` on, the HUD reads `FrameJankRecorder`'s JankStats through
  `FrameJankRecorder.frameListener` (no second listener on the window). With `analytics` off, the HUD creates its own
  JankStats for the resumed activity, only while recording (`opt-else:analytics` in `frame/FrameFeed.kt`). Hidden = not
  recording = no listener, and nothing allocated (the rings are created on the first recording).
- **Route as JankStats state:** the app reports the top destination's serial name (R8 keeps it readable) to
  `:core:devtools`' `CurrentScreen` (main code, `<opt:devtools>`, `ui/ReportCurrentScreen.kt`); the HUD puts it into the
  window's `PerformanceMetricsState` as `route=<name>` while it records and removes it when it stops, so a prod build
  never carries the state, and a recording started mid-session (no-analytics path: JankStats created late) still knows
  its screen. A frame that spans a navigation carries both routes and counts for the new one.
- **Janky** = missed its deadline (FrameMetrics overrun > 0 on API 31+, the FrameTimeline count `perf.sh`/gfxinfo
  report; JankStats' own `isJank` is "over twice the interval" and counts about half). Before API 31: CPU time over the
  refresh interval. Slow > 16 ms and frozen > 700 ms (Android vitals) on the frame's total; long main-thread work = UI
  thread over 50 ms.
- **FPS while drawing:** each frame keeps the display busy until the next frame starts, but never longer than its own
  duration rounded up to whole intervals, so idle gaps do not lower it. The pill reads "idle" below 3 frames or 100 ms
  of drawing in the last second, or no frame in 500 ms: the pill's own redraws (at most 4/s) never count as drawing,
  so it cannot keep itself awake.
- **Collection cost:** fixed-size primitive arrays (8192-frame ring, 33 per-screen histograms of 225 buckets), written
  on JankStats' thread under one uncontended lock; no allocation, boxing or logging per frame. Memory and GC each second,
  PSS (`Debug.getMemoryInfo`) at most every 15 s and only in a pause in the drawing (run 3, above), start-up from `ApplicationStartInfo`
  (Android 15+; none earlier, rather than an in-app estimate that misses the pre-app part), requests from the inspector.
- **StrictMode** thread checks on the main thread while recording (penalty listener counts them; API 28+), the app's
  previous policy restored on stop.
- **Defaults:** recording on in debug builds, off in release-like dev builds (devRelease, benchmark); the developer's
  switch is kept in DataStore (`perfhud` file); the launch extra `perf.hud=on|off` overrides for one process (for
  `perf.sh --extra perf.hud=on`); read defensively (two exact values, `BadParcelableException` caught).
- **adb path:** the receiver is `exported="false"`; adb reaches it with `adb shell run-as <id> am broadcast ...` on a
  debuggable build only. First version used an exported receiver guarded by `android.permission.DUMP` (androidx
  ProfileInstallReceiver's pattern), dropped because devRelease is a release build type and REQUIREMENTS 40 says the
  adb path is debug-only and not exported in release.
- **Strings:** `res/values/donottranslate.xml` (developer-only copy, never shown in a shippable build; lint and
  `tests/test_translations.py` both skip a file of that name). `:core:devtools`' own panel copy stays as WP06 wrote it.
- **Tab label "Perf":** the panel's segmented control has five equal segments on a phone (Network, Stats, Flags, Build,
  Perf); "Performance" would be truncated, which REQUIREMENTS 18 counts as a defect.
- **Measurements on a spec without sign-in:** the everything preset starts at onboarding and a sign-in that needs a
  backend, and the base (`wp/WP06`) has none of WP04's `perf.fakeSession` launch hooks or tab test tags that `perf.sh`
  waits for. The phone builds use `out/HUD/specs/hud-everything.json` (everything less auth, onboarding and
  googlesignin; analytics on, so the shared-JankStats path) with the dev API at jsonplaceholder (as WP04's specs do),
  app id `com.example.hud.everything`, and `hud-standard.json` (standard less auth and onboarding: no analytics, so the
  HUD's own JankStats). The overhead is measured with WP04's `parse_gfx`/`parse_framestats` in `out/HUD/overhead.py`
  (gfxinfo reset and dumped around every step, as `perf.sh` does). `perf.sh` itself runs once HUD merges after WP04.

## What changed (by module/path)

Paths under `templates/android/` unless they start with `generator/` or `web/`.

- `core/analytics/.../jank/FrameJankRecorder.kt`: `frameListener` (volatile, called per frame after counting);
  `StateKeys` builds a frame's analytics key only when its `StateInfo` instances change.
- `core/devtools`: `page/DevToolsPage.kt` (key, title, content, optional badge line), `di/DevToolsPagesModule.kt`
  (`@Multibinds`), `screen/CurrentScreen.kt`; `DevTools` takes `currentScreen` and `pages`; the badge shows each page's
  line under its label and opens the panel on that page; the panel lists contributed tabs after the built-in ones and
  shows the current screen in Build.
- `app/.../ui/ReportCurrentScreen.kt` (owned by `devtools`), called from both branches of `AppRoot`;
  `app/build.gradle.kts` (`<opt:perfhud>` debug and dev dependencies); `settings.gradle.kts`.
- `core/perfhud` (new): `frame/` (FrameLog, FrameHistogram, ScreenTable, FrameIntake, FrameFeed, RefreshRate,
  readings), `memory/`, `startup/`, `checks/` (BuildChecks, StrictModeWatch), `network/NetworkTimings`, `score/`
  (OptimisationScore, MetricText), `log/PerfLogcat`, `report/` (PerformanceReport JSON, MarkdownReport, ReportExporter,
  ReportReceiver), `recording/` (PerformanceRecorder, RecordingSettings, PerformanceSnapshot), `overlay/PerformancePill`,
  `panel/` (PerformanceTab, ScoreSection, FrameSections, DetailSections, `graph/` FrameTimeGraph, FpsGraph,
  HistogramGraph, MemoryGraph), `di/PerfHudModule`, `res/values/donottranslate.xml`, unit tests.
- `features/perfhud.toml` (default on, requires devtools, owns `core/perfhud` and the doc), `presets.toml` (standard),
  `features/devtools.toml` (owns ReportCurrentScreen.kt), `docs/PERFORMANCE-OVERLAY.md` (new), `docs/ARCHITECTURE.md`.
- `generator/tests/fixtures/v2/everything.json`, `generator/tests/test_android_target.py` (3 tests),
  `web/frontend/lib/catalogue.json` (regenerated).
- Run 2: `core/ui/.../tooling/` (DevTooling, DevToolingModule), `core/common/.../environment/AppEnvironment.kt`,
  `core/devtools/.../di/DevToolsModule.kt` (seam binding, network observer), `DevTools.kt` (implements the seam),
  `DevToolsOverlay.kt`, `DevToolsPanel.kt`, `DevEnvironment.kt` (deleted), devtools `build.gradle.kts`;
  `app/.../di/AppEnvironmentModule.kt` (was `DevToolsModule.kt`), `MainActivity.kt`, `AppRoot.kt`,
  `ReportCurrentScreen.kt`, `app/build.gradle.kts`; build-logic `DevToolingGuard.kt` + test, the flavours plugin;
  perfhud `overlay/RedrawDuringFrames.kt` (new), `PerformancePill.kt`, `FrameIntake.kt` and its test; docs
  (`ARCHITECTURE.md`: the guard, "Dev tooling is absent from shippable builds"; `PERFORMANCE-OVERLAY.md`);
  `features/devtools.toml` (owns the seam, the environment, the guard); root `README.md` (inspector row, overlay
  row); `generator/tests/test_android_target.py` (dev tooling wiring, no seam without devtools);
  `generator/tests/consistency/hilt.py` (`@BindsOptionalOf`); the catalogue.
- Run 3: the `wp/WP06` merge (`675662b`); perfhud `panel/graph/FrameTimeGraph.kt` (label gutter),
  `HistogramGraph.kt` (last tick inside); `memory/PssSchedule.kt` (new) and its test, `frame/FrameLog.kt`
  (`drewWithin`) and its test, `recording/PerformanceRecorder.kt` (sampler loop), `res/values/donottranslate.xml`
  (memory caption); `docs/PERFORMANCE-OVERLAY.md` ("PSS only in a pause", the memory row). Workspace:
  `out/HUD/show.py`, `pss_probe.py`, `r3_builds.win.sh`, `r3_phone.win.sh`, `r3b_phone.win.sh`;
  `accept_builds.win.sh` now builds the everything phone APKs first.

- Run 4: perfhud `score/OptimisationScore.kt` (null total and grade before the frames; `UiThreadP90/P99`),
  `score/MetricText.kt` (`minutesAndSeconds`, "waiting" for an enqueued profile), `frame/FrameLog.kt` and
  `FrameReadings.kt` (`uiMillis` ring and series; `count` gone), `frame/FrameFeed.kt` (route off every window),
  `checks/StrictModeWatch.kt` (`StrictModeExample`), `memory/MemoryLog.kt` (`startRun`, GC per run; `count` gone),
  `log/PerfLogcat.kt` (`begin(frames, memory)`, `gc=` per window), `recording/PerformanceRecorder.kt`,
  `report/PerformanceReport.kt` (schema 2), `report/MarkdownReport.kt`, `report/ReportReceiver.kt`, the panel and
  overlay files (constants, `collectAsStateWithLifecycle`, the graph's solid main-thread part, stable graph
  lambdas, the pill's size and debug colour), `res/values/donottranslate.xml`, `build.gradle.kts`
  (lifecycle-runtime-compose); tests `recording/PerformanceRecorderTest.kt`, `frame/FrameFeedTest.kt`,
  `frame/FrameFeeds.kt` (new), `OptimisationScoreTest`, `ReportFormatsTest`, `FrameLogTest`, `MemoryLogTest`,
  `PerfLogcatTest`; `core/ui/.../DevTooling.kt` and `app/build.gradle.kts` (comments);
  `docs/PERFORMANCE-OVERLAY.md`. Workspace: `out/HUD/f1_builds.win.sh`, `f1_base.win.sh`, `f1_phone.win.sh`,
  `f1_accept.win.sh`, `startup.py` (new), `overhead.win.sh` (`ID` from the environment), `apk_inspect.py` (the
  seam names).

## Public APIs added, changed, removed (signatures)

```kotlin
// :core:analytics
class FrameJankRecorder { @Volatile var frameListener: JankStats.OnFrameListener? }
// :core:common (owned by devtools)
data class AppEnvironment(name: String, versionName: String, applicationId: String, apiBaseUrl: String,
    isDebugBuild: Boolean)
// :core:ui (owned by devtools); bound by :core:devtools, injected as Optional<DevTooling>
interface DevTooling { fun onScreenChanged(name: String); @Composable fun Overlay() }
// :core:devtools
class DevToolsPage(val key: String, @get:StringRes val title: Int, val content: @Composable () -> Unit,
    val badgeContent: (@Composable (openPage: () -> Unit) -> Unit)? = null)
class CurrentScreen { val name: StateFlow<String?>; fun report(name: String) }
internal class DevTools(environment: AppEnvironment, log, currentScreen, pages, flags, flagOverrides) : DevTooling
// removed: DevEnvironment (and its visible/label), public DevTools and DevToolsOverlay, :app's DevToolsModule
// build-logic: verify<Variant>CarriesNoDevTooling for stagingRelease, prodRelease, playstoreRelease
// :core:perfhud: internal throughout; its surface is the @IntoSet DevToolsPage and the receiver's actions
//   <package>.core.perfhud.action.EXPORT / RECORD (--ez recording), and the launch extra perf.hud=on|off.
// The exported JSON (run 4): schema 2. score.total and score.grade null before 300 frames; metrics
//   FrameTimeP90/P99 renamed UiThreadP90/P99 (main-thread time against one interval); strictMode.examples
//   are {kind, frame|null} objects, not strings; memory.gc* count only while the sampler ran.
```

## Measurements

### Run 4 (fix #1; final head; read these first)

Same method as run 3 (`out/HUD/overhead.py`, 6 rounds alternating hidden/shown launches of a devRelease installed
fresh and compiled `speed-profile`, `dumpsys gfxinfo` reset and dumped around every step), APKs built from `0cea54c`
(`out/HUD/apks-f1/`; the one later code commit, `e753d88`, changes the histogram's data source and tests only, and the
Perf tab is not composed while measuring). Limits: +0.3 points of jank, under 0.5 ms per frame. Summary:
`perf/HUD/measurements.json` (`fix1_*` keys; run 3's final numbers moved under `run3_da30c7f`).


**Standard preset, no analytics: the overlay's own JankStats and FrameMetrics listener**
(`perf/HUD/overhead-standard-f1/`, app id `com.example.hud.standard.dev`; the configuration the review found
unmeasured):


| Journey | Frames hidden / shown | Janky % hidden → shown | Δ points | UI thread mean Δ | Frame total mean Δ |
|---|---|---|---|---|---|
| Scroll | 10455 / 10440 | 0.39 → 0.54 | +0.15 | +0.095 ms | −0.011 ms |
| Tabs | 759 / 757 | 3.29 → 3.30 | +0.01 | +0.040 ms | −1.682 ms |
| Push/pop | 2556 / 2567 | 4.62 → 4.52 | −0.10 | +0.085 ms | +0.594 ms |
| **All** | 13770 / 13764 | 1.34 → 1.43 | **+0.09** | **+0.089 ms** | +0.013 ms |

Both limits met. Janky scroll frames per launch, hidden 11, 5, 9, 9, 3, 4 (mean 6.8) vs shown 11, 14, 10, 10, 6, 5
(9.3): the ranges overlap. Main-thread scroll p99 1.78 ms hidden, 3.91 ms shown (see the everything run below: the p99
moves this much between runs in either direction). Idle: 0 frames in 5 s, hidden and shown, every launch. PerfHud
window lines: none hidden, 8-10 per shown launch. The frame-total column leaves out frames still in flight when
gfxinfo was dumped (`FrameCompleted` 0: 104 hidden and 65 shown scroll rows); `result.json`'s own `total_mean_ms`,
which counted them, is wrong (−3.4 ms hidden), and `overhead.py` now skips them.


**Everything preset, analytics on: the analytics recorder's JankStats shared** (`perf/HUD/overhead-everything-f1/`,
app id `com.example.hud.everything.dev`, the same final code):

| Journey | Frames hidden / shown | Janky % hidden → shown | Δ points | UI thread mean Δ | Frame total mean Δ |
|---|---|---|---|---|---|
| Scroll | 10261 / 10285 | 0.33 → 0.37 | +0.04 | +0.028 ms | +0.102 ms |
| Tabs | 1326 / 1266 | 3.39 → 3.32 | −0.07 | −0.045 ms | −0.730 ms |
| Push/pop | 2539 / 2562 | 5.00 → 4.64 | −0.36 | −0.029 ms | +0.241 ms |
| **All** | 14126 / 14113 | 1.46 → 1.41 | **−0.05** | **+0.003 ms** | +0.027 ms |

Both limits met. Janky scroll frames per launch, hidden 4, 5, 5, 5, 6, 9 vs shown 8, 3, 4, 5, 9, 9. Idle: 0 frames in
5 s, hidden and shown. PerfHud window lines: none hidden, 8-9 per shown launch. Main-thread scroll p99: 3.91 ms
hidden, 3.73 ms shown here, against 1.78 / 3.91 ms on the standard preset and 1.80 / 3.69 ms in run 3's everything
run: the p99 (about 86 of 8,600 scroll frames) moves by 2 ms between runs and between arms in either direction, so
it does not single out the overlay (Open issues).

**Hidden adds no frame listener** (`perf/HUD/checks-f1/checks-listener.json`, both presets from `0cea54c`, launched
`perf.hud=off` then `on` and scrolled): standard hidden has no `FrameMetricsAgg` thread (JankStats' frame-metrics
HandlerThread) and writes no PerfHud line; shown it has the thread, `DefaultExecutor` (the sampler's `delay`) and 3
lines. Everything (analytics on) has `FrameMetricsAgg` either way (the analytics recorder's); shown adds
`DefaultExecutor` and the lines, and a `Metrics Backgro…` thread that is not in the app's dex (a Play services
module loaded at run time, present by timing).

**APK inspection** (`perf/HUD/checks-f1/apk-inspection-f1.json`, `out/HUD/apk_inspect.py` now also matching the seams
`DevTooling`, `AppEnvironment` and `ReportCurrentScreen` by source name): the everything prodRelease from `0cea54c` is
clean on all five looks (0 R8 mapping classes, seams included; 0 dex classes, manifest entries, resources, strings);
devRelease carries the tooling as it should (176 mapping classes, `AppEnvironment` among them; the receiver; 135
resources; 12 markers).

**Hidden cold start** (`perf/HUD/startup-f1/result.json`, `out/HUD/startup.py`; the review's open question): the
everything devRelease from `0cea54c` against the same spec built from `wp/WP06` (`08c0d0a`, no perfhud, WP06's
devtools wiring), each installed fresh and compiled `speed-profile`, launched with no extra (so hidden by default)
15 times per install in the order base, HUD, base, HUD; `am start -W -S` TotalTime, as `perf.sh coldstart` reads it.

| Build | Median per install | All 30 launches: median / mean | Range |
|---|---|---|---|
| `wp/WP06` base | 504, 515 ms | 507 / 505.5 ms | 458-553 ms |
| `wp/HUD` | 520, 515 ms | 517 / 512.8 ms | 459-565 ms |

HUD minus base: +10 ms on the median (2 %), +7 ms on the mean; a bootstrap of the medians gives −8.5 to +19.5 ms
(95 %), so the difference is within this phone's launch noise at 30 launches each, and the second pair of installs
had equal medians. What the hidden recorder adds at launch is construction on the main thread during the activity's
injection (the recorder and its collaborators, an activity-lifecycle callback, a DataStore object) and one stored-
choice read on the I/O dispatcher; it cannot be zero while the launch extra and the debug default must be honoured
from the first activity. Open issues has the way to move it after the first frame if a gate ever needs it.

### Run 3

Same method as run 2 below (`out/HUD/overhead.py`, 6 rounds alternating hidden/shown launches of the everything
devRelease, speed-profile compiled, gfxinfo reset and dumped around every step). Limits: +0.3 points of jank,
under 0.5 ms per frame. Summary: `perf/HUD/measurements.json` (the final head at the top level; run 3 before the
fix, the probe and run 2 under their own keys).

| Build | Journey | Frames hidden / shown | Janky % hidden → shown | Δ points | UI thread mean Δ |
|---|---|---|---|---|---|
| `675662b` (merge, before the PSS fix), `overhead-everything-r3/` | Scroll | 10241 / 10078 | 0.23 → 0.53 | **+0.30** | +0.168 ms |
| | Tabs | 1255 / 1259 | 3.35 → 3.49 | +0.14 | +0.141 ms |
| | Push/pop | 2554 / 2547 | 4.82 → 4.67 | −0.15 | +0.073 ms |
| | **All** | 14050 / 13884 | 1.35 → 1.56 | **+0.21** | **+0.145 ms** |
| `da30c7f` (PSS in a pause; final), `overhead-everything-r3b/` | Scroll | 10814 / 10946 | 0.65 → 0.65 | **0.00** | +0.053 ms |
| | Tabs | 1263 / 1258 | 3.56 → 3.42 | −0.14 | −0.004 ms |
| | Push/pop | 2541 / 2544 | 5.04 → 4.95 | −0.09 | +0.065 ms |
| | **All** | 14618 / 14748 | 1.66 → 1.63 | **−0.03** | **+0.049 ms** |

Janky scroll frames per launch, hidden vs shown: before the fix 3, 4, 3, 8, 1, 5 vs 7, 9, 11, 8, 9, 9 (mean 8.8
vs 4.0; only one hidden launch reached the shown range); after it 12, 12, 12, 12, 12, 10 vs 12, 10, 13, 12, 11, 13. The
hidden baseline itself moved between the two runs (0.23 % vs 0.65 % on scroll, an hour apart), which is why only
the alternating A/B within a run is compared. Idle: 0 frames in 5 s, hidden and shown, every launch. PerfHud
summary lines: none hidden, 10-12 per shown launch.

**PSS probe** (`perf/HUD/pss-probe/`, `out/HUD/pss_probe.py`, overlay hidden, 4 rounds of 8 flings each way):
flings alone 32 janky of 4940 frames (0.65 %); flings with two `dumpsys meminfo` reads each (the app runs
`Debug.getMemoryInfo`; median 450 ms per dumpsys) 91 of 4814 (1.89 %): about one janky frame per read. This is
what `da30c7f` acts on.

**APK inspection** (`checks/apk-inspection-r3.json`, the merge's builds, same tool as run 2): everything prodRelease
clean (0 R8 mapping classes, 0 dex classes, 0 manifest entries, 0 resources, 0 strings); the `wp/WP06` base
prodRelease still shows its 4 mapping classes; devRelease and devDebug carry the tooling as they should.
`checks/apk-inspection-r3b.json` repeats it on the final head's prodRelease and devRelease.

**Demo capture** (`perf/HUD/show/`, `out/HUD/show.py`, final head): devRelease with recording on, 1967 frames
over a minute of scrolling, every tab and two push/pops while `screenrecord` ran: the pill (`1-`, `2-`), every
part of the Perf tab (`3-tab-1..8`), the export (`4-exported.png`, the pulled `perf-report-20261019-071456.json`
and `.md`), the PerfHud log, and `demo-devRelease.mp4` (720x1600, 80 s); devDebug's tab with the
"pessimistic" notice (`6-debug-tab.png`). Numbers drawn while `screenrecord` runs are worse than without it; the
capture shows the overlay, the overhead table above is the measurement. The devRelease is left installed on the
phone so the overlay can be tried there.

All on the Redmi 9a7c74f4 (120 Hz), run 2, from `wp/HUD` `e64f194` (the pill fix) unless said otherwise. Raw files
under `perf/HUD/`; summary `perf/HUD/measurements.json`.

**Overlay shown vs hidden** (`perf/HUD/overhead-everything/`, `out/HUD/overhead.py`, 6 rounds alternating
`perf.hud=off|on` launches of the everything devRelease, profile installed and compiled `speed-profile`; each
launch: 5 s idle, 12 scroll flings, 5 tab taps, 4 push+pop; `dumpsys gfxinfo` reset and dumped around every step,
parsed with WP04's `parse_gfx`/`parse_framestats`). Limits: +0.3 points of jank, under 0.5 ms per frame.

| Journey | Frames hidden / shown | Janky % hidden | shown | Δ points | UI thread mean Δ | Frame total mean Δ | p90 / p99 hidden → shown |
|---|---|---|---|---|---|---|---|
| Scroll | 11043 / 10907 | 0.66 | 0.65 | −0.01 | +0.086 ms | −0.42 ms | 9/17 → 10/19 ms |
| Tabs | 1269 / 1262 | 3.70 | 3.41 | −0.29 | +0.048 ms | −0.76 ms | 19/53 → 18/48 ms |
| Push/pop | 2562 / 2561 | 4.57 | 4.45 | −0.12 | +0.073 ms | +0.41 ms | 16/40 → 16/40 ms |
| **All** | 14874 / 14730 | 1.59 | 1.55 | **−0.04** | **+0.078 ms** | −0.29 ms | 10/25 → 12/26 ms |

Idle: 0 frames in 5 s hidden and shown, every round. PerfHud summary lines: none hidden, 9-15 per shown launch.
Both limits met. The base's own jank (tabs 3.7 %, push/pop 4.6 %) is `wp/WP06`'s template, before WP04's
smoothness work. The scroll p90/p99 moved one gfxinfo bucket (1-2 ms) while the frame-total mean fell; the UI
thread pays 0.08 ms per frame on average (the frame intake on JankStats' thread, StrictMode's checks, and the
pill re-recorded in at most four frames a second).

Before the fix (`perf/HUD/overhead-prelim-a021190/`, run 1's devRelease, 4 rounds): **+1.30 points overall**
(tabs +5.38, push/pop +3.58, scroll +0.15), UI +0.03 ms, and 20 frames (all janky) in every 5 idle seconds
shown: the pill drew its own frames (Progress, 02:05).

**Hidden adds no frame listener** (`perf/HUD/checks/checks-listener.json`, devRelease, launched with
`perf.hud=off` then `on`, scrolled): the standard preset (no analytics, so the overlay's own JankStats) has no
`FrameMetricsAgg` (JankStats' HandlerThread) hidden and has it shown, and writes no PerfHud line hidden (3
shown); the only other new thread shown is `DefaultExecutor` (the sampler's `delay`). The everything preset
(analytics on) has JankStats' thread either way, the analytics recorder's own; shown adds `DefaultExecutor` and
the PerfHud lines, hidden nothing.

**adb path** (`checks-adb.json`, everything devDebug): `run-as <id> am broadcast --user 0 -n <id>/…ReportReceiver
-a …RECORD --ez recording true`, then `…EXPORT`: result data is the JSON's path; both files pulled
(`perf/HUD/checks/perf-report-20261019-032211.{json,md}`); recording off again. Without `run-as` the broadcast
completes undelivered (no data). devRelease: `run-as: package not debuggable`, and the direct broadcast is not
delivered either. Without `--user 0`, `run-as … am broadcast` throws (INTERACT_ACROSS_USERS), which is why run 1's
first check pulled nothing.

**Prod release** (`checks-prod.json`, `prod-before.png`, `prod-after.png`): the everything prodRelease from
`wp/WP06` (before) and from `wp/HUD` (after), each launched with `--es perf.hud on` and scrolled: no badge or pill
on screen (uiautomator and the screenshots), no PerfHud line, and the same threads: 67 before, 68 after, every
name the same but the binder pool's size (4 vs 5 `binder:` threads, which the pool grows on demand) and three
OkHttp thread names that carry an R8-renamed class name (`l23 …` vs `e23 …`). The green "120" in the corner of
the screenshots is the developer option "Show refresh rate", left on by another package (WP06's handoff says
the same); this run changed no device setting.

**APK inspection** (`perf/HUD/checks/apk-inspection.json`, `out/HUD/apk_inspect.py`: the R8 mapping, dexdump,
aapt2 manifest and resource table, raw strings in dex/manifest/arsc):

| APK | R8 mapping classes | dex classes | manifest | resources | strings |
|---|---|---|---|---|---|
| everything prodRelease, wp/HUD | 0 | 0 | 0 | 0 | 0 |
| everything prodRelease, wp/WP06 (before) | **4** (`DevEnvironment`, `DevTools`, `DevToolsLog`, an overlay lambda) | 0 | 0 | 0 | 0 |
| everything devRelease, wp/HUD | 169 | 1 | 1 (the receiver) | 132 | 12 markers |
| everything devDebug, wp/HUD | (no mapping) | 374 | 1 | 132 | 13 markers |

Run 1's string-only inspection could not tell the before from the after: R8 had already stripped the inspector's
strings from the WP06 prod build (its `visible` folded to false), but kept the four classes MainActivity injected.

**Builds and tests** (run 2): template targeted `logs/r2-t1.log`, `r2-t2.log` green (build-logic incl.
`DevToolingGuardTest`, devtools, perfhud, analytics, `:app` unit tests, lint of devtools and perfhud,
`hiltJavaCompile` devDebug/devRelease/prodRelease, the three release guards); phone builds `logs/r2-pb2.log`
(everything devRelease + prodRelease + devDebug, standard devRelease, base prodRelease; one Metaspace OOM on the
first attempt, `logs/r2-pb.log`, retried by the script). Generator tests 300 run, 0 failures (4 skipped).

## Compose performance audit (jetpack-compose-performance, all seven categories)

Run 2, over every composable and modifier this package wrote or changed, after reading the skill and
`references/state-and-stability.md`, `layouts.md`, `effects-and-threading.md`, `graphics-performance.md`,
`lazy-layouts.md`, `animation.md` and the custom-modifiers guide.

| Composable | Finding | Verdict |
|---|---|---|
| `PerformancePill` / `LivePill` | Run 1's 4 Hz `LaunchedEffect` wrote three snapshot ints read in draw: deferred reads, no recomposition, but each write drew a frame of its own, which on the phone fed back into the janky share it showed (Progress, 02:05). | **Fixed**: no state, draw-phase reads of the frame log, `redrawDuringFrames` (cat. 7, `Modifier.Node`; cat. 5, no timer). Text measured through a remembered `TextMeasurer` (cat. 6); the size is measured once, so a number never relayouts the badge (cat. 2). |
| `RedrawDuringFrames` (node) | Listener added on attach, removed on detach; `shouldAutoInvalidate = false`; no allocation per frame. | Clean (cat. 7). |
| `PerformanceTab` | `snapshot` state every 500 ms recomposes the numbers while the tab is open, which is the user's request being served; the graphs read `version` in their draw phase only. | Kept on purpose: two recompositions a second of a dialog the developer opened, against graphs that never recompose. |
| `FrameTimeGraph`, `FpsGraph`, `HistogramGraph`, `MemoryGraph` | `drawWithCache` for paths, dash effects and label layouts; `Path.rewind()`; series arrays remembered once; `version()` read in `onDrawBehind`. One `String.format` per draw of the FPS and memory scale labels. | Clean (cat. 1, 6); the label strings are at most two a second while the tab is open. |
| `FrameSections`, `ScreenRow`, `DetailSections`, `ScoreSection` | Bounded lists (at most 33 screens, 5 endpoints, 10 StrictMode examples, 14 metrics) in `Column`s, no lazy layout (cat. 3: small and bounded); `sortedByDescending` of at most 33 screens per recomposition, cheaper than a `remember` slot. | Clean. |
| `FrameTimeGraph`, `HistogramGraph` (run 3 change) | The gutter width and label gap are computed once in `drawWithCache`; the per-draw label placement uses float locals and `max`/`min`, no allocation; the version read stays in `onDrawBehind` (cat. 1, 6). | Clean. |
| `DevToolsOverlay`, `EdgeBadge` (devtools) | The page lines are invoked in a bounded `forEach`; the runtime visibility check is gone, since the module is only in builds that show it. | Clean. |
| `PanelContent`, `BuildTab` (devtools) | Tab keys remembered on `pages`; titles resolved per composition (a handful of `stringResource` calls); the screen name collected with lifecycle. | Clean. |
| `FrameTimeGraph` (run 4) | Two `drawRect`s per bar, colours picked by `when` over `Color` locals (a value class: no boxing), the faded tones made once per composition with `copy(alpha)`; the series' new `uiMillis` array is allocated once with the series (cat. 1, 6). | Clean. |
| `FrameSections` → `FpsGraph`, `HistogramGraph` (run 4) | The rate lambdas captured the snapshot, so each 500 ms refresh made new lambda instances and `drawWithCache` rebuilt its dash effect and labels; they now capture the `Float` rate, which strong skipping memoises (cat. 1, 6). | **Fixed.** |
| `PerformancePill`, `PerformanceTab` (run 4) | `collectAsStateWithLifecycle` (A-2); the pill's size remembered on all three formats; no new state. | Clean. |
| `ScoreSection`, `StrictModeSection` (run 4) | Null-checked total and grade read once into locals; at most 10 StrictMode examples worded with `stringResource` in a bounded `forEach`. | Clean. |
| `AppRoot`, `ReportCurrentScreen` (app) | Optional effect guarded with `if (devTooling != null)` (cat. 5); the screen reported from a `snapshotFlow` over the back stack in a `LaunchedEffect`, with `rememberUpdatedState` for the lambda, so a navigation recomposes nothing (cat. 1, 5). A `SideEffect` would read the top key in composition and recompose the root on every navigation. | Clean. |

## Rules and gotchas the docs must state

For WP12 (REQUIREMENTS 25); `templates/android/docs/PERFORMANCE-OVERLAY.md` already says most of it in the project.

- **Judge smoothness with the overlay hidden, on devRelease.** The gates and the final numbers are taken hidden, which
  is devRelease's and the benchmark build type's default. Shown, it costs the numbers in Measurements.
- **Debug builds are pessimistic.** No R8, no baseline profile, mostly interpreted: on the Redmi a devDebug scroll
  read about 50 % janky where devRelease reads a few percent. The overlay says so (tab, log, report) and draws its
  debug numbers without a verdict colour; never quote a debug build's numbers.
- **Janky means missed its deadline** (FrameTimeline), as `perf.sh` and `dumpsys gfxinfo` count it. JankStats' own
  `isJank` ("over twice the interval") counts about half of those; the overlay and the field analytics differ by
  design.
- **Frames per second is "while drawing".** An app that has nothing to draw is not slow: Android draws only on
  change. Too little drawing to judge reads idle; a frame keeps the display busy until the next one starts, capped
  at its own duration rounded up to whole intervals.
- **The inspector and the overlay are absent from stagingRelease, prodRelease and playstoreRelease**, not hidden:
  `debugImplementation` + `"devImplementation"`, the root reaching them through `Optional<DevTooling>`, and
  `verify<Variant>CarriesNoDevTooling` failing the build if any module pulls them into a release outside the dev
  flavour. A staging release has no badge; give QA a stagingDebug or a devRelease.
- **One JankStats per window.** Dev tooling that wants frames takes them from `FrameJankRecorder.frameListener` when
  analytics is on; it never adds a second JankStats.
- **Hidden means no listener.** The overlay off puts nothing on the window, samples nothing and allocates no ring;
  the recorder starts JankStats (or the shared listener) only while recording. It is still built at launch, with
  the inspector, on the main thread: an activity-lifecycle callback, a DataStore and one read of the stored choice
  (Measurements, run 4, for what that costs a cold start).
- **Nothing per frame, and no frame of its own.** Collection writes fixed primitive arrays on JankStats' thread; no
  allocation, boxing or log line per frame. The pill holds no state and is redrawn at most 4x/s from the window's
  pre-draw pass (`redrawDuringFrames`), so only inside frames the app draws anyway: a lone redraw after idle misses
  its deadline on a real phone (measured: 21 ms, all janky), and a timer-driven readout makes the jank it reports.
  While the app is idle the pill keeps the numbers of its last drawing. Graphs draw only while the Perf tab is open.
- **A PSS read costs a frame if it lands in one.** `Debug.getMemoryInfo` (and `dumpsys meminfo`, which makes the
  app run it) walks every mapping under the address-space lock; on the Redmi, reads during scroll flings cost
  about one janky frame each (`perf/HUD/pss-probe/`). The overlay reads PSS only after 250 ms without a frame, at
  most every 15 s. The same rule for app code and for measuring: never sample meminfo inside a measured journey;
  `perf.sh`'s `meminfo` is a scenario of its own, which keeps it that way.
- **Screens are labelled by the key's serial name** (`ReportCurrentScreen` in the root, `route=<name>` as JankStats
  state while recording). A key with a custom `@SerialName` shows that name; one missing from `navKeys` falls back to
  its class name, which R8 obfuscates.
- **The overlay lives in `:core:perfhud`, wired with `debugImplementation` and `"devImplementation"` only.** Never
  `implementation`: that would ship it. The build guard and a generator test guard the wiring;
  `out/HUD/apk_inspect.py` checks an APK (dexdump, aapt2 manifest and resources, raw strings).
- **adb path:** `adb shell run-as <id> am broadcast -n <id>/<package>.core.perfhud.report.ReportReceiver -a
  <package>.core.perfhud.action.EXPORT` on a debug build (the receiver is not exported); devRelease exports from the
  tab; `--es perf.hud on|off` at launch switches recording for that process (`perf.sh --extra perf.hud=on`).
- **StrictMode** thread checks are on for the main thread while recording (API 28+), the app's own policy restored on
  stop. Start-up times need Android 15 (`ApplicationStartInfo`).
- **The score** grades each metric against `gates.toml`, Android vitals and the build checks; unmeasured metrics are
  left out, and there is no score at all before 300 frames (the build checks alone would rate an unused app). Its
  percentiles are main-thread time against one refresh interval: a frame's full duration, vsync to GPU done,
  routinely runs past one interval while on time, because the pipeline gives it more; whether it made its deadline
  is the janky count.
- **Gate runs launch with the overlay off.** The Live stats switch is kept in DataStore across launches and across
  reinstalls that keep the app's data, so one toggle by a developer would put the overlay into every later
  `perf.sh` run of that devRelease. WP04/WP13 launch gate runs with `--es perf.hud off` (`perf.sh --extra
  perf.hud=off`) or `pm clear` the app first.
- **StrictMode while recording merges with the app's policy.** The checks are added to the app's own thread policy,
  penalties included: a policy with `penaltyDeath` dies of the added disk-read checks too, and a policy the app sets
  while recording replaces the overlay's until the next start.
- **The log's scopes.** A window line's counts (`gc=` included) are that window's and its memory sizes the latest
  sample's; a screen line sums every visit to the screen since recording began.

## Open issues

- **`perf.sh` has not measured the overlay yet**, by design of the branch: `wp/HUD` is cut from `wp/WP06`, whose
  template has none of the `tab_<i>`/`primary_list` test tags or the `perf.fakeSession` launch hook that `perf.sh`
  waits for, so the overhead is measured with WP04's own parsers (`parse_gfx`, `parse_framestats`) through
  `out/HUD/overhead.py`, the same gfxinfo reset/dump per step. Once HUD sits on top of WP04 (to-release order),
  WP13 runs `perf.sh --variant devRelease --extra perf.hud=on|off` for the official numbers.
- **Merge order:** the score's fixes and `docs/PERFORMANCE-OVERLAY.md` name `tools/perf/perf.sh` and
  `tools/perf/gates.toml`, which arrive with WP04 (unconditional in every generated project there). Merged before
  WP04, a generated project would point at tools it does not have yet; `to-release.js` merges WP04 first.
- **The demo's SHOW.md** is the workflow's capture/verify step, not this one; `perf/HUD/show/` and `out/HUD/show.py`
  are there for it (the script drives both builds, records, and pulls the export). `perf/HUD/show/` (run 3) shows the
  score and graphs before run 4's changes; `perf/HUD/show-f1/` (run 4, release only, from `0cea54c`) has the new score
  and frame-time graph, but its histogram predates `e753d88`. Recapture both builds from the final head for SHOW.md.
- **For WP04/WP13 (run 4):** `perf.py` sets `perf.fakeSession` by default; setting `perf.hud=off` by default the
  same way would make every gate run independent of a stored Live stats choice (Rules: "Gate runs launch with the
  overlay off").
- **App memory target at the merge (run 4):** the score's A for app memory (48 MB, `OptimisationScore.kt` limits and
  KDoc, `perfhud_fix_app_memory`, `PERFORMANCE-OVERLAY.md`'s table) copies `meminfo.pss_app_mb` as it was. On
  `wp/WP04` (`4a15dda`, Oct 19 07:45) that row is now 58.4 (solid) / 53.3 (standard), non-regression. Re-sync these
  four copies when HUD merges after WP04 (to-release order), not before: WP04's numbers still move.
- **Pill redraw cost (run 4, kept):** each pill redraw formats two strings (`String.format`, boxing two Ints) and
  lays out two `TextLayoutResult`s, at most four times a second inside app frames. The main-thread scroll p99 does
  not isolate it: 1.80 → 3.69 ms (run 3, everything), 1.78 → 3.91 ms (run 4, standard), but 3.91 → 3.73 ms (run 4,
  everything), so it moves by 2 ms between runs either way, while the mean moves +0.003 to +0.09 ms and the jank not
  at all beyond noise. Within the limits; a glyph cache (digits laid out once in the mono font, drawn at fixed
  advances from a reused `CharArray`) would remove the cost if a later package wants it gone.
- **Hidden start-up cost (run 4, measured, kept):** +10 ms median cold start in devRelease against `wp/WP06`
  (within noise, Measurements). If a cold-start gate ever needs it back: provide the page with
  `dagger.Lazy<PerformanceRecorder>` and build the recorder after the first frame, keeping only `FrameFeed` (the
  lifecycle callback that catches the launch extra) and the stored-choice read at launch; recording on in debug and
  on request would then start one frame later. Not done in run 4: it changes when recording can begin and needs its
  own acceptance.
- **Not extended (run 4):** `DevToolingGuardTest` still covers the graph walk only, not the dev-flavour skip or the
  `check`/`pre<Variant>Build` wiring, and the guard covers the `release` build type only (prod/staging
  `benchmarkRelease` and `nonMinifiedRelease` are not shippable). The pill's `minTouchTarget = 0.dp` stays (a
  dev-only control nested in the badge), and so does `DevToolsPanel`'s literal `"Screen"` (`:core:devtools`' own
  literal-copy convention, kept from WP06).

## Lock time used

- Run 4: Gradle 08:24-08:25 (perfhud compile), 08:30-08:31 (perfhud tests), 08:31-08:32 (the mutation check),
  08:36-08:37 (first `f1_builds`, failed in lint on Metaspace), 08:39-08:50 (`f1_builds`), 08:53-09:07 (perfhud
  tests), 09:08-09:11 (`f1_base`), 09:11-09:37 (`f1_accept`). From 08:53 to 09:05 that hold waited on memory: the machine's
  commit charge had about 650 MB left of the 1500 MB `locked` asks for, with an idle Gradle daemon (3.2 GB) and its
  Kotlin daemon that another agent's build had started at 08:49. adb 08:46-09:08 (`f1_phone.win.sh`), 09:11-09:17
  (`f1_startup.win.sh`), 09:17-09:19 (`show.py`); WP04's `final-gates.win.sh` held it 08:14-08:46.
- Run 3: Gradle 06:12-06:21 (everything phone builds), 06:53-07:24 (`accept_builds.win.sh`: phone APKs,
  template `build`, the two fixtures and the old standard copy's check), 07:43-07:48 (standard regenerated and
  checked); an earlier attempt at 06:52 stopped at once (the
  generator could not move `out/HUD/everything` aside while the APK inspection read it). adb 06:21-06:33 (overhead
  + first capture), 06:38-06:41 (PSS probe), 06:59-07:15 (overhead again + capture). WP04 held Gradle 06:21-06:52
  waiting for memory that run 3's idle daemons held until their 30-minute timeout.
