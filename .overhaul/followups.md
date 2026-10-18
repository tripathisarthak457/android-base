# Follow-ups for WP13

Items a package could not close on its own branch: review findings left open when the rounds ran out, and work
that needs another package's code first. WP13 closes every item (or records why it is rejected).

## WP06

- **Field jank attribution by navigation state (SMOOTHNESS-PLAN S16).** `FrameJankRecorder` (`:core:analytics`,
  `<opt:analytics>`) already counts frames per `PerformanceMetricsState` state and reports them anonymously when the
  app leaves the foreground; only the tags are missing, because they come from WP04's `ShellObserver` seam
  (`app/.../startup/LaunchHook.kt` on `wp/WP04`), which was not on the integration branch when WP06 finished. Once
  WP04 has merged: add an `<opt:analytics>` `ShellObserver` in `:app` (multibound like WP04's `ShellLog`) that
  runs `snapshotFlow { Triple(state.moving, state.selectedIndex, state.current.keys) }` and puts `nav` into
  `PerformanceMetricsState.getHolderForHierarchy(window.decorView).state` while the shell moves: `tab` on a change
  of tab, `push:<top key's class simple name>` for a deeper stack, `pop` for a shallower one, removed when
  `moving` turns false. Never set it per frame from composition. Test: a fake shell state moving through a push
  and a pop puts and removes the tag. Details: `handoff/WP06.md`, "Open issues".
- **Repository fakes into `testFixtures` (STANDARDS-PLAN ST-03, T-2).** Needs `testFixtures { enable = true }` in
  WP02-M2's data convention plugin and `testImplementation(testFixtures(project(":data:x")))` in the feature
  build files (WP02-M3). The fakes compile against the new repository APIs where they are.
- **WP06's device checks on the Redmi: closed (Oct 14, 18:03).** The drag check and QA-44 passed on 9a7c74f4 at
  `1dcacd9` (`out/WP06/logs/accept13.log`); MIUI's gesture navigation honours the badge's `systemGestureExclusion()`.
  Numbers in `handoff/WP06.md`, "Measurements".
- **Emulator device scripts and "System UI isn't responding" (WP04's `tools/perf`, WP14, whoever records on
  `periflow_api35`).** On a loaded laptop the cold-booted AVD shows that dialog over the app after boot, even after
  `am wait-for-broadcast-idle`. WP04's four RTL recordings on Oct 14 at 11:45 failed on it ("the tab bar (tab_0)
  did not appear": each `failure_run0_000.xml` under `perf/WP04/rtl/emulator/` is the dialog), as did WP06's
  `accept7` and `accept9`. WP06's checks answer Wait for any process but the app under test and look again
  (`out/WP06/system_dialogs.py`, 30 lines); `perf.py`'s wait for its first view could do the same.
- **`SampleListViewModel` can stay on Empty with rows in its state (WP02-M3, the list screen).** Its observer of
  `repository.items` updates `items` but only ever moves `loadState` from Loading to Refreshing, so once the
  refresh's read-back has set `Empty`, rows that arrive later through the observer stay hidden behind "Nothing
  here yet". WP06 fixed the trigger on the Redmi (the repository's read-back returned the list from before the
  write; `handoff/WP06.md`, Progress, run 8), but the observer should also move `Empty` to `Success` when rows
  arrive, so a repository that publishes a write late cannot hide them.
- **Windows the content opens sit above the app-lock cover (WP03's `AppMenu`; the DevTools panel until WP06 moves
  it).** `AppLockGate` covers the activity's own window. A `Popup` or `Dialog` the content opened before the lock
  engaged (Home with a menu open, back after 30 s) is a window of its own above the cover: still visible, still
  taking taps on actions for the content the cover hides. On the integration branch at `8672003` that is every
  `AppDialog`, `AppBottomSheet`, `AppDropdownMenu` and `AppTooltip`. WP03's overlay host draws sheets and alerts
  inside the window (SHEET-SPEC D-22), which leaves `AppMenu`'s `Popup` (`wp/WP03`
  `component/overlay/AppMenu.kt:84`) and WP06's DevTools `Dialog` (moving to an `AppSheet`, SHEET-SPEC §19.8). For
  `AppMenu`: close it on `Lifecycle.Event.ON_STOP`, as iOS closes menus when the app leaves the foreground, or host
  it in the window like the sheets; then a test that a menu open at `onStop` is gone at `onStart`. The docs rule
  (WP12): nothing that shows the app's data opens a window of its own.
- **`python -m tests.consistency` cannot run on Windows (WIN, the generator).** Its `ProcessPoolExecutor` hands
  workers `run_one`, defined in the package's `__main__.py`; Windows spawns workers, and `multiprocessing` never
  re-imports a package `__main__` in a worker (`spawn._fixup_main_from_name` skips names ending in `.__main__`),
  so every worker dies with "module '__main__' has no attribute 'run_one'" and the run ends in
  `BrokenProcessPool` before checking anything (WP06's `out/WP06/logs/accept14.log`, WP04's
  `out/WP04/logs/r22-gen.log`). Not in the generator baseline, which covers the unit tests only. Fix: move the
  runner's body into an importable module (`tests/consistency/runner.py`) and keep `__main__.py` a two-line entry
  point. Until then `out/WP06/consistency_win.py` runs the same `main()` after importing it under its own name.
  Still open after WIN merged (Oct 18): WIN left `tests/consistency/` as it was.
