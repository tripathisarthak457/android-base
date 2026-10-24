# android-base overhaul: STATUS (Oct 10, 2026, 16:05 IST)

This is the single file to read after a context compaction or restart. Then read `PROGRESS.md` (timeline),
`REQUIREMENTS.md` (1–32 plus memory safety) and `notes/INDEX.md`.

- **Repository:** `~/Documents/projects/tools/android-base`, integration branch `overhaul/multiplatform-v2`. Nothing
  is pushed. `main` is untouched.
- **Workspace:** `~/Documents/projects/tools/.overhaul/` (not in git).
- **The user's standing instructions:**
  - Keep running until they say stop.
  - Full-width subagents, all on Opus. Never compromise quality: a Google/AOSP-level review before every merge, phone
    gates on release builds only.
  - Don't ask questions; decide and record.
  - PeriFlow stays on the local branch `overhaul/smooth-glass`, never pushed.
  - No push or merge to `main` until the full matrix is green.

---

> **STAGING FIRST (the user, Oct 19 15:50): "FIRST TARGET IN NEXT SESSION IS TO FINISH UP WORK AND MERGE ALL CODE TO A SINGLE BRANCH "STAGING" ... I WANNA SEE ALL CHANGES COMBINED AND TESTED AND THEN ONLY NEXT WORK CONTINUES".** Their choices (asked with pros and cons): every package with work goes in (PeriFlow never); one combined Google/AOSP review replaces the per-package reviews still open; staging is pushed as a backup; old branches stay as history, new work branches `staging-wp/<package>` are cut from staging. **SETUP-ONLY RELEASE FIRST (the user, 21:11 Oct 24; REQUIREMENTS 59): the No UI components option is built and gated TONIGHT for a production deploy tomorrow morning (the user's explicit go in chat first). Workflow: scripts/noui-setup-only.js, task `wy3raatbn`, run `wf_7de6b1d7-ff8` (launched 21:15; architecture reviews, plan, three lanes in wt/NOUI-GEN, NOUI-WEB, NOUI-DOCS, integration in wt/NOUI on staging-wp/NOUI, reviews, gate, release candidate release/setup-only; resume by run id after a pause, then the cache check). It gets first claim on quota, Gradle and the phone; staging's run continues beside it; the media package and the fast-track release run STAY PAUSED (their 21:43 resume one-shot was deleted) until the setup-only release is through its gate. Announce by 06:00 Oct 25 if the morning is at risk.** **PACED 18:02 Oct 24 (51 %, 3h38m to the 21:40 reset): the media package and the fast-track release run are STOPPED (their leftover commands too); staging runs alone; one-shot `6d13f812` at 20:43 resumes the fast-track release run if the window is at 78 % or less (staging alone burns about 0.1 point a minute, so the leftover quota carries it through the reset without another stop); one-shot `c45a713c` at 21:43 resumes media (and the release run if still stopped); then the cache check.** **RUNNING from 16:38 Oct 24 until 14:00 Oct 25 (the user, 16:36: "continue and work till 2 pm tommorrow keep auto starting work when limits reset"): staging `w2z9smmls` (run `wf_a767f315-51c`, script scripts/staging-finish.js: fix round 2 under REQUIREMENTS 58, re-review 3, requirements, report; NEVER resume the old run `wf_8be4c97d-b05`: at 16:37 its resume missed the cache past the merge (identical prompts, new keys) and replayed the Oct 20 test phase, stopped 17:2x; its finished results are in staging/finish-inputs.json), media `ws8l2i5ad` (run `wf_47f9d133-816`), fast-track release `wi38ivfig` (run `wf_31bb8b89-166`, args mode fast-track), heat log `bm5ptqwzu`; autopilot `d2314ca6`, hourly `7a3b6231`, wind-down one-shot `9a50e20b` at 13:17 Oct 25; full quota with pacing and automatic resumes after each reset.** **OVER (resumed 16:38 Oct 24): PAUSED by the user at 09:55 Oct 24 ("why resume just use till 99% and stop working i will resume manually"): the user starts the next session by hand; NO timer exists. Stopped runs (resume each by run id): staging `scripts/consolidate-staging.js` run `wf_8be4c97d-b05` (fix round 2 restarts; it must read staging/MERGE-NOTES.md's REQUIREMENTS 58 note: change gates.toml to the visible ceilings and stop chasing the C1 scroll difference); media package `scripts/fast-track-media.js` run `wf_47f9d133-816`; fast-track release `scripts/release-from-staging.js` run `wf_31bb8b89-166` with args {mode: 'fast-track'}. Resume steps: re-create the autopilot ("13,33,53 * * * *") and hourly ("3 9-21 * * *") crons from SESSION-2026-10-18.md / the prompts in AUTOPILOT-PROMPT.md, resume the three runs, full quota with pacing. Before the release run goes to release mode: take REG off the critical path (REQUIREMENTS 58) and recompute the ETA. A background clean-up (scratchpad winddown_1024.py) waits for the last two locked commands (a staging phone A-B and a fast-track build) and then stops the daemons, adb and thermlog; its log is logs/winddown-2026-10-24.log.** **PAUSED for usage at 09:49 Oct 24 (93%): staging stopped (run `wf_8be4c97d-b05`; was `wkebg71u2`); the media package and the fast-track release run were already paced out; all three resume by one-shot at 11:21 after the 11:20 reset. REQUIREMENTS 58 (noise-aware jank gates) is in staging/MERGE-NOTES.md for fix round 2.** **OVER at 09:05 (the user: "continue"; normal full-quota running): STOP AT 99% (the user, 08:28 Oct 24): all three runs go until 96% of the window that resets 11:20, then the wind-down (no resume). Media `w7k3rf0cv`, fast-track release `w2mzzh7xn` resumed 08:28.** **ANSWERED 08:0x Oct 24: the five first-frame rows keep their old limits; decide again after fix round 2 / REG with fresh numbers (decisions/LOG.md).** **RESUMED 01:14 Oct 24 after the usage pause (00:03-01:14): staging `wkebg71u2`, media (stopped 08:15 by pacing; was `ww6025fay`), fast-track release (stopped 08:15 by pacing; was `wx72f9zeu`) (resumed 06:21 after the reset; earlier `w9p595mmn`, `wi90qb78r`); PACED earlier at 02:27: the media package and the fast-track release run stopped so staging (the critical path) runs alone to the 06:20 reset; they resume by one-shot at 06:14/06:21.** **RUNNING from 21:43 Oct 23 (the user: "continue the work, work fast but at full quality to still be within deadline"; nothing ran from 09:00 Oct 21 to now): staging `wvm1eg5yt` (run `wf_8be4c97d-b05`, fix phase), media plan `wv0rrtytn` (run `wf_99fc5425-a7d`), release-pipeline plan `wiqsm46gj` (run `wf_aee425b3-cf1`), FAST TRACK images and video `wfv7lfze2` (run `wf_47f9d133-816`, staging-wp/MEDIA, REQUIREMENTS 56), FAST TRACK release stages `w58v287n1` (run `wf_31bb8b89-166`, scripts/release-from-staging.js mode fast-track: WP12-DOCS, WP12-KNOW, WP02-CONT, WP14, WP10-WEB, WP13-PREP; the release-pipeline plan finished 23:25), heat log `bdawj17eq`; autopilot and hourly crons re-created (ids in AUTOPILOT-PROMPT.md). No stop time; full quota every window, early resumes; REQUIREMENTS 55: when staging returns, push, send the report, and start the release run without waiting. ** Earlier: **PAUSED by the user at 08:56 Oct 21 ("After [the work project] is fully zipped stop the work, I need to work on something else"): the user starts the next session by hand. The work project is DELIVERED (v1 04:2x, v2 07:02, final v2.1 08:55, all verified). Nothing runs: staging stopped (run `wf_8be4c97d-b05`, fix phase, last commit d429cbfb pushed; resume by run id), the media plan (run `wf_99fc5425-a7d`) and the release-pipeline plan (run `wf_aee425b3-cf1`) paused; every cron and one-shot deleted; Gradle and Kotlin daemons, adb server and thermlog stopped. Resume steps: re-create the autopilot (13,33,53) and hourly (3 9-21) crons from AUTOPILOT-PROMPT.md and SESSION-2026-10-18.md, resume staging by run id, then the plans on leftover quota; REQUIREMENTS 55 applies after staging. ** **TOP PRIORITY FROM 20:05 OCT 20: the user's WORK project (a Compose Multiplatform project for their employer; names and notes only in the scratchpad work folder, see AUTOPILOT-PROMPT.md), a working zip due 06:00 Oct 21; the earlier android-base CRM zip request is withdrawn (the user: "i dont need an android generator tommorrow"). Research and blueprint done 23:3x; build (run `wf_d3a5ccd0-efb`) FINISHED 07:00 Oct 21: v1 sent 04:2x, v2 (reviewed) sent 07:0x; final fixes `w3bomosko` (run `wf_aa32b70f-2ec`) for the two findings the last review left open, then v2.1 and staging resumes. Precedence: WORK, then staging, then the plans.** **REQUIREMENTS 55 (20:4x): everything waiting for the user's approval is approved, to start after WORK: staging-wp work starts as soon as staging returns (the report is still sent); main still needs the gate and their explicit go.** **RUNNING from 19:14 Oct 20 until 09:30 Oct 21 (the user, after a full restart: "start working and keep working till 8:40 am tommorrow use full limits"; 19:24: "IN ANY CASE FORGET 8:40 AND STOP BY 9:30 AM", "focus on finishing work but no QUALITY COMPROMISES"): staging (run `wf_8be4c97d-b05`, fix phase; stopped 21:34 for the work project's quota, RESUMED 22:4x as `wxtf99opc`, stopped 01:00-08:11 Oct 21 for the work project's quota, RUNNING again as task `w31ybfn0r` since 08:11; the DEV badge ModulateAlpha change was left staged in wt/STAGING by the stopped fix agent) runs first and alone; the media plan (run `wf_99fc5425-a7d`) and the release-pipeline plan (scripts/plan-release-pipeline.js, run `wf_aee425b3-cf1`) were stopped at 19:23 and resume only on leftover quota (AUTOPILOT top rule); autopilot `9be88581`, hourly `cb7d3bd5`, heat log `bf8rc0w6j`, wind-down one-shot `6800beaf` at 09:18 Oct 21 (WINDDOWN-PROMPT.md, no resume after it). Full quota every window (resets 22:10, 03:10, 08:10), early resumes. The restart cleared wlanext: 20.7 GB commit left at 19:13. (The CRM zip request of 19:24 was withdrawn at 20:05.)** Earlier: **PAUSED by the user at 18:26 Oct 20 for a 30-minute break (they shut the PC down); resume by hand ('continue'/'start'): re-create the autopilot (13,33,53) and hourly (3 9-21, with the parked-packages line) crons, resume staging `wf_8be4c97d-b05` (finished steps: staging:capture-phone, staging:merge, staging:prepare, staging:review-perf-multiplatform#1, staging:verify-phone; it was in its fix phase, 9 fix commits pushed, testing the app lock) and the media plan `wf_99fc5425-a7d` (finished: lookup:media, plan:media, review:media) by run id. Full quota, early resumes.** Earlier: **RUNNING from 17:04 Oct 20 (resumed 6 minutes before the 17:10 reset to use the leftover, the user's rule): staging `wi3md4ajc` (run `wf_8be4c97d-b05`, review by area), media plan `wp083s24x` (run `wf_99fc5425-a7d`, Opus plan; lookup cached); autopilot `286754cb`, hourly `dbf1bb19`. Full quota every window, early resumes.** Earlier: **RUNNING from 15:38 Oct 20 (the user: "Continue the work and keep working in overnight mode fully using limits"): the window was at 80% from the user's own use (reset 17:10), so the media plan's Sonnet lookup finished and is cached (run `wf_99fc5425-a7d`); paused for usage at 16:14 (87%) before its Opus plan step so nothing is cut off; the one-shot `67d194cc` at 17:13 resumes staging `wf_8be4c97d-b05` (review by area next) and the media plan; autopilot `286754cb`; hourly cron re-created at 17:13. Full quota every window, day and night.** Earlier, superseded: **PAUSED by the user's choice at 11:34 Oct 20: the window that starts at the 12:10 reset is left to the user ("dont resume work in upcoming session do in next session automatically"); no autopilot or hourly cron runs in it. The one-shot `99619039` at 17:15 resumes in the next window (it re-checks the reset time first): re-create the crons, resume staging `wf_8be4c97d-b05` by run id (review by area next) and launch plan-media.js.** Earlier: **RUNNING again from 10:50 Oct 20 (the user: "Keep working, and also keep working across reset"; full quota every window day and night): staging `wsxuia79b` (review by area next), approved-work plans FINISHED 11:06 (`wqlqav2lz`), autopilot `3dd2d9c8`, hourly `b60de5ea`. After the 12:10 reset launch scripts/plan-media.js (REQUIREMENTS 53).** Earlier: **PAUSED by the user at 10:20 Oct 20; resume by hand ('start'): re-create the autopilot (13,33,53) and hourly (3 9-21) crons from SESSION-2026-10-18.md "Morning Oct 20" and resume by run id: staging `wf_8be4c97d-b05` (prepare, merge and both test halves done; next: the review by area, then fix, requirements matrix, report) and the approved-work plans `wf_b7cc5b7e-b81` (all five plans and the review done; the revisions of freeze and keyboard-slides restart). Full-quota rule applies after the resume.** Earlier: **RESUMED 07:09 Oct 20 (the user: "Start working"): staging `wcyijk728` (its desktop matrix and phone tests restart). Queued questions ANSWERED (REQUIREMENTS 51) from 07:09: Telegram proposals, overlay design, District sheets.** Earlier: RESUMED 02:12 Oct 20 after the reset (0%; full quota every window, pause started at 95%): staging `wyrlk47au`, District study `wg59r2au7`, Telegram research `w2k6av6je`, overlay design `wbfwarhe8`.** Earlier: PAUSED for usage at 01:09 Earlier: STAGING RUNNING from 23:01 (`w2qwbph6s` since 23:05) beside the District phone study (`wckk6ne7e`); the bug fix returned 23:00.** Earlier: RESUMED 21:12 after the reset (usage 0%; 99% windows, pause at 98%, until the 08:55 wind-down): bug fix `wf8cr139t` and coverage audit `w9iqzm0cp` running; `scripts/district-phone-study.js` is written and launches when the bug fix returns, beside consolidate-staging (whose Prepare and Merge do not use the phone); District proposals wait for its measured verdict.** Earlier: PAUSED for usage at 20:26 (96%). Earlier: RESUMED 19:46 (the user: "home"): bug fix `wf1qdqm5x` and District research `ww5u7q8fk` resumed by run id; autopilot `23f83142` (since Oct 20 01:12; full quota every window), hourly `42b4db7d` (now lists parked packages), heat log `bcq6a6rg5`; overnight non-stop until 09:00 Oct 20 (this window stops at 93%, resets 21:09; then 98%).** Earlier: PAUSED by the user at 18:55 (going home); resume by hand: when the user says 'home' or 'start', resume the bug fix (`scripts/bugfix-back-see-through.js`, run `wf_769cf06c-ba5`) and the District research (`scripts/research-district.js`, run `wf_b450657a-93b`) by run id, re-create the autopilot cron (13,33,53; prompt in SESSION-2026-10-18.md "Evening Oct 19") and the hourly status (3 9-21), and run non-stop overnight until 09:00 Oct 20 at the night cap (98%, pause at 94%). Bug fix stopped in `bugfix:capture-before` (combine done: `_preview` at `22992d3c` has WP04+T+C1, WP06, WP03, HUD; 178 held-frame shots, the WP04-alone cover scene was being captured). District research stopped in `review:research` (gather, verify and `compare:proposals` done; `research/district/RESEARCH.md`, `PHONE-STUDY.md`, `SOURCES.md` written, not yet reviewed).** Earlier: RESUMED 16:13 (window resets 21:09; day cap 80%): the user's bug fix was RUNNING (task `w6zziy5fp`, run `wf_769cf06c-ba5`; the District research `wijzh4v76` runs beside it); consolidate-staging waits for it.** Order: FIRST the user's bug (15:41, `bugs/BUG-2026-10-19-back-see-through.md`: the back card is see-through over the page beneath), alone: launch `scripts/bugfix-back-see-through.js`; when it returns, launch `scripts/consolidate-staging.js` (prepare, merge, desktop and phone tests, review by area, fix loop, report `staging/STAGING-REPORT.md`); do NOT resume `to-release.js`, `hud-first.js` or `catalog-preview.js` (superseded). Then push staging, send the user the report, APKs and screenshots, and wait for their go before any new work (REQUIREMENTS 44).
> **RUNNING from 13:56 on Oct 19 (the user: "continue work")** until the Android release: 80% by day (pause at 76%), 98% at night (pause at 94%), resuming 3 minutes after each reset. HUD-first `wdc7v6zzj`, release `w22w6sflc`, back gesture `wxw9694vf` (run ids in §1); autopilot `42dc454c` (reads `scripts/AUTOPILOT-PROMPT.md`), hourly status `b4cff7e6` (09:03-21:03), heat log `bkw83ewi0`. Crons die if the Claude app restarts: re-create them from this line. The laptop was on battery at 13:55. **CI-only PRs: both done.** #1 shows "merged" (GitHub auto-marked it; nothing merged through GitHub). #2 Linux CI all green at 15:55 (Vercel preview aside), closed unmerged; WIN's Linux fixes reach the integration line through staging.
> Previous: paused 09:52-10:45 by the user (going to the office); the start steps it listed were used at 10:45.
> Previous: PAUSED (Oct 19, 03:30) after the night run 01:30-03:30; see `SESSION-2026-10-18.md` "Night run Oct 19 01:30-03:30".
> **Oct 18, 19:10: idle until the user starts the next session by hand. Read `SESSION-2026-10-18.md` first (state, decisions, lessons, the restart steps).** Resume from `SESSION-2026-10-17.md` first. Only §1's run id can be resumed, and only from the session that launched it; a new session launches fresh.
> **Resume from `SESSION-2026-10-16.md` first** (latest: power-offs, thermal sensor), then `SESSION-2026-10-15.md` (its Oct 16 sections at the end), then `SESSION-2026-10-14.md`. Fable's findings: `FABLE-FINDINGS.md`. **Every agent runs on Opus 5.5 only** (the user, Oct 15 20:55).
> Previously: `SESSION-2026-10-14.md` (latest; then `SESSION-2026-10-13.md` for background).
> Older note: `SESSION-2026-10-13.md` (written 16:58 Oct 13): what was achieved on Oct 12–13, resume steps, the order of what comes next.

## 0. Since Oct 12, 14:50

The laptop rebooted at 14:57 on Oct 12 and again at 10:41 on Oct 13, then slept with the lid closed from 11:12 to 13:17.
The 18:03 one-shot never fired, so nothing ran between Oct 12, 14:50 and Oct 13, 13:20. Every package's commits are
intact.

**PAUSED until the user starts the next session by hand (Oct 18, about 16:55: "fix it right now only. i will begin next session manually u dont auto start it").** No cron or one-shot exists: nothing restarts by itself. The three workflows stopped at 16:09 (release `wwu5szppg`, smoothness `w51p1v05s`, HUD-first `wpz7r0agd`; same run ids as below). The user's bug (`bugs/BUG-2026-10-18-scroll-direction.md`: carousel and list moved the opposite way) is FIXED on `wp/WP03` (`daafdbd` + `91cb638`: the rubber band's drag-back sign, the fling leftover, and the throw back from a stretched band), review clean, verified on the phone (task `womkdvrqs`, run `wf_f47e153f-a84`, done 19:00). `wp/WP05-glass` still carries the buggy `AppOverscroll.kt` and needs both commits before any phone run (its handoff says so). WP03's `accept.sh` has not been re-run for these commits. The user has the catalog APK (`out/share/android-base-catalog-scrollfix-91cb638.apk`). When the user starts the next session: resume the three workflows (§1's commands), then re-create the autopilot and the hourly status (`SESSION-2026-10-17.md` has both prompts; the caps are in memory `usage-reserve-10-percent`).

