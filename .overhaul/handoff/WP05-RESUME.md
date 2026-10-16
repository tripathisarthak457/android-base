# WP05 resume brief (liquid glass) — written 2026-10-14 ~18:55 IST, read-only pass

> Run 12 (Oct 15, 16:00) acted on this brief: `handoff/WP05.md` is current again at `5d515a0` (run 11 in "Progress",
> "Lens on tap" rewritten for run 11's lift, run 11's mutation check run as `logs/lens/run12-*`), and step 3's merge
> order now starts with `wp/WP05-glass` (WP05.md, "Next run starts here"). Read WP05.md first; the rest of this brief
> is the state at Oct 14, 18:55.

## 1. Exact state
- Worktree `$OV/wt/WP05`, branch `wp/WP05`, **clean** (`git status`/`git diff --stat` empty). 14 commits over
  `overhaul/multiplatform-v2`; last `30c03ad` (18:17:19) "Give each lens test one behaviour", before it `fb16383`
  (18:17:14) lift rises from the glass on screen, `samplesBackdrop` a written flag. Neither is in WP05.md "## Progress"
  (stops at run 10, `6c2c23a`) nor in its "Public APIs" (still says derived).
- Tests: last known **green** — `logs/lens/run11-test1.log` (18:16) `:core:designsystem:testDebugUnitTest` BUILD
  SUCCESSFUL (count not in log; no `run11-results/` copied). Run 10: 79 pass (`run10-test2.log`). Run 11's mutation
  check never ran (`run11-mutants.out` only says "waiting for lock", started 18:17:23). Timings: `run11-timing.txt`.

## 2. Still owed (merged tags: WP01, WP02-M1, WP09, WP10 only)
- [ ] R35 `LiquidTabs(orientation)` bar+rail, lens wired, `AppTabBar` Glass branch, `followCovered` alpha layer fixed
- [ ] R35 gate: `glass.quality=Live` tab-tap janky %/GPU p90 within `glass_live` vs `LiveNoLens`; CP2 tap-morph strips
  (next tab, two away, Back-stack, reversal, two taps 300 ms apart; before/after). "Lens on tap" lift figures are
  run 10's (0.01/0.16/0.52/0.94); run 11's are 0.07/0.25/0.50/0.79 (`run11-timing.txt`).
- [ ] R33 (SHEET-SPEC §19.3, §11.5; final Oct 13): `foundation/glass/GlassSheetBackdrop.kt` via `SheetBackdropHolder`
  written by `GlassStage`; live sampling only over the shell with no cover; bar-inside-glass or documented gap;
  `SceneBackdrop` invalidation on `scene.localPositionOf(surface)` (still `positionInRoot()`, `SceneBackdrop.kt:60`);
  Glass `sheetInset`/`sheetMaterial`; `sheet.frosted.bodyAlpha` = `frostedColors()`; §16.1 `glass_live` rows
  (`sheet_drag_glass.janky_pct` <= 1.0); alignment check under the receding presenter; revise `design/wp/WP05.md:64`
  and the handoff's "sheets never sample" (handoff "Next run" still says the spec is unwritten: stale).
- [ ] R34: nothing routed to WP05 (`qa/shimmer/` has no WP05 item); keep the shell stage from clipping content.
- [ ] R36: WP05 is part of Android "100% done": all of "What remains" 1-10 (tokens, shell stage, Reduce transparency,
  `kyant` code rule, `CatalogGlass`, `GlassBenchmark`+gates+fixtures, feature `owns`, phone, CP2) and a merge tag.
- [ ] Post-merge chores: `RobolectricSdk`->`TestSdk`, `setThemed`, `junit4.v2` for `GlassStageTest`/`GlassSurfaceTest`,
  regenerate `catalogue.json`; run 11's mutation check (never ran).

## 3. Next three steps
1. `handoff/WP05.md`: run 11 into "## Progress"; fix "Public APIs", "Lens on tap" figures, "Next run starts here".
2. Before the merges: `foundation/glass/SceneBackdrop.kt:58-61` keyed on `localPositionOf`; test in `GlassStageTest.kt`.
3. On `merged/WP03`+`merged/WP04`: merge `overhaul/multiplatform-v2` (conflicts: `core/designsystem/build.gradle.kts`,
   `gradle/libs.versions.toml`, `web/frontend/lib/catalogue.json`), then `ds/component/navigation/LiquidTabs.kt`.

## 4. Waits for
- WP03 merge (`285275d`, 47 dirty, WP03-skel mid-merge): tokens, design style, catalog, `TestSdk`, sheet seams.
- WP04 merge (`bdf61e8`, 1 dirty): tab bar, `followCovered`, `ShellChrome`, benchmark module, `perf.sh`, card stack.
- WP06 + WP02-M3 (not started) for Reduce transparency; WP02-M2 `CodeRules.kt` for the `kyant` rule.
- Phone Redmi `9a7c74f4` (API 37, Live) for every gate, once LiquidTabs, the shell stage and the benchmark exist.
- CP2: per-tier recordings (tap morph, lens drag, scroll, minimize, sheet blur/bodyAlpha) and the user's pick.
