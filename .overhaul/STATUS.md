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

> **Oct 17, 23:05: running overnight (§1); the user is asleep.** Resume from `SESSION-2026-10-17.md` first. Only §1's run id can be resumed, and only from the session that launched it; a new session launches fresh.
> **Resume from `SESSION-2026-10-16.md` first** (latest: power-offs, thermal sensor), then `SESSION-2026-10-15.md` (its Oct 16 sections at the end), then `SESSION-2026-10-14.md`. Fable's findings: `FABLE-FINDINGS.md`. **Every agent runs on Opus 5.5 only** (the user, Oct 15 20:55).
> Previously: `SESSION-2026-10-14.md` (latest; then `SESSION-2026-10-13.md` for background).
> Older note: `SESSION-2026-10-13.md` (written 16:58 Oct 13): what was achieved on Oct 12–13, resume steps, the order of what comes next.

## 0. Since Oct 12, 14:50

The laptop rebooted at 14:57 on Oct 12 and again at 10:41 on Oct 13, then slept with the lid closed from 11:12 to 13:17.
The 18:03 one-shot never fired, so nothing ran between Oct 12, 14:50 and Oct 13, 13:20. Every package's commits are
intact.

**Resumed 05:13 on Oct 18 after the usage pause (03:00-05:13): release `wg14qe5jm`, smoothness `wnpv3c78t`. This window (05:10-10:10) runs past 09:00, so its cap is 90%.**

## 1. Running overnight (resumed Oct 17, 23:05 IST; the user: "continue and keep working through the night i will sleep")

| Workflow | Task id | Run id | Script | Notes |
|---|---|---|---|---|
| Release (all packages) | `wg14qe5jm` (resumed Oct 18 05:13; before: `whd05nhmo` 23:05; before: `wf32czekj` 14:15, `w4f2f2lwb` 14:37, paused 16:42) | `wf_dca0df06-58a` | `D:/projects/tools/.overhaul/scripts/to-release.js` | Resume replays the completed calls (WIN run and fixes 1-2, the first runs of WP08, WP02-M2, WP07-A); WIN goes to review round 3, then the first merge. NET (Cronet + Brotli) waits for WP06 and WP02-M2 |
| Deep smoothness | `wnpv3c78t` (resumed Oct 18 05:13; first `w7quli9q3`, launched Oct 17, 23:58: the user asked to use the window's last ~50% before the 00:10 reset) | `wf_04982e22-7b2` | `D:/projects/tools/.overhaul/scripts/deep-smoothness-fable.js` | One deep-work agent on `wp/WP04-perf` (Opus; Fable is off in the script): round 1's pending measurement first, then root causes and fixes, Opus review loop; WP04 merges the branch. Shares the phone and the Gradle lock with the release run |

- **Resume after a cutoff (this session only):** `Workflow({scriptPath, resumeFromRunId})` with the values above. A new
  session launches the script fresh.
- **Autopilot:** cron `b41aeb4f` (handles both workflows) at :13, :33 and :53 (session-only, expires Oct 24). Usage caps (the user, 23:10): 90% of the
  5-hour window during 09:00-21:00 IST, up to 99% overnight (stops at ~97%); at the cap it pauses and schedules a one-shot
  resume 3 minutes after the reset; kill a hung build (gradle lock held > 75 min with
  the CPU under 15% for 30 min) and note it in the owning handoff, back up after merges.
- **Cronet/Brotli groundwork for NET:** done 00:25: `research/NET-CRONET-BROTLI-NOTES.md` (50 KB), linked from REQUIREMENTS 39.
- **Morning summary:** one-shot cron `138d1d8c` at 07:57 on Oct 18.
- **Heat and memory history:** `bin/thermlog` as background task `b7nanas02` (`logs/thermal-windows.log`); the hung-build
  check reads it. No memory Monitor overnight (its 30-minute re-arms would cost usage; `locked` waits for memory before
  every build).
- **The afternoon's hang:** WP06's acceptance build stalled at 16:20 with an idle CPU and held the Gradle lock until about
  18:45, so WP04's phone step and the queued WP03 and WP05 builds never ran. Notes are at the top of the WP06, WP04, WP03,
  WP05 and WIN handoffs.
- **Worktrees at 23:00:** WIN `91ba5d7b` (22 ahead), WP02 `13eb1f14` (44), WP03 `e80fe436` (80, 50 dirty: the sheet WIP),
  WP04 `20d134a8` (98), WP05 `f11af5da` (17), WP06 `4b1a8fc1` (37), WP07 `fdd01b3d` (16), WP08 `e5bb8cfe` (66).
- **Merged so far:** WP01, WP02-M1, WP09, WP10 (`a402d87`).
- **Usage at 23:00:** 20% of the window that resets at 00:10 IST.

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
