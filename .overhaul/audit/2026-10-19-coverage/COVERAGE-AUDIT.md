# Coverage audit: what the handover asked for, and where it really stands (Oct 19, 21:55, revision 2)

This audit is read-only. It merges the three inventories in this folder (`inv-user.md`, `inv-plan.md`, `inv-now.md`) into
distinct items. It checks each item against git (`git -C android-base` branch heads, `merge-base --is-ancestor`,
`merged/*` tags, worktree status), the handoffs, the logs and the two planning scripts (`scripts/to-release.js`,
`scripts/consolidate-staging.js`). Nothing was changed.

Revision 2 corrects the first version in six ways. Most of the glass work, the zoom transition, the first-swipe shader
warm-up and the site's CI and `included.tsx` claims turned out to have owners. WP04's Oct 19 acceptance was recorded,
in the orchestrator's session note. PeriFlow is parked by the user's own choices. Requirement sub-points that no
package implements yet go through staging's requirements matrix, so this audit no longer counts them as forgotten.

## Part 1, in plain English

### The short answer

Liquid glass was not the only thing that went quiet, but less was dropped than it first looked.

- **Three side workflows were never restarted.** After the Oct 16 power-off, the full liquid glass, the shimmer phone
  check and the Android-skills performance audit were not relaunched on Windows.
- **All three have a path again since your Oct 19 staging decision.** Staging commits and merges the glass branch,
  WP05 owns most of the remaining glass, and staging's requirements matrix will flag the rest as gaps and route them.
- **A short list has no owner at all.** These are notes outside `REQUIREMENTS.md`, which the matrix will not see: two
  security notes, a template reformat, size budgets, a signed-in `/admin` check, a flaky stability check, and a few
  minor items. The orchestrator has to route them by hand.
- **No Android UI work has been merged since Oct 14.** Staging is the plan that merges it. It starts once the back-card
  bug fix finishes, and that fix is running now.

### What the three workflows that never restarted left behind

On Oct 16 all five workflows died in a power-off and were not relaunched (`SESSION-2026-10-16.md:50`). The handover
planned to restart them on Windows in three waves (`handover/HANDOVER.md:954-955`). Only wave 1 ran: the release
workflow and the smoothness work, plus HUD-first, which came later. The evidence:

- The workflow table in `STATUS.md` §1 (lines 38-49) has no shimmer, skills or glass run.
- `SESSION-2026-10-18.md:15-19` lists only the release, deep-smoothness and HUD-first workflows.
- `out/WP05-glass/logs/` has nothing after Oct 16.
- `qa/shimmer/` has no `VERIFY.md`.

You asked on Oct 16 that the agents that had been on Fable should finish their goals on Opus
(`handover/USER-MESSAGES.md:1138`). For these three, that did not happen between Oct 17 and Oct 19. Since your
staging decision (REQUIREMENTS 44: no package work before staging), their goals go through staging.

1. **The full liquid glass (REQUIREMENTS 37).**
   - **The bottom bar's glass** is 43 uncommitted files in `wt/WP05-glass`. They have been untouched since Oct 15 15:53
     (restored on Oct 17; a copy is pushed as `backup/WP05-glass-wip-2026-10-16`). The last compile failed, and the
     branch still carries the old rubber band that scrolled the wrong way.
     - **Its owner is now staging.** Steps 2-3 of `consolidate-staging.js` (lines 68-69) commit the worktree as WIP and
       merge the glass branch, with WP03's scroll fix winning.
     - **The risk is duplicated work, not a deadlock.** If the merge agent leaves the branch out because it does not
       build (`consolidate-staging.js:78` allows that), WP05 falls back to its own plan. Its next run says "build
       LiquidTabs here" when the glass branch is not committed (`handoff/WP05.md:160`), and it would rebuild the bar
       beside 43 files that already do it.
   - **WP05 owns most of the rest.** Its scope is "The whole package" (`scripts/to-release.js:81`). Its design and
     "What remains" cover the following, all NOT STARTED and waiting for WP03 and WP04, which means for staging:
     - glass top-bar buttons (`design/wp/WP05.md:99`)
     - the Glass design style (`design/wp/WP05.md:103-104`; `handoff/WP05.md:735-739`)
     - the Glass sheet (`handoff/WP05.md:165-168`; `research/SHEET-SPEC.md` §19.3)
     - the catalog's Glass section, `CatalogGlass` (`design/wp/WP05.md:106-107`; `handoff/WP05.md:749-750`)
     - the Live-tier benchmarks and the `glass_live` gates (`design/wp/WP05.md:108-121`; `handoff/WP05.md:751-760`)
     - the CP2 recordings
   - **Nothing implements the rest of REQUIREMENTS 37 yet.**
     - The glass toggle, slider, segmented control, buttons, alert and toast at the playground's level.
     - Side-by-side strips against the Kyant playground. These need you to install the playground app (decision 5
       below).
     - REQUIREMENTS 37's full effect list as the bar's acceptance bar: magnification, dispersion at the rim, rim light,
       the press swell, drag-to-select with velocity. WP05's plan allows dispersion and rim light in the lens
       (`handoff/WP05.md:155-158`), and staging's phone verifier looks for blur, rim light and the lens
       (`consolidate-staging.js:103`). No package's acceptance checks the whole list.

     These are sub-points of REQUIREMENTS 37, so staging's matrix will mark them "partly" or "not met" and route them
     to `staging-wp/WP05` (`consolidate-staging.js:149, 157, 164`; `REQUIREMENTS.md:266`).
2. **The shimmer phone check (REQUIREMENTS 34).** The shimmer fix is done and inside `wp/WP03`, but the step that proves
   it on the phone (`qa/shimmer/VERIFY.md`) never ran.
   - The matrix treats a requirement with no proof as "partly" at best, so it will route this item.
   - QA2's light, dark and font 1.5 screenshots cover the clipping half (`to-release.js:94`). Nothing covers the
     shimmer sweep itself.
3. **The template-wide Compose performance audit (REQUIREMENTS 38, your Oct 16 ask).** Nobody wrote
   `research/COMPOSE-PERF-AUDIT.md`.
   - **Audited:** WP04, WP04-perf, WP05's foundation, WP06, HUD and WP02's scaffold each audited their own code.
   - **No full audit recorded:** the design system (WP03, the biggest UI surface), the uncommitted glass bar, and the
     multiplatform composables of WP07-A.
   - REQUIREMENTS 38.5 is a sub-point, so the matrix will route it.

Why it matters for "light and smooth": glass is the most GPU-hungry feature in the app, and the jank you saw in
PeriFlow came from glass. An unaudited design system and an unverified shimmer are exactly where per-frame
recomposition and wasted GPU work hide.

### Gaps the requirements matrix will route (recommend naming them to its auditors)

Staging's Requirements phase judges every sub-point of `REQUIREMENTS.md` on the combined code
(`consolidate-staging.js:132-158`). It routes each "partly" or "not met" row to a `staging-wp/<package>` branch
(`:162-164`), and every later package run starts from those gaps (`REQUIREMENTS.md:266`). These items have that
routing path but no package that implements them yet. The auditors judge from code and proof, so naming these to them
makes the routing certain:

- **REQUIREMENTS 34:** the shimmer's phone proof.
- **REQUIREMENTS 37:** the components, the playground strips and the full effect set (above).
- **REQUIREMENTS 38.5:** the template-wide performance audit.
- **REQUIREMENTS 42, the back gesture's hitch row:** still red after the back-card fix (6.91 against 5.0 ms/s). The
  next lever is the "scrim fold" (`handoff/WP04-perf.md:879-881`), and no script runs WP04-perf's leftovers now. The
  matrix review already names this row (`consolidate-staging.js:151`).
- **REQUIREMENTS 42, "keep cutting memory":** the app sits at about 84 MB after the test journey (it is a ratchet now).
- **REQUIREMENTS 23.8, the adaptive guard test (`AdaptiveAuditTest`):** WP02-M2 recorded that it "has no owner"
  (`handoff/WP02-M2.md:803`).
- **REQUIREMENTS 36, accessibility on the phone:** the design system has semantics unit tests, but no package produces
  TalkBack proof. The release gate's prompt names accessibility (`to-release.js:364`), so a missing proof would come out
  as NO-GO there. Only the package that produces the proof is missing.
- **REQUIREMENTS 41, first fling after install on the merged tree:** staging's phone steps re-measure the scroll rows
  but not the first fling (`handoff/WP04-perf.md:883-886`).
- **REQUIREMENTS 35 and 37, the lens and the glass:** QA2's scope already builds an everything-with-glass project and
  records the glass bar (`to-release.js:94`). It does not list 34, 35 or 37 among the requirements it checks.
- **REQUIREMENTS 40, the HUD demo:** staging's phone step captures the overlay pill and its Performance tab
  (`consolidate-staging.js:93`). It records no clip and exports no report, both of which REQUIREMENTS 40 asked to see
  (`REQUIREMENTS.md:196`).

### What has no owner at all (outside REQUIREMENTS.md, so the orchestrator must route it by hand)

- **A security hole in app lock.** WP04's reviewer found it and sent it to WP06, but it never arrived. Opening a link,
  or coming back from Recents, within 30 seconds of the lock screen reopens the app **unlocked**
  (`handoff/WP04.md:1393-1395`).
  - Neither this nor the unchecked link host below is in WP06's handoff or in `followups.md`.
  - Staging's reviewers rate problems that predate the merge as nonblocking and move them to the package's new branch
    (`consolidate-staging.js:114`). Neither note is in WP06's handoff, so nothing carries them there unless someone
    names them.
- **Links are accepted without checking that the https host is yours** (`handoff/WP04.md:1396-1397`). WP13's
  intent-security audit (`to-release.js:104`) may catch it, but the note itself never reached WP06.
- **A flaky Compose stability check.** It passes or fails depending on which compile ran last. WP03 handed it to WP02
  (`handoff/WP03.md:1079`), and it never reached WP02's handoff. It is the proof for "compiler metrics clean"
  (REQUIREMENTS 22), and a matrix auditor who runs it can get a green result by luck.
- **A one-time reformat of the template.** Until it lands, the "everything" preset fails its own `check`
  (`handoff/WP02-M2.md:343, 793`).
  - The documented timing is "after the last UI wave" (`research/STANDARDS-PLAN.md:411`; `handoff/WP02-M2.md:310,
    561`), and no script contains the step.
  - Staging's fix loop records a known package failure like this in `staging/CARRY-OVER.md` (`consolidate-staging.js:125`),
    so it will show as a known red there, not a surprise.
- **APK size budgets.** The release script writes `tools/size_budgets.toml` when WP02-M2 and WP06 merge
  (`to-release.js:245`). Staging merges them its own way, so that step never fires, including after the release script
  is re-based on staging.