## 1. Running (Oct 19, from 13:56)

| Workflow | Task id | Run id | Script | Notes |
|---|---|---|---|---|
| Release (all packages) | `w22w6sflc` (stopped 15:44, on hold for staging; history `wevvu4dq7`, history `wi1ehb82a`, history `wrghh6gof`, `wwu5szppg`, `wf32czekj`, `w4f2f2lwb`, `whd05nhmo`, `wg14qe5jm`) | `wf_dca0df06-58a` | `D:/projects/tools/.overhaul/scripts/to-release.js` | Night run: WIN's follow-up (three commits: `python -m tests.consistency` runs on Windows) reviewed APPROVE and merged into the integration branch (now `4dd3aca`; tag `merged/WIN` stays at `5a16778`). **WP04** run 27 fixed review #1's 12 blocking findings in 7 commits (`39a6546`..`a6ba578`); `fix#1` was in flight at the stop; its untracked `HostCrossfadeTest.kt` ran at 03:55 after the stop: the Back-during-crossfade case FAILED again (`out/WP04/logs/r27-test5.log`, HostCrossfadeTest.kt:74), which by the handoff's own plan points to a real priority bug in `HostCrossfade`'s swallow; next run: fix that, commit, the full `accept.win.sh`, review #2. **WP03** `1dc5714`: found that every preset without pickers or collections failed `./gradlew build` (two design-system tests imported optional components), fixed; `accept.sh` ACCEPT OK 03:19; iOS fling within 2.5% of iOS distance on the phone; waits for WP04. **WP02-M2** 4 commits (integration merged, Android skills v1.0.15 vendored, rules point to skills); waits for WP04, WP06. WP05, WP06, WP07-A, WP08 waiting on WP04 (no change). Next merge: WP04, then WP03, WP06 |
| **HUD-first (the user's priority)** | `wdc7v6zzj` (stopped 15:44, on hold for staging; history `wo0on6b8p`, history `w1svw7zt2`, history `w0cpqe059`, `wpz7r0agd`) | `wf_bb3d445a-ed0` | `D:/projects/tools/.overhaul/scripts/hud-first.js` | `HUD:run` (run 2) was in progress at the stop, so a resume restarts it from `handoff/HUD.md`. Night run: dev tooling moved out of prod (`16cff222`: `debugImplementation`/`devImplementation`, a `verify<Variant>CarriesNoDevTooling` guard), sampler stops in the background, the pill redraws only inside frames the app draws anyway (`e64f1948`; the first overhead measurement showed +1.3 points of jank from the pill's own redraw loop), JankStats route comment corrected; head `3fe54d39`. Its acceptance builds finished BUILDS OK at 03:55 (`out/HUD/logs/r2-acc.log`). Still to do: the overhead re-measure, review loop, then the phone demo (`perf/HUD/SHOW.md`) |
| **Back gesture, Google's way** (the user, 06:25): **FINISHED Oct 19 14:45** | `wxw9694vf` (done) | `wf_9aadb368-d4b` | `D:/projects/tools/.overhaul/scripts/back-gesture-google.js` | Done; do not relaunch. Verdict: ship T + C1 (`out/WP04-perf/backcard-texture.patch` then `out/WP04-perf/back-google/c1.patch`); C2 (prewarm) not shipped. Hitches 38 -> 15, late frames 22 -> 7, no lagging frame, nothing worse. WP04 told to apply it (note 14:50). Release hitch row still red (6.91 vs 5.0): next the scrim fold (DESIGN §6); the user decides the limit only if still red after that |
| **WIN Linux fix**: **FINISHED Oct 19 15:28, review clean** | `wlfzdl167` (done) | `wf_dd10b7cd-8fb` | `D:/projects/tools/.overhaul/scripts/win-linux-fix.js` | Done: `a5db7b0` (the --git repo's scripts executable on disk as in the commit), `070a91b` (feature modules scaffolded against the template's current APIs; ScaffoldTest fails on the old code). 307 generator tests OK. Pushed as wp/WIN; PR #2 runs Linux CI; merge after it is green (AUTOPILOT step 9). At WP02-M2's merge take WP02's scaffold.py whole and keep the new test (followups.md) |
| **Catalog preview** (the user, 15:40): STOPPED 15:44, superseded | `wz6f3yfv0` (stopped) | `wf_df0155b8-4f2` | `D:/projects/tools/.overhaul/scripts/catalog-preview.js` | Do not resume: the bug run finishes its combined tree (`wt/_preview`, local branch `preview/catalog-2026-10-19`) to reproduce the user's bug, and staging replaces the preview. Its notes: `out/preview/NOTES.md` |
| **The user's bug: see-through back card** (15:41) | **RETURNED 23:00 as blocked-on-smoothness** `wf8cr139t` (history `wf1qdqm5x`): the see-through card exists only on wp/HUD's old Navigation 3 crossfade pop (the user's app); the newest code shows one opaque card in all 105 proof frames; tests added on wp/WP04 (1ec5d90d, 6c6ee608; merged into the preview at 722ced37). The proof found the combined tree's back gesture regressed against T + C1 (hitch 12.7 vs 6.9 ms/s, jank 1.2-1.5 vs 0.3-0.4 %, A/B controlled); handed to consolidate-staging as a blocking known issue (first suspect: the HUD in the measured devBenchmarkRelease). The review loop did not run (blocked); staging's combined review covers the test commits (history `w6zziy5fp`, `wttpq00cp`); combine done | `wf_769cf06c-ba5` | `D:/projects/tools/.overhaul/scripts/bugfix-back-see-through.js` | Split at 16:13 per the model routing: Opus combines the tree (WP03 and HUD into `_preview`) and writes `bugs/2026-10-19-back-licenses/CAPTURE-PROCEDURE.md`; a Sonnet operator captures three builds (combined, WP04 alone, HUD alone); Opus verifies the frames, root-causes and fixes on the owning branch with tests; Sonnet captures the proof and pushpop_gesture; an Opus verifier writes the Result; Opus review loop (up to 3). Pre-split script: scratchpad `bugfix-back-see-through.pre-split.js`. The user's phone: all 23 test apps uninstalled at 15:45 (`bugs/2026-10-19-back-licenses/uninstalled.txt`) |
| **District research** (the user, 17:00; REQUIREMENTS 45) | **FINISHED 20:24** `ww5u7q8fk`; review found 5 blocking, all fixed in the revision; `research/district/RESEARCH.md` final | `wf_b450657a-93b` | `D:/projects/tools/.overhaul/scripts/research-district.js` | Read-only, no phone, runs beside the bug fix. Sonnet lookups, Opus verifier, Opus gap analysis and proposals, Opus review. Output `research/district/RESEARCH.md` + `PHONE-STUDY.md`. When it returns: send the user the summary and ask each proposal in chat (pros, cons, catches); the phone study waits for the phone and the user's install and sign-in |
| **Coverage audit** (the user, 19:58: "anything else u havent seen or read yet from the handover?") | **FINISHED 22:03** `w9iqzm0cp` (history `w888r27ls`); 227 items: 52 merged, 42 on branches, 11 in progress, 33 waiting, 57 not started, 15 forgotten, 17 superseded; review's 6 blocking fixed in revision 2; forgotten items routed into consolidate-staging.js (H-4/H-5 security blocking in its review; matrix and report route the rest); 11 user decisions never asked | `wf_5776dd78-2ba` | `D:/projects/tools/.overhaul/scripts/coverage-audit.js` | Read-only. Sonnet inventories of the handover, requirements, plan and today's notes; Opus audit against branches and handoffs; Opus review. Output `audit/2026-10-19-coverage/COVERAGE-AUDIT.md`. When it returns: send the user the plain-English part, ask any open user decisions in chat, and fold forgotten items into staging or the staging-wp plan |
| **District phone study** (the user, 20:27 and 20:30; REQUIREMENTS 45) | **FINISHED 03:41, INCONCLUSIVE** `wg59r2au7` (history `wckk6ne7e`): the template side is fully measured (catalog 1.35 % janky, sample app 1.39 %; sheets critically damped k about 342, t90 211-217 ms; 18 of 18 list-sheet opens miss their first frame on measure/layout 10-16 ms, which is P3); District's side stopped at a failed catalog pre-flight at Large (PHONE-STUDY §9; the safe_target fix is in), so only District's whole-session counters exist (7.21 % janky, four 180-225 ms stalls, but an uncompiled first launch with live content). TO DO: re-run District's half once Android has compiled it (read its compilation filter; never force it) and the phone is free after staging's phone tests; ask the user in the morning which District sheets they love (PHONE-STUDY §2.1 question 1) | `wf_582f1d08-2d1` | `D:/projects/tools/.overhaul/scripts/district-phone-study.js` | Sonnet operator runs `research/district/PHONE-STUDY.md` on District and the newest combined build (both compiled), Opus analysis and verdict (is the template ahead on the Redmi), Opus review. Output `research/district/PHONE-RESULTS.md`; `research/district/phone/` stays local (backup excluded). Then ask the user only the proposals the numbers support |
| **Plans for the approved work** (REQUIREMENTS 51; full-quota rule) | **FINISHED 11:06** `wqlqav2lz`: plans/2026-10-20/ freeze, lists-images, keyboard-slides, sheet-picture-trial, overlay; the review's 4 blocking fixed in the revisions; build order: (1) HUD out of the measured builds first, (2) WP12's docs base in staging before any item, (3) inert seams from WP03, (4) the items, each merged with its docs | `wf_b7cc5b7e-b81` | `D:/projects/tools/.overhaul/scripts/plan-approved-work.js` | Read-only. Five Opus planners (freeze P1/P2/P4; lists and images P3/P7/P9 + District P3; keyboard and slides P5/P8/P10/P11; the P6 sheet picture trial; the overlay metrics) then one Opus reviewer for conflicts and build order. Output `plans/2026-10-20/` |
| **Smoothness docs draft** (REQUIREMENTS 50; full-quota rule) | **FINISHED 06:18** `whjq6sejm`: docs-draft/PERFORMANCE.md, AGENTS-rules.md, PENDING.md (review's 6 blocking fixed; every cited name checked on staging); its code gaps (a lost Play Store cold-start fix, mid-slide loading, fonts preload not wired, stale comments) noted in staging/MERGE-NOTES.md for staging's review | `wf_601c27b2-e33` | `D:/projects/tools/.overhaul/scripts/docs-smoothness-draft.js` | Read-only, outside the repo: Sonnet inventory of every docs rule, gotcha and lesson; Opus drafts `docs-draft/PERFORMANCE.md`, `AGENTS-rules.md`, `PENDING.md`; Opus review. WP12 commits it on staging-wp after the user's go |
| **Overlay metrics design** (the user, 00:33; REQUIREMENTS 49) | **FINISHED 03:42** `wbfwarhe8` (history `wr0iwj48e`): DESIGN.md, 18 metrics (recomposition mid-motion score, per-frame recomposition and composition findings, idle recomposition, a 10 s Capture by composable and line, janky after the first frame, late first frames, jank cause split, skipped vsyncs, allocation per frame, blocking GC, thermal headroom, exit reason and more), review's 5 blocking fixed; 3 questions for the user (what the pill shows, recomposition in the score, the DEV badge if it costs frames): ASK IN THE MORNING. Key finding for staging: the baseline-profile plugin copies devRelease's implementation dependencies into the measured builds, so the HUD must be wired devReleaseRuntimeOnly | `wf_e7d1fe72-515` | `D:/projects/tools/.overhaul/scripts/hud-metrics-design.js` | Read-only. Recomposition score, 'not optimised' flags, other cheap signals, a cost budget. Output `research/hud-metrics/DESIGN.md`; ask the user its questions; build on staging-wp/HUD after staging |
| **Telegram research** (the user, 23:08 and 23:14: Telegram only; REQUIREMENTS 48) | **RUNNING** (from 23:13; Telegram only and optimisation first since 23:17) FINISHED 02:35 `w2k6av6je` (history `wem24fecd`, `wff9nzrnt`, `wi6akxpon`); revision 2 fixed the review's 5 blocking; 11 proposals ready (P1 hold decorative motion while moving, P2 hold data until landed, P3 list-cheap images, P4 wait for first content, P5 keyboard one-layout, P6 sheets from a saved picture (rule exception), P7 fetch further ahead, P8 cap the time step, P9 docs, P10 swipe back anywhere, P11 quick push); ASK THEM WHEN THE USER IS AWAKE (not overnight: a pending question blocks the crons); source recycled | `wf_f7a0009c-016` | `D:/projects/tools/.overhaul/scripts/research-telegram.js` | Read-only (source in research/telegram/src, excluded from backups). Sonnet inventories, Opus verify, compare, review. Output `research/telegram/RESEARCH.md` + `PHONE-STUDY.md`; then ask the proposals one by one; recycle the source afterwards |
| **STAGING** (REQUIREMENTS 44, 46, 47) | **RUNNING** (from 23:01; restarted 23:05 by resume after adding the merge-regression hunt) PAUSED 06:20 `wyrlk47au` (history `w2qwbph6s`, `w87emhkll`) | `wf_8be4c97d-b05` | `D:/projects/tools/.overhaul/scripts/consolidate-staging.js` | Prepare, merge (MIT LICENSE, npm security upgrades), desktop and phone tests (Sonnet runs, Opus verifiers; glass bar captured), review by area (H-4/H-5 security blocking), fix loop (the back-gesture regression blocking), requirements matrix, report. When it returns: secret-scan, push staging and staging-wp/*, send the report, APKs and screenshots, ask before new work |
| Deep smoothness: **FINISHED Oct 19 03:02 (state partial)** | `wmoxduc84` (done; history `w51p1v05s`, `w7quli9q3`, `wnpv3c78t`) | `wf_04982e22-7b2` | `D:/projects/tools/.overhaul/scripts/deep-smoothness-fable.js` | Done; do not relaunch. Commits on `wp/WP04-perf`: `2b91187f` (perf.py screenshots and videos go to /data/local/tmp: /sdcard woke Google Photos, which added 33-37 ms GPU frames), `392020f9` (merged `wp/WP04`). First-fling hitch root-caused (a CircularRRectOp program compiled on the first drag; WP03's AppSurface removes it; re-check after WP03 merges). **Decided by the user Oct 19 06:16 (REQUIREMENTS 42): adopt** `out/WP04-perf/backcard-texture.patch`, and gate what users feel; WP04 applies both. (back gesture card drawn from a texture: RenderThread hitches 38 -> 2, janky-after-first 0.8-0.93% -> 0.4-0.53%; costs about one vsync of follow latency and about 10 MB GPU while up; an exception to A11.3 rule 1; the agent recommends adopting). HEAD: cold start 337/370 ms passes; still failing: tabs 1.15% (first frame, device limit), push/pop p99 26 ms, gesture 2.39%, scroll bar-minimizing 0.51%, app PSS 84 MB vs 48. Full notes: `handoff/WP04-perf.md` (Oct 19 summary at the top) |

- **One-window cap (the user, 15:15):** the window that resets at 20:10 stops at 45% (the user needs usage for their own work); the
  autopilot stops at 41% or more and adds 7-minute checks from 28%. Normal caps return after 20:10.
- **Oct 19, 01:30-03:30 run (over; autopilot deleted 03:30):** autopilot cron `3097bde4` (`13,33,53`; the prompt of `scripts/AUTOPILOT-PROMPT.md` plus "ends at 03:30,
  no resume after 03:30"), no hourly status (the user is asleep), heat log background task `b15jma22i`, stop one-shot `2d762ea0`
  at 03:30. Preflight 01:28: usage 5% (resets 06:10), phone `9a7c74f4` attached, on mains at 99%, both locks free, free RAM
  about 1.3 GB (the locks' memory wait holds builds until there is room). Weekly Fable limit at 73%.
- **Oct 18 (history):** autopilot cron `d9c23bb2` (replaced 15:15; three workflows; usage caps 90% 09:00-21:00 IST, 99% at night, stopping 5 points early;
  one-shot resume after each reset; hung-build kill; battery warning; backups after merges). **Hourly status:** cron
  `02f44fa3` (about :03). **Heat log:** background task `b7s05gauq`.
- **New requirements (Oct 18, 14:00):** 40 (live FPS and performance stats beside the DEV badge in dev and debug builds, a
  performance log and a 0-100 optimisation score; package HUD after WP06 and WP04; WP12 and WP13 wait for it) and 41
  (iOS scrolling by default everywhere; WP03 physics, WP04-perf jank and the first-fling hitch, QA2 feel check). The
  script change (HUD, QA2, WP12, WP13 scopes) takes effect at the next resume; WP03 and WP04-perf read item 41 on their next run.
- **After the 10:05 stop** (notes at the top of the WP04, WP03 and WP04-perf handoffs): WP04 `r25-accept` ended
  `FINAL-GATES OK` (10:41) and `RECORD-R25 DONE`; its landscape run and WP03's `r18_phone.sh` never got the phone (the app
  restarted at about 10:41). Smoothness phases 10 and 11 ended DONE.

### The 10:05 pause (history)

## 1. PAUSED (Oct 18, 10:05) by the user

The user (08:22): "Save progress properly and stop the work after 95 mins" (moved to 10:05; over the 90% cap allowed
this once). At 10:05 both workflows were stopped and every schedule removed (autopilot, resume one-shots): nothing
relaunches by itself. Usage was about 85% of the window that resets at 10:10.

| Workflow | Last task id | Run id | Script | In flight at the stop |
|---|---|---|---|---|
| Release (all packages) | `wg14qe5jm` (task history: `wf32czekj`, `w4f2f2lwb`, `whd05nhmo`, `wg14qe5jm`) | `wf_dca0df06-58a` | `D:/projects/tools/.overhaul/scripts/to-release.js` | WP04 run 25 (final phone gates, REQ 34 recordings), WP03 run (sheet fixes, phone checks), WP05 run. Waiting with results recorded: WP06 (accepted, merges after WP04 and WP03), WP02-M2, WP07-A, WP08 |
| Deep smoothness | `wnpv3c78t` (first `w7quli9q3`) | `wf_04982e22-7b2` | `D:/projects/tools/.overhaul/scripts/deep-smoothness-fable.js` | The deep-work agent (round 1 measured on the phone; proving the first-fling scroll hitch after `pm clear`) |

**To resume:**
- In this orchestrator session (`8d80ef9d`): `Workflow({scriptPath: "D:/projects/tools/.overhaul/scripts/to-release.js",
  resumeFromRunId: "wf_dca0df06-58a"})` and `Workflow({scriptPath: "D:/projects/tools/.overhaul/scripts/deep-smoothness-fable.js",
  resumeFromRunId: "wf_04982e22-7b2"})`. Completed calls replay from cache; the interrupted ones re-run from their handoffs.
- In a new session: launch both scripts fresh (no `resumeFromRunId`).
- Then re-create the autopilot (`CronList` first; `13,33,53 * * * *`) from `scripts/AUTOPILOT-PROMPT.md` plus the rules
  used on Oct 17-18: apply every step to both workflows; usage caps 90% of the 5-hour window during 09:00-21:00 IST (and
  before 09:00 for a window that resets after 09:00), up to 99% at night (stop at ~97%); at a cap, stop the workflows and
  schedule a one-shot resume 3 minutes after the reset; kill a build that holds the Gradle lock over 75 minutes with the
  CPU under 15% for 30 minutes and note it in the owning handoff; restart `bin/thermlog` if its log goes stale; back up
  after merges. Optional: the memory watch (Monitor running the session scratchpad's `memwatch.py`; it expires every 30
  minutes) and the morning summary one-shot.

**Phone jobs still finishing after the stop (10:05), queued on the adb lock:** WP04 `record-r25.win.sh` and
`r25-landscape.sh` (`perf/WP04/run25-*`), the smoothness branch's install + `:benchmark:connectedBenchmarkReleaseAndroidTest`
run, WP03 `out/WP03/scripts/r18_phone.sh`. A background watcher records each result in the owning handoff when they end.

- **Worktrees at 10:05 (all clean):** WIN `5a16778b` (merged), WP02 `13eb1f14` (44 ahead), WP03 `0ac820a2` (100), WP04
  `f60c0c7f` (108), WP04-perf `e2fdb081` (110), WP05 `dee21ca5` (18), WP06 `7b14c0d2` (40), WP07 `4ef0ba71` (18),
  WP08 `e5bb8cfe` (66).
- **Merged:** WP01, WP02-M1, WP09, WP10, **WIN (Oct 18 01:40, `5a16778`)**.
- **Backups:** branches and `overhaul-workspace` pushed at 08:30 and again in this wind-down.
- **Decisions in force:** REQUIREMENTS 39 (Cronet) and Brotli-by-default in the Android release (package NET, groundwork
  `research/NET-CRONET-BROTLI-NOTES.md`); usage caps by time of day (memory `usage-reserve-10-percent`).

### Historical (Linux, Oct 12): the release workflow's first launch; these ids are dead, do not resume them

One workflow now carries everything to a release candidate: `scripts/to-release.js` (session copy:
`/home/crazyhusir/.claude/projects/-home-crazyhusir-Documents-projects-tools--overhaul/e3f9060d-6364-4a81-a9a2-20851e25521f/workflows/scripts/android-base-to-release-wf_5f9024db-13d.js`).

| Task id | Run id | What |
|---|---|---|
| `wc21h1pgu` | `wf_5f9024db-13d` | All 17 remaining packages with dependency waits, review loops (4 rounds, then leftovers to `followups.md` for WP13), serialized merges, the CP2 decision, QA round 2, `materialbridge` (MB), WP12 docs and WP13 verification |

Side workflows (also resumable with `resumeFromRunId`):
- Bottom sheet design, `w11n7i2ac` / `wf_b121894a-047` (REQUIREMENTS 33): writes `research/SHEET-SPEC.md`.
- Shimmer and clipping fix, `wmxwnoqok` / `wf_a32dd290-a8c` (REQUIREMENTS 34): writes `qa/shimmer/` and the side branch `wp/WP03-skel`.

- **Resume after a usage cutoff:** `Workflow({scriptPath: <session copy above>, resumeFromRunId: <last run id>})`. Completed
  agents replay from cache; prompts are deterministic and every implementation prompt resumes from the handoff and git
  state. A new session cannot resume old run ids: launch `scripts/to-release.js` fresh instead.
- **Concurrency:** at most 6 agents at once (8 CPUs, the workflow cap is CPUs − 2). The Gradle lock serialises builds
  anyway.
- **Release:** the workflow stops before the release actions. When it returns `releaseCandidate: true`, the orchestrator
  reads `report/FINAL.md`, merges into `main` and pushes (the user approved this at the start), which deploys to Vercel.
- **Autopilot:** cron `95151bcb` at :13, :33 and :53 (session-only; re-create it from `scripts/AUTOPILOT-PROMPT.md`
  after a restart, and run `CronList` first so there is only one).
- **Memory monitor:** task `bnieph9zk` (under 1.2 GB). It expires after 30 min; re-arm it if needed.
- **`bin/memwait`:** since Oct 12 it needs 3.5 GB only on a cold start; with a Gradle daemon already resident (which the
  build reuses) it needs 1.5 GB. The old version is `bin/memwait.bak`.
- **Old workflows:** `waves-0-1g.js` and `wave-2-early-c.js` are retired, and both ended at the Oct 10 16:40 cutoff.
- **Journals:** `~/.claude/projects/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/subagents/workflows/<run id>/journal.jsonl`.
- **The lid:** closing it suspends the laptop and stops everything (Oct 10 18:06 to Oct 12 13:06 was lost that way).

## 2. Package state at 16:04

| Package | Branch, worktree `wt/<x>` | Commits ahead / dirty | State |
|---|---|---|---|
| WP00 | merged | | `6bc38e9` repository move and `Chrome.kt` |
| WP01 | `merged/WP01` (`709612d`) | | Generator engine v2, spec and catalogue v2, tokens, 249 tests |
| WP02-M1 | `merged/WP02-M1` (`54d078d`) | | Pure-Kotlin core, `:core:mvi`, `api/` packages |
| WP09 | `merged/WP09` (`8672003`) | | API per target, precheck, telemetry v2 |
| **WP10** website | `wp/WP10` | 48 / 0 | Accepted (188/188 tests, `next build`, click-through). Review findings being fixed, then merge |
| **WP04** navigation, shell, perf | `wp/WP04` | 56 / 4 | Phase 2 done. Phase 3 running on the phone: recordings in `perf/WP04/{phase3a-frames,tab-switch,back-during-push,predictive-back}`, recomposition counting, profiles. Remaining: tab-switch effect (REQ 28), Gmail back (24), back during push (32), pill (30), gates, baseline profiles |
| **WP03** design system | `wp/WP03` | 38 / 5 | Phase A done, plus the large-title search accessory. Remaining: call-site sweep, phase B (CP1 tokens), pull to refresh (REQ 29; first "after" recording was 16.8% janky and the title overlapped content, both routed in `handoff/WP03.md`), motion items, adaptive components, QA defects |
| **WP02-M2** architecture, build | `wp/WP02` | 31 / 0 | Modular `:api`/`:impl` generation, code rules, Kotlin 2.4.21 and other bumps, vendored Android skills (they appear as scoped skills in the worktree), R8 analyzer docs. Database scaffolding waits for WP06 |
| **WP06** data, network, security | `wp/WP06` | 19 / 0 | `:core:network` rebuilt; the uncommitted work is now committed. Remaining: the rest of the package |
| **WP08** SwiftUI | `wp/WP08` | 20 / 0 | In progress |
| **WP05-early** glass foundation | `wp/WP05` | 1 / 4 | Kyant behind `<opt:liquidglass>`; the foundation is in progress |
| **WP07-A** Compose Multiplatform | `wp/WP07` | 8 / 14 | The generator target is generated, in beta. Foundation in progress. The shell port waits for WP04 |
| **WP11-early** PeriFlow | PeriFlow `wt/WP11`, local `overhaul/smooth-glass` | 2 / 45 | perf tools wired to PeriFlow. Baseline and PeriFlow-only fixes in progress. **Never push** |

Not started: WP02-M3, WP07-B and WP07-C, WP12 docs, WP13 verification and release, WP14 tools, the optional
`materialbridge`.

**Merge order from here:**
1. WP10
2. WP04
3. WP03
4. WP06
5. WP02-M2
6. WP08
7. WP05 (finish after WP03 and WP04; CP2 glass decision)
8. WP07-A, then B and C
9. WP02-M3
10. WP14
11. WP11 (local only)
12. WP12
13. WP13 (full matrix, final phone gates, before/after report)
14. Merge to `main`

## 3. Decisions already made (do not reopen)

- **Design system:** custom and foundation-only. No Material components. `androidx.compose.material3.adaptive:*` is
  allowed (verified to have no material3 dependency). No Material "look" style. `materialbridge` is optional and last
  (REQ 31).
- **CP1 motion** (`decisions/CP1.md`):
  - push/pop at 0.344 s with no bounce, parallax 0.333;
  - tab switch: a 12 dp nudge with a background veil, an indicator moving on two edges, the icon dipping to 0.84 and
    springing back;
  - bar labels always shown, minimize on scroll;
  - BackGate releases at 99%.
- **Architecture:** compact by default, modular as an option. Koin for Compose Multiplatform; JetBrains Navigation 3
  1.1.2. SwiftUI uses `@Observable` MVI, iOS 17, XcodeGen. Swift is "coming soon" on the site; Compose Multiplatform
  is "Beta: iOS unverified".
- **Telemetry:** anonymous only.
- **Google's Android skills** are installed as a Claude Code plugin (`android-skills@android-skills`). Digest:
  `research/ANDROID-SKILLS-DIGEST.md`. Generated projects vendor the relevant ones.
- **Phone:** the Redmi `9a7c74f4`. `screenrecord` needs `--size 720x1600`; full resolution fails on this encoder.

## 4. Phone numbers so far (Redmi, 120 Hz, release builds)

| Metric | Old template | New build, phase 2 | Target |
|---|---|---|---|
| Rapid back lands correctly | no (exits) | **yes** | yes |
| Rapid back, janky | 5.32% | 2.62% | ≤ 1% |
| Tab switch first frame, median / max | 19.0 / 45.3 ms | **6.5 / 16.5 ms** | ≤ 8 / 8.3 ms |
| Tab switch, janky | 1.58% | 1.9% (worse) | ~0% |
| Push/pop with the Back key, janky / p99 | 4.03% / 34 ms | 2.34% / 57 ms (p99 worse: cold pop) | ~0–0.5% / ≤ 16 ms |
| Pop first frame, median / max | 19.1 / 27.6 ms | 15.0 / 36.9 ms | ≤ 8 / 16 ms |
| Back gesture push/pop, janky | 3.54% | 2.82% | ~0% |
| Scroll janky / GPU p90 | 0.22% / 6 ms | 0.15% / 6 ms | ≤ 0.5% / ≤ 7 ms |
| Cold start | 336 ms | 355 ms (no baseline profile yet) | below 336 ms |
| PSS | 109.8 MB | 93.8 MB | ≤ 70 MB |
| New pull to refresh | — | 0.54% (light) / 0.67% (dark) janky, GPU p90 6 ms (`perf/WP03/ptr/measure-head`; the first rebuild measured 16.8% / 8.2%) | ≤ 1% |

Phase-3 gated numbers are pending. On whether iOS level is possible: perceptually yes. Push/pop realistically land at
0–1% at 120 Hz, using warm previous screens, baseline profiles and an animation clock started after the first frame.
Compose animations run on the main thread, unlike Core Animation, so main-thread discipline is mandatory.

## 5. The laptop freeze (Oct 10, 15:23) and the memory guard

- **Diagnosis:** no panic, OOM or thermal entries in the logs. The log stops after a silent gap: the machine froze
  under memory pressure, with zram swap at swappiness 180 and no OOM daemon. It came about 3 minutes after 9 agents
  launched builds. The 12:07 shutdown earlier was clean.
- **Fix in place:**
  - `bin/locked gradle` runs `bin/memwait` first (waits for ≥ 3.5 GB free), caps Gradle at 4 workers, and sets the
    daemon idle timeout to 15 min.
  - `REQUIREMENTS.md` "Memory safety": every heavy non-Gradle command runs through `bin/memwait`.
  - PeriFlow builds pass the template's daemon arguments, so they share the one daemon.
  - The original wrapper is saved as `bin/locked.bak`.
- **The user's own action, recommended:** `sudo apt install earlyoom`, plus the `/etc/default/earlyoom` line given in
  chat, which prefers java and avoids the desktop and Claude.

## 6. Resuming after a restart

1. Check the state:
   - `git -C <repo> tag -l 'merged/*'`
   - each `wt/<x>`: commits ahead, dirty files
   - `adb devices`
   - `free -m`
   - the usage window, with `mcp__ccd_session_mgmt__get_usage`
2. Relaunch `scripts/waves-0-1g.js` and `scripts/wave-2-early-c.js`, adapted to the state:
   - pre-fire newly merged tags;
   - review mode for accepted packages;
   - resume mode otherwise;
   - remove packages that have merged.

   New sessions cannot resume old run ids, so launch fresh.
3. Re-create the autopilot cron at `53 * * * *` with the prompt kept in `scripts/AUTOPILOT-PROMPT.md`.
4. When WP04 and WP03 merge: finish WP05 and WP07-A (remove the "waiting" scope) and add the merge step for them; then
   wave 2 (WP02-M3), wave 3 (WP07-B and WP07-C, WP11 second half, WP12, WP14) and wave 4 (WP13), using the same
   pattern: notes/INDEX, dependency waits, review loop, serialized merges.

## 7. ETA (at 16:05)

| Milestone | Estimate |
|---|---|
| WP10 merged | ~16:30 |
| WP04 merged | Tonight |
| WP03, WP02-M2 and WP08 merged | Tonight to tomorrow morning |
| WP06 merged | Tomorrow morning |
| Wave 2 | Oct 11 |
| Wave 3 | Oct 11–12 |
| Wave 4 and release | **Oct 12–13** |

These assume the laptop stays up and the autopilot uses every usage window. Each freeze or app restart ends the
session, and work only resumes after a "continue".
