# WP04 resume brief (2026-10-14 18:55 IST; `wp/WP04`, worktree `$OV/wt/WP04`; full context `handoff/WP04.md`)

## 1. Exact state
- Last commit `bdf61e8` (18:01) "Recompose only the two tabs a switch changes" (`shell/tabs/KeptTabs.kt`); missing from
  the handoff's Progress list (stops at `50a20d1`). Progress state: ESCALATED (B.1 perf timebox, Open issues item 1).
- Uncommitted, 1 line: `core/navigation/src/test/.../shell/ResizeContinuityTest.kt:54` now `remember { builds.merge(key, 1, Int::plus) }`,
  fixing lint `RememberReturnType` that failed `:core:navigation:lintDebug` in `$O/logs/r16-accept-local.log` (BUILD
  FAILED, though `r16-iter2.log` says "exit 0"). Looks finished; the re-run `r16-lint.log` stops at 18:17 with no result.
- Tests: 120 navigation unit tests green at 18:01 (`r16-nav-test.log`); accept-local RED on that lint only.
- Phone: `phase3e` (everything) stopped at run 2/3 `rapid_back`, 18:17 (`r16-gates2.log`, no process alive); standard
  and lean not run. Its APKs were built at `338d26c`, so they lack `50a20d1` and `bdf61e8`.

## 2. Still owed
- [ ] REQ 19/20 phase 3 gates: finish `phase3e` (3 presets), fill Measurements "Phase 3e" and the escalation's PSS/recompose lines.
- [ ] REQ 34 (orchestrator note "shimmer and clipping"; not started, code as at `c14848d`): K3 Docked/Minimal/Chunky
      scroll-edge bar (`AppTabBar.kt:279-292`, front list `canScrollForward` read in draw); K6 lazy `chromeContentPadding`
      maxed with system bars (`foundation/layout/Chrome.kt:93`) + no-recompose test; K11 `shell/BottomEdgeFade.kt` tail
      <= list gutter, fade to front screen's background; K13 window-wide settled top chrome for WP06's badge; D9 dark cold
      start (`MainActivity.followThemeOutsideTheApp` :192 is async, not before frame 1); D12 labels/pill at font 1.5.
- [ ] REQ 33 / SHEET-SPEC §19.2, all after WP03 merges: `Presentation.Sheet(SheetSpec)` + `PresentationKey`, `presentation/sheet/*`,
      card stack (QA-11/M6), §11.3 seams, B-36/B-43/B-58, §16.1 perf rows, §15.3/§15.4 tests. Plus WP03 leftovers:
      `AppTheme.springs`/`reduceMotion`, filled glyphs, AD-15 posture/input traits.
- [ ] REQ 35: owned by WP05/WP11/WP07-B/QA2; WP04 only keeps programmatic selection and `tabs_tap` usable (inferred).
- [ ] REQ 36: WP04 blocks the Android release via 19/20, 33, 34; merge needs the ruling and `accept.sh` green.

## 3. Next three steps
1. Under the gradle lock run `:core:navigation:lintDebug` (`$OV/wt/WP04/templates/android`), commit `ResizeContinuityTest.kt`,
   re-run `accept-local.sh` to green, add `bdf61e8` to Progress in `handoff/WP04.md`.
2. Rebuild the three presets at HEAD; under the adb lock rerun `phone-gates.sh phase3e everything standard lean` into
   `$P/phase3e`; fill Measurements "Phase 3e" in `handoff/WP04.md`.
3. Code REQ 34 (no phone needed): `core/designsystem/.../foundation/layout/Chrome.kt` (K6, K13),
   `core/navigation/.../shell/BottomEdgeFade.kt` (K11), `.../component/navigation/AppTabBar.kt` `BarSurface` (K3),
   `app/.../MainActivity.kt` (D9); tests, then phone frames.

## 4. Waits for
- Orchestrator ruling on the perf escalation before `accept.sh` passes its `perf.sh --gate` line.
- The phone (Redmi `9a7c74f4`, unlocked) for phase 3e and every REQ 34 acceptance (release build, light/dark, font 1.5).
- WP03's merge (order WP10, WP04, WP03) for REQ 33, leftovers, `LocalScaffoldBackground` (K11), the `AppScaffold`
  double fill and scroll rows; WP02-M3 for a sample sheet. CP2 is WP05's glass decision, not a WP04 wait.