- **A signed-in click-through of `/admin`** (`handoff/WP10.md:840`). WP13's design does not name it, and it needs you to
  ask for an admin token to be typed. This is the only one of WP10's website release blockers with no owner. The other
  open items are yours: the LICENSE file, the Vercel settings and the `npm audit` upgrades.
- **Minor notes that reached nobody:**
  - `recompose.push.max` budget 2 once WP02-M3 lands (H-7).
  - The DevTools panel's empty state padding (H-11).
  - `AppScaffold` padding a pane's inner edge (H-12).
  - `perf.py` defaulting `perf.hud=off` (H-14).
  - Accepting WP01's deviations 1 and 11 (H-17).
- **At risk at the HUD merge:** re-syncing the four 48 MB app-memory copies when HUD merges after WP04
  (`handoff/HUD.md:1029-1032`). It is in HUD's Open issues, which staging's Prepare step reads.
- **Housekeeping text that has gone stale:**
  - `STATUS.md`'s header and its package table.
  - `notes/INDEX.md`.
  - Three lines of `D:/projects/tools/CLAUDE.md`:
    - `:38` still says 6 generator tests fail.
    - `:42` says gh is not on this machine, but `C:/Program Files/GitHub CLI/gh.exe` exists.
    - `:29` says `KONAN_DATA_DIR` is set "once the user sets it", but the global rules list it as set.

### Owned, contrary to how it may look

- **Zoom (hero) transitions and the first-swipe shader warm-up.** WP04 decided to build the zoom (a new
  `NavTransitionStyle.Zoom`) "in the WP04 run after WP03 merges" (`handoff/WP04.md:726-737`). Its motion-perf notes list
  the first-use warm-up (S6, S7) and the zoom (M5) as missing (`:1597-1606`). WP04's scope covers every WP04 item in
  the research plans (`to-release.js:69`). Today `NavTransitionStyle` on `wp/WP04` has only `Push` and `None`, so
  neither is built. Both carry over to `staging-wp/WP04` through WP04's handoff, and WP02-M3 uses the zoom in the sample.
- **The site's CI claim and the "end-state" claims in `included.tsx`.** WP12 makes the hero, `included`, the FAQ and the
  privacy page "match the code" (`design/wp/WP12.md:38, 70`). WP13 rewrites `ci.yml` so the matrix gates CI
  (`design/wp/WP13.md:65`). WP10's handoff hands these blockers to WP12 and WP13 (`handoff/WP10.md:807`).

### Waiting (nothing is stalled without a planned unblocker)

- **Every Android package waits on staging.** WP03, WP06, WP08, WP02-M2, WP05 and WP07-A have waited 3 to 6 days on
  WP04's merge, directly or through WP03. Staging is the planned unblocker, and it waits only for the bug fix that is
  running now. The bug fix committed a gate row (`1ec5d90`, 21:38) and a test (`6c6ee60`, 21:48) on `wp/WP04`.
- **WP04's last full acceptance (Oct 19, 08:46) failed two non-regression rows.**
  - The rows: Rapid Back's slowest frame (8.63 against 7.9 ms) and scrolling with the bar minimizing (0.59 against
    0.5 %) (`out/WP04/logs/r28-accept2.log:8129, 8140`).
  - The orchestrator recorded this in `SESSION-2026-10-18.md:126-130`. WP04's handoff still tells its next run to
    read the log (`handoff/WP04.md:50`), and no WP04 run has acted on the result: the release workflow stopped at 15:44.
  - My own reading of the same log's release table: the back gesture's hitches at 16.39 and Rapid Back's at 12.32,
    against 5 ms/s (`:8054, 8064`). Both were measured before the back-card patches.
  - Staging's phone run re-measures these rows on the combined build.
- **The glass decision (CP2)** waits for WP05's recordings (`perf/WP05/` does not exist yet), which wait for staging.
- **PeriFlow (WP11) is parked by your choices, not stalled.**
  - Oct 17: it stays out of the relaunch (a default you accepted).
  - Oct 19: "PeriFlow never" for staging (`REQUIREMENTS.md:236`; `decisions/LOG.md:25`).
  - It is not part of either deploy (`REQUIREMENTS.md:164`).

### Decisions only you can make that were never asked in chat

These are listed in the handover (`handover/REQUIREMENTS-STATUS-FINAL.md` §9), in handoffs or in `PROGRESS.md`.
`decisions/LOG.md` has no answer for any of them.

1. **The Utility style's bar changed from Docked to Floating.** WP03 already committed it (`e80fe43`, Oct 17) without
   your confirmation.
2. **A `LICENSE` file.** The site says MIT, and the repository has none. This is a release blocker.
3. **Vercel settings only you can set:** `ENABLE_TARGETS=android`, `ADMIN_TOKEN`, `DATABASE_URL`, `TRUSTED_PROXY`. This
   is a release blocker.
4. **Whether an agent may pick CP2 (the glass look),** as one did for CP1. The scripts assume yes.
5. **Whether you will install the Kyant playground app on the phone** for side-by-side glass recordings.
6. **Generated signing keys now refuse non-ASCII passwords** (`ee311a8`, already in the integration branch). Declining
   means reverting it.
7. **Approvals that wait:**
   - the Gradle 9.8.1 download
   - `protobuf` for the R8 analyzer
   - the `npm audit` package upgrades

   Temporary display-size changes on the Redmi are also listed, but WP04 already used them and restored the display.
8. **Fold and posture.** Are the results from the old Linux emulator (run 12) enough for the release? This machine has no
   foldable and no emulator image.
9. **The shimmer's janky sweep:** 0.69 % against the 0.5 % target. Accept it or keep working.
10. **The District research's raw lookups** that name third-party people went out in a public push (`e628493b`). A
    history rewrite needs your OK.
11. **Smaller ones:**
    - whether the raw-data draft release is still wanted
    - the signed-in `/admin` check (you would have to ask for a token to be typed)
    - the failed Vercel preview, which needs your Vercel login to read
    - whether list-row glyphs should grow at large text sizes (`handoff/WP03.md:1086`)

### What should own each gap, and where in the order

| Gap | Owner | When |
|---|---|---|
| Glass bar (43 files) | staging steps 2-3, then WP05 | Staging commits it as WIP and merges it (`consolidate-staging.js:68-69`). Tell the merge agent to repair the compile rather than leave the branch out. If it does stay out, WP05's next run should merge that WIP branch instead of following its "build LiquidTabs here" fallback. |
| WP05's own glass work: top-bar buttons, Glass style, Glass sheet, `CatalogGlass`, `glass_live` gates, CP2 recordings | `staging-wp/WP05` | After your go. Already in WP05's scope and handoff. |
| REQUIREMENTS 37 parts nothing implements: toggle, slider, segmented control, buttons, alert, toast; playground strips; the full effect set as the bar's acceptance bar | the matrix, routed to `staging-wp/WP05` | Name them to the matrix auditors. When the release script is re-based, WP05's scope should name REQUIREMENTS 37 and `handoff/WP05-glass.md`. |
| Shimmer proof (34), performance audit (38.5), gesture hitch and memory cuts (42), `AdaptiveAuditTest` (23.8), TalkBack proof (36), first fling on the merged tree (41), QA2 checking 34, 35 and 37 | the matrix, routed to `staging-wp/WP03`, `WP04`, `WP05` and QA2's scope | Name them to the matrix auditors. For 38.5, a read-only Opus audit of staging's combined code can feed `REQUIREMENTS-GAPS.md`. Fold WP04-perf's leftovers into WP04's line. |
| App-lock bypass and the unchecked link host | WP06 (fix), WP13 (intent-security audit) | Staging's reviewers will rate them nonblocking because they predate the merge, so put them by hand at the top of `staging-wp/WP06`'s work, and name them to staging's android-ui reviewer. |
| Template reformat | orchestrator, one mechanical commit | Keep the documented timing, after the last UI wave. Add the step to the re-based release script. Staging records the `everything` check failure in `CARRY-OVER.md`. If it should land on staging instead, log that change in `decisions/LOG.md`. |
| `size_budgets.toml` | the re-based release script (WP13) | When it is re-based on staging. |
| Compose stability flake | `staging-wp/WP02` (build-logic) | After your go. |
| Signed-in `/admin` check | WP13, with your token request | Before the release gate. |
| Minor notes (H-7, H-11, H-12, H-14, H-17) and the HUD memory re-sync | the receiving package's `staging-wp` branch; staging's merge for the re-sync | Route by hand when the branches are cut. |
| Zoom/hero, first-swipe shader warm-up | WP04 (planned for its run after WP03 merges) | Nothing to add: WP04's handoff carries them into `staging-wp/WP04`. |
| Site CI claim, `included.tsx` claims | WP12 (copy), WP13 (CI rewrite and verification) | Nothing to add: both are in their designs. |
| The ten decisions above | you | Ask them together, once, in chat, as the handover asks (`handover/HANDOVER.md:361-362`). |

One limit on a first-version recommendation: staging's area reviewers should treat a jetpack-compose-performance
violation as blocking only in code that staging's merges and repairs change. Under your staging choices, older package
findings move to the `staging-wp` branches (`REQUIREMENTS.md:236-239`; `consolidate-staging.js:114`).

### What is fine

- **Merged and done:**
  - the generator engine v2 (WP01)
  - the pure-Kotlin core (WP02-M1)
  - the API and anonymous telemetry (WP09)
  - the website with an Android-only picker, and the admin portal (WP10)
  - the Windows fixes (WIN)
- **Finished on branches, entering staging:**
  - WP04: the navigation shell, predictive back and the back-during-push fix.
  - WP03 (`ACCEPT OK` Oct 19 03:19): the design system, press feedback, pull to refresh, the bottom sheet, the
    scroll-direction fix and iOS scrolling.
  - WP06 (`ACCEPT OK` Oct 18): data, network, security and the devtools drag fix.
  - HUD: the performance overlay, with prod excluded.
  - WP02: the generator's M2, with the Android skills 1.0.15 vendored.
  - WP08 (`ACCEPT OK` Oct 19): the SwiftUI template.
  - WP05: the glass foundation and the travelling lens.
  - WP07: Compose Multiplatform phase A, without the shell port.
- **Planned and owned, not started on purpose:**
  - In `scripts/to-release.js`: WP02-M3, WP12 (docs), WP13 (verification and the final report), WP14, MB, QA2, NET
    (Cronet and Brotli), CAT (catalog), WP07-B and WP07-C.
  - In `scripts/consolidate-staging.js`: staging and the requirements matrix.
- **Running: your back-card bug fix,** alone and first. Its root cause is found (`bugs/BUG-2026-10-19-back-see-through.md`
  §Root cause). The app on your phone was an older HUD build, branched from `wp/WP06`, that never had WP04's shell. WP04
  fixed the double exposure on Oct 9. The bug run is closing its test blind spots on `wp/WP04`.
