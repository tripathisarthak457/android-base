# WP02-M2 resume brief (2026-10-15, run 15; refreshed 2026-10-16, run 16; the living record is `WP02-M2.md` "Progress")

## 1. Exact state
- Worktree `.overhaul/wt/WP02`, branch `wp/WP02`, HEAD `13eb1f1` ("Fail a feature's scroll container that the chrome does not pad"); 44 commits past `overhaul/multiplatform-v2` (`a402d87`, merged into the branch as `4150f8b`).
- `git status`: clean. Nothing uncommitted or staged.
- Tests last known GREEN at `13eb1f1`: generator tests 305 OK, 2 skipped (`out/WP02/logs/r15-tests.log`); full consistency matrix 147 projects / 0 new issues (`logs/r14-consistency-full.log`); build-logic tests and the template's `verifyCodeRules` with the `scroll-chrome` baseline (`logs/r14-rules2.log`); M2 `accept.sh` ACCEPT OK at `503c610` (`logs/accept-m2.log`). The generated projects' `verifyCodeRules` under `scroll-chrome` passed (`out/WP02/logs/r15-rules.log` ends `R15 OK`: the template plus everything, standard-modular, lean and standard).
- Handoff state: `waiting` (on WP04 and WP06). Merged tags: WP01, WP02-M1, WP09, WP10. WP04 at `fb866b3`, WP06 at `a8129c4`, WP03 at `b595f9d`, none merged (checked run 16, Oct 16 10:40; `git merge-tree` still predicts 11, 27 and 8 conflicting files).
- **Another workflow commits on this branch:** `scripts/android-skills-v1-0-15.js` (task `we159x0cq` since the Oct 16 relaunch, run `wf_ec030645-26e`, REQUIREMENTS 38) vendors Google's Android skills v1.0.15 feature-aware in this worktree and appends "Android skills v1.0.15 (Oct 15)" to `WP02-M2.md`. Before editing, `git log --oneline -15 wp/WP02` and `git status`: if its commits are there, review them as your own (Open issues in the handoff); if it is mid-work (dirty tree you did not make), do not touch its files.

## 2. Still owed (checklist)
- [ ] Merge integration when WP04 and WP06 (and WP03) have merged, per handoff "Continuation plan" step 1 (11 files vs WP04, 27 vs WP06, 8 vs WP03); generator tests first.
- [ ] Post-merge hold: `verifyCodeRules --continue`, `lint`, `verifyManifestRules` with every feature on; baseline WP04/WP06 leftovers under owner headings; delete the `scroll-chrome` entries WP04's screens empty (plan step 2).
- [ ] Scaffold on WP04's chrome contract: imports from `foundation.layout`, `ReportScrollEdge` with hoisted list/scroll state in both scaffold screens, `AuthFrame` padding; `r10-fit.sh` after the snippet edit (plan step 3).
- [ ] Database scaffolding (`database-*` points): data module in WP06's reference shape, `database` on in `modules-*` fixtures (plan step 4).
- [ ] `sharedTransitions`: nothing unless WP04's merged code has a `SharedTransitionLayout` (Deviation 10; plan step 5).
- [ ] Skills v1.0.15 commits from the other workflow: review, `test_vendored_skills.py`, quick consistency, handoff rows (Open issues).
- [ ] Re-accept: `out/WP02/accept.sh`, sizes via `out/WP02/r5-sizes.sh`, update Progress/Measurements/Lock time (plan step 6).
- [x] REQ 33 (bottom sheet), REQ 35 (lens), REQ 37 (full glass): no WP02 work (owners WP03/WP04/WP05/WP07-B/WP08/QA2/WP12; WP05-glass/WP11).
- [ ] REQ 36 (Android first): no new work, but keep "full Android matrix green": rerun the full consistency matrix (147 projects) after each merge. Never push or merge to main.

## 3. Next three steps (paths under `wt/WP02/`)
1. Check the skills workflow's commits on `wp/WP02` (review them as your own) and whether `research/COMPOSE-PERF-AUDIT.md` exists; if it routes findings to WP02, run the jetpack-compose-performance audit over `snippets/scaffold/feature/{List,Detail}Screen.kt` and fix them (then `out/WP02/r10-fit.sh`).
2. When `merged/WP04` (and `merged/WP06`) exist: `git merge overhaul/multiplatform-v2`; resolve per the continuation plan (`scaffold.py`, `snippets/scaffold/feature/Navigation.kt` on `ActivityRetainedComponent`, `target.py` keeping `imports.sort_sources` last, prefixed strings, `presets.toml`/`removed.toml`); regenerate `web/frontend/lib/catalogue.json`; generator tests.
3. Continuation steps 2 to 6 in order; `out/WP02/accept.sh` last.

## 4. Waits on
- WP04 merge (chrome contract `foundation/layout/Chrome.kt` with `ReportScrollEdge`, activity-retained navigator, `scroll-chrome` baseline entries its screens empty) and WP06 merge (`database.py`, `:core:database`, data reference shape). Planned order: WP04, WP03, WP06, then WP02-M2 (STATUS.md). WP03 only for the `@AppScreenPreviews` path contract (`scaffold.py:24`); not blocking.
- Phone (Redmi, `adb.lock`): nothing in M2 (the `sharedTransitions` gate fell away with Deviation 10). Gradle steps take `gradle.lock` (`out/WP02/accept.sh`, consistency).
- CP2: none. CP2 is WP05's glass decision, which merges after WP02-M2; this package does not wait for it.
