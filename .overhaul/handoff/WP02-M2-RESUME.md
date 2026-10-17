# WP02-M2 resume brief (refreshed 2026-10-17, run 17, the first on Windows; the living record is `WP02-M2.md` "Progress")

## 1. Exact state
- Worktree `D:/projects/tools/.overhaul/wt/WP02`, branch `wp/WP02`, HEAD `13eb1f1` ("Fail a feature's scroll container
  that the chrome does not pad"); the integration branch (`a402d87`) is in (`4150f8b`).
- `git status`: clean. Nothing uncommitted or staged. Run 17 changed only workspace files (`.overhaul/out/WP02`,
  this brief, the handoff).
- On this machine at `13eb1f1` (run 17): generator tests 305, failing only the six host failures of
  `logs/windows/generator-baseline.md` (`out/WP02/logs/r17-tests-windows.log`); `consistency --quick` 63 projects,
  0 new issues (`out/WP02/logs/r17-consistency-quick.log`). On Linux before: full matrix 147 projects, 0 new issues;
  `verifyCodeRules` with the `scroll-chrome` baseline on the template and four presets (`logs/r15-rules.log`).
- **The last ACCEPT OK (`503c610`, Linux) predates runs 9 to 11.** Its cross-navigation helper no longer matched the
  scaffold after the ktlint rewrap; fixed in run 17 (`out/WP02/apply_crossnav_patch.py`). `out/WP02/accept.win.sh`
  is the Windows port; its Gradle-free steps pass on scratch copies (`out/WP02/logs/r17-dry.log`, `R17 DRY OK`).
  The full re-acceptance runs after the merges (continuation step 6).
- Handoff state: `waiting` on WP04 and WP06. Merged tags: WP01, WP02-M1, WP09, WP10. Heads at run 17: WP04
  `20d134a`, WP06 `10e2916`, WP03 `b595f9d`, WIN `ad0e436`; `git merge-tree` predicts 11, 27, 8 and 3 conflicting files.
- **This package is on the Android release's critical path:** package NET (REQUIREMENTS 39, Cronet, in the Android
  release since the user's Oct 17 message) starts only after WP06 and WP02-M2 have merged.
- The skills workflow (`scripts/android-skills-v1-0-15.js`, REQUIREMENTS 38) launches only while this package waits
  and commits on `wp/WP02` in this worktree. Before editing, `git log --oneline -15 wp/WP02` and `git status`: review
  its commits as your own (handoff Open issues); if it is mid-work (a dirty tree you did not make), do not touch its
  files.

## 2. Still owed (checklist; details in the handoff "Continuation plan")
- [ ] Step 0: WIN's host rules once WIN has merged (3 conflicts; this branch's own `.write_text(` writers through
      `genkit.core.copy.write_text`; `test_host.py` green; generator tests pass in full on this machine).
- [ ] Step 1: merge the integration branch when WP04 and WP06 (and WP03) have merged; generator tests first.
- [ ] Step 2: `verifyCodeRules --continue`, `lint`, `verifyManifestRules` with every feature on; baseline leftovers
      under owner headings; delete the `scroll-chrome` entries WP04's screens empty.
- [ ] Step 2a: move `AndroidLogger` out of `BaseAppApplication.kt` into `app/log/AndroidLogger.kt` (ST-08; WP04's
      Deviation 17 leaves it to whichever of WP02-M2 and WP06 merges last, which is this package).
- [ ] Step 3: scaffold on WP04's chrome contract (`foundation.layout` imports, `ReportScrollEdge` with hoisted states,
      `AuthFrame` padding); port and run `out/WP02/r10-fit.sh`.
- [ ] Step 4: database scaffolding in WP06's reference shape; `database` on in the `modules-*` fixtures.
- [ ] Step 5: `sharedTransitions` only if WP04's merged code has a `SharedTransitionLayout` (still none at `20d134a`).
- [ ] Skills v1.0.15 commits from the other workflow, if any: review, `test_vendored_skills.py`, consistency, handoff.
- [ ] `research/COMPOSE-PERF-AUDIT.md` (not written yet): if it routes findings to WP02, run the
      jetpack-compose-performance audit over `snippets/scaffold/feature/{List,Detail}Screen.kt` and fix them.
- [ ] Step 6: `out/WP02/accept.win.sh` in the background, sizes (port `r5-sizes.sh`/`sizes.sh` first), Progress,
      Measurements, Lock time. Never push or merge to main.

## 3. Next three steps (paths under `wt/WP02/`)
1. `git -C D:/projects/tools/android-base tag -l 'merged/*'`: if WIN merged, do step 0 right after merging the
   integration branch; if WP04 and WP06 merged, steps 1 and 2.
2. Steps 2a, 3, 4 in order, generator tests after each (`python -I -B -m unittest discover -s tests -t .` in
   `generator/` through `bin/memwait`, or `python $O/gen_tests.py .`).
3. Step 6, last.

## 4. Waits on
- WP04 merge (chrome contract `foundation/layout/Chrome.kt` with `ReportScrollEdge`, activity-retained navigator,
  `scroll-chrome` baseline entries its screens empty) and WP06 merge (`database.py`, `:core:database`, the data
  reference shape, the rewritten `BaseAppApplication.kt`). WP03 only for the `@AppScreenPreviews` path contract
  (`scaffold.py:24`); not blocking. WIN is not a wait, but merges first and brings step 0.
- Phone: nothing in M2. Gradle steps take `gradle.lock` (`accept.win.sh`, `r10-fit.sh`).
- CP2: none.
