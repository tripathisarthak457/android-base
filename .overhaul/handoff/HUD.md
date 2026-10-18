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

## Design and decisions (recorded because the user is not to be asked)

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

## Public APIs added, changed, removed (signatures)

```kotlin
// :core:analytics
class FrameJankRecorder { @Volatile var frameListener: JankStats.OnFrameListener? }
// :core:devtools
class DevToolsPage(val key: String, @get:StringRes val title: Int, val content: @Composable () -> Unit,
    val badgeContent: (@Composable (openPage: () -> Unit) -> Unit)? = null)
class CurrentScreen { val name: StateFlow<String?>; fun report(name: String) }
class DevTools(environment, log, currentScreen: CurrentScreen, pages: Set<DevToolsPage>, flags, flagOverrides)
// :core:perfhud: internal throughout; its surface is the @IntoSet DevToolsPage and the receiver's actions
//   <package>.core.perfhud.action.EXPORT / RECORD (--ez recording), and the launch extra perf.hud=on|off.
```

## Measurements

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
- **Frames per second is "while drawing".** An app that has nothing to draw reads idle, not a low number: Android
  draws only on change. A frame keeps the display busy until the next one starts, capped at its own duration rounded
  up to whole intervals.
- **One JankStats per window.** Dev tooling that wants frames takes them from `FrameJankRecorder.frameListener` when
  analytics is on; it never adds a second JankStats.
- **Hidden means no listener.** The overlay off puts nothing on the window, samples nothing and allocates nothing;
  the recorder starts JankStats (or the shared listener) only while recording.
- **Nothing per frame.** Collection writes fixed primitive arrays on JankStats' thread; no allocation, boxing or log
  line per frame. The pill is drawn from draw-phase reads, refreshed 4x/s and only when a number changed, so it never
  recomposes the app and never keeps an idle app drawing. Graphs draw only while the Perf tab is open.
- **Screens are labelled by the key's serial name** (`ReportCurrentScreen` in the root, `route=<name>` as JankStats
  state while recording). A key with a custom `@SerialName` shows that name; one missing from `navKeys` falls back to
  its class name, which R8 obfuscates.
- **The overlay lives in `:core:perfhud`, wired with `debugImplementation` and `"devImplementation"` only.** Never
  `implementation`: that would ship it. A generator test guards the wiring; `out/HUD/apk_inspect.py` checks an APK.
- **adb path:** `adb shell run-as <id> am broadcast -n <id>/<package>.core.perfhud.report.ReportReceiver -a
  <package>.core.perfhud.action.EXPORT` on a debug build (the receiver is not exported); devRelease exports from the
  tab; `--es perf.hud on|off` at launch switches recording for that process (`perf.sh --extra perf.hud=on`).
- **StrictMode** thread checks are on for the main thread while recording (API 28+), the app's own policy restored on
  stop. Start-up times need Android 15 (`ApplicationStartInfo`).
- **The score** grades each metric against `gates.toml`, Android vitals and the build checks; unmeasured metrics are
  left out, and fewer than 300 frames are not graded.

## Open issues

## Lock time used
