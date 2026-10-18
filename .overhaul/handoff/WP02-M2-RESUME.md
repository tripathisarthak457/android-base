# WP02-M2 resume brief (refreshed 2026-10-19, run 18; the living record is `WP02-M2.md` "Progress")

## 1. Exact state
- Worktree `D:/projects/tools/.overhaul/wt/WP02`, branch `wp/WP02`. Run 18 commits: `7a4ff90` (merge of the
  integration branch with WIN, `5a16778`), `f470046` (Google's Android skills v1.0.15), `56690ff` (the code rules name
  the skill page to read; the stability conf's reasoning), `3a2fa0e` (merge of WIN's follow-up, integration
  `4dd3aca`). `git status` clean.
- Merged tags: WIN, WP01, WP02-M1, WP09, WP10. WP04 and WP06 have not merged.
- On this machine at `3a2fa0e`: generator tests 352, OK (5 skipped, all host skips), so the six Windows host
  failures are gone (`out/WP02/logs/r18-tests3.log`); `python -m tests.consistency --quick` runs natively now: 63
  projects, 0 new issues (`out/WP02/logs/r18-consistency-quick3.log`); `catalogue --check` OK.
- The skills workflow (REQUIREMENTS 38) never committed here; run 18 did the vendoring itself. Nothing of it is left
  for another workflow.
- The last ACCEPT OK (`503c610`, Linux) predates runs 9 to 11; `out/WP02/accept.win.sh` is the Windows port, and the
  full re-acceptance runs after the merges (continuation step 6).
- **This package is on the Android release's critical path:** NET (Cronet, REQUIREMENTS 39) starts only after WP06
  and WP02-M2 have merged.

## 2. Still owed (details in the handoff "Continuation plan")
- [x] Step 0: WIN's host rules (run 18, `7a4ff90`).
- [ ] Step 1: merge the integration branch when WP04 and WP06 (and WP03) have merged; generator tests first.
- [ ] Step 2: `verifyCodeRules --continue`, `lint`, `verifyManifestRules` with every feature on; baseline leftovers
      under owner headings; delete the `scroll-chrome` entries WP04's screens empty.
- [ ] Step 2a: move `AndroidLogger` out of `BaseAppApplication.kt` into `app/log/AndroidLogger.kt` (ST-08).
- [ ] Step 3: scaffold on WP04's chrome contract, together with the jetpack-compose-performance audit findings for the
      scaffold (handoff Open issues: `LoadingList` as a `Column`, `contentType` on the rows, `SideEffect(Unit)` for
      `Appeared`); port and run `out/WP02/r10-fit.sh`.
- [ ] Step 4: database scaffolding in WP06's reference shape; `database` on in the `modules-*` fixtures.
- [ ] Step 5: `sharedTransitions` only if WP04's merged code has a `SharedTransitionLayout`.
- [ ] Step 6: `out/WP02/accept.win.sh` in the background, sizes, Progress, Measurements, Lock time. Never push or
      merge to main.

## 3. Next three steps (paths under `wt/WP02/`)
1. `git -C D:/projects/tools/android-base tag -l 'merged/*'`: if WP04 and WP06 merged, steps 1 and 2.
2. Steps 2a, 3, 4 in order, generator tests after each (`python -I -B -m unittest discover -s tests -t .` in
   `generator/` through `bin/memwait`).
3. Step 6, last.

## 4. Waits on
- WP04 merge (chrome contract `foundation/layout/Chrome.kt` with `ReportScrollEdge`, activity-retained navigator,
  `scroll-chrome` baseline entries its screens empty) and WP06 merge (`database.py`, `:core:database`, the data
  reference shape, the rewritten `BaseAppApplication.kt`, the removal of `widget`). WP03 only for the
  `@AppScreenPreviews` path contract (`scaffold.py:24`); not blocking.
- Phone: nothing in M2. Gradle steps take `gradle.lock` (`accept.win.sh`, `r10-fit.sh`).
- CP2: none.
