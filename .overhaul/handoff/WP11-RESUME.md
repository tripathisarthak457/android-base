# WP11 resume brief (PeriFlow, `wt/WP11`, local branch `overhaul/smooth-glass`, never push) - written 2026-10-14

## 1. Exact state
- Last commit `e57b40c` (10-14 17:29) "Magnify the travelling lens's image, not its drawing"; 15 commits on `main` (`240871f`).
  Lens chain for REQ 35: `58c17e0` > `bda089d` > `70dc632` > `dcc2663` > `7ccce38` > `e57b40c`.
- Working tree clean: no uncommitted files, no stash. Nothing half-done on disk.
- Tests: `:core:designsystem:testDebugUnitTest` green on `e57b40c`'s tree (`logs/WP11/lens-tap-build8.log`, TEST EXIT 0,
  APK `perf/WP11/apk/lens-tap-5-arm64.apk`). All modules last green at `f9f357e` only (`logs/WP11/test-apk1.log`).
- Phone run 9 (`logs/WP11/lens-tap-phone5.log`, 18:04, Live pinned, 3 A/B pairs vs after-early-2): tab switch janky
  3.62% (base 3.41, gate 2.5: both fail), GPU p90 13 ms (base 14), hitch 20.8 ms/s (base 17.0), visited first frame max
  15.5 ms (gate 20, passes). First-lift atrace failed for lens-4 and lens-5 (no primary_list row on tab 2); the script
  was edited at 17:55 (`run.at_root(0)`) but never re-run. Issue cost not fixed: tabs_011 issue max 40-100 ms vs base 8-12.
- Handoff `## Lens on tap` describes run 6 (`bda089d`) only and still has the `DEVICE_RESULTS` placeholder; 70dc632..e57b40c
  undocumented there. SESSION-2026-10-14: review and verification pending.

## 2. Still owed (checklist)
- [x] REQ 35 behaviour (Live only, taps + back stack + deep link, G1 kept, drag keeps press, reduce motion flat): e57b40c, unit-tested.
- [ ] REQ 35 gate: tab janky 3.62% > 2.5 (base also fails); hitch +3.9 ms/s and issue time 40-100 ms vs 8-12 over base.
- [ ] REQ 35 strips: recorded (`perf/WP11/lens-tap/strips-lens-5{,-across}/`, `every-frame-lens-5/`); visual check pending.
- [ ] REQ 35 fresh-context review (lensFlow in `scripts/to-release.js`, 3 rounds max); `LENS:periflow` not fired.
- [ ] Handoff: update `## Lens on tap` for runs 7-9, replace `DEVICE_RESULTS`; all-module tests on HEAD.
- [ ] REQ 33 (sheet): no PeriFlow item in `research/SHEET-SPEC.md`; WP11 only ports the generator's sheets (second half).
- [ ] REQ 34 (shimmer/clipping): no owner note in WP11.md; only arrives through the generator ports.
- [x] REQ 36: PeriFlow is in neither deploy; WP11 never blocks the Android release (FINAL.md may say "pending").
- [ ] Second half: steps 2-4; ports LiquidTabs, SceneBackdrop, KeptTabs v2 (+`LocalTabVisible`), BackGate, sheets,
      ChromeInsets; final measure into `perf/WP11/after/` with `accept.sh`; device gates (`perf/WP11/gates.toml`).

## 3. Next three steps
1. Find the per-lift cost: re-run `logs/WP11/lens-first-lift-trace.py` (lens-tap-5 vs after-early-2) and fix it in
   `periflow-android/core/designsystem/src/main/kotlin/com/jorwen/periflow/core/designsystem/foundation/glass/LiquidLens.kt`
   (and `component/navigation/GlassBottomBar.kt`, `GlassRail.kt`; tests in `.../src/test/.../glass/LiquidLensTest.kt`).
2. Rebuild and re-gate: `logs/WP11/lens-tap-build4.sh lens-tap-6`, then `logs/WP11/lens-tap-phone5.sh lens-tap-6-arm64.apk lens-6`.
   The 3-iteration budget is spent (lens-2..5): if janky stays > 2.5, record "blocked" with the A/B evidence (base fails too).
3. Rewrite `handoff/WP11.md` `## Lens on tap` (runs 7-9, numbers, strip paths), then the fresh-context review on
   `git -C wt/WP11 diff 06ad83e..HEAD -- periflow-android` and all-module tests with `logs/WP11/test-and-apk.sh`.

## 4. Waits for
- The lens review passing (fires `LENS:periflow`, which starts the WP11 package flow in `scripts/to-release.js`); QA2 re-verifies REQ 35.
- The phone, Redmi 9a7c74f4 (M2101K6I), for every gate and strip (via `bin/locked adb`); emulator `periflow_api35` = correctness only.
- Second half: WP04, WP05 (its lens fix had 2 blocking review items), WP06 merged, and `decisions/CP2.md` (only CP1.md exists).
