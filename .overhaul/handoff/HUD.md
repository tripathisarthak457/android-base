# HUD handoff (REQUIREMENTS 40: live performance overlay, log, export, score)

## Progress

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

## Design and decisions (recorded because the user is not to be asked)

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
  PSS (`Debug.getMemoryInfo`, milliseconds of a background core) every 15 s, start-up from `ApplicationStartInfo`
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
```

## Measurements

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
| `DevToolsOverlay`, `EdgeBadge` (devtools) | The page lines are invoked in a bounded `forEach`; the runtime visibility check is gone, since the module is only in builds that show it. | Clean. |
| `PanelContent`, `BuildTab` (devtools) | Tab keys remembered on `pages`; titles resolved per composition (a handful of `stringResource` calls); the screen name collected with lifecycle. | Clean. |
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
- **Hidden means no listener.** The overlay off puts nothing on the window, samples nothing and allocates nothing;
  the recorder starts JankStats (or the shared listener) only while recording.
- **Nothing per frame, and no frame of its own.** Collection writes fixed primitive arrays on JankStats' thread; no
  allocation, boxing or log line per frame. The pill holds no state and is redrawn at most 4x/s from the window's
  pre-draw pass (`redrawDuringFrames`), so only inside frames the app draws anyway: a lone redraw after idle misses
  its deadline on a real phone (measured: 21 ms, all janky), and a timer-driven readout makes the jank it reports.
  While the app is idle the pill keeps the numbers of its last drawing. Graphs draw only while the Perf tab is open.
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
  left out, and fewer than 300 frames are not graded.

## Open issues

## Lock time used