- **District research is finished.** Its proposals wait for the phone study (`district-phone-study.js`).
- **Your decisions so far are applied:**
  - no merge-all
  - native Windows
  - the gates on what users feel, and the back-card texture (REQUIREMENTS 42)
  - staging first (44)
  - every requirement judged the smoothest way (46)
  - PeriFlow parked

## Part 2, reference

### How each status was decided

| Status | Meaning |
|---|---|
| MERGED | in `overhaul/multiplatform-v2` (`merge-base --is-ancestor` or a `merged/*` tag) |
| ON BRANCH | committed on a package branch, not merged; enters staging |
| RUNNING / IN PROGRESS | a workflow runs it now, or there were commits or work in the last two days |
| WAITING | blocked on a named merge, decision or tool; the evidence says whether that is still true |
| NOT STARTED | not begun, but a package in `to-release.js`, a step in `consolidate-staging.js` or a package's own handoff plan owns it |
| MATRIX-ROUTED | a `REQUIREMENTS.md` sub-point that no package implements yet; staging's Requirements phase (`consolidate-staging.js:132-158`) judges it, with no proof meaning "partly" at best, and routes it to a `staging-wp` branch (`:162-164`; `REQUIREMENTS.md:266`) |
| FORGOTTEN | outside `REQUIREMENTS.md` (so the matrix will not see it) and with no owner in any running or planned script, or a note that never reached its owner; the orchestrator must route it by hand |
| STALLED | an owner exists on paper, nothing moved for more than two days, and nothing is planned to unblock it (none found: everything that waited that long waits on staging) |
| SUPERSEDED | replaced by a later user decision (named) |
| DONE | finished work that is not code: a decision taken, a document written, a process in force |
| DECIDED | parked or settled by a user decision (named) |
| DECISION OPEN / ANSWERED / FUTURE | a decision only the user can take: never asked, answered (when), or due later |

**Git facts behind the table, as of Oct 19 21:51.** The bug run commits to `wp/WP04` and the preview tree while this is
written, so those two heads may have moved since.

- **Integration:** `3a0d38e` (Oct 19 14:04). Tags: `merged/WP01`, `WP02-M1`, `WP09`, `WP10` and `WIN` only.
- **Package heads,** with commits ahead of integration and the last own commit:

  | Branch | Head | Ahead | Last own commit / note |
  |---|---|---|---|
  | WP02 | `3a2fa0e` | 48 | Oct 19 03:16 |
  | WP03 | `1dc5714` | 110 | Oct 19 02:32 |
  | WP03-skel | `46ec781` | 78 | contained in WP03 |
  | WP04 | `6c6ee60` | 126 | Oct 19 21:48: the bug run's `1ec5d90` (21:38, `perf.py`, `gates.toml`, `test_perf.py`) and `6c6ee60` (`BackCardDuringPushDrawingTest`), after `4a15dda` (07:45) |
  | WP04-perf | `e404e39` | 127 | contains `4a15dda` but not WP04's two newest commits |
  | WP05 | `3c07982` | 19 | Oct 17 15:34 |
  | WP05-glass | `282dd3c` | 170 | Oct 15 15:53; **43 uncommitted files**; contains neither today's WP03 nor WP04, nor `daafdbd`/`91cb638` |
  | WP06 | `08c0d0a` | 42 | Oct 19 02:06 |
  | WP07 | `1852993` | 19 | Oct 18 05:16 |
  | WP08 | `3f92908` | 68 | Oct 19 01:46 |
  | HUD | `936fa2e` | 95 | Oct 19 15:20; contains WP06 |
  | HUD-statekeys | `1945b94` | 68 | not in HUD |
  | WIN | `070a91b` | 2 | |
  | preview | `722ced3` | 339 | WP04 at `6c6ee60` + T + C1 (`eb4f263`), WP06, WP03, HUD |

- **Worktrees:** `wt/WP05-glass` is the only dirty one (43 files). `wt/WP04` is clean since `6c6ee60`.
- **Branches that do not exist:** WP02-M3, WP07-B/C, WP11, WP12, WP13, WP14, NET, QA2, MB, CAT and staging. The
  `staging/` folder is empty, and `report/` and `decisions/CP2.md` do not exist.
- **Locks, all held by the bug run:** adb since 21:45, gradle since 21:49 (`:core:navigation` tests on the preview
  tree), apksign since 19:47.

### The full table

Id prefixes:

| Prefix | Meaning |
|---|---|
| R | requirement |
| D | definition of done |
| G | gate |
| P | package |
| H | handoff or orchestrator note |
| F | follow-up |
| B | bug |
| U | user decision |
| A | user ask outside the requirements |
| S | standing rule or superseded instruction |
| X | discrepancy between documents |

Source abbreviations:

| Abbreviation | File |
|---|---|
| REQ | `.overhaul/REQUIREMENTS.md` |
| HO | `handover/HANDOVER.md` |
| RSF | `handover/REQUIREMENTS-STATUS-FINAL.md` |
| MP | `design/MASTER-PLAN.md` |
| UM | `handover/USER-MESSAGES.md` |