- **A time limit on every generated project's test tasks (WP02, build-logic; WP12 docs).** A unit test JVM that stops
  making progress holds its build for ever: on Oct 17 `:core:applock:testDebugUnitTest` sat idle from 16:18 to 18:45
  under the shared Gradle lock (`handoff/WP06.md`, Progress run 15; the test is fixed on `wp/WP06` `f8640b2`). A
  `tasks.withType<Test>().configureEach { timeout.set(Duration.ofMinutes(15)) }` in the Android convention plugin
  turns the next such stall into a failed task on a developer's machine and in CI instead of a hung runner. Until then
  WP06's scripts pass `out/WP06/test-timeout.init.gradle` with `-I`, and other packages' long scripts can do the same.
- **What to check once NET's Cronet transport lands (REQUIREMENTS 39).** Read from WP06's network module
  (`handoff/WP06.md`, "Seams for NET"): an application interceptor that short-circuits OkHttp also skips its
  `Cache` and `CertificatePinner`, so on the Cronet path check that the pins are mirrored into the engine (with
  its bypass for user-installed trust anchors off), that the engine keeps no disk cache sign-out cannot empty
  (the OkHttp cache is a `SessionScopedStore`), that WebSockets still connect over plain OkHttp, and that images
  go through it too (Coil is on the shared client since `wp/WP06` `10e2916`).

## WP04

- **The sheet system's navigation half and the card stack (REQUIREMENTS 33, `research/SHEET-SPEC.md` §19.2; QA-11,
  MOTION-PLAN M6).** WP03 merges after WP04 and builds `AppSheet`, `AppSheetState`, `SheetMaterial` and the four
  `foundation/layout/` seam files the spec puts WP04's part on, so none of it could be built on `wp/WP04`. After
  WP03 merges: `Presentation.Sheet(SheetSpec)` with `PresentationKey` (delete `SheetDetent` and the string metadata
  key, §4.11); `presentation/sheet/` (`SheetSceneStrategy`, `SheetScene`, `SheetSessions` with its decorator and
  saver, `PresentedSheetFrame`, `AppSheetNavHost`), the host building its own strategy and `AppShell`'s
  `sheetStrategy` parameter removed (§11.2); covers' `cardStackPresenter(level)` and `hideFromAccessibility` beneath
  modal presentations (N8); the shell seams of §11.3 (`cardStackPresenter(stack, 0)`, window backing, status-bar
  flip, `LocalHostMotion`, `LocalEntryVisibility`, `LocalPaneBounds`, `LocalPresentationLevel`,
  `WindowChromeWriter` (was `BottomChromeWriter`) N9, the `Chrome.kt` reconciliation N11); `ShellState.submit` per B-58; Back per B-36/B-43;
  the §16.1 `perf.sh` scenarios and `gates.toml` rows, `sheet_large`, the `no_offscreen` trace check and profile
  steps that open, drag and dismiss a sheet; tests §15.3, `CardStackShellTest` and the §15.4 card-stack journey.
  Also WP04's leftovers from WP03's list: `AppTheme.springs.screen`/`navigation` and `reduceMotion` into
  `ScreenMotion` and the bar, the filled glyphs in `AppDestinations`, `rememberWindowPosture` and
  `rememberInputTraits` into `AppTheme` (AD-15). Details: `handoff/WP04.md`, "Open issues".
- **REQUIREMENTS 34 seams that WP03's merge wires (WP04 run 17; `handoff/WP04.md`, Deviation 27 and Open issues).**
  WP03's `AppScaffold` writes its settled top inset and its `background` into `LocalScreenChrome.current.topBar` and
  `.background` (the shell then publishes the front screen's top bar as `LocalWindowTopChrome`, which WP06's DEV
  badge reads, K13, and fades the bottom edge into the screen's background, K11); `AppBottomActionBar` writes
  `LocalScreenChrome.current.bottomBar` (`LocalScreenBottomBar` is gone); `AppTheme` provides a fresh
  `LocalWindowTopChrome` per window beside `LocalBottomChrome`; with content running under the top bar,
  `chromeContentPadding`'s top becomes `base + max(top chrome, statusBars.top)` (K6). WP02-M3: `AppPagingList`
  takes its `LazyListState` so the feed can `ReportScrollEdge` and `ScrollToTopOnReselect`.
- **The back card's shaders on the first swipe after an install (SMOOTHNESS S6).** Skia compiles the programs for
  a scaled, round-clipped (and, on commit, faded) screen on the render thread the first time a user swipes back:
  up to 70 ms on one frame, once per install. `TabWarmUp` already draws each warmed tab once under the tab in front;
  giving that draw the card's pose (scale, round clip, `ModulateAlpha` below 1) would compile them while the app
  is idle. Measure `first_gesture` before and after.
- **Zoom (hero) transitions, MOTION-PLAN M5 / REQUIREMENTS 21.** WP04's Deviation 18, run 16 note: the zoom is
  built in the shell's layer and draw blocks (`NavTransitionStyle.Zoom`, `Modifier.zoomSource(key)`, `StackScene`
  posing the arriving page from the source's bounds, the slide as the fall-back), so no `sharedTransitions` option
  and no `SharedTransitionLayout` lookahead; WP02-M2's handoff waits on this. Then WP02-M3's sample opens a row's
  detail with it, and `perf.sh pushpop` and a `record.sh push` strip verify it (composing frame within +1 ms).
