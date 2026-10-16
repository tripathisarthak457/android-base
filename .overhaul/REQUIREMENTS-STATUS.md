# Requirements status (REQUIREMENTS.md 1-36)

Written 2026-10-14 (skeleton; being filled in section by section).

## Summary
(pending)

## Sources read
- Merged tags: `git -C android-base for-each-ref refs/tags/merged/*`: `merged/WP02-M1` 54d078d (10-09 07:35), `merged/WP01` 709612d (10-09 08:37), `merged/WP09` 8672003 (10-09 17:16), `merged/WP10` a402d87 (10-14 17:54). `merges.md` has one entry per tag (all "OK", clean `--no-ff`).
- Worktree logs: `git -C wt/<WP> log overhaul/multiplatform-v2..HEAD` for WP02-WP08 (WP01, WP09, WP10 are 0 ahead); `wt/WP11` against PeriFlow `main`; `wt/WP03-skel` against `wp/WP03`.
- `## Progress` of every `handoff/*.md`. `WP03-skel.md`, `WP06-early.md` and `WP08-review-r1.md` have no Progress section; their header and "Tests and checks" were read instead. `WP11.md` Progress is dated run 5 (10-12) and predates the lens commits; `WP07-A.md` Progress is dated 10-10 (run 3).
- Perf: every `perf/**/summary.md` (WP04 and WP11 only; WP02/05/06/07/08 have none), `perf/WP03/ptr/measure-before` and `measure-head` (gfxinfo).
- `qa/shimmer/`: `AUDIT.md` (10-12 13:58), `evidence/EVIDENCE.md`, `logs/`, `report/` (only `d4-period-recheck.*`). `qa/shimmer/REPORT.md` and `VERIFY.md` do not exist.
- `research/SHEET-SPEC.md` header: revision 2, 2026-10-13; work split in §19.1-19.8.
- `SESSION-2026-10-14.md` (13:13).
- Spot checks named in the Evidence column (handoff Open issues / Release blockers sections, file existence).

## Status by requirement
| # | Requirement | Status | Evidence | Owner package | What remains |
|---|---|---|---|---|---|
(pending)

## Branch state per package
Read 2026-10-14 18:52-18:55 IST; agents were still committing while this was read. "Ahead" = `git -C wt/<WP> rev-list --count overhaul/multiplatform-v2..HEAD` (WP11 against `main`, WP03-skel against `wp/WP03`). "Dirty" = lines of `git status --porcelain`.

| Package | Branch | HEAD | Ahead | Dirty | Merged tag | Last commit (time) |
|---|---|---|---|---|---|---|
| WP01 engine/spec/catalogue | wp/WP01 | d19f79c | 0 | 0 | merged/WP01 (709612d, 10-09 08:37) | merged |
| WP02-M1 core/MVI | wp/WP02 | 4d1a412 | 38 (M2 work) | 0 | merged/WP02-M1 (54d078d, 10-09 07:35) | `4d1a412` Scaffold in ktlint's layout at every module-name length (10-13 16:32) |
| WP02-M2 architecture | wp/WP02 | 4d1a412 | 38 | 0 | none | same branch as above; waits for WP04 and WP06 (SESSION-2026-10-14 §3) |
| WP02-M3 | none | - | - | - | none | handoff/WP02-M3.md Progress: "Not started" |
| WP03 design system | wp/WP03 | 285275d | 68 | 47 | none | `285275d` Give sheets their own token section and a material per design style (10-14 18:08) |
| WP03-skel shimmer | wp/WP03-skel | 46ec781 | 10 (9 fixes + merge commit) | 0 | none (an earlier state was fast-forwarded into wp/WP03 at `b8bdea6`, WP03 reflog) | `46ec781` Merge wp/WP03 into wp/WP03-skel (10-14 ~18:54; the catalogue.json conflict seen at 18:52 was resolved in it); last fix `e2cec54` Keep what a component's own bone covers off the shimmer (18:48) |
| WP04 navigation | wp/WP04 | bdf61e8 | 83 | 1 | none | `bdf61e8` Recompose only the two tabs a switch changes (10-14 18:01) |
| WP05 glass | wp/WP05 | 30c03ad | 14 | 0 | none | `30c03ad` Give each lens test one behaviour (10-14 18:17) |
| WP06 data/security | wp/WP06 | 1dcacd9 | 30 | 2 | none | `1dcacd9` Read the badge's latest bounds and direction from its drag (10-14 12:50) |
| WP07(-A) Compose Multiplatform | wp/WP07 | 99239af | 15 | 0 | none | `99239af` Opt every test compilation into the coroutine test API (10-12 14:16) |
| WP08 SwiftUI | wp/WP08 | 37558cc | 64 | 0 | none | `37558cc` Keep a cancelled shimmer loop from clearing a newer loop's sweep (10-14 17:30) |
| WP09 web API | wp/WP09 | 3d580cd | 0 | 0 | merged/WP09 (8672003, 10-09 17:16) | merged |
| WP10 website/admin | wp/WP10 | d702b16 | 0 | 0 | merged/WP10 (a402d87, 10-14 17:54) | merged (merges.md, 2026-10-14 entry) |
| WP11 PeriFlow | overhaul/smooth-glass (PeriFlow repo) | e57b40c | 15 vs main | 0 | n/a (local branch, not pushed by rule) | `e57b40c` Magnify the travelling lens's image, not its drawing (10-14 17:29) |
| WP12 docs | none | - | - | - | none | handoff/WP12.md Progress: "Not started" |
| WP13 Android verification | none | - | - | - | none | no handoff, no worktree |
| WP14, MB, QA2 | none | - | - | - | none | no handoff, no worktree (named in SESSION-2026-10-14 §4) |

Note: SESSION-2026-10-14.md (13:13) says merged tags are WP01, WP02-M1, WP09; WP10 merged later (tag `merged/WP10`, 17:54).

## Android release blockers (REQUIREMENTS 36)
(pending)
