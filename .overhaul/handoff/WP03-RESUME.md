# WP03 resume brief (read 2026-10-14 18:55 IST; run 12 stopped about 18:17; `O=$OV/out/WP03`)

## 1. Exact state
- HEAD `285275d` (18:08, sheet tokens), after `9823262` (QA-29) and `b8bdea6` (skel fast-forward); 68 ahead of v2; unmerged.
- Uncommitted (34 tracked, 13 untracked): the SHEET-SPEC rev-2 sheet system (`component/sheet/**`, overlay host,
  `AppBottomSheet`, legacy `AppActionSheet`; old `AppSheet*.kt`, `core/navigation/SheetScene.kt` deleted), 6 seams in
  `foundation/layout/`, `LocalSheetSurface` call sites, catalog `sections/sheets/`, D10, D11/D12 (`text/TextScale.kt`),
  K3 (Utility bar `Floating`), strings in 5 languages, about 20 sheet tests. Feature-complete per run 12; NOT green.
- Last test `$O/logs/r12-t4.log` (18:13) RED: `:core:designsystem` compile, `syncAnchors` unresolved at
  `component/sheet/gesture/SheetFlingBehavior.kt:88`. Edited after it, never compiled: that file (18:14),
  `SheetDetentDemos.kt` (18:14), `SheetNestedScrollTest.kt` (18:16); `r12-t5` never got the lock. t1-t3 failures
  (`PostureTest:84`, `TextScale` `isSp`, `LargeTitleTopBarTest:190`) fixed in source, unconfirmed. Last green: run 8.
- Sheet code rules (`$O/code-rules/`) RED: `r12-rules1.log` 3/43 (`CodeRulesTest:195`, `:203`, `SheetRuleCasesTest:33`).
- `wp/WP03-skel` has 10 commits past `b8bdea6` (head `46ec781`, 18:53, wp/WP03 merged in), not yet in wp/WP03.

## 2. Still owed (REQUIREMENTS 33-36)
- [ ] R33 §19.1: compile and `testDebugUnitTest` green, then commit in steps (sheet + call sites, catalog page, D10, D11/D12, K3).
- [ ] R33 §19.1.10: `abiValidation()` in `core/designsystem/build.gradle.kts` and a committed `api/*.api` (absent) equal to §4.
- [ ] R33 §15.5 screenshots (font 1.5/2.0, dark, RTL; none yet), §10.4 budgets, compiler metrics (stable, skippable).
- [ ] R33 §19.1.9: fix the 3 failing rule cases; they land in `verifyCodeRules` at the WP02-M2/WP03 merge.
- [ ] R33: catalog `destination`/`steps` demos on `AppSheetNavHost` (WP04); K12 medium-detent viewport checked on phone.
- [ ] R34: re-merge `wp/WP03-skel`, re-run DS tests; at the WP04 merge M-1..M-3 (`WP03-skel.md`), K4, K14.
- [ ] R35: no WP03 deliverable (WP05/WP11 own the lens); merges must leave `foundation/glass/` as WP05 has it.
- [ ] R36: `$O/accept.sh` green, sweep `--check` clean, CP1 sheet/toast and catalog shots on the Redmi, WP04 items 1-6.

## 3. Next three steps
1. Back up the dirty tree (`git diff`, tar of untracked) into `$O/`, then `$O/scripts/gw.sh r13-t1 templates/android
   :core:designsystem:testDebugUnitTest :catalog:assembleDebug`; fix from `component/sheet/gesture/SheetFlingBehavior.kt`,
   `component/sheet/SheetAnchors.kt` (`internal fun AppSheetState.syncAnchors`), `sheet/layout/SheetFrameUpdates.kt`.
2. Once green, commit in the steps above; then `git merge wp/WP03-skel`, re-run the DS tests, re-emit and check tokens
   (`python3 -m genkit.tokens --check`) and `web/frontend/lib/catalogue.json` (skel hit a conflict there).
3. Add `kotlin { abiValidation() }` to `templates/android/core/designsystem/build.gradle.kts`, dump and commit `api/`,
   diff it against SHEET-SPEC §4; fix `$O/code-rules/SheetRules.kt` / `SheetRuleCasesTest.kt`; run `$O/accept.sh`.

## 4. Waiting on
- WP04 merge: `AppSheetNavHost` demos, `Presentation.Sheet` and the shell seams (§19.2), Remaining-work items 1-6, K4/K14.
- WP02-M2: `verifyCodeRules` to receive the sheet rules. `wp/WP03-skel`: its agent is still committing (VERIFY.md absent).
- The phone (Redmi `9a7c74f4`; adb lock busy at 18:06): CP1 sheet/toast, catalog screenshots, K12 (PTR is done, run 8).
- CP2 (after WP03 and WP04, with WP05): decides `sheet.glass.blur` / `bodyAlpha` (SHEET-SPEC §19.3); WP03 re-emits tokens.