#### Requirements

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| R-0 | Smooth, light apps; every choice judged at 60/90/120 fps on the Redmi | REQ:11-14; HO:576-577; UM 54, 59 | all | IN PROGRESS | Perf work on WP04, WP04-perf, WP03 branches; nothing merged; release rows still red (gesture hitch 6.91 vs 5.0, scroll_minimize 0.59 vs 0.5); app PSS about 84 MB after the journey (`handoff/WP04-perf.md:887-891`) | Oct 19 |
| R-1 | Navigation glitches: tab switch, push/pop, rapid Back (reproduce, root cause, regression test) | REQ:17-20; HO:591 | WP04 | ON BRANCH | `wp/WP04`: BackGate, `RapidBackTest`; r28 Rapid Back lands right, janky 0.92 % pass; slowest frame 8.63 vs 7.9 (non-regression) and hitch 12.32 vs 5 ms/s fail (`r28-accept2.log:8063-8064`) | Oct 19 21:48 |
| R-2 | No ripple; iOS-like spring press | REQ:21; HO:592 | WP03 | ON BRANCH | `PressFeedback` in 23 files on `wp/WP03` | Oct 19 |
| R-3 | Optional Kyant glass, optimised for FPS | REQ:22; HO:593 | WP05 | ON BRANCH (foundation) / NOT STARTED (bar: staging steps 2-3, then WP05) | Foundation and lens on `wp/WP05`; the bar exists only uncommitted in `wt/WP05-glass`; `consolidate-staging.js:68-69` commits and merges it | Oct 17 15:34 (WP05) |
| R-3.3 | Fix PeriFlow's glass too | REQ:22 | WP11 | DECIDED (parked) | Out of the relaunch since Oct 17 (memory `android-base-windows-decisions` item 5); "PeriFlow never" for staging (REQ:236; `decisions/LOG.md:25`); in neither deploy (REQ:164); `PERIFLOW = false` (`to-release.js:32`) | Oct 19 15:52 |
| R-4 | Apple-level components; iOS animations; design more; remove heavy ones | REQ:23; HO:594 | WP03 (+WP04) | ON BRANCH | WP03 components, sheet system committed; acceptance OK 03:19 Oct 19 (`out/WP03/logs/accept.log`) | Oct 19 02:32 |
| R-5 | Better floating bottom bars | REQ:24; HO:595 | WP04, WP05 | ON BRANCH (solid) / NOT STARTED (glass: staging steps 2-3, then WP05) | `AppTabBar` on `wp/WP04`; `LiquidTabs.kt` exists only untracked in `wt/WP05-glass` | Oct 19 / Oct 15 |
| R-6 | Multi-module architecture for 1M-user apps, not over-engineered | REQ:25; HO:596 | WP02 | MERGED (M1) / ON BRANCH (M2) / NOT STARTED (M3) | `merged/WP02-M1`; `wp/WP02` 48 ahead, its last `ACCEPT OK` predates runs 9-11 (`handoff/WP02-M2-RESUME.md:14`); WP02-M3 in `to-release.js:87` | Oct 19 03:16 |
| R-7 | No AI slop; comments only for why | REQ:26; HO:597 | reviewers; WP13 sweep | IN PROGRESS / NOT STARTED (sweep) | Enforced in each review; repository-wide sweep is WP13's (`to-release.js:104`) | ongoing |
| R-8 | Devtools API-log drag bug and similar bugs | REQ:27; HO:598 | WP06 | ON BRANCH | Passed on the Redmi (followups.md:21, closed Oct 14); `wp/WP06` | Oct 19 02:06 |
| R-9 | Edge cases and every build option | REQ:28; HO:599 | WP02, WP13 | ON BRANCH (consistency) / NOT STARTED (WP13 matrix) | Consistency 63 projects OK on `wp/WP02` (handoff); 14-row and wide matrix are WP13's | Oct 19 |
| R-10 | Practically useful generator features | REQ:29; HO:600 | WP01, WP14, NET, HUD | MERGED (engine) / NOT STARTED (WP14) | `merged/WP01`; WP14 in `to-release.js:89` | Oct 9 |
| R-11 | Compose Multiplatform generator (Android + iOS) | REQ:32; HO:601 | WP07 | ON BRANCH (phase A, partial) / NOT STARTED (B, C) | `wp/WP07` 19 ahead; shell port waits for WP04 (`handoff/WP07-A.md:8-17`); B and C are phase B (REQ:164) | Oct 18 05:16 |
| R-12 | SwiftUI generator, hidden "coming soon" | REQ:33; HO:602 | WP08, WP10 | ON BRANCH (template) / MERGED (hidden on site) | `wp/WP08` `ACCEPT OK` (`out/WP08/logs/accept-2026-10-19-merge-win.log`); first compile needs a Mac | Oct 19 01:46 |
| R-13 | Portal, script, repo docs; platform picker | REQ:36; HO:603 | WP10, WP12 | MERGED (picker) / NOT STARTED (docs) | `merged/WP10` (Android only, others "coming soon"); WP12 in `to-release.js:99` | Oct 14 |
| R-14 | Anonymous admin telemetry (geo, browser, UTM, funnel, failures, per platform; no sign-in) | REQ:37; HO:604 | WP09, WP10 | MERGED | `merged/WP09`, `merged/WP10` | Oct 14 |
| R-15 | Better AGENTS.md, README, CLAUDE.md, docs in repo and generated projects | REQ:38; HO:605 | WP12 (+WP02 skills) | ON BRANCH (skills lines) / NOT STARTED (docs) | `f470046` vendors skills and AGENTS lines on `wp/WP02`; `docs-draft/` has 4 drafts | Oct 19 02:48 |
| R-16 | Document the project perfectly | REQ:39; HO:606 | WP12 | NOT STARTED | WP12 in `to-release.js:99`; no branch | — |
| R-17 | No opaque white block around the bar; content scrolls under it | REQ:50; HO:607 | WP04 | ON BRANCH | `wp/WP04` shell | Oct 19 |
| R-18 | Continuous defect hunting; the four named defects | REQ:51-55; HO:608 | all; QA2 | IN PROGRESS / NOT STARTED (QA2) | QA round 1 routed 47 defects; per-module icon still glyph-by-name (`handoff/WP02-M2.md`, QA-10); QA2 in `to-release.js:93` | ongoing |
| R-19 | iOS-level smoothness, ~0 % janky, no dropped first frames | REQ:56; HO:609 | WP04, WP04-perf | IN PROGRESS | First frames: device limit (`handoff/WP04-perf.md:892`); rows reshaped by REQ 42; gesture hitch and scroll_minimize still red | Oct 19 14:45 |
| R-20 | Final report: before/after on the phone, 3 presets, PeriFlow | REQ:57; HO:610 | WP13 | NOT STARTED | `report/` does not exist; PeriFlow numbers may say pending (`to-release.js:104`) | — |
| R-21 | Full iOS motion set, interruptible, draw-phase, within gates | REQ:58-70; HO:611 | WP03, WP04, WP05, QA2 | ON BRANCH (most) | Push/pop, back card, sheet, PTR, press, rubber band, context menu, swipe actions on `wp/WP03`/`wp/WP04`; glass morph uncommitted; QA2 verification not started | Oct 19 |
| R-21.2 | Zoom / hero transitions | REQ:60; MP M5 | WP04, WP02-M3 (sample) | NOT STARTED (WP04, after WP03's merge) | Decided as `NavTransitionStyle.Zoom` "in the WP04 run after WP03 merges" (`handoff/WP04.md:726-737`; `:1603-1606`); MOTION-PLAN assigns M5 to WP04 (`research/MOTION-PLAN.md:139, 518`); WP04's scope covers every WP04 research-plan item (`to-release.js:69`); `NavTransitionStyle` on `wp/WP04` has only `Push` and `None`; carries to `staging-wp/WP04` via WP04's handoff | Oct 16 |
| R-22 | Optimise recomposition; compiler metrics clean; recomposition tests | REQ:71; HO:612 | WP03, WP04, WP02 | ON BRANCH (tests) / FORGOTTEN (metrics check's flake, H-8) | `ShellRecompositionTest`, `RecompositionJourneysTest`; `checkComposeStability` nondeterministic (`handoff/WP03.md:1079`) | Oct 19 |
| R-23 | Adaptive UI everywhere; template forces it | REQ:72-81; HO:613 | WP03, WP04, WP02 | ON BRANCH (partly) / MATRIX-ROUTED (`AdaptiveAuditTest`, REQ 23.8; H-9) / DECISION OPEN (foldables, U-21) | List-detail on `wp/WP04`; `@AppScreenPreviews` on `wp/WP03`; fold and posture rest on run 12's Linux emulator results, no hinge emulator here (`handoff/WP04.md:1083-1088`); large sizes passed on the phone with display overrides (same lines) | Oct 19 |
| R-24 | Gmail-style predictive back | REQ:82; HO:614 | WP04 | ON BRANCH | `BackCard` on `wp/WP04`; T + C1 only in the preview tree (`eb4f263`); recordings `research/predictive-back/gmail/` are not on this machine (Drive only, real e-mails) | Oct 19 |
| R-25 | Every smoothness finding documented for users and agents | REQ:83-92; HO:615 | WP12 | NOT STARTED | Each handoff keeps "Rules and gotchas the docs must state"; WP12 waits in `to-release.js:99` | — |
| R-26 | Google Android skills installed, used, vendored | REQ:95-97; HO:616 | WP02-M2 | DONE (installed) / ON BRANCH (vendored) | Plugin 1.0.15 on the machine; `f470046` on `wp/WP02` | Oct 19 02:48 |
| R-27 | Google/AOSP-quality code, deeper structure | REQ:98-103; HO:617 | reviewers | IN PROGRESS | Per-package reviews; staging's combined review next (REQ:236-237) | ongoing |
| R-28 | Polished tab-switch animation, within gates, recorded | REQ:104-107; HO:618 | WP04 | ON BRANCH | `wp/WP04`; tab janky now a ratchet (REQ 42) | Oct 19 |
| R-29 | Pull to refresh redesign, black shadow fixed, recorded | REQ:108-111; HO:619 | WP03 | ON BRANCH | `AppPullToRefresh` on `wp/WP03`; 0.54/0.67 % janky (STATUS.md:204); `qa/ptr/` | Oct 18 |
| R-30 | DEV badge over the large title; tab pill crowding | REQ:112-114; HO:620 | WP06, WP04 | ON BRANCH (pill) / WAITING (badge K13, on staging) | Pill `fe182db`; K13 in `handoff/WP06.md:971-983` | Oct 19 |
| R-31 | No Material components; material3.adaptive allowed; optional materialbridge | REQ:115-117; HO:621 | decided; MB | DONE (policy) / NOT STARTED (MB) | MB in `to-release.js:91` | Oct 9 |
| R-32 | Back swipe during a push runs predictive back; `back_during_push` scenario | REQ:118-121; HO:622 | WP04 | ON BRANCH | r28: one opaque card, lands on the list, both pass; bug run adds `BackCardDuringPushDrawingTest` (`6c6ee60`) | Oct 19 21:48 |
| R-33 | Bottom sheet system (spec, detents, card stack, glass, parity) | REQ:125-136; HO:623 | WP03, WP04, WP05, WP07-B, WP08, QA2, WP12 | ON BRANCH (WP03 part) / WAITING (WP04 §19.2, card stack, WP06 §19.8, on WP03's merge) / NOT STARTED (Glass sheet and Glass style: WP05, after WP03 and WP04) | `SheetSurfaceNode` on `wp/WP03`; F-11; KGP ABI validation blocks the API dump (`handoff/WP03.md:1085`); WP05's Glass sheet through SHEET-SPEC §19.3 (`handoff/WP05.md:165-168`), Glass style (`:735-739`) | Oct 19 |
| R-34 | One coherent shimmer band; nothing clipped | REQ:137-140; HO:624 | WP03 (via WP03-skel) | ON BRANCH (fix) / MATRIX-ROUTED (phone proof) | `wp/WP03-skel` contained in `wp/WP03`; `qa/shimmer/VERIFY.md` absent; its workflow not relaunched (HO:887-888, 954-955); QA2's light, dark, font 1.5 screens cover the clipping conditions, not the sweep (`to-release.js:94`) | Oct 16 |
| R-35 | Travelling lens on tab taps, cheaply, within `glass_live` | REQ:141-155; HO:625 | WP05, WP11, WP07-B, QA2 | ON BRANCH (lens state) / NOT STARTED (bar wiring, CP2 strips, `glass_live` row: WP05) / MATRIX-ROUTED (QA2's check) / DECIDED (PeriFlow part parked) | `904a2a8..5d515a0` on `wp/WP05`; no bar wires it except the uncommitted `LiquidTabs.kt`; `perf/WP05/` absent; QA2's requirement list omits 35 (`to-release.js:94`) | Oct 15 |
| R-36 | Android first: release when 100 % done; user approves deploy | REQ:156-164; HO:626 | WP13, gate, user | NOT STARTED | `RELEASE-GATE-android.md` absent | — |
| R-37 | Full Kyant glass at playground parity: bar, then components, playground page | REQ:165-167; HO:627 | WP05 (`to-release.js:81`); staging for the bar's WIP | NOT STARTED (bar: staging steps 2-3, then WP05; top-bar buttons, Glass sheet, `CatalogGlass`, `glass_live` gates: WP05) / MATRIX-ROUTED (toggle, slider, segmented control, buttons, alert, toast at playground parity; side-by-side playground strips; the full effect set as the bar's acceptance bar) | 43 uncommitted files untouched since Oct 15 15:53; last compile failed (HO:898-900); `full-liquid-glass-fable.js` never launched on Windows; WP05's design and handoff own the listed parts (`design/wp/WP05.md:99-121`; `handoff/WP05.md:698-768`); risk: duplication if the glass branch stays out of staging and WP05 follows its fallback (`handoff/WP05.md:160`) | Oct 15 15:53 |
| R-38 | Skills 1.0.15 vendored feature-aware; jetpack-compose-performance used, violations blocking, template-wide audit | REQ:168-173; HO:628 | WP02 (vendoring), all | ON BRANCH (vendoring) / MATRIX-ROUTED (REQ 38.5 audit, `COMPOSE-PERF-AUDIT.md`) | `f470046`; audit sections in WP04, WP04-perf, WP05, WP06, HUD, WP02-M2 handoffs; WP03 applies single rules (`handoff/WP03.md:975-977`) but records no full audit; none for WP07-A or the glass bar; `research/COMPOSE-PERF-AUDIT.md` absent | Oct 16 (audit) |
| R-39 | Cronet opt-in, Brotli on by default, proof, tests | REQ:175-186; HO:629 | NET | NOT STARTED | NET in `to-release.js:123`, starts after WP06 and WP02-M2 merge; groundwork `research/NET-CRONET-BROTLI-NOTES.md` | Oct 18 |
| R-40 | Live FPS overlay, log, export, score; nothing in prod; shown first | REQ:188-197 | HUD | ON BRANCH | `wp/HUD` `936fa2e`; overhead within limits (`handoff/HUD.md:573-626`), measured on a tree without WP04's shell; prod guard `f589a82`; demo see H-15 | Oct 19 15:20 |
| R-41 | iOS scrolling on every surface; jank and first-fling gates; proof | REQ:199-203 | WP03, WP04-perf, QA2, WP12 | ON BRANCH (physics) / MATRIX-ROUTED (first fling on the merged tree, H-19) | Sweep and tests on `wp/WP03`; fling within 2.5 % of iOS; scroll_minimize 0.59 vs 0.5 (`r28-accept2.log:8140`) | Oct 19 |
| R-42 | Back card texture; gate what users feel; keep cutting memory | REQ:204-224 | WP04, WP04-perf, QA2, WP12 | ON BRANCH (gates `4a15dda`; new rows `1ec5d90`) / NOT STARTED on WP04 (T + C1: staging applies) / MATRIX-ROUTED (gesture hitch release row, scrim fold; memory cuts) | Patches only in preview `eb4f263`; staging applies them (`consolidate-staging.js:69`); A11.3 exception text owed (H-3); gesture hitch 6.91 vs 5.0; the matrix review names this row (`consolidate-staging.js:151`) | Oct 19 21:38 |
| R-43 | Catalog shows everything: preview now, content later | REQ:225-234 | orchestrator, CAT | SUPERSEDED (preview, by staging) / NOT STARTED (CAT) | `catalog-preview.js` stopped 15:44; bug run's `wt/_preview` replaces it; CAT in `to-release.js:132` | Oct 19 |
| R-44 | Staging first, then `staging-wp/*`, report, user's go | REQ:235-244 | orchestrator | NOT STARTED (waits for the bug fix, running) | No `staging` branch; `staging/` empty | Oct 19 19:58 (script) |
| R-45 | District research, every change approved, phone study observes only | REQ:246-256 | orchestrator | DONE (research) / NOT STARTED (phone study, proposals) | `research/district/RESEARCH.md`; `district-phone-study.js` written; proposals held for it (PROGRESS 20:31, 21:14) | Oct 19 21:14 |
| R-46 | Every requirement met the smoothest way, with proof | REQ:258-266 | staging auditors | NOT STARTED | Requirements phase in `consolidate-staging.js:132-158` | Oct 19 19:57 |
| R-P1 | Port the good parts of PeriFlow and VitFleet | REQ:42 | plan | DONE (as plan input) | `audit/periflow-learnings.md`, `audit/vitfleet-learnings.md`, cited across MP | Oct 9 |
| R-P2 | Code review and fix bad code | REQ:43 | reviewers | IN PROGRESS | Package reviews; staging's combined review next | ongoing |
| R-P4 | Main only after a green matrix, then gate and go-ahead | REQ:44, 162 | user | DONE (in force) | `main` = `99c4edf` (Sep 24) untouched | — |
| R-P5 | PeriFlow on a local branch, never pushed | REQ:44 | — | SUPERSEDED (Oct 16: pushed to its private repo, HO:366-371) | — | Oct 16 |
| R-P6 | Gradle and adb through `bin/locked`; memory safety; heavy commands through `memwait` | REQ:45-47 | all | DONE (in force) | `bin/locked.py`, `bin/memwait`; memory wait revised 21:41 (`decisions/LOG.md:40`) | Oct 19 21:41 |
| R-P7 | No emulator: code first, poll for the phone, record phone steps | REQ:46 | all | DONE (in force) | — | — |

#### Definition of done and gates

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| D-1 | Every applicable requirement merged | HO:636-637 | all | NOT STARTED (staging) | Only WP01, WP02-M1, WP09, WP10, WIN merged; staging replaces package merges | Oct 19 |
| D-2 | Smoothness holds on release builds | HO:638-640 | WP04, WP13 | IN PROGRESS | See G-1, G-2 | Oct 19 |
| D-3 | Security: WP06 plus intent and permission audits over a generated app | HO:641-642 | WP06, WP13 | ON BRANCH (WP06 audits) / NOT STARTED (WP13 audits, `to-release.js:104`) / FORGOTTEN (H-4, H-5) | `handoff/WP06.md:672-673`; the app-lock bypass and link-host notes never reached WP06 | Oct 19 |
| D-4 | Adaptive, accessibility, sheet, shimmer, lens, glass, skills | HO:643-644 | WP03-WP05, WP02 | MATRIX-ROUTED (TalkBack proof on the phone) | No package produces it: QA2's scope (`to-release.js:94`) and WP13's (`:104`) do not name TalkBack; CAT asks for TalkBack labels (`:133`); WP03 has semantics unit tests (`design/wp/WP03.md:140`); the release gate names accessibility (`:364`); the other parts are R-23, R-33-R-35, R-37, R-38 | — |
| D-5 | Docs verified against the code | HO:645 | WP12 | NOT STARTED | `to-release.js:100` | — |
| D-6 | Android matrix green; follow-ups closed; slop sweep | HO:646-647 | WP13 | NOT STARTED / FORGOTTEN (template reformat, H-10) | `everything` with `formatting` fails `check` until the reformat (`handoff/WP02-M2.md:793`) | — |
| D-7 | `report/FINAL.md` with before/after | HO:648 | WP13 | NOT STARTED | `to-release.js:104` | — |
| D-8a | `ENABLE_TARGETS=android` on Vercel | HO:649; `handoff/WP10.md:809` | user | DECISION OPEN | Never asked (no entry in `decisions/LOG.md`) | Oct 14 |
| D-8b | Site's CI claim true; WP13 matrix gates CI | HO:649-650; `handoff/WP10.md:814-818` | WP13 (CI rewrite), WP12 (copy) | NOT STARTED | WP13 rewrites `ci.yml` (`design/wp/WP13.md:65`); WP12's copy edits make hero and FAQ match the code (`design/wp/WP12.md:38, 70`); `ae886b5` already updated `ci.yml` partly | Oct 19 |
| D-8c | `docs/telemetry-and-privacy.md`, linked from hero, FAQ, footer | HO:650-651 | WP12 | NOT STARTED | `design/wp/WP12.md:51` | — |
| D-8d | `LICENSE` matching the site's MIT | HO:651 | user | DECISION OPEN | Never asked | Oct 14 |
| D-8e | End-state claims in `included.tsx` true | HO:651-652; `handoff/WP10.md:826` | WP12 (copy), WP13 (verify) | NOT STARTED | `design/wp/WP12.md:38, 70` ("included" among the pages that must match the code); WP10 hands its blockers to WP12 and WP13 (`handoff/WP10.md:807`) | Oct 14 |
| D-8f | Signed-in click-through of `/admin` | HO:652; `handoff/WP10.md:840` | WP13, user | FORGOTTEN / DECISION OPEN | Not in `design/wp/WP13.md` or WP13's scope; needs the user to ask for a token to be typed | Oct 14 |
| D-8g | `npm audit` advisories: next 15.5.27, postcss 8.5.23 | `handoff/WP10.md:853` | WP13, user | DECISION OPEN | Needs an install approval; never asked | Oct 14 |
| D-9 | `RELEASE-GATE-android.md` GO | HO:653 | gate agent | NOT STARTED | Gate prompt in `to-release.js` predates REQ 42 and 46; AUTOPILOT step 6 adds REQ 46 | — |
| D-10 | The user's go-ahead, then merge to main, push, deploy | HO:654 | user | DECISION FUTURE | — | — |
| D-11 | PeriFlow not in the release; FINAL may say pending | HO:655 | — | DONE (rule) | — | — |
| D-12 | Live site: only Android selectable | HO:656 | WP10 | MERGED | `merged/WP10` | Oct 14 |
| D-B1..B5 | Phase B: WP07-B/C, WP08 merge, WP12-B, WP13-B, multiplatform gate, CMP beta on the site | HO:660-666 | WP07, WP08, WP12, WP13, user | NOT STARTED (deferred by REQ 36) | Owned in `to-release.js` ORDER | — |
| G-1 | WP04 acceptance at `4a15dda` (merge and release gates) | `handoff/WP04.md:47-54` | WP04 | WAITING (failed two rows; recorded but not acted on) | Recorded in `SESSION-2026-10-18.md:126-130` (Rapid Back max 8.63 vs 7.9; scroll_minimize 0.59 vs 0.5; `r28-accept2.log:8129, 8140`), not in WP04's handoff, which still says to read the log (`handoff/WP04.md:50`); no WP04 run since (to-release stopped 15:44); the audit's own reading of the release table: gesture hitch 16.39 and Rapid Back hitch 12.32 vs 5 ms/s (`:8054, 8064`, before T + C1); staging's phone run re-measures | Oct 19 08:46 |
| G-2 | Back gesture hitch release limit (≤ 5 ms/s) | REQ:212-214; `handoff/WP04-perf.md:879-881` | WP04, WP04-perf | MATRIX-ROUTED | 6.91 after T + C1; next lever DESIGN §6 scrim fold; no script runs WP04-perf now; the matrix review names this row (`consolidate-staging.js:151`) | Oct 19 14:45 |
| G-3 | PSS row and memory | HO:687; REQ:214-221 | WP04, WP04-perf | DONE (ratchet, REQ 42) / MATRIX-ROUTED (memory cuts, REQ 42.8) | Total 109.7 MB, less buffers 56.3 (r28) | Oct 19 |
| G-4 | Cold start | HO:686 | WP04 | ON BRANCH | Speed profile 324 ms, installed 387 ms, pass (r28) | Oct 19 |
| G-5 | Large-window perf scenarios (`tabs_large`, `scroll_large` …) | `handoff/WP04-perf.md:893-895`; ADAPTIVE §0.7 | WP04, QA2 | ON BRANCH (adaptive steps, run 24) / NOT STARTED (large perf rows) | WP04's `adaptive.sh --allow-display-overrides` passed and restored the display (`handoff/WP04.md:1083-1086`); RSF:968-969 still lists the approval as waiting | Oct 19 |
| G-6 | `glass_live` and Frosted-vs-Solid gates (A4.9) | MP:1403-1405; RSF:789 | WP05 | NOT STARTED (after WP03 and WP04) | `design/wp/WP05.md:108-121`; `handoff/WP05.md:751-760`; never measured; `perf/WP05/` absent | — |
| G-7 | `tools/size_budgets.toml` (measured + 5 %) | RSF:825; `to-release.js:245` | integrator | FORGOTTEN (staging bypasses the merge step) | File not written; the trigger fires only inside `mergePackage` for WP02-M2/WP06, which staging merges instead | — |
| G-8 | Ratchet rows in `gates.toml` after WP04 | `to-release.js:244`; MP:161-164 | WP04 | ON BRANCH | Written from run 26 in `4a15dda`; bug run adds rows in `1ec5d90` | Oct 19 21:38 |
| G-9 | Full device re-gate after WP02-M3 merges | MP:2258-2260; `to-release.js:246` | integrator | NOT STARTED | Merge-time step of `to-release.js`; still fires after the rebase, because WP02-M3 merges after staging | — |
| G-10 | 14-row matrix and the wide matrix | MP:2130-2143, 2231-2237 | WP13 | NOT STARTED | `to-release.js:104` | — |
| G-11 | Perf smoke after each UI merge | MP:2253-2257 | integrator | SUPERSEDED (staging tests the combined app once, REQ 44) | `consolidate-staging.js` Test phase | — |
| G-12 | Release gate for phase B | HO:665 | gate agent | NOT STARTED | — | — |
| CP-1 | CP1 motion and press feel | HO:823 | decision agent | DONE | `decisions/CP1.md` | Oct 9 |
| CP-2 | CP2 glass feel | HO:824-827 | WP05, cp2Flow | WAITING (WP05's recordings, after staging) / DECISION OPEN (may an agent decide, U-7) | `decisions/CP2.md` and `perf/WP05/` absent; cp2Flow lives in the stopped `to-release.js` | — |
| CP-3 | CP3 replaced by the release gate | HO:828-831 | — | SUPERSEDED (REQ 36) | — | — |
| MERGE | Package merge order (HO 4.4, PAB 5.4, RSF 6.3, MP B.5; they differ) | HO:796-812 | integrator | SUPERSEDED (REQ 44: staging decides the order) | `consolidate-staging.js` Prepare phase | Oct 19 |

#### Packages

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| P-WIN | Generator correct on Windows; Linux CI fixes | `handoff/WIN.md` | WIN | MERGED (`5a16778`, `4dd3aca`, `3a0d38e`) / ON BRANCH (`a5db7b0`, `070a91b`) | PR #2 Linux CI green, closed unmerged; fixes reach integration via staging | Oct 19 14:31 |
| P-WP01 | Engine v2, spec v2, catalogue, tokens | `handoff/WP01.md` | WP01 | MERGED | `merged/WP01`; deviations 1 and 11 still need the orchestrator's acceptance (H-17) | Oct 9 |
| P-WP02-M1 | Pure core, MVI, api packages | `handoff/WP02-M1.md` | WP02 | MERGED | `merged/WP02-M1`; open items routed to WP02-M3, WP04, WP06 (`:346-371`) | Oct 9 |
| P-WP02-M2 | Conventions, code rules, presets, skills, scaffold | `handoff/WP02-M2-RESUME.md` | WP02 | ON BRANCH / WAITING (on staging) | Steps 1-6 open; 352 tests OK; needs a fresh acceptance | Oct 19 03:16 |
| P-WP02-M3 | Reference features on the new design system | `handoff/WP02-M3.md` | WP02 | NOT STARTED | `to-release.js:87`; sample-detail patch saved `out/WP04-perf/sample-detail-related-after-settle.patch` | Oct 18 (notes) |
| P-WP03 | Design system, sheets, scroll | `handoff/WP03.md:7-11` | WP03 | ON BRANCH / WAITING (on staging, for items 0-10) | `ACCEPT OK` 03:19 Oct 19 | Oct 19 02:32 |
| P-WP03-skel | Shimmer and clipping | `handoff/WP03-skel.md` | WP03 | SUPERSEDED (contained in `wp/WP03`) / MATRIX-ROUTED (phone proof) | `merge-base --is-ancestor` true | Oct 14 |
| P-WP04 | Navigation shell, back card, gates | `handoff/WP04.md:1-60` | WP04 | ON BRANCH / WAITING (on staging; r28's two red rows and review #2 open) | Run 28's next steps 1-5 not started; bug run's `1ec5d90`, `6c6ee60` on the branch | Oct 19 21:48 |
| P-WP04-perf | Deep smoothness | `handoff/WP04-perf.md:877-895` | WP04-perf | ON BRANCH (`2b91187`, harness) / MATRIX-ROUTED (open items: G-2, G-3, first fling) | Both its scripts finished; not a package in `to-release.js`; staging's Report step cuts a branch for every package whose handoff still has work (`consolidate-staging.js:162`); folding it into `staging-wp/WP04` is the recommendation | Oct 19 14:45 |
| P-WP05 | Glass foundation, lens, then the glass package | `handoff/WP05.md:1-14`; `to-release.js:81` | WP05 | ON BRANCH / WAITING (on staging) | Runs 16-20 made no code change; last own commit Oct 17 15:34; "What remains" owns the bar, Glass style, `CatalogGlass`, gates, CP2 (`:698-768`) | Oct 19 10:45 (handoff) |
| P-WP05-glass | Full glass, bar first | `handoff/WP05-glass.md` | staging steps 2-3, then WP05 | NOT STARTED (staging commits and merges it; risk: duplication) | 43 uncommitted files; `daafdbd`, `91cb638` missing; backup `origin/backup/WP05-glass-wip-2026-10-16`; last compile failed; the merge agent may leave out a branch that does not build (`consolidate-staging.js:78`), and WP05's fallback then rebuilds the bar (`handoff/WP05.md:160`) | Oct 15 15:53 |
| P-WP06 | Data, network, security, devtools | `handoff/WP06.md:8-11, 66-88` | WP06 | ON BRANCH / WAITING (on staging) | `accept.win.sh` not run at `08c0d0a`; last `ACCEPT OK` `7b14c0d` | Oct 19 02:06 |
| P-WP07-A | CMP phase A | `handoff/WP07-A.md:8-17` | WP07 | ON BRANCH / WAITING (on staging: WP04 shell port, WP02-M2) | No Gradle build yet by decision (`:50-57`) | Oct 18 05:16 |
| P-WP07-BC | CMP phases B, C | MP:2453-2455 | WP07 | NOT STARTED (phase B) | `to-release.js:95, 97` | — |
| P-WP08 | SwiftUI template | `handoff/WP08.md:185-203` | WP08 | ON BRANCH / WAITING (on staging: WP03 tokens) | Open 7: apply `skeleton-tokens-swift.patch` after WP03 merges (`:998-1038`) | Oct 19 01:46 |
| P-WP09 | API and telemetry v2 | `handoff/WP09.md` | WP09 | MERGED | `merged/WP09` | Oct 9 |
| P-WP10 | Website and admin | `handoff/WP10.md:8-14` | WP10 | MERGED | `merged/WP10`; release blockers D-8 | Oct 14 |
| P-WP11 | PeriFlow | `handoff/WP11.md`, `WP11-RESUME.md` | WP11 | DECIDED (parked) | Out of the relaunch since Oct 17 (memory decisions item 5, "don't re-ask"); "PeriFlow never" for staging (REQ:236); not in either deploy (REQ:164); never blocks the release (HO:972) | Oct 15 |
| P-WP12 | Docs (Android), WP12-B | `to-release.js:99, 106` | WP12 | NOT STARTED | — | — |
| P-WP13 | Verification, FINAL.md, WP13-B | `to-release.js:103, 108` | WP13 | NOT STARTED | No handoff file yet | — |
| P-WP14 | Project tools | `to-release.js:89` | WP14 | NOT STARTED | — | — |
| P-MB | materialbridge | `to-release.js:91` | MB | NOT STARTED | — | — |
| P-QA2 | Visual and motion QA round 2 | `to-release.js:93-94` | QA2 | NOT STARTED / MATRIX-ROUTED (explicit 34, 35, 37 checks, the shimmer sweep, TalkBack) | Scope already builds everything-with-glass, records the glass bar, and shoots light, dark and font 1.5 | — |
| P-NET | Cronet and Brotli | `to-release.js:123` | NET | NOT STARTED | — | — |
| P-HUD | Performance overlay | `handoff/HUD.md` | HUD | ON BRANCH | Fix #3 done; review #4 superseded by staging's review | Oct 19 15:20 |
| P-CAT | Catalog content | `to-release.js:132` | CAT | NOT STARTED | — | — |
| P-preview | Combined preview tree | STATUS.md:44 | bug run | IN PROGRESS | `preview/catalog-2026-10-19` `722ced3` (merged `6c6ee60` at 21:48) | Oct 19 21:48 |

#### Handoff and orchestrator notes

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| H-1 | "WP05: merge wp/WP05-glass into wp/WP05 at the start of your next run" | `handoff/WP05-glass.md:171` | staging steps 2-3, then WP05 | NOT STARTED (staging; risk: duplication) | WP05 takes the branch only once committed (`handoff/WP05.md:152-159`) and otherwise builds `LiquidTabs` itself (`:160`); `consolidate-staging.js:68-69` WIP-commits the worktree and merges the glass branch | Oct 15 |
| H-2 | Apply T + C1 on `wp/WP04` (14:50 note) | `handoff/WP04.md:8` | WP04 | NOT STARTED on WP04 (staging applies) | `wp/WP04`'s two commits after 07:45 are the bug run's (`1ec5d90`, `6c6ee60`), neither the patches; T + C1 only in preview `eb4f263`; `consolidate-staging.js:69` | Oct 19 14:50 |
| H-3 | Write the A11.3 rule 1 exception (rule text, code comment, docs) | REQ:210-211; `out/WP04-perf/back-google/RESULT.md:360-363` | WP04, WP12 | NOT STARTED | The patches do not carry the rule text; REQ 42.9 names WP04 | Oct 19 |
| H-4 | App lock: a link or Recents within 30 s reopens unlocked | `handoff/WP04.md:1393-1395` | WP06 | FORGOTTEN (security) | Not in `handoff/WP06.md` or `followups.md`; staging's reviewers rate pre-merge issues nonblocking (`consolidate-staging.js:114`) | Oct 19 |
| H-5 | `DeepLink.from` does not check an https link's host | `handoff/WP04.md:1396-1397` | WP06, WP13 | FORGOTTEN (security) | Not in `handoff/WP06.md` or `followups.md`; WP13's intent-security audit may catch it (`to-release.js:104`) | Oct 19 |
| H-6 | Orchestrator sign-off for two WP04 edits outside its paths | `handoff/WP04.md:1390-1392` | orchestrator | NOT STARTED (staging's combined review covers it) | — | Oct 19 |
| H-7 | `recompose.push.max` budget 2 once M3 lands; ignored effects | `handoff/WP04.md:1398` | WP02-M3 | FORGOTTEN (minor) | Not in `handoff/WP02-M3.md` | Oct 19 |
| H-8 | `checkComposeStability` depends on which compilation ran last | `handoff/WP03.md:1079` | WP02 | FORGOTTEN | Not in `handoff/WP02-M2.md` open issues; a matrix auditor can get a green run by luck | Oct 16 |
| H-9 | `AdaptiveAuditTest` has no owner | `handoff/WP02-M2.md:803` | none named | MATRIX-ROUTED (REQ 23.8) | ADAPTIVE-PLAN §11.6, §16; REQ 23.8 asks for build-time rules, multi-size previews and screenshot tests | Oct 19 |
| H-10 | Template reformat, "the orchestrator's one commit" | `handoff/WP02-M2.md:343, 793` | orchestrator | FORGOTTEN | In no script; documented timing "after the last UI wave" (`research/STANDARDS-PLAN.md:411`; `handoff/WP02-M2.md:310, 561`); staging's fix loop records the `everything` failure in `CARRY-OVER.md` (`consolidate-staging.js:125`) | Oct 19 |
| H-11 | DevToolsPanel's empty state sits too high (padding) | `handoff/WP03-skel.md:144-147` | WP06 | FORGOTTEN (minor; may go with §19.8) | Not in `handoff/WP06.md` | Oct 14 |
| H-12 | `AppScaffold` pads a pane's inner edge; K10 device check pending | `handoff/WP03-skel.md:149-151` | WP03/WP04 | FORGOTTEN (minor) | Not in either handoff | Oct 14 |
| H-13 | Port the skeleton to WP04's `TabVisibility` before shipping WP03-after-WP04 | `handoff/WP03-skel.md:140-143` | WP03 | NOT STARTED (planned) | `handoff/WP03.md:1134`; preview `NOTES.md:66-71` did it | Oct 19 |
| H-14 | `perf.py` should default `perf.hud=off` | `handoff/HUD.md:1025-1045` | WP04, WP13 | FORGOTTEN (minor) | Not in `handoff/WP04.md`; WP13 has no handoff | Oct 19 |
| H-15 | HUD demo `perf/HUD/SHOW.md`: screenshots, a recording | `handoff/HUD.md:1021`; REQ:196 | hud-first (stopped) | MATRIX-ROUTED (partly covered) | Staging's phone step captures the pill and the Performance tab only (`consolidate-staging.js:93`); no clip or exported report; `perf/HUD/show/` has older shots, no SHOW.md | Oct 19 |
| H-16 | Stale lines in `D:/projects/tools/CLAUDE.md` and the release script's ORCH | `handoff/WIN.md:1415` | orchestrator | FORGOTTEN (minor, stale text) | `CLAUDE.md:38` and `to-release.js:111, 148` still say 6 generator tests fail; `CLAUDE.md:42` says gh is absent but `C:/Program Files/GitHub CLI/gh.exe` exists; `CLAUDE.md:29` says `KONAN_DATA_DIR` "once the user sets it" while the global rules list it as set | Oct 19 |
| H-17 | Accept WP01's product-visible deviations 1 and 11 | `handoff/WP01.md:679` | orchestrator | FORGOTTEN (minor) | No record in `decisions/LOG.md` | Oct 9 |
| H-18 | Oct 15 note to read the performance skill | `handoff/WP05-glass.md:173-175` | WP05-glass | SUPERSEDED (the work moves to staging and WP05; every agent's prompt carries the skills rule, `to-release.js:145`; REQ 38.3) | — | Oct 15 |
| H-19 | Re-run `firstfling.win.sh` and scroll rows on the merged tree | `handoff/WP04-perf.md:883-886` | WP04-perf | MATRIX-ROUTED (first fling) / NOT STARTED (scroll rows: staging's perf run) | `consolidate-staging.js:93` phone steps have no first-fling run | Oct 19 |
| H-20 | WP06 cold-start trace of keystore2 and the Play binding; Play prompts measurement | `handoff/WP06.md:13-28, 71-88` | WP06 | WAITING (on staging) | — | Oct 19 |
| H-21 | WP02-M2's merge makes WP03, WP04, WP06 lower or re-path `code-rules-baseline.txt` | `handoff/WP02-M2.md:797` | staging merge | NOT STARTED (staging risk) | — | Oct 19 |
| H-22 | At WP02-M2's merge take WP02's `scaffold.py` whole, keep WIN's test | `handoff/WIN.md:72`; followups.md:168 | staging merge | NOT STARTED (staging) | — | Oct 19 |
| H-23 | WP08: apply skeleton token patches after WP03 merges | `handoff/WP08.md:998-1038` | WP08 | WAITING (on staging) | — | Oct 19 |
| H-24 | Run the skills tool on the multiplatform template | `handoff/WP02-M2.md:836-838` | WP07 | WAITING (on staging) | — | Oct 19 |
| H-25 | A list row's glyphs keep their size at large text; "left as a design decision" | `handoff/WP03.md:1086` | user/WP03 | DECISION OPEN (minor) | Never asked | Oct 16 |
| H-26 | Autopilot prompt's push-policy line | HO:370-371 | orchestrator | DONE | `scripts/AUTOPILOT-PROMPT.md` last line | Oct 19 |
| H-27 | PSS row: after the journey or after restore | `handoff/WP04-perf.md:891` | WP04, user | DONE (REQ 42: PSS is a ratchet) | — | Oct 19 06:16 |
| H-28 | Re-sync the four 48 MB app-memory copies when HUD merges after WP04 | `handoff/HUD.md:1029-1032` | staging merge | NOT STARTED (at risk) | In HUD's Open issues, which staging's Prepare step reads (`consolidate-staging.js:67`); WP04's row is now 58.4/53.3 | Oct 19 |

#### Follow-ups and bugs

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| F-1 | Field jank attribution by navigation state | followups.md:8 | WP13/WP06 | NOT STARTED | WP13 closes `followups.md` (`to-release.js:104`) | Oct 19 |
| F-2 | Repository fakes into `testFixtures` | followups.md:18 | WP02-M3/WP13 | NOT STARTED | — | Oct 19 |
| F-3 | WP06 device checks | followups.md:21 | WP06 | DONE (closed Oct 14) | — | Oct 14 |
| F-4 | Emulator scripts and "System UI isn't responding" | followups.md:24 | WP04/WP14 | SUPERSEDED (no emulator on Windows) | — | Oct 14 |
| F-5 | `SampleListViewModel` stuck on Empty | followups.md:30 | WP02-M3 | NOT STARTED | — | Oct 19 |
| F-6 | Popups and dialogs above the app-lock cover | followups.md:36 | WP03/WP06/WP12 | NOT STARTED (WP13 closes follow-ups; a security fix belongs to WP06) | — | Oct 19 |
| F-7 | Consistency matrix on Windows | followups.md:46 | WIN | MERGED (`4dd3aca`) | — | Oct 19 |
| F-8 | Time limit on generated projects' test tasks | followups.md:61 | WP02-M2/WP12 | NOT STARTED | — | Oct 19 |
| F-9 | Cronet checks once NET lands | followups.md:67 | NET | NOT STARTED | — | Oct 19 |
| F-10 | DEV badge over a rail or sidebar | followups.md:73 | WP04/WP06 | NOT STARTED | — | Oct 19 |
| F-11 | Sheet navigation half and card stack | followups.md:85 | WP04 | WAITING (on staging) | — | Oct 19 |
| F-12 | REQ 34 seams that WP03's merge wires | followups.md:100 | WP03/WP04/WP02-M3 | NOT STARTED (staging merge) | — | Oct 19 |
| F-13 | Back card shaders compiled on the first swipe (up to 70 ms once) | followups.md:108 | WP04 | NOT STARTED (WP04, after WP03's merge) | First use (S6, S7) in WP04's motion-perf notes (`handoff/WP04.md:1597-1600`); SMOOTHNESS-PLAN assigns S6's warm-up to WP04 (`research/SMOOTHNESS-PLAN.md:344`); carries to `staging-wp/WP04` | Oct 19 |
| F-14 | Zoom (hero) transitions | followups.md:113 | WP04/WP02-M3 | NOT STARTED (see R-21.2) | `handoff/WP04.md:726-737` | Oct 16 |
| F-15 | Generator suite on a POSIX host | followups.md:121 | orchestrator | DONE (PR #2 green) | — | Oct 19 15:56 |
| F-16 | Generated keystore passwords printable ASCII | followups.md:134 | user | DECISION OPEN | `ee311a8` merged in `3a0d38e` | Oct 19 |
| F-17 | `windows-latest` generator CI job | followups.md:139 | WP13 | NOT STARTED | — | Oct 19 |
| F-18 | `test_zip` long path on POSIX | followups.md:144 | WIN | ON BRANCH (`a5db7b0`) | — | Oct 19 |
| F-19 | Stale `ci.yml` | followups.md:166 | WIN, WP13 | MERGED (partly, `ae886b5`) / NOT STARTED (WP13 rewrite, `design/wp/WP13.md:65`) | — | Oct 19 |
| F-20 | Failed Vercel preview of PR #1, cause unread | followups.md:167 | user, WP10/WP13 | DECISION OPEN (needs the user's Vercel login) | — | Oct 19 |
| F-21 | Generated-project jobs fail at `AppResult` | followups.md:168 | WIN | ON BRANCH (`070a91b`) | — | Oct 19 |
| B-1 | Scroll direction at the rubber band | `bugs/BUG-2026-10-18-scroll-direction.md` | WP03 | ON BRANCH (fixed, phone-verified with synthetic input) | `daafdbd`, `91cb638` in `wp/WP03`, not in `wp/WP05-glass`; no hand check by the user recorded (`:165-169`) | Oct 18 18:35 |
| B-2 | See-through back card (the user's report) | `bugs/BUG-2026-10-19-back-see-through.md` | bug run | RUNNING | Root cause written (§Root cause): the user's app was a HUD build from `wp/WP06` without WP04's shell; WP04's `c928fb91` (Oct 9) removed it; the right-edge DEV badge takes half-height back swipes (blind spot 3); bug run committed `1ec5d90`, `6c6ee60` on `wp/WP04`; Result section still empty | Oct 19 21:48 |
| B-3 | WP04's double-exposure check covers a swipe during a push, not a settled pushed screen | `bugs/BUG-2026-10-19-back-see-through.md:32` | bug run | IN PROGRESS | `1ec5d90` gates a back swipe held over a settled screen | Oct 19 21:38 |

#### Decisions only the user can make

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| U-1 | Merge-all into main | HO:305-321; RSF:954-961 | user | DECISION ANSWERED (Oct 17: no) | `SESSION-2026-10-17.md:18-20` | Oct 17 |
| U-2 | PeriFlow lens deviation (3.62 % vs 2.5 %) | HO:322-327; RSF:928-935 | user | DECISION FUTURE (moot while PeriFlow is parked; only if the user brings it back) | — | Oct 15 |
| U-3 | What "smooth enough" means | HO:330-337; RSF:936-943 | user | DECISION ANSWERED (Oct 19 06:16, REQ 42) | `decisions/LOG.md:11` | Oct 19 |
| U-4 | Scroll 0.5 % gate | RSF:944-945 | user | DECISION ANSWERED (REQ 42 keeps REQ 41's limits); the row is still red | — | Oct 19 |
| U-5 | PSS and cold start on `everything` | RSF:946-949 | user | DECISION ANSWERED (PSS a ratchet; cold start passes) | — | Oct 19 |
| U-6 | Shimmer sweep 0.69 % vs 0.5 % | RSF:950-951 | user | DECISION OPEN (never asked) | — | Oct 16 |
| U-7 | May an agent decide CP2 as with CP1 | RSF:952-953; HO:350 | user | DECISION OPEN (never asked; scripts assume yes) | — | Oct 16 |
| U-8 | Utility bar Docked → Floating | RSF:962-963; HO:347 | user | DECISION OPEN (never asked; already committed `e80fe43`, Oct 17 15:38) | — | Oct 17 |
| U-9 | MIT `LICENSE` | RSF:964-965 | user | DECISION OPEN (never asked; release blocker) | — | Oct 14 |
| U-10 | Vercel settings | RSF:966-967 | user | DECISION OPEN (never asked; release blocker) | — | Oct 14 |
| U-11 | Gradle 9.8.1 download, `protobuf`, display overrides | RSF:968-969 | user | DECISION OPEN (never asked; WP04 already used display overrides and restored them in run 24) | — | Oct 16 |
| U-12 | Kyant playground APK on the phone | RSF:970; HO:340-341 | user | DECISION OPEN (never asked; needed for REQ 37's side-by-side strips) | — | Oct 15 |
| U-13 | The release go-ahead | RSF:971 | user | DECISION FUTURE | — | — |
| U-14 | WSL2 setup; Drive backup confirmation | RSF:972-975 | user | DECISION OPEN (Drive upload unconfirmed: no record that it happened) / SUPERSEDED (WSL: native Windows, Oct 17) | memory `android-base-windows-decisions` items 1, 5 | Oct 17 |
| U-15 | Raw-data draft release still wanted | RSF:976-977 | user | DECISION OPEN (never asked; low) | — | Oct 16 |
| U-16 | Workspace history rewrite | RSF:978-979 | user | DECISION ANSWERED (Oct 17) | — | Oct 17 |
| U-17 | GitHub Support purge request | HO:358-359 | user | DECISION OPEN (the user's to-do; completion not recorded) | `handover/local-only/github-support-purge-request.md` | Oct 17 |
| U-18 | OS changes: earlyoom, power and lid, WSL limits | HO:342 | user | SUPERSEDED (earlyoom: Linux; WSL limits: native Windows) / informational (power and lid: the user's own setting, already covered by the global rules) | — | Oct 17 |
| U-19 | Keystore passwords printable ASCII | `handoff/WIN.md:1440`; followups.md:134 | user | DECISION OPEN (never asked in chat) | `decisions/LOG.md:19` | Oct 19 |
| U-20 | District raw lookups naming people in a public push | PROGRESS Oct 19 20:24 | user | DECISION OPEN | `e628493b` (Oct 19 19:00) | Oct 19 20:24 |
| U-21 | Whether run 12's Linux emulator results are enough proof for fold and posture | `handoff/WP04.md:1087-1088` | user | DECISION OPEN (never asked) | No hinge emulator or system image on this machine (global rules) | Oct 18 |
| U-22 | `npm audit` upgrades | `handoff/WP10.md:853` | user | DECISION OPEN (never asked) | — | Oct 14 |
| U-23 | Signed-in `/admin` click-through | `handoff/WP10.md:840` | user | DECISION OPEN | — | Oct 14 |
| U-24 | Back gesture hitch limit if still red after the scrim fold | STATUS.md:42 | user | DECISION FUTURE | — | Oct 19 |
| U-25 | PeriFlow's return | memory decisions item 5; REQ:236 | user | DECISION ANSWERED (parked: default accepted Oct 17, "don't re-ask"; "PeriFlow never" for staging, Oct 19 15:52) | `decisions/LOG.md:25`; REQ:164 | Oct 19 |
| U-26 | District proposals, one by one | REQ:250-251 | user | DECISION FUTURE (after the phone study) | PROGRESS Oct 19 20:31 | Oct 19 |
| U-27 | Private CI for package branches | MP:2239-2241 | user | DECISION ANSWERED (CI-only PRs, each time) | `decisions/LOG.md:16, 22` | Oct 19 |
| U-28 | Kickoff approvals (Docker images, npm ci, Kotlin/Native, Maven groups) | MP:2514-2523 | user | SUPERSEDED (ORCH grants npm ci, Maven, K/N; no Docker on Windows) | `to-release.js:144` | Oct 17 |

#### User asks outside the numbered requirements

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| A-1 | "agents which were on fable should still finish their goals using opus" | UM:1136-1139 | orchestrator | SUPERSEDED (since REQ 44, Oct 19 15:50: no package work before staging) / missed Oct 17-19 for 3 of 4 | Deep smoothness finished; glass, shimmer verify and the skills audit never relaunched (`STATUS.md` §1 table; `SESSION-2026-10-18.md:15-19`); their goals are now with staging and WP05 (glass) and the matrix (shimmer proof, REQ 38.5) | Oct 16 |
| A-2 | Fix `FABLE-FINDINGS.md` | UM:1138 | orchestrator | DONE | File edited (Oct 19 01:36) | Oct 19 |
| A-3 | Keep working on CMP and Swift at their own pace | UM:913-917 | WP07, WP08 | WAITING (on staging) | — | Oct 19 |
| A-4 | Use the connected phone; emulator if it goes away | UM:437-457 | all | SUPERSEDED (no emulator on Windows, REQ:46) | — | Oct 17 |
| A-5 | Find the crash reason | UM:706-710, 1129-1133 | orchestrator | SUPERSEDED (Linux; moved to Windows) | — | Oct 16 |
| A-6 | Back up everything off the machine | UM:1153-1225 | orchestrator | DONE (GitHub and Drive zips) | HO:14-32; the Drive upload itself is unconfirmed (U-14) | Oct 16 |
| A-7 | Install the Android skills for the user | UM:552-557 | orchestrator | DONE | plugin 1.0.15 | Oct 15 |
| A-8 | September asks (Lottie, web previews, site theme, Actions build time) | UM:85-432 | earlier release | DONE (in the Sept release; `AppLottie.kt` on `main`) / not carried (Actions build time, low) | — | Sep 24 |
| A-9 | Status and ETA on request; hourly status | UM, HO:252 | orchestrator | DONE (in force; hourly cron) | STATUS.md | Oct 19 |
| A-10 | Save notes before limits and compaction | HO:253 | orchestrator | DONE (in force) | — | Oct 19 |
| A-11 | Record the Gmail predictive back | UM:540-544 | WP04 | DONE (`research/predictive-back/SPEC.md`; recordings off this machine) | — | Oct 9 |

#### Standing rules and superseded instructions

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| S-1 | Keep going until told to stop; relaunch after cutoffs | HO:230-231 | orchestrator | DONE (in force; the side workflows not relaunched Oct 17-19 are the exception found here) | — | ongoing |
| S-2 | Never compromise quality; release-build phone gates | HO:232-233 | all | DONE (in force) | — | ongoing |
| S-3 | Decide and record; ask only what only the user can do, as questions with pros and cons | HO:234-236; AUTOPILOT step 8 | orchestrator | DONE (in force) / gap: U-6..U-12, U-19..U-22 never asked | — | ongoing |
| S-4 | A bug the user reports comes first, alone | HO:249-250 | orchestrator | RUNNING (B-2) | — | Oct 19 |
| S-5 | Main is production; never push main; no merge-all | HO:240-244 | orchestrator | DONE (in force) | `main` untouched | — |
| S-6 | Opus for code and judgments; Sonnet only for supervised mechanical work | HO:251; memory working rules | orchestrator | DONE (in force) | — | Oct 18 |
| S-7 | Non-main backup pushes allowed; agents never push; PeriFlow only to its private repo | HO:254-257; memory | orchestrator | DONE (in force) | — | Oct 19 |
| S-8 | Fable first, then Opus | HO:372 | — | SUPERSEDED (Opus 5.5 only, Oct 15-16) | — | Oct 16 |
| S-9 | Push verified work to main without asking (Sept memory) | HO:373-375 | — | SUPERSEDED (REQ 36) | — | Oct 14 |
| S-10 | STATUS.md's top and package table | HO:376-377 | — | SUPERSEDED (stale: see X-1) | — | Oct 10 |

#### Discrepancies between documents

| # | Item | Source | Owner | Real status | Evidence | Last activity |
|---|---|---|---|---|---|---|
| X-1 | STATUS.md header says nothing is pushed and REQUIREMENTS runs to 32; its package table is from Oct 10 | STATUS.md:1-7, 134-152 | orchestrator | FORGOTTEN (housekeeping) | Origin has every `wp/*`; REQUIREMENTS runs to 46 | Oct 10 |
| X-2 | `notes/INDEX.md` names REQUIREMENTS 17-25 only, and `research/predictive-back/gmail/` and `decisions/CP2.md`, which do not exist here | notes/INDEX.md:9, 16, 19 | orchestrator | FORGOTTEN (housekeeping) | — | Oct 16 |
| X-3 | STATUS calls the HostCrossfade Back failure "a real priority bug"; WP04 run 28 found it was the test's own timing | STATUS.md:40; `handoff/WP04.md:12-23` | WP04 | DONE (WP04 is right: `daf0b82`, mutation-checked) | — | Oct 19 |
| X-4 | STATUS gives HUD's head as `3fe54d39` with the overhead re-measure to do; the branch is at `936fa2e` and re-measured | STATUS.md:41; `handoff/HUD.md:573-626` | orchestrator | FORGOTTEN (housekeeping) | — | Oct 19 |
| X-5 | Waits differ: STATUS says WP05, WP06, WP07-A, WP08 wait on WP04; WP08 waits on WP03, WP06 on WP04 and WP03, WP07-A on WP04 and WP02-M2 | STATUS.md:40; handoffs | orchestrator | DONE (moot under staging) | — | Oct 19 |
| X-6 | PSS gate 70/80 MB (HO 3.5, MP) vs 48 MB (RSF 6.2, HO 4.6) | HO:687, 844-845 | WP04 | SUPERSEDED (REQ 42: a ratchet) | — | Oct 19 |
| X-7 | HO says 39 requirements; RSF covers 1-38; REQUIREMENTS now has 46 | HO:629; RSF:982 | orchestrator | DONE (REQUIREMENTS.md is the source) | — | Oct 19 |
| X-8 | WP08 accept counts differ (39/290 vs 29/280); WP08's latest run says 40 iOS tests, 332 in the suite | PAB:476-478; RSF:270 | WP08 | DONE (superseded by the Oct 19 acceptance) | `out/WP08/logs/accept-2026-10-19-merge-win.log` | Oct 19 |
| X-9 | `to-release.js` ORCH says "Today is 2026-10-17", predates REQ 37's ownership, REQ 42's gates, REQ 43-46 in several scopes | `to-release.js:144, 148` | orchestrator | NOT STARTED (the rebase on staging after the user's go, REQ:243-244) | — | Oct 19 15:33 |

### Counts

There are 227 distinct items, merged from about 700 inventory entries. Each row is counted once, by the first label
in its status cell (counted by script from the table above). 16 rows carry MATRIX-ROUTED somewhere in their status,
and 18 carry FORGOTTEN. No row is STALLED.

| Status (first label) | Rows |
|---|---|
| MERGED, DONE, DECIDED, DECISION ANSWERED | 52 |
| ON BRANCH | 42 |
| RUNNING, IN PROGRESS | 11 |
| WAITING, DECISION OPEN, DECISION FUTURE | 33 |
| NOT STARTED (owned by a script, a staging step or a package's plan) | 52 |
| MATRIX-ROUTED (no implementer yet; staging's matrix routes it) | 5 |
| FORGOTTEN (route by hand) | 15 |
| STALLED | 0 |
| SUPERSEDED | 17 |

FORGOTTEN by first label: D-8f, G-7, H-4, H-5, H-7, H-8, H-10, H-11, H-12, H-14, H-16, H-17, X-1, X-2, X-4.
MATRIX-ROUTED anywhere in the status: R-23, R-34, R-35, R-37, R-38, R-41, R-42, D-4, G-2, G-3, P-WP03-skel,
P-WP04-perf, P-QA2, H-9, H-15, H-19.
