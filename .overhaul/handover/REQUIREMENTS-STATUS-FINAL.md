# Requirements status, final (handover before the laptop wipe)

Written **Oct 16, 2026, 11:20–11:50 IST** on the Linux laptop, read-only (no build, no phone, no git write). It
replaces the older, unfinished draft `.overhaul/REQUIREMENTS-STATUS.md` (Oct 14, whose tables say "(pending)").

It covers **every item of `.overhaul/REQUIREMENTS.md`** (1–38 in full; **item 39**, Cronet/HTTP3, was added on Oct 16
at 12:10, after this file was written, and is in the appendix at the very end; every sub-item and addendum, plus the "Non-negotiable
outcome" and "Process" sections) and the **original overhaul goals of `.overhaul/design/MASTER-PLAN.md`** (§1 "The
decisions in one page", §2 gates, the checkpoints and the package plan). For each item: the user's words, the status,
the evidence (branch and commit, handoff section, perf or QA path, review verdict), what remains and who owns it, and
the check that proves it at the Android release gate.

The last two sections are the **one-page summary table** (§8) and the **decisions the user must make** (§9).

---

## 0. How to read this file

### 0.1 Status words

| Status | Meaning |
|---|---|
| **done-and-merged** | Built, reviewed and merged into the integration branch `overhaul/multiplatform-v2` (a `merged/*` tag exists). Not the same as released: `main` is the release branch. |
| **done-on-branch** | Built and tested on its package branch (`wp/<WP>`), not yet merged. A phone gate or a merge may still be outstanding; the line says which. |
| **in progress** | Partly built; some parts are committed, others uncommitted or missing. |
| **not started** | No code for it on any branch (drafts or plans may exist). |
| **blocked** | Cannot move until something outside the owner's control happens (a user decision, another package's merge, a device). The line names the blocker. |

A requirement with several parts gets a table with one status per part.

### 0.2 Paths

On Windows the workspace root is one folder (call it `tools/`) holding `.overhaul/` and `android-base/` side by side.
Paths in this file are relative to that root. The old Linux absolute path is
`/home/crazyhusir/Documents/projects/tools/<same relative path>`.

| Short name | Relative path | Old Linux path |
|---|---|---|
| `OV` | `.overhaul/` | `/home/crazyhusir/Documents/projects/tools/.overhaul/` |
| `R` | `android-base/` (repo `github.com/tripathisarthak457/android-base`, PUBLIC) | `/home/crazyhusir/Documents/projects/tools/android-base/` |
| `wt/<WP>` | `.overhaul/wt/<WP>` (a git worktree of `R` on branch `wp/<WP>`; not in the backup, recreate it) | same under `/home/crazyhusir/...` |
| `P/<WP>` | `.overhaul/perf/<WP>` (phone results: `summary.md`/`summary.json` are in the git backup; raw frames, recordings and traces are in the draft release) | |
| `H/<WP>` | `.overhaul/handoff/<WP>.md` (each package's living record) | |
| PeriFlow | separate PRIVATE repo `github.com/JorwenLabs/periflow` (formerly `tripathisarthak457/periflow`, which redirects), branch `overhaul/smooth-glass` (worktree was `.overhaul/wt/WP11`) | `/home/crazyhusir/Documents/projects/jorwen/periflow` |

Where the backup puts things (from `.overhaul/handover/MANIFEST.md`): the workspace is the orphan branch
`overhaul-workspace` of `R`; raw data was planned for the owner-only DRAFT release `overhaul-raw-data-2026-10-16`,
which **did not exist yet at 12:45 on Oct 16** (`PUSHED.md` §3; if it is still missing, the files are in the user's
Drive backup: `RESUME-ON-WINDOWS.md` §3.10); the WIP of the dirty worktrees is on `backup/<name>-wip-2026-10-16`
branches (pushed; names and SHAs in `PUSHED.md` §1.3). **Not backed up at all:** the Gmail reference recordings
`research/predictive-back/gmail/` (other people's e-mails; MANIFEST §5). What they taught is in
`research/predictive-back/SPEC.md`.

### 0.3 Git state this file was checked against (11:40 IST, Oct 16)

Checked with `git -C android-base for-each-ref`, `git rev-list --count`, `git status --porcelain` in each worktree,
`git merge-base --is-ancestor`, and `git ls-remote origin` (GitHub had only `main` at `99c4edf` at 11:40).

| Ref | Head | Commits ahead of integration | Uncommitted entries | Merged? |
|---|---|---|---|---|
| `main` | `99c4edf` (Sep 24) | — | — | release branch, untouched (Vercel deploys it) |
| `overhaul/multiplatform-v2` | `a402d87` (Oct 14 17:54) | 233 ahead of `main` | 0 | integration branch |
| `merged/WP02-M1` | `54d078d` (Oct 9 07:35) | | | tag |
| `merged/WP01` | `709612d` (Oct 9 08:37) | | | tag |
| `merged/WP09` | `8672003` (Oct 9 17:16) | | | tag |
| `merged/WP10` | `a402d87` (Oct 14 17:54) | | | tag |
| (WP00, no tag) | `6bc38e9` (Oct 9 03:40) | on the integration branch | | yes (repository move, `Chrome.kt` seed) |
| `wp/WP01` | `d19f79c` | 0 | 0 | yes |
| `wp/WP02` | `13eb1f1` (Oct 15 16:25) | 44 | 0 | M1 yes; M2 no |
| `wp/WP03` | `b595f9d` (Oct 15 16:18) | 79 | **47** (34 tracked + 13 untracked: the sheet system) | no |
| `wp/WP03-skel` | `46ec781` (Oct 14 18:53) | contained in `wp/WP03` (verified ancestor) | 0 | via WP03 |
| `wp/WP04` | `fb866b3` (Oct 16 10:38) | 95 | 0 | no |
| `wp/WP04-perf` | `fb866b3` (same commit as `wp/WP04`) | 95 | **4** (`feature/sample/.../detail/SampleDetail{Contract,Route,ViewModel}.kt`, `SampleDetailViewModelTest.kt`) | via WP04 |
| `wp/WP05` | `5d515a0` (Oct 15 15:37) | 15 (153 behind) | 0 | no |
| `wp/WP05-glass` | `282dd3c` (Oct 15 15:53) | 170 (only two merges: `5078521` = `wp/WP04` at `3c9c403`, `282dd3c` = `wp/WP03` at `285275d`; **no commit of its own**) | **43** (35 modified + 8 untracked: the glass bar) | no |
| `wp/WP06` | `a8129c4` (Oct 15 16:15) | 32 | 0 | no |
| `wp/WP07` | `99239af` (Oct 12 14:16) | 15 (153 behind) | 0 | no |
| `wp/WP08` | `d05a682` (Oct 15 15:33) | 65 | 0 | no |
| `wp/WP09` | `3d580cd` | 0 | 0 | yes |
| `wp/WP10` | `d702b16` | 0 | 0 | yes |
| PeriFlow `overhaul/smooth-glass` | `45b91a2` (Oct 15 16:12) | 16 over PeriFlow `main` `240871f` | **2** (`LiquidLens.kt`, `LiquidLensTest.kt`) | n/a (never merged; PeriFlow is in no deploy) |

**A pending change to this picture.** `PROGRESS.md` (last lines, Oct 16 ~11:30) records that the user asked in chat
to merge every branch of android-base (and PeriFlow) into `main`, with Vercel deploys from `main` paused, and that
`scripts/merge-all-to-main.js` would run after the backup. At 11:40 nothing of it had happened (`main` = `99c4edf`
locally and on GitHub). **If it has run when you read this, "merged into main" does not mean "released" or "gate
passed":** every status below is measured against the release bar of REQUIREMENTS 36, and the WIP snapshots it may
have merged can be uncommitted, non-compiling work (WP03's sheet). Re-check with
`git log --oneline -1 origin/main` and `.overhaul/handover/MERGE-ALL-*.md` if those exist.

### 0.4 What this file did not verify

- No build or test was run (the laptop powers off under Gradle heat). "Tests green" claims come from the handoffs
  and their logs, with the log path given.
- Phone numbers come from `perf/**/summary.md`, `metrics.json` and the handoffs. Nothing was re-measured.
- Where a handoff and git disagree, git wins and §7 lists the disagreement.

---

## 1. Requirements 1–10: the Android generator

### R1. Fix all navigation glitches

> "Fix ALL navigation glitches: bottom-bar tab switches are 'glitchy / pushy'; push/pop transitions glitch; EXIT/BACK
> transition glitches when navigating back quickly (rapid back presses while a pop animation is still running) —
> reproduce on device with rapid `adb shell input keyevent KEYCODE_BACK`, fix the root cause, add a regression test."

| Part | Status | Evidence | Remains (owner) | Release-gate check |
|---|---|---|---|---|
| Tab switches | **done-on-branch** (behaviour); janky gate escalated (see R19) | `wp/WP04`: `1ae29bc` (shell rebuilt: kept tabs, gate, warm pops), `5fcac60` (park hidden tabs, reveal the incoming one), `e753cb0`, `bdf61e8`, `fb866b3`. Audit root cause: every switch rebuilt the whole tab and crossfaded two tabs (`PROGRESS.md` "Audit headlines"). Phone (Redmi, release): visited-tab first frame 18.97 / 45.28 ms (old template, `P/WP04/baseline`) → 5.49 / 12.06 ms (`P/WP04/phase3c/summary.md`, gate 8 / 16: pass). `ShellRecompositionTest` | Merge WP04. Tab-switch janky 1.15 % vs ≤ 1 % (first frame after idle, R19) | `perf.sh --gate solid tabs tabs_tap` on `everything`, `standard`, `lean`; `record.sh tab_switch` strip with no blank or double-exposed frame |
| Push/pop | **done-on-branch**; p99 gate fails | `c928fb9` (each stack drawn in one parent, `StackScene`), `edb0dd5`, `6436b75`, `ac56e69`. Pop first frame 19.09 / 27.64 → 5.09 / 11.03 ms; Back-key push/pop janky 4.03 % → 1.68 % (gate 2: pass), p99 34 → 31 ms (gate 25: **fail**, first frames after idle) | Merge; p99 decision (R19) | `perf.sh pushpop pushpop_gesture` |
| Rapid Back (exit glitch) | **done-on-branch**, gate passes | Root cause (`H/WP04` Measurements, "Rapid-Back glitch as observed"): presses after the first were applied at once, so the third left the app; 18/18 bursts ended in the launcher on the old tree. Fix: `back/BackGate` coalesces presses during motion into one pop at 99 % of travel, never past the root; `NavGuard`. Regression tests: `RapidBackTest` (core/navigation unit tests) and the `rapid_back` scenario of `tools/perf/perf.sh` (asserts the process lives and lands on the expected root). Phone: old template "every burst left the app"; new lands on the root at depth 4 in every burst, janky 2.16 % (gate 4), coalesced pop 7.57 / 7.92 ms (gate 8 / 16) (`phase3c`) | Merge | `RapidBackTest`; `perf.sh rapid_back nav_stress` |

### R2. Remove ripple and press overlays; iOS-like press feedback

> "Remove unnecessary ripple / press overlays. Press feedback must be iOS-like (subtle, spring-based, cheap), not a
> grey flash."

- **Status:** done-on-branch.
- **Evidence:** `wp/WP03` `foundation/Pressable.kt` (`PressFeedback { None, Scale, Dim, Highlight }`, `NoIndication`,
  one `Modifier.Node`), `AppIndication` deleted; table "Press feedback as built (A4.1)" in `H/WP03`. Tests
  `PressableTest` (no recomposition on press), `RowHighlightTest`. Phone recordings `P/WP03/cp1-phone/`
  (`press_buttons`, `press_card_row`, `switch`, `segmented`): "nothing flashes grey". CP1 springs decided in
  `decisions/CP1.md`, written to `tokens/motion.json` (`974d089`). Build rules on `wp/WP02`: `verifyCodeRules` ids
  `ripple`, `material`, `bare-clickable` (`5495d9f`, `dbd4075`). The integration branch already refuses Material
  imports through `verifyComposeUsage` (`H/WP10` "Release blockers": true today).
- **Remains:** merge WP03 (blocked by its uncommitted sheet work, R33) and WP02-M2; re-run the call-site sweep after
  each merge; WP02-M3 moves the features onto the new design system. Owners WP03, WP02.
- **Gate check:** `./gradlew verifyCodeRules` on a generated `everything`;
  `python3 .overhaul/out/WP03/scripts/sweep_callsites.py templates/android --check`; QA2 recordings of buttons, rows
  and cards on the phone.

### R3. Liquid glass (Kyant backdrop) as an optional, FPS-optimised feature; fix PeriFlow too

> "Liquid glass (Kyant backdrop, io.github.kyant0:backdrop) as an OPTIONAL feature for Android, optimised for FPS
> (PeriFlow's version dropped FPS — fix there too)."

| Part | Status | Evidence | Remains (owner) | Gate check |
|---|---|---|---|---|
| Generator glass foundation | **done-on-branch** (`wp/WP05`) | `<opt:liquidglass>` feature; four tiers `Live`/`LiveNoLens`/`Frosted`/`Solid` (`GlassQuality`, GPU verdict, Reduce transparency), `SceneBackdrop` recorded once, `glassSurface`, the lens. 89 unit/Robolectric tests pass at `5d515a0` (`OV/logs/lens/run13-test1.log`). `H/WP05` "Measurements: None yet" | Everything under `H/WP05` "What remains" 1–10 (bar, tokens, shell stage, settings row, `kyant` code rule, catalog, benchmark, fixtures, phone, CP2). Owner WP05 | `perf.sh --gate glass_live` on `everything-glass` with `--extra glass.quality=Live` (the run must log `perf.glass: tier=Live stage=true`), plus the `glass_frosted_vs_solid` column |
| Generator glass bar | **in progress** (uncommitted) | `wt/WP05-glass`: `LiquidTabs.kt`, `PressGlow.kt`, `RimLight.kt`, `LiquidPress.kt`, `GlassTier.kt` untracked; see R37 | Commit, review, phone gates. Owner WP05 (side branch) | as above |
| PeriFlow glass cost | **in progress** | PeriFlow `overhaul/smooth-glass`: `c1b0225` (glass by tier, only floating chrome samples), `18228c1` (bar kept composed), `5be20f9`, `c81b309`, `06ad83e`. Before (`P/WP11/before/summary.md`): tab switch janky 1.84 %, push/pop 4.53 %, idle 632 frames on one tab, cold start 495 ms, PSS 224.7 MB. The "after-early" runs have no summary (`P/WP11/after-early/run1-2`) | WP11 second half (ports of `LiquidTabs`, `SceneBackdrop`, KeptTabs v2, BackGate, sheets) and the final `P/WP11/after/` measurement. Owner WP11 | `perf.sh --gates perf/WP11/gates.toml --gate periflow` |

### R4. Apple-level components; more components; remove heavy ones

> "Components polished to Apple-software level; smoothest, optimised, iOS-like animations that do not hurt
> performance. Design more components; remove heavy ones nobody needs in practice."

- **Status:** in progress (most done-on-branch; the sheet system uncommitted).
- **Evidence:** `H/WP03` "Public APIs", "Removed (with replacement)" (Lottie, the widget, FAB, tooltip, dropdown,
  snackbar, dialogs replaced), new components (inset-grouped rows, `AppLargeTitleTopBar`, `AppContextMenu`
  `bc4bddd`, `AppSearchField`, toast/HUD, `AppBottomActionBar`, spokes `AppActivityIndicator`, `AppSwipeActions`),
  optional groups `pickers`/`formfields`/`datadisplay`/`collections` as features (`a7f18df`), the table "Bugs fixed,
  with their tests" (each audit bug with its regression test). Phone: CP1 set `P/WP03/cp1-phone/`. Review: package-level fresh review not yet run (merge pending).
- **Remains (WP03):** make the sheet system compile and commit it (R33); the WP03-after-WP04 merge (21 conflicting
  files; a tested resolution exists in `282dd3c` on `wp/WP05-glass`, decisions in `H/WP05-glass` §2.0); catalog
  screenshots on the phone; QA2 round.
- **Gate check:** `:core:designsystem:testDebugUnitTest`; `python3 -m unittest generator.tests.test_catalog_coverage`
  (every public `App*` has a catalog entry); catalog shots light, dark, RTL, font 1.5 on the Redmi; QA2 report with no
  open S1/S2 defect.

### R5. Better floating bottom bars

> "Better floating bottom bars."

- **Status:** done-on-branch (WP04), phone "after" shots outstanding.
- **Evidence:** `wp/WP04` `4ba82c9` (stretching tinted pill, masked labels, glyph bounce, scrolling rail), `c65b47e`
  (content fades under the floating bar), `d0127ad` (bar → rail → sidebar, "More"), `a0e8ed6` (park, 120 Hz vote),
  `0a05924` (sinks under the keyboard), `fe182db` (labels at large text), `7a50e56` (docked bar's scroll-edge look,
  K3). Variants `BarTreatment` × `TabLabels`, accessory, trailing action, minimize: catalog `sections/CatalogNavigation.kt`;
  rules in `H/WP04` "Rules and gotchas" (pill 44 dp in a 64 dp capsule, indicator lead 0.22 s / trail 0.32 s).
- **Remains (WP04):** merge; the REQ 34 "after" shots (only `P/WP04/req34/before-338d26c` exists); `scroll_minimize`
  janky 0.66–0.89 % vs 0.5 % (R19). Glass bar: R35/R37.
- **Gate check:** `record.sh` strips of the Utility bar, light and dark, at rest and during a pull (no glyph cut by a
  hard edge); `perf.sh scroll_minimize`; `TabBarTest`.

### R6. Best-in-class multi-module architecture

> "Best-in-class multi-module architecture: deeper directory structure, better organisation that can handle 1M-user,
> feature-heavy apps — without over-engineering small apps."

| Part | Status | Evidence | Remains | Gate check |
|---|---|---|---|---|
| Pure core, MVI, `api/` packages (WP02-M1) | **done-and-merged** | `merged/WP02-M1` `54d078d`; review round 1 (2 blocking fixed, `OV/reviews/WP02-M1-round1.json`); `merges.md` (117 generator tests, template build) | — | — |
| Compact by default, `architecture: "modular"` split, conventions, presets, `verifyCodeRules` (WP02-M2) | **done-on-branch** (`wp/WP02`, waiting) | M2 `accept.sh` ACCEPT OK at `503c610` (`out/WP02/logs/accept-m2.log`); full consistency matrix 147 projects, 0 new issues (`out/WP02/logs/r14-consistency-full.log`); 305 generator tests at `13eb1f1` (`out/WP02/logs/r15-tests.log`); scope table `H/WP02-M2` "Status of every item" | Merge after WP04 and WP06 (`git merge-tree` predicts 11 conflicting files vs WP04, 27 vs WP06, 8 vs WP03); database scaffolding after WP06; re-accept. Owner WP02 | generator tests; full consistency; `modules-compact`/`modules-modular` fixtures `assembleDevDebug testDevDebugUnitTest` |
| Data layer (WP06) | **done-on-branch** | `wp/WP06`: Room 3, one network client, offline-first samples (see R10, R36 security) | Merge after WP03 | WP06 `accept.sh` |
| Feature package moves (ST-01), features on the new DS (WP02-M3) | **not started** | `H/WP02-M3` "Progress: Not started" | Whole milestone. Owner WP02-M3 | full re-gate after its merge (MASTER-PLAN A12.8 step 4) |

### R7. Remove AI-slop code

> "Remove AI-slop code (narrating comments, needless abstraction, dead code). Comments only where they explain WHY.
> It must not look AI-generated."

- **Status:** in progress.
- **Evidence:** `verifyCodeRules` ids `slop-comment`, `commented-code`, `file-length` on `wp/WP02`; standards in
  `research/CODE-STANDARDS.md` (C-1 … C-5) used by every reviewer. Merged packages passed fresh-context reviews: WP01
  (three fix rounds, `H/WP01` Progress), WP02-M1 (round 1), WP09 (two rounds, `OV/reviews/WP09-round1.json`), WP10
  (eleven reviews, the last clean, `H/WP10` Progress).
- **Remains:** the unmerged packages' reviews (A12.8 step 1); WP13's slop sweep; `test_docs.py` banned-phrase check
  (A12.6, WP12/WP13). Owners: each package, WP13.
- **Gate check:** `verifyCodeRules` with 0 allow markers; `test_docs.py`; one fresh-context review of
  `git diff main...overhaul/multiplatform-v2` before release.

### R8. Devtools API-log panel cannot be dragged; find and fix similar bugs

> "Fix the devtools API-log panel that cannot be dragged; find and fix all similar bugs."

- **Status:** done-on-branch (generator and PeriFlow); one follow-up waits for WP04.
- **Evidence:** generator `wp/WP06` `9cdcf6e`, `d2febdb` (out of the system back-gesture zone), `1dcacd9` (reads the
  latest bounds). Phone: `out/WP06/logs/accept13.log` (Oct 14 18:03, at `1dcacd9`): `BADGE DRAG OK`, moved (−948, −773)
  px, 96.6 % of the finger's path; emulator `accept10.log` the same. Old build: a 503 × 1038 px swipe moved it 7 × 14 px
  (audit). Similar bugs fixed: QA-44 … QA-47 (`H/WP06` "Scope items"). PeriFlow: `106a701`.
- **Remains (WP06):** K13 (badge's top bound = the front screen's whole top chrome, after WP04 merges); the DevTools
  panel from `Dialog` to `AppSheet` (SHEET-SPEC §19.8, after WP03 merges).
- **Gate check:** `out/WP06/accept.sh` on the phone and the emulator, with `badge_qa_check.py --top-chrome`.

### R9. Test edge cases and all build options

> "Test edge cases and ALL types of build options, lean to full, all combinations."

- **Status:** in progress (static matrix done; real Gradle matrix not run).
- **Evidence:** static consistency matrix (WP01 `tests/consistency`, merged; WP02-M2 full run 147 projects, 0 new
  issues); hostile-name fixtures (`out/gen-n-*`); WP02-M2's scaffold at module-name lengths 1–30 (`out/WP02/logs/r10-fit.log`).
  The integration branch's `.github/workflows/ci.yml` builds only `none, lean, everything, all` and still imports
  `genkit.spec` (pre-WP01 module path; `H/WP01` Open issues says WP13 rewrites it).
- **Remains (WP13):** the 14-row local Gradle matrix with size budgets (A12.3, `tools/size_budgets.toml`), CI rewrite
  with the nightly wide matrix of about 70 rows (A12.7).
- **Gate check:** `tools/matrix.py` all rows green; CI `android-matrix-wide` green on the release pull request.

### R10. Practically useful generator features

> "Add practically useful generator features."

| Part | Status | Evidence |
|---|---|---|
| Engine v2, spec v2, `--set`, headless contract, catalogue v2, token pipeline; fixes: double rename (`com.base.apple` → `applele`), `--icon` XML, keystores with one PKCS12 password | **done-and-merged** (WP01, `709612d`) | `H/WP01` Progress (ACCEPT OK, 249 tests, golden comparison) |
| Modular mode, `--domain`, presets `lean`/`standard`/`everything`, `ci`, `formatting`, locale filters, scaffold | **done-on-branch** (WP02-M2) | `H/WP02-M2` scope table |
| `offlinequeue`, `appstatus`, Firebase split (`crashlytics`, `firebaseanalytics`, `remoteconfig`, `push`), Room 3, `widget`/`workmanager`/`room` removed | **done-on-branch** (WP06) | `wp/WP06` `dd4f270`, `b0673c8`, `17dc2d5`, `21d3bbc` |
| `performance` feature (perf tools, benchmarks, baseline profiles), `tools/adaptive` | **done-on-branch** (WP04) | `140d0a0`, `335bcca`, `c14848d` |
| `upgrade_project.py` (+ `--diff`), `add_tab.py`, `add_language.py`, v2 `add_feature`/`remove_feature` in both modes (MASTER-PLAN §1 item 14 also names `make_modular.py`) | **not started** (WP14) | no handoff, no worktree |

- **Gate check:** `generator/tests/test_project_tools.py` (WP14), matrix row 14, the generator unit tests.

---

## 2. Requirements 11–16: new generators, web, API, docs

### R11. Compose Multiplatform generator (Android + iOS)

> "Compose Multiplatform generator (Android + iOS targets), best architecture, same design-system quality."

- **Status:** in progress (phase A partial). Moved to phase B by R36: not part of the Android release.
- **Evidence:** `wp/WP07` `99239af` (Oct 12; 15 ahead, 153 behind): layout, build-logic, Koin, Ktor, Room 3,
  DataStore, generator target `targets/multiplatform`, `tools/sync_check.py`, 25 generator tests (`H/WP07-A`
  Progress). Its full build was queued, never confirmed; `$O/accept-a.sh` cannot pass before the shell port.
- **Remains (WP07):** phase A items 1–7 (`H/WP07-A` "What remains": port WP04's shell, list what JetBrains
  navigation3-ui 1.1.2 cannot do, port the shell tests, extend `sync.toml`, posture/input, vendored skills,
  acceptance); WP07-B (design system, glass, sheet §19.4 and shimmer §4.1 parity, Android device gates `cmp_android`);
  WP07-C (`deeplink`, `auth`); macOS CI.
- **Gate check (phase B):** `RELEASE-GATE-multiplatform.md`; `tools/sync_check.py`; the
  `:shared:compileKotlinIosSimulatorArm64` attempt; the macOS CI job decides "Beta: iOS unverified".

### R12. SwiftUI generator, hidden as "coming soon"

> "SwiftUI native generator (SwiftUI + @Observable MVI, iOS 17 min, XcodeGen, local SPM packages, iOS 26 glass behind
> #available). Code complete but HIDDEN / 'coming soon' on the web portal (no Mac to test)."

- **Status:** done-on-branch (code complete, never compiled); "coming soon" on the site is done-and-merged (WP10).
- **Evidence:** `wp/WP08` `d05a682` (65 ahead): `templates/ios`, generator target `targets/ios`, static checks
  `generator/tests/test_target_ios.py` with `ViolationsTest`; `out/WP08/accept.sh` ACCEPT OK (29 iOS tests, 280 in
  the suite); review r1's six findings fixed (`H/WP08-review-r1.md`, `H/WP08`); sheet parity (SHEET-SPEC §19.5) and
  shimmer parity (`2bc4c4a`, `7400ba3`, `37558cc`) built.
- **Remains (WP08, WP13-B):** re-emit `DesignTokens.swift` and `AccentColor` after WP03's tokens merge; merge in
  phase B; first compile on a Mac (`H/WP08` Open issues 1: Release build, simulator scenarios, sheet previews).
- **Gate check (phase B):** macOS CI `xcodebuild build test` (non-gating while "coming soon"); static checks green.

### R13. Web portal, Python script, repo docs; platform picker

> "Update web portal, Python script, open-source repo docs with all changes. Platform picker: Android Native |
> Compose Multiplatform | Swift (coming soon)."

| Part | Status | Evidence | Remains |
|---|---|---|---|
| Web portal (configurator, previews, admin) | **done-and-merged** (WP10, `a402d87`) | ACCEPT OK at `d702b16` (`out/WP10/logs/accept-run14.log`: catalogue `--check`, tsc, 220/220 tests, `next build`); eleven review rounds | `H/WP10` "Release blockers" (below) |
| Platform picker | **done-and-merged**, changed by R36 | Android Native selectable; Compose Multiplatform and Swift shown "coming soon" (`ddc533f`, `7bde1b1`) | Phase B opens Compose Multiplatform (WP10 `OFFERED` set) |
| Python script (generator) | **done-and-merged** (WP01) | `merged/WP01` | WP02-M2, WP06, WP14 changes still to merge |
| Open-source repo docs | **not started** (WP12) | drafts in `docs-draft/` | WP12 |

**Release blockers recorded by WP10** (each must be cleared before the site ships): `ENABLE_TARGETS=android` on the
deploy (the API default is `android,multiplatform`); the CI claim in `components/hero.tsx` and `faq.tsx` (true only
once WP13's matrix gates CI); `docs/telemetry-and-privacy.md` missing (linked from hero, FAQ, footer); **no `LICENSE`
file in the repository** while the FAQ and footer say "MIT" (verified: `git show overhaul/multiplatform-v2:LICENSE`
fails); end-state claims in `components/included.tsx`. Also open: nobody has clicked through `/admin` signed in.

- **Gate check:** the blockers above cleared; `npm ci && npx tsc --noEmit && node --test "features/**/*.test.ts" &&
  npx next build`; a browser click-through of the site and of `/admin` signed in.

### R14. Admin portal: richer anonymous telemetry

> "Admin portal: much more detailed metrics of who generated what — richer ANONYMOUS telemetry only (geo region/city
> via Vercel headers, browser/OS/device class, referrer + UTM, funnel opened→configured→generated→downloaded, time in
> configurator, returning visitors, failure rates per feature combination, per-platform). No sign-in, no email."

- **Status:** done-and-merged (WP09 `8672003` + WP10 `a402d87`).
- **Evidence:** `H/WP09` "Public API" and "Telemetry storage": events `landed` (referrer origin, UTM, `returning`,
  viewport), `step_viewed`, `platform_selected`, `preset_chosen`, `generate_clicked`, `download_started`; the server
  adds country/region/city from Vercel's headers (only under `TRUSTED_PROXY=vercel`), browser and OS family, device
  class; `X-Config-Ms` time in configurator; failures by preset, commit, feature and feature pairs; per target. No
  visitor sign-in; the admin uses a bearer `ADMIN_TOKEN`.
- **Remains:** a signed-in browser check of `/admin` (WP10 Open issues); production env values (`DATABASE_URL`,
  `ADMIN_TOKEN`, `TRUSTED_PROXY`) are on Vercel and unknown from this laptop. WP09 open issues (telemetry wait during
  a database outage, rollup undercount) are non-blocking by its own account.
- **Gate check:** A12.7 `deploy` job smoke against the deployed URL; one generation visible in `/admin`.

### R15. Better agent docs in the repo and in every generated project

> "Improve AGENTS.md, README, CLAUDE.md, docs/* and every file an AI agent reads — both in the generator repo and in
> every generated project — so agents build MORE POLISHED, SMOOTH apps from the generated project (motion/perf/design
> rules, checklists, do/don't with code)."

- **Status:** in progress (pieces on branches; the package itself not started).
- **Evidence:** `wp/WP02` adds the Google-skills sections to the repo `AGENTS.md` and the template's `AGENTS.md` §8;
  `wp/WP08` wrote the iOS `AGENTS.md` (`9b65178`); every handoff has "Rules and gotchas the docs must state";
  `docs-draft/{SMOOTHNESS,MOTION,AGENTS-SECTION,ADAPTIVE}.md` (Oct 12, claims marked `[verify]`).
- **Remains (WP12):** the whole package (`H/WP12` orchestrator notes); fix the stale drafts (`H/WP04` lists the tab
  "veil" text in `docs-draft/MOTION.md` and `SMOOTHNESS.md` item 5 as stale).
- **Gate check:** A12.6: a fresh agent given only a generated `standard` project adds an `orders` feature that passes
  `./gradlew check` and a second agent's review; `test_docs.py`; `tests/docs_tasks.sh`.

### R16. Document the project perfectly

> "Document the project perfectly."

- **Status:** not started (WP12). Inputs ready: `docs-draft/`, `RELEASE-NOTES-android-draft.md` (Oct 14, sections
  "(pending)"), every handoff.
- **Gate check:** as R15, plus the README feature table equals the catalogue (`test_docs.py`).

---

## 3. Process (REQUIREMENTS "Process" and memory safety)

| Item (user's words) | Status | Evidence | Remains / note |
|---|---|---|---|
| "Port the good recent bits from PeriFlow and VitFleet back into the generator." | in progress | `audit/periflow-learnings.md`, `audit/vitfleet-learnings.md`; WP03 `animation/Emphasis.kt` (VitFleet), toggle-row fix (VitFleet B20), data-table widths (P17); WP06's badge patch written to be portable (`H/WP06` "Devtools badge fix as a portable patch") | Each owner's merge |
| "Code review and fix bad code." | in progress | Review loop per package (A12.8); merged ones reviewed (R7) | Unmerged packages' reviews |
| "Git: branch overhaul/multiplatform-v2; merge to main only when the full matrix is green (CI gates the Vercel deploy). PeriFlow fixes: local branch, committed, NOT pushed." | **changed by the user on Oct 16** | The user allowed pushing every branch to the public android-base repo and PeriFlow's branch to its private repo for the backup ("push to public there is no problem, just push each and every file periflow too"). Merging to `main` still waits for the release gate and the user's go-ahead (R36), except for the pending merge-all request in §0.3 | Decide which model holds after the wipe (§9) |
| "Gradle builds and adb sessions are serialised through `bin/locked` (gradle\|adb)." | done | `bin/locked`: Gradle at 2 workers, `nice 10`, daemon idle 15 min; adb with `flock -o` (an adb server started inside `flock` held the lock forever, fixed Oct 16; old copy `bin/locked.pre-close`) | `bin/locked` and `memwait` are Linux scripts (`flock`, `/sys/class/thermal`). **On Windows they need WSL2 or a rewrite (not verified).** |
| "If the phone disconnects, use the emulator AVD periflow_api35." | in use | correctness only, never gated (`H/WP04` "On the emulator") | Recreate the AVD on Windows |
| Memory safety (Oct 10 freeze) | done, extended | `bin/memwait` waits for free memory and, since Oct 16, for `x86_pkg_temp` below 80 °C (`THERMWAIT_C`); `bin/thermlog` logs sensors to `logs/thermal.log`. Oct 16: three power-offs, most likely CPU heat under Gradle (95–100 °C), SEN3 sensor erratic (`SESSION-2026-10-16.md`) | Windows' Intel DTT driver manages heat; still keep builds serialised |

### Non-negotiable outcome

> "The generator must produce SMOOTH, LIGHT apps. … Every decision is judged against: does it keep 60/90/120 fps on a
> mid-range phone (Redmi Note 10 Pro, serial 9a7c74f4)?"

- **Status:** in progress. Steady frames are smooth (hitch ratio 0 ms/s on tab switches, phase3c); the first frame
  of each transition after idle misses the deadline on this phone (R19). Lightness: `standard` app PSS 41.7 MB (gate
  48: pass), cold start 285 / 312 ms (pass); `everything` 52–54 MB (fail) and 419–440 ms as installed (fail).

---

## 4. Requirements 17–25 (added Oct 9, 08:40)

### R17. No opaque white block around the bottom bar

> "The bottom bar must not have an opaque white block around it that cuts off the UI. Content scrolls under a
> floating bar, and only the scroll content is padded."

- **Status:** done-on-branch (WP04); WP03's half uncommitted; phone "after" outstanding.
- **Evidence:** `wp/WP04` `511be84` (pad every tab screen for the floating bar), `c65b47e`, `7a50e56` (docked bar:
  surface and rule only while content runs under it, K3), `ChromeLayeringTest` (a root list runs under the bar and its
  last row rests above it). `wp/WP02` rule `scroll-chrome` (`13eb1f1`: the build fails a feature's scroll container
  the chrome does not pad). The defect as the user saw it: `qa/shimmer/evidence/frames/r8-1335-light_pull-bottom-cut.png`.
  WP03's half (Utility style defaults to `Floating`) is in `wt/WP03`'s uncommitted tree.
- **Remains:** WP04 merge, WP03 commit and merge; the phone shots on a build with WP04's shell. Owners WP04, WP03.
- **Gate check:** phone, release build, Utility style, light and dark, at rest and during a pull: no glyph cut by a
  hard horizontal edge above the bar; the list end rests a gutter above the bar (`qa/shimmer/REPORT.md` §5.4 step 7).

### R18. Continuous visual defect hunting

> "Visual defect hunting is continuous. … opaque blocks, clipped content, truncated labels, overlaps, wrong insets,
> inconsistent spacing, poor dark mode, large-font breakage, RTL, tablet."

| Defect named by the user | Status | Evidence |
|---|---|---|
| "7 tabs crammed into one bar, with 'Settin…' truncated" | done-on-branch | WP04 `d0127ad` (bar ≤ 5 tabs, else 4 + More; rail ≤ 7); WP02-M2 tab cap and More warning (`d21a77d`, AD-24); website preview of More (`fe76977`) |
| "Scaffolded modules all get the same icon" | done-on-branch | WP02-M2 `d21a77d` (distinct icons, QA-10). A per-module `icon` in the spec is still open (`H/WP02-M2` Open issues) |
| "The selected tab's label overflows its indicator" | done-on-branch | WP04 `4ba82c9` (masked two-layer labels; a label never passes the pill), `fe182db` |
| "The bottom edge under the floating bar … needs an edge fade" | done-on-branch | WP04 `c65b47e`, `BottomEdgeFade` (K11 tail fixed in run 17) |
| Continuous hunting | in progress | QA round 1: `qa/r1/DEFECTS.md` (47 defects, each with an owner and `for-<WP>.md`); shimmer and clipping audit `qa/shimmer/AUDIT.md`. **QA2 (round 2) not started** |

- **Gate check:** QA2's report on the merged build: light, dark, font 1.5 and 2.0, RTL (`debug.force_rtl` plus an app
  locale, `H/WP04` "Measuring"), tablet and fold on the emulator; no open S1/S2.

### R19. iOS-level smoothness, the lowest achievable jank

> "Smoothness target: iOS level, the lowest achievable jank rate. … the gates in MASTER-PLAN §2 are the floor, not the
> goal. On the Redmi at 120 Hz, aim for ~0% janky frames on tab switches, push/pop, back and scroll, with no dropped
> first frames."

- **Status:** in progress; **the solid gates cannot all pass on this phone as written** (WP04 escalation, `H/WP04`
  Open issues first item). Needs a decision (§9).
- **Root cause (measured, `FABLE-FINDINGS.md`, `H/WP04-perf` §1):** almost every janky frame is the *first frame
  after idle*. Steady frames take 9.5 ms; the first one 17–29 ms against FrameTimeline's 13.67 ms deadline, because the
  CPU big cores sit at their 652 MHz floor and the GPU at its idle clock. Tab switch janky = exactly one frame per
  switch (1/87 = 1.15 %); push/pop p99 and gesture janky are the same frames. Performance hints (ADPF) do not move the
  clocks on this phone (HWUI already sends them). The only lever is a cheaper first frame. Scroll hitches come from the
  RenderThread landing on a little core for one frame.
- **Fixes already in `wp/WP04`** (merge of `wp/WP04-perf`, `730f8e0`): a leaving screen keeps its pane context
  (`6436b75`, the pop's first frame no longer composes 416 composables), the bar's tap reads selection through state
  (`1ec7462`), the pill is a rounded rect (`b7e8074`), one background fill per frame (`ac56e69`); then `fb866b3` (a
  hidden tab's lifecycle drops a frame after the switch).
- **Not measured yet:** the build with those fixes. `out/WP04-perf/logs/measure2.log` shows the HEAD build (`3c9c403`)
  measured from Oct 15 20:40 (runs 1–2 complete; run 3 stopped after `recompose` with `uiautomator could not dump the
  screen`, so no summary was written), then on Oct 16 10:26 the "fixed" build
  failed with `perf: device 9a7c74f4 is not attached`; the "floor" build (content removed) was never measured.
- **Latest numbers:** §6.2 (MASTER-PLAN §2 gate table).
- **Remains:** WP04-perf round 2 (HEAD vs fixed vs floor on the phone; Perfetto traces); if fixed ≈ floor, record the
  first-frame jank as a device limit in the release gate; re-measure scroll after WP03 merges (`AppOverscroll` moves
  content in placement with no layer); the first-gesture shader warm-up (`followups.md`, S6). Owners WP04 (with its
  side branch), WP13.
- **Gate check:** `perf.sh --gate solid --runs 3` on release builds of `everything`, `standard`, `lean` from the merged
  integration head, reading `late_first_frames`/`transitions` and the hitch ratio beside each janky row.

### R20. A before/after report on the phone

> "The final report must show before/after numbers measured on the phone: old template vs new for the everything,
> standard and lean presets, and PeriFlow before vs after. Cover janky %, frame-time p50/p90/p99, GPU p90, first-frame
> UI time, cold start and PSS."

- **Status:** in progress (data partial; the report is not written: `report/FINAL.md` does not exist).
- **Evidence available:** old template `P/WP04/baseline`, `baseline-standard`, `baseline-lean` (Oct 9); new
  `phase2`, `phase3c` (`everything` at `d10ef94`, 3 runs), `phase3c-standard`, `phase3d-lean`, `phase3e/run1` (one run,
  APKs at `338d26c`), `P/WP04-perf/everything/run1-2` (`3c9c403`). PeriFlow before `P/WP11/before/summary.md`; PeriFlow
  after: only the lens A/B pairs (`P/WP11/lens-tap/ab-lens-*`, tab switch only).
- **Remains:** final runs on the merged head for all three presets (WP13; WP04 phase 3f was planned in
  `out/WP04/r20-chain.sh` and never ran); PeriFlow final `P/WP11/after/` (WP11); the report (WP13).
- **Gate check:** the report's table has every listed metric for old vs new × `everything`/`standard`/`lean` and
  PeriFlow before vs after, each with its `summary.json` path.

### R21. The full set of iOS-like animations, each optimal

> "Smoothness must not come at the expense of animation. … Every animation is interruptible and keeps its velocity,
> runs in the layout/draw phase with no per-frame recomposition, and passes the perf gates."

| Animation (user's list) | Status | Evidence | Remains (owner) |
|---|---|---|---|
| Push/pop with parallax and dim | done-on-branch | WP04 `c928fb9`; `screen` spring 0.344 s, no bounce, parallax 0.333, scrim 0.10 light / 0.24 dark (`decisions/CP1.md`) | merge |
| Interactive back swipe | done-on-branch | Gmail card on Android (R24); iOS edge swipe is the multiplatform/iOS targets' (WP07, WP08 native) | WP07-B |
| Zoom/hero transitions | **not started** | WP04 Deviation 18: decided to build it in the shell's layer and draw blocks (`NavTransitionStyle.Zoom`, `Modifier.zoomSource`) after WP03 merges; `followups.md` WP04 | WP04, WP02-M3 (sample) |
| Modal sheets with detents and the card stack | in progress / not started | sheet uncommitted (R33); card stack not built (QA-11, MOTION M6, SHEET-SPEC §19.2) | WP03, WP04 |
| Context menus with a preview | done-on-branch | WP03 `bc4bddd`; right click `31be531` | merge |
| Large-title collapse | done-on-branch | WP03 `48c9542`, `ec4dcb7`, `LargeTitleTopBarTest` | K4 at the WP03 merge |
| Tab bar minimize on scroll | done-on-branch | WP04 `BarMinimize.OnScrollDown`; gate `scroll_minimize` fails at 0.66–0.89 % | R19 |
| Rubber-band overscroll; iOS deceleration | done-on-branch (DS) | WP03 `6c040ba` (`AppOverscroll`, `AppScroll.flingBehavior()`) | features adopt it (WP02-M3, M11) |
| Spring press states | done-on-branch | R2 | — |
| Switch, segmented, picker motion | done-on-branch | WP03 `63d2fd2` (controls keep the finger's speed), `abe0f1c`, `c0b8e86`, `AppWheelPicker` | phone check (QA2) |
| Alert and action-sheet scale-in | done-on-branch | WP03 `AppAlert`, `AppActionSheet`, `b212504` | action sheet moves onto the new sheet (R33) |
| List insert/delete/reorder | in progress | DS `appAnimateItem` (WP03); features not started | WP02-M3 (M18) |
| Swipe actions | done-on-branch | WP03 `AppSwipeActions`, `SwipeActionsTest` | — |
| Pull to refresh | done-on-branch | R29 | — |
| Search bar focus expansion | done-on-branch (unverified on the phone) | WP03 `AppSearchField` (MOTION M21 was "PARTIAL" on Oct 9; later state not re-checked here) | QA2 |
| Keyboard-synced motion | in progress | WP04 bar sinks under the keyboard (`0a05924`); sheets' IME handling uncommitted; screens not started | WP03, WP02-M3; K14 at merge |
| Toast/HUD | done-on-branch | WP03 `8eae2c0`, `OverlayHostTest`; phone CP1 toast run was not driven (emulator set only) | QA2 phone check |
| Glass morphing | in progress | R35, R37 | WP05 |
| Interruptible, keeps velocity, no per-frame recomposition | partly | back gesture: 0 recompositions while the card follows the finger (phase3c); PTR 0 per frame; BackGate coalesces instead of retargeting (WP04 Deviation 19: `StackScene` starts each change from rest; the retarget variant was not tried) | QA2 strips; R22 |

- **Gate check:** QA2 recordings (`record.sh`, `--size 720x1600`) of each motion; `RecompositionJourneysTest`,
  `ShellRecompositionTest`, `perf.sh recompose`.

### R22. Optimise recomposition everywhere

> "Stable parameters, deferred reads, no per-frame recomposition during animations, and Compose compiler metrics
> clean for the design system and the shell. Verify it with recomposition-count tests on the critical journeys."

- **Status:** in progress (shell budgets met on the last phone run; features pending).
- **Evidence:** frames that recompose per transition, budget 2: phase3c tab 5 / pop 4 (fail) → `phase3e/run1` tab 1,
  pop 2 (pass), push 5 (ratchet), back-gesture progress 0 (`P/WP04/phase3e/run1/metrics.json`). Fixes: WP04
  Deviations 24–26 (`227e1e9`, `e753cb0`, `338d26c`), `6436b75`. Tests: `ShellRecompositionTest`, WP03
  `RecompositionJourneysTest` (`7869e35`, `5fecce8`). Compiler gate and stability config on `wp/WP02` (`a2ab1b7`,
  `f58f4cb`); scaffolded modules get a `<Feature>RecompositionTest`.
- **Remains:** reference features' recomposition tests (WP02-M3, RECOMPOSITION-PLAN R10); compiler metrics checked on
  the merged tree; the template-wide jetpack-compose-performance audit (R38).
- **Gate check:** `perf.sh recompose` rows within budget; compiler metrics report (stable, skippable) clean for
  `:core:designsystem` and `:core:navigation`.

### R23. Adaptive UI everywhere

> "Every component and every screen responds to the window size class, width and height … The template must force
> generated projects to stay adaptive … The same principle applies in the Compose Multiplatform template … and the
> SwiftUI template."

| Part | Status | Evidence | Remains |
|---|---|---|---|
| All breakpoints incl. large and extra-large | done-on-branch | WP03 adaptive layer `42d10b7` (AD-01..03); WP04 `ShellChromePolicy`; emulator `P/WP04/emulator-large`, `P/WP04/adaptive/emulator` | `large` gate column needs a display override on the Redmi (ADAPTIVE §0.7: only with the user's OK) |
| Foldables: tabletop/book, hinge, fold/unfold continuity | partly | continuity done (`2f1fd62`, `fb71785`, AVD `wp04_fold_api35`, `out/WP04/emu-fold.sh`); posture into `AppTheme` (`rememberWindowPosture`, AD-15) waits for the WP03 merge | WP04 after WP03 |
| Chromebooks and desktop windowing | done-on-branch (emulator only) | `configChanges`, `<layout>`, multi-instance, `MultiInstanceTest`, `adaptive.sh multiinstance` | no ChromeOS device test |
| Keyboard, mouse, trackpad, stylus | partly | shortcuts `ec309c1`, right click and hover `31be531`; stylus: no evidence found | QA2 |
| No orientation or resizability locks | done-on-branch | lint ids; `verifyCodeRules` `adaptive-orientation`; `WindowManifestTest` | — |
| Bar → rail → sidebar | done-on-branch | `d0127ad` | — |
| Canonical layouts | partly | list-detail and supporting pane via `StackPlan` (`43112a9`; Deviation 12: not Material's scene strategies); feed grid and settings list-detail are WP02-M3 | WP02-M3 |
| Forcing it in generated projects | partly | code rules `adaptive-*` (WP02-M2); screenshot-test convention and CI wiring (`18ab5ab`) but **no goldens recorded**; `@AppScreenPreviews` path contract (WP03); `AdaptiveAuditTest` has no owner (`H/WP02-M2` Open issues) | WP03, WP02-M3, WP12, WP13 |
| Compose Multiplatform (iPad, desktop) | not started | WP07-B | phase B |
| SwiftUI (size classes, `NavigationSplitView`) | done-on-branch | WP08 AD-60 (`99a6a09`, `20bf84e`) | Mac compile |

- **Gate check:** `tools/adaptive/adaptive.sh sizes fold posture multiinstance keyboard` on the emulator; the `large`
  scenarios; screenshot tests at six sizes once goldens exist.

### R24. Predictive back like Gmail

> "During the back gesture the current screen shrinks into a rounded card that follows the finger, the previous screen
> shows behind it, the card flies out on commit and springs back on cancel. Back-to-home matches the system animation.
> … It must hold the same perf gates and work with BackGate (rapid back) and the tab bar."

- **Status:** done-on-branch (screens and covers); sheets wait for R33; one perf row fails.
- **Evidence:** WP04 `c928fb9` (the Gmail back card), `acd1bc3` (covers too), constants per
  `research/predictive-back/SPEC.md` (scale 0.90, 8 dp far-edge gap, follows the finger up to 24 dp, 28 dp corners,
  0.32 scrim in 50 ms, commit fades 100 ms with `ModulateAlpha`). Phone recordings
  `P/WP04/predictive-back/{back_slow_commit,back_slow_cancel,back_fast_flick,gesture_pop,back_to_home,back_slow_commit_right,dark/back_slow_commit}`
  (frames in the draft release). Works with BackGate and the bar (bar shown at rest under the card, `a0e8ed6`).
  Phone: back gesture's first three frames 2.39 ms median (pass), 0 recompositions while following; push/pop with the
  gesture janky 2.26 % (phase3c) / 2.1 % (phase3e) vs ≤ 2 % (**fail**, first frames, R19).
- **Remains:** merge; the first swipe after an install compiles shaders (up to 70 ms once; `followups.md` WP04, S6);
  sheet predictive back (WP03/WP04, R33); multiplatform parity (WP07).
- **Gate check:** `record.sh back_slow_commit back_slow_cancel back_fast_flick back_to_home` strips matching SPEC §2;
  `perf.sh pushpop_gesture first_gesture`.

### R25. Every smoothness finding documented where agents follow it

> "In each generated project (Android, Compose Multiplatform, iOS) and in the generator repo, AGENTS.md, CLAUDE.md, the
> other agents' entry files and the docs/ guides must carry: …"

| Bullet | Status | Inputs ready |
|---|---|---|
| Smoothness rules and forbidden patterns, with do/don't code | not started (WP12) | `docs-draft/SMOOTHNESS.md` (12 forbidden patterns), MASTER-PLAN A11.3, `research/SMOOTHNESS-PLAN.md` §6 |
| Root causes with fix and reason (tab rebuilds, bar out of sync, full-screen alpha, rapid back, cold first frames, glass costs, recomposition in animations, ripple, stale gesture captures) | not started | `docs-draft/SMOOTHNESS.md` (14 root causes), `FABLE-FINDINGS.md`, `H/WP04` and `H/WP04-perf` "Rules", `H/WP05` "Rules" |
| Measured before/after numbers | not started | R20 data |
| Perf budgets and gates; how to measure (`perf.sh`, release builds only) | not started | `H/WP04` "Measuring", `tools/perf/gates.toml` |
| iOS motion catalogue with springs; Gmail predictive back | not started | `docs-draft/MOTION.md`, `decisions/CP1.md`, SPEC |
| Adaptive rules and recomposition budgets | not started | `docs-draft/ADAPTIVE.md`, `research/ADAPTIVE-PLAN.md` §15, `RECOMPOSITION-PLAN.md` §6 |
| Pre-merge checklist; short enforceable `AGENTS.md` | not started | `docs-draft/AGENTS-SECTION.md` (55 lines, 9-item checklist) |

- **Owner:** WP12 (Android and repo docs, phase A), WP12-B (multiplatform and iOS, phase B).
- **Gate check:** as R15; every rule cited to merged code (the drafts' `[verify]` marks resolved).

---

## 5. Requirements 26–38 (added Oct 9 11:50 to Oct 16)

### R26. Google's official Android agent skills

> "Agents use them. … The generator ships them too. Every generated project (Android and Compose Multiplatform)
> vendors the relevant skills with their licence under its agent directories … and its AGENTS.md/CLAUDE.md tell agents
> when to use each. The generator repo's own AGENTS.md does the same."

| Part | Status | Evidence | Remains |
|---|---|---|---|
| Agents use them | done | plugin `android-skills@android-skills` (now 1.0.15); every prompt in `scripts/*.js` names them | reinstall the plugin on Windows |
| Android template vendors them with licence; AGENTS.md table | done-on-branch (`wp/WP02`, `09ba713`, `ca5e6c9`) at **v1.0.13** | `templates/android/.agents/skills/README.md`: 10 always + 2 with `auth`, `LICENSE.txt` beside each `SKILL.md`, `MANIFEST.sha256`, `.claude/skills/` pointers; template `AGENTS.md` §8; `test_vendored_skills.py` | merge; v1.0.15 (R38) |
| Repo `AGENTS.md` section | done-on-branch (`wp/WP02`) | repo `AGENTS.md` "Google's Android skills" | merge |
| Compose Multiplatform template | not started | `H/WP07-A` "What remains" 6 | WP07 |

- **Gate check:** `python3 -m unittest generator.tests.test_vendored_skills`; a generated `everything` has every
  listed skill with its `LICENSE.txt`.

### R27. Google/AOSP-quality code, deeper structure

> "The bar is code that would pass review in androidx/AOSP … Deeper, well-organised package and folder structure inside
> every module. Zero AI-slop comments. Study AOSP and androidx sources … Reviewers judge against this bar."

- **Status:** in progress (applied per package; final reviews pending).
- **Evidence:** `research/CODE-STANDARDS.md`, `research/STANDARDS-PLAN.md`; package moves ST-03..ST-08 (WP03 `6d0ef8d`,
  `afd6e36`; WP04's `core/navigation` file map in `H/WP04`; WP06 run 3); handoffs cite androidx sources for each
  deviation (e.g. WP04 Deviations 3, 4, 16; WP05 D1, D6).
- **Remains:** fresh reviews of WP02-M2, WP03, WP04, WP05, WP06, WP08 before merge; `explicitApi()` for the core
  libraries (tier 2, `H/WP02-M2` Open issues); WP03's ABI dump (`abiValidation`, `api/*.api`).
- **Gate check:** each merge's review verdict without blocking findings (A12.8 step 1).

### R28. Polished tab-switch animation

> "A tasteful content transition between kept tabs (no rebuild, no double exposure, no offscreen alpha on full
> screens), plus selection motion on the bar: the indicator morphs or stretches toward its target, and the icon bounces
> on select. It must hit the smoothness gates on the Redmi. Record it."

- **Status:** done-on-branch; the janky gate is part of the R19 decision.
- **Evidence:** WP04 `5fcac60` (reveal: 12 dp nudge on the incoming tab, outgoing tab parked at once), `4ba82c9`
  (indicator on two edges: lead 0.22 s, trail 0.32 s, stretch ≤ 1.5 slots; icon dips to 0.84 and springs back),
  `77c58df` (the 0.6 veil dropped, CP1 §4's own fallback, WP04 Deviation 22: it cost a janky frame per switch).
  Recordings `P/WP04/tab-switch/{light,dark,four-tabs}`, 2× font on the emulator; phone RTL clips came out left to
  right (locale did not take), emulator RTL correct (`P/WP04/rtl/emulator/`). Tab janky 1.15 % vs ≤ 1 %.
- **Remains:** the gate decision (§9); the glass bar's version (R35/R37).
- **Gate check:** `perf.sh tabs tabs_tap`; `record.sh tab_switch` light/dark/RTL.

### R29. Pull to refresh redesign; the black shadow bug

> "Target, iOS level: the content follows the finger with rubber-band resistance; the spinner is revealed progressively
> as you pull (UIRefreshControl spokes); a haptic tick at the threshold; on release the content rests at the spinner
> height while it refreshes, then springs back carrying its velocity; no surface disc, no shadow; correct in light and
> dark; zero recomposition per frame. Owner: WP03. Record before and after."

- **Status:** done-on-branch.
- **Evidence:** WP03 `92045c7` (one node, rubber band, trigger), `4a3f9e4` (spinner and indicator in their own
  layers), `ec4dcb7` (content never drawn across the large title); haptic at the threshold
  (`component/refresh/AppPullToRefresh.kt`, `AppHaptic.Threshold`). Phone (`H/WP03` Measurements, `P/WP03/ptr/`):
  before 1.10 % / 1.08 % janky, GPU p90 9 / 7 ms; current 0.54 % light / 0.53 % dark, GPU p90 6 ms (target ≤ 1 %,
  ≤ 7 ms: met). Recompose only at refresh start and end (36 slices per set). Before recordings `qa/ptr/`, after
  `P/WP03/ptr/r8-recorded/`.
- **Remains:** merge; features use `AppPullToRefresh` with `indicatorTop` (WP02-M3, M20); WP04's trace saw a 105 px
  alpha layer from the indicator during a scroll (`H/WP04` Phase 3 §2): re-check after the merge.
- **Gate check:** `out/WP03/scripts/ptr_ab.sh` on the merged build, light and dark.

### R30. DEV badge over "Samples"; the crowded pill

> "The DEV badge overlaps the large 'Samples' title (devtools, WP06). The selected tab's filled pill nearly fills the
> bar's height and crowds it (WP04)."

| Part | Status | Evidence | Remains |
|---|---|---|---|
| DEV badge | done-on-branch | QA-44 `9cdcf6e`; phone: title [44,144][314,229] vs badge [948,1323][1080,1455], clear (`H/WP06` "Device checks") | K13 for a dragged badge (after the WP04 merge) |
| Pill | done-on-branch | WP04: pill 44 dp in a 64 dp capsule (`4ba82c9`); D12 spacing at font 1.5 (`fe182db`) | phone shot at font 1.5 |

### R31. Material 3 policy

> "Keep the custom foundation-only design system and do not depend on any Material component library … Allowed:
> material3.adaptive … Optional later (wave 3, off by default): a materialbridge feature."

| Part | Status | Evidence |
|---|---|---|
| No Material components; adaptive allowed | done-on-branch (rule) + done-and-merged (old rule) | `verifyCodeRules` `material` (allows `material3.adaptive.`), `ripple`, `adaptive-material-scope` on `wp/WP02`; integration branch's `verifyComposeUsage` already refuses `androidx.compose.material` imports. WP04 added no `material3.adaptive` dependency because `StackPlan` places the panes (Deviation 12) |
| `materialbridge` (MB) | **not started** | defined in `scripts/to-release.js` (`MB`), no handoff |

- **Gate check:** `verifyCodeRules`; `./gradlew :app:dependencies` of `everything` shows no `material3` except
  `material3.adaptive`.

### R32. Back swipe during a push

> "A back swipe at any moment, mid-push included, runs the Gmail-style predictive back (REQUIREMENTS 24) from the
> screen's current position. … Never translucent, never double-exposed, every entry opaque. Swipe edge respected …
> Add a back_during_push scenario to perf.sh … and run it on the phone."

- **Status:** done-on-branch.
- **Evidence:** the old build reproduced 3/3 (`qa/user/back-glitch-1309.png`, `qa/user/midback/bdp_compare.png`).
  New: phase3c gate rows "Back swipe during a push: one opaque card, no double exposure: yes" and "lands on the list:
  yes"; recordings `P/WP04/back-during-push/{left,right}` (the card takes over from the detail's position and shrinks
  toward the far edge, opaque until the commit fade). Scenario `back_during_push` in `perf.sh` (`4513af2`). WP04 did
  not use a spring retarget (Deviation 19).
- **Remains:** merge; the multiplatform template holds the gesture during a push (no mid-flight back in JetBrains
  navigation3 1.1.2, `H/WP07-A`).
- **Gate check:** `perf.sh back_during_push` and `record.sh back_during_push` from the left and right edges.

### R33. A highly customisable bottom sheet

> "It is the one sheet system of every generated app, so it must be the best part of the design system, not an
> afterthought." (spec: `research/SHEET-SPEC.md`)

| Part (owner) | Status | Evidence / blocker |
|---|---|---|
| Spec | done | `research/SHEET-SPEC.md` revision 2 (Oct 13; 4108 lines; three designs, three judges, two critics; §19 work split, §22 rejected proposals) |
| Component, state, tokens, catalog (WP03, §19.1) | **in progress, not compiling, uncommitted** | `wt/WP03` dirty tree (backup of Oct 15: `.overhaul/backup/WP03-2026-10-15/`; WIP branch `backup/<name>-wip-2026-10-16`). Last compile error `out/WP03/logs/r12-t4.log`: `syncAnchors` unresolved at `component/sheet/gesture/SheetFlingBehavior.kt:88`; the Oct 15 20:55 run `r14-t1.log` failed earlier, in the build script: `included(...)` is deprecated in the `abiValidation` block at `core/designsystem/build.gradle.kts:25`. Sheet code-rule cases 3/43 failing (`r12-rules1.log`). No ABI dump, no screenshots. Token section committed (`285275d`) |
| Presenter, sheet scene, card stack, predictive back, BackGate (WP04, §19.2) | not started | waits for WP03's merge (`H/WP04` Open issues) |
| Glass style (WP05, §19.3) | in progress | catalog demo only, uncommitted in `wt/WP05-glass` |
| DevTools panel as a sheet (WP06, §19.8) | not started | waits for WP03 |
| Sample sheet destination (WP02-M3) | not started | |
| Compose Multiplatform parity (WP07-B) | not started | phase B |
| SwiftUI `appSheet` (WP08, §19.5) | done-on-branch | `H/WP08` Deviation 15; tokens re-emit pending |
| Phone verification (QA2), docs (WP12) | not started | |

- **Gate check:** SHEET-SPEC §15 tests (incl. `CardStackShellTest`), §16.1 perf rows (`sheet_large`,
  `sheet_drag_glass` janky ≤ 1.0 %), the `no_offscreen` trace check, recordings of detents, card stack and dismiss on
  the phone.

### R34. Shimmer issues and cut content

> "Shimmer: one coherent band must cross every placeholder in the same coordinate space … Zero recomposition per
> frame. Skeletons shaped like the content … Content cut: nothing is clipped by chrome … in light, in dark and at font
> scale 1.5."

Single source of truth: `qa/shimmer/REPORT.md` (written to disk Oct 15; §0 state table, §5 acceptance, §6 owners).

| Part | Status | Evidence | Remains (owner) |
|---|---|---|---|
| Shimmer R1–R9 | done-on-branch (in `wp/WP03` via `wp/WP03-skel` `46ec781`) | two review rounds with 9 blocking findings fixed, review #3 approved (`SESSION-2026-10-15.md`); phone Oct 15 (`qa/shimmer/after/`): one band (median deviation 1.5–2.3 px), highlight, idle 0 frames, cold-start geometry all **pass**; sweep janky **0.69 % vs 0.5 %** recorded as a deviation pending attribution; 129/129 DS tests on the merged tree | `qa/shimmer/VERIFY.md` was never written; attribution of the 0.19-point excess (WP03-skel / QA2) |
| K1, K2, K7, K9 | done-on-branch (WP03), verified Oct 15 | REPORT §1.3, §5.4 step 5 | — |
| K3, K6, K11, D9, D12 | done-on-branch (WP04, committed Oct 15 16:16: `7a50e56`, `fe182db`, `dc5c26c`, `3ff25f8`) | `ChromePaddingTest`, `ChromeLayeringTest`, `TabBarTest`, `WindowManifestTest`; only "before" shots exist (`P/WP04/req34/before-338d26c`, `P/WP04/d9/before-338d26c`) | "after" phone shots (WP04) |
| K3, WP03's half (Utility → `Floating`) | in progress (uncommitted, `wt/WP03`) | REPORT §0 | WP03 commit (see §9 item 8) |
| K5, K8 (search cross-fade, chips cut) | not started | WP02-M3 not started | WP02-M3 |
| K10 (3-button nav, landscape) | coded, device check pending | `AppScaffold` pads `safeDrawing` horizontally | WP03 / QA2 |
| K12 (sheet content at the medium detent) | in progress | R33 | WP03 |
| K13 (badge vs large title) | prepared | `out/WP06/badge_qa_check.py --top-chrome` | WP06 after WP04 merge |
| K4, K14, M-1…M-3 | at the WP03-after-WP04 merge | REPORT §2.13 | whoever merges |
| Scaffolder (K2, C-1) | done-on-branch for its APIs (WP02-M2 `bf4949d`) | redaction moves to WP02-M3 | WP02-M3 |
| SwiftUI / multiplatform parity | built / not started | REPORT §4 | WP08 tokens; WP07-B |

- **Gate check:** REPORT §5: unit tests, zero recomposition per frame, screenshots, phone steps 1–7 including K3 on
  the merged shell, in light, dark and at font 1.5.

### R35. The liquid lens distortion on a tab tap

> "On a tap, the lens travels to the new tab as a liquid droplet. It lifts and swells slightly, refracts and magnifies
> what is under it while it moves, stretches along the direction of travel, and settles with the selection spring …
> Gates: with glass.quality=Live, tab-tap janky % and GPU p90 stay within the glass_live gate … Record before and after
> frame strips."

| Part (owner) | Status | Evidence | Remains |
|---|---|---|---|
| Generator lens (WP05) | **in progress**: the lens is done-on-branch, but nothing on a device shows it yet | `wp/WP05` `904a2a8` … `5d515a0`; travel read off the selection spring's energy, swell, stretch, magnification, tint fade, all in the lens's layer and draw; 89 tests and 19 mutants (`OV/logs/lens/run12-*`, `run13-test1.log`); fresh reviews in runs 10–13, the last kept it unchanged. Tap timings computed, not measured (`OV/logs/lens/run11-timing.txt`) | `LiquidTabs` (uncommitted in `wt/WP05-glass`), glass_live gate on the phone, CP2 tap-morph strips |
| PeriFlow lens (WP11) | **blocked: user decision** | `overhaul/smooth-glass` `58c17e0` → `45b91a2` (+2 dirty files: one more cost attempt in progress). A/B on the Redmi, Live pinned, tab switch janky: **lens-5 (Oct 14 18:04) 3.62 % vs base 3.41 %, gate 2.5 %**; **lens-6 (`45b91a2`, Oct 15 16:25) 3.76 % vs base 3.44 %**, and its visited-tab first frame max 29.65 ms vs gate 20 (lens-5 was 15.5 ms) (`P/WP11/lens-tap/measure-lens-6/summary.md`, `measure-base-ab-lens-6/`). The base itself fails the gate. The three-iteration budget is spent. Fresh-context review (`lensFlow` in `to-release.js`) not passed | §9 item 1 |
| Multiplatform port (WP07-B), phone verification (QA2) | not started | | phase B / QA2 |

- **Gate check:** `perf.sh --extra glass.quality=Live tabs tabs_tap` within `glass_live` (janky ≤ 2.5 %, first frame
  ≤ 10 / 20 ms) and GPU p90 not above the `LiveNoLens` run by more than the column allows; before/after strips of a tap
  to the next tab, two away, a Back, a reversal and two taps 300 ms apart.

### R36. Android first

> "Make the generator live as soon as the Android work is 100% done, and only then. Compose Multiplatform and Swift
> continue afterwards and ship later."

| Part | Status | Evidence / gap |
|---|---|---|
| Every Android-applicable requirement met | not met | see §8 |
| Smoothness 19/20 on the Redmi, release builds | not met | R19, R20 |
| Security (WP06, intent and permission audits) | done-on-branch | `H/WP06` "Scope items" (intent and permission rows), token leak fixed (`12b13f8`, `5387c01`, `SessionScopedStore`), app-lock overlay that cannot be bypassed (`b8fe022`, `a8129c4`); not merged |
| Adaptive UI, accessibility | on branches | R23; WP03 a11y fixes (A4.6), `PaletteContrastTest` |
| Sheet (33), shimmer and clipping (34), lens (35) | not met | R33–R35 |
| Docs (25) | not started | R25 |
| Full Android matrix green | not run | WP13 |
| Order: WP13 verifies → `RELEASE-GATE-android.md` → user approves → merge, push, Vercel | not started | `RELEASE-GATE-android.md` does not exist; the gate step is `releaseGate('android', …)` in `scripts/to-release.js` |
| Live site offers Android only; others "coming soon" | done-and-merged (WP10) | `ddc533f`, `7bde1b1`; `ENABLE_TARGETS=android` still to set on the deploy |
| Phase B (WP07-B/C, WP08 merge, WP12-B, WP13-B, second gate) | not started | WP08 done-on-branch |

- **Gate check:** `RELEASE-GATE-android.md` with `go = true`, then the user's explicit go-ahead in chat.

### R37. The full liquid glass effect at the playground's level

> "Restore every Kyant liquid-glass effect in the iOS-style components, the bottom bar first, with real similarity to
> the library's playground/catalog bottom bar … Then glass top-bar buttons, toggle, slider, segmented control, buttons,
> sheet, alert and toast, plus a Glass playground section in the catalog. … full effects at the Live tier, made cheap
> rather than removed."

- **Status:** in progress (bar written, uncommitted; everything else not started).
- **Evidence:** `H/WP05-glass` §1 (the playground read from source: `research/glass/kyant-catalog/`,
  `research/glass/kyant-src/`, Kyant 2.0.1), §2.1 (each playground effect against our cheap equivalent: blur and
  vibrancy, rim refraction `lens(24, 24)`, rim light, cached shadow, press swell, drag give, press glow, tabs seen
  through the lens, lens dispersion `lens(12, 20, chromaticAberration = true)`, swell 1.39×, velocity stretch, tap
  morph, drag-to-select, shader warm-up). `wp/WP05-glass` has **no commit of its own**: 35 modified + 8 untracked
  files in `wt/WP05-glass` (`LiquidTabs.kt`, `GlassButtons.kt`, `LiquidPress.kt`, `PressGlow.kt`, `RimLight.kt`,
  `GlassTier.kt`, fixtures `everything-glass.json`, `lean-glass.json`).
- **Remains (WP05-glass, then WP05):** commit the bar; top-bar buttons, toggle, slider, segmented, buttons, sheet,
  alert, toast, the catalog's Glass playground; phone strips side by side with the Kyant playground (the user installs
  the playground APK; agents must not download it); `glass_live` gates; an Opus review; WP05 merges it; WP11 ports the
  bar to PeriFlow.
- **Gate check:** as R35, per component; the `kyant` code rule; side-by-side strips approved at CP2.

### R38. Google's Android skills v1.0.15

> "Incorporate the new and updated skills … Every project the generator produces vendors the skills its features use
> (feature-aware), and its AGENTS.md/CLAUDE.md say when to use each. … All agents apply the updated rules in their
> areas."
> Oct 16 addendum: "use the jetpack-compose-performance skill, not only vendor it."

| Part | Status | Evidence |
|---|---|---|
| Plugin on the machine at 1.0.15 | done (reinstall on Windows) | `WIPE-READINESS.md` §2.5 |
| Digest updated | done | `research/ANDROID-SKILLS-DIGEST.md` (Oct 15 20:57; upstream commit `5bda39f…`; says the templates still vendor v1.0.13) |
| Feature-aware vendoring on `wp/WP02` | **not started on the branch** | no commit on `wp/WP02` after `13eb1f1` (Oct 15 16:25); `templates/android/.agents/skills/README.md` still names v1.0.13 |
| Agents apply the updated rules | done in the prompts | `scripts/*.js` (backups `*.pre-compose-perf`) |
| Every agent audits its Compose changes; reviewers treat a violation as BLOCKING | done in the prompts | same; WP06 run 12 started such an audit (`H/WP06` Progress; result not recorded) |
| Template-wide audit `research/COMPOSE-PERF-AUDIT.md`, routed to WP03/WP04/WP05/WP02 | **not started** | file absent |
| Generated projects vendor jetpack-compose-performance and their AGENTS.md says to run it | not started | — |

- **Owner:** the `android-skills-v1-0-15.js` workflow on `wp/WP02`, reviewed by WP02-M2.
- **Gate check:** `test_vendored_skills.py` against v1.0.15; a generated `everything` lists
  `jetpack-compose-performance` in `AGENTS.md`; every routed audit finding closed or rejected with a reason.

---

## 6. The original goals of MASTER-PLAN.md

### 6.1 §1 "The decisions in one page"

| # | Goal (shortened) | Status | Evidence | Owner of what remains |
|---|---|---|---|---|
| 1 | Layout `templates/{android,multiplatform,ios}`, stdlib engine with a plugin per target, `template.toml`, `features/*.toml`, `presets.toml`, shared `tokens/*.json` | done-and-merged (engine, Android) / done-on-branch (multiplatform WP07, ios WP08) | WP00 `6bc38e9`, `merged/WP01` | WP07, WP08 merges |
| 2 | Spec v2, v1 upgrade, marker engine v2, single-pass rename | done-and-merged | `merged/WP01` | — |
| 3 | Android architecture: compact authored, `modular` split, pure `:core:common`, `:core:mvi`, `ModuleGraph` | M1 done-and-merged; M2 done-on-branch; M3 not started | R6 | WP02 |
| 4 | Navigation: Navigation 3 1.2.0, KeptTabs v2, slide-only push/pop, draw-phase scrim, warm pops, BackGate, persistent bar, layering contract, chrome insets, deep links | done-on-branch | R1, R24, R28, R32; `H/WP04` | WP04 merge; sheets §19.2 |
| 5 | Design system: `NoIndication`, `PressFeedback`, silent haptics, `(response, bounce)` springs, iOS type scale, new components, optional groups, deletions, `verifyCodeRules` | done-on-branch (sheet uncommitted) | R2, R4; rule on `wp/WP02` | WP03, WP02 |
| 6 | Liquid glass, off by default, four tiers, scene recorded once, budget GPU p90 ≤ 8 ms and janky ≤ 2 % while scrolling under the bar | in progress, **never measured** | R3, R35, R37 | WP05 |
| 7 | Feature catalogue v2 (removals, Firebase split, `liquidglass`, `performance`, `offlinequeue`, `appstatus`, `ci`, `formatting`, groups; Room 3) | done-on-branch (WP02-M2, WP04, WP06, WP03) | R10 | merges |
| 8 | MVI/data: non-suspending `handleEvent`, `launchLatest`/`launchExclusive`, `TextFieldState` in the ViewModel, `AppError`, offline-first, one `NetworkMonitor`, serialised refresh, full sign-out teardown | M1 parts done-and-merged; data parts done-on-branch (WP06) | `H/WP02-M1`, `H/WP06` "network client and auth flow as built" | WP06 merge |
| 9 | Compose Multiplatform (AGP 9 layout, Koin 4.2.2, JetBrains Navigation 3 1.1.2, Ktor, Room 3 KMP, DS ported, `sync_check.py`, "Beta: iOS unverified") | in progress (phase A partial) | R11 | WP07 |
| 10 | SwiftUI (hidden): SPM `Core`/`Features`, `@Observable` stores, iOS 26 glass behind `#available`, SwiftData, Swift Testing, XcodeGen | done-on-branch, uncompiled | R12 | WP08, WP13-B |
| 11 | Web: platform picker, configurator modules with tests, `/api/v1/{target}/…`, security fixes, versioned telemetry, admin with ranges, filters, charts, CSV | done-and-merged | R13, R14 | WP12 (copy), deploy blockers |
| 12 | Verification: unit tests and static matrix (done); 14-build local Gradle matrix; nightly ~70-row CI; macOS jobs; device gates for E/S/L incl. rapid Back, nav stress, restore; size budgets | partly | static matrix done; Gradle matrix, CI, size budgets not done; device gates measured on `wp/WP04` builds only | WP13 |
| 13 | Process: fresh reviewer per package, device smoke after UI merges, CP1/CP2 from recordings | partly | reviews on merged packages; CP1 decided (`decisions/CP1.md`); **CP2 not decided** (`decisions/CP2.md` absent); no UI package merged yet, so no device smoke has run | orchestrator |
| 14 | Generator tools: `upgrade_project.py`, `make_modular.py`, `add_tab.py`, `add_language.py`, `--diff`, v2-aware add/remove | not started | — | WP14 |

### 6.2 §2 gates on the Redmi (solid column), latest numbers

Sources: old template `P/WP04/baseline` (Oct 9); `P/WP04/phase3c/summary.md` (`everything` at `d10ef94`, 3 runs,
the last full gated run); `P/WP04/phase3e/run1/metrics.json` (one run at `338d26c`, not a gated median);
`phase3c-standard`, `phase3d-lean`. None of these builds contains `730f8e0` or `fb866b3` (R19).

| Row | Old template | Latest new (`everything`) | Solid limit | Result |
|---|---|---|---|---|
| Tab switch janky | 1.58 % | 1.15 % (3c and 3e; standard 1.15 %) | ≤ 1.0 % | **fail** (first frame after idle) |
| Tab switch to a visited tab, first frame median / max | 18.97 / 45.28 ms | 5.49 / 12.06 ms (standard 5.10 / 9.73) | ≤ 8 / 16 | pass |
| First visit to a tab | 19.41 / 25.51 ms | 4.44 / 9.23 ms | ratchet | report |
| Push, the frame that composes the screen | 8.35 / 9.98 ms | 9.42 / 18.27 ms (3c); 14.22 median (3e) | ratchet (target ≤ 12 / 20) | report |
| Pop first frame median / max | 19.09 / 27.64 ms | 5.09 / 11.03 ms (standard 6.54 / 10.55) | ≤ 8 / 16 | pass |
| Push/pop Back key janky / p99 | 4.03 % / 34 ms | 1.68 % / 31 ms (3e 1.58 / 32; standard 1.68; lean **2.24**) | ≤ 2 % / 25 ms | janky pass (lean fail); **p99 fail** |
| Push/pop back gesture janky | 3.54 % | 2.26 % (3e 2.1) | ≤ 2 % | **fail** (marginal) |
| Rapid Back | left the app | lands; 2.16 %; coalesced pop 7.57 / 7.92 ms | no crash, lands, ≤ 4 %, ≤ 8 / 16 | pass |
| Navigation stress | — | alive, invariants hold, 3.13 % (3e single run 4.71 %) | ≤ 4 % | pass in 3c |
| Restore after process death | — | tab, stacks, presented stack restored; Back pops | yes | pass |
| Scroll a root list janky / GPU p90 | 0.22 % / 6 ms | 0.88 % / 6 ms (standard 0.52 %) | ≤ 0.5 % / 7 ms | **janky fail** |
| Scroll with the bar minimizing | — | 0.89 % / 7 ms (3e 0.66 %) | ≤ 0.5 % / 7 ms | **fail** |
| Idle 5 s, frames | 0 | 0 | 0 | pass |
| Cold start, speed-profile | 336 ms | 390 ms (3c); 364 (3e); standard 285; lean 287 | ≤ 380 | everything 3c fail, 3e pass; S/L pass |
| Cold start as installed | — | 419 ms (3c); 440 (3e); standard 312; lean 316 | ≤ 400 | everything **fail**; S/L pass |
| PSS (app's own, WP04 Deviation 20) | 54.5–75.3 MB | 53.6 MB (3e 52.0); standard 41.7 | ≤ 48 MB (the plan's 70 MB total is unreachable: window buffers alone are ~51–54 MB) | everything **fail**; standard pass |
| Recompositions per switch / pop | — | 5 / 4 (3c) → 1 / 2 (3e) | ≤ 2 | pass on 3e |
| Glass `Live` column, `Frosted` vs `Solid` | PeriFlow 4.0 % tab, 6.8 % scroll | **not measured** | §2 glass column | — |
| Size budgets (`tools/size_budgets.toml`) | — | sizes recorded by WP02-M2 and WP06 (`H/WP02-M2`, `P/WP06/sizes.md`) | measured + 5 % | file not written (WP13/orchestrator) |

Note: the `P/WP04-perf/everything` runs of Oct 15 (build `3c9c403`) show app PSS 70.0 and 72.8 MB, far above
phase3e's 52.0 MB for nearly the same code. The cause is not known; re-measure before relying on either.

### 6.3 Checkpoints and package plan

| Item | Status |
|---|---|
| CP1 motion and press feel | decided Oct 9 by the decision agent (`decisions/CP1.md`); values in `tokens/motion.json` on `wp/WP03` (`974d089`) |
| CP2 glass feel | **not decided**: needs WP05's per-tier recordings (`P/WP05/cp2/` does not exist) |
| CP3 release | not reached (R36) |
| WP00, WP01, WP02-M1, WP09, WP10 | done-and-merged |
| WP02-M2, WP04 (+ `wp/WP04-perf`), WP06, WP08 | done-on-branch, waiting to merge (WP04 escalated) |
| WP03 (+ `wp/WP03-skel` merged in) | in progress (sheet uncommitted) |
| WP05 (+ `wp/WP05-glass`) | in progress |
| WP07-A | in progress; WP07-B, WP07-C not started |
| WP11 (PeriFlow) | first half done, lens blocked; second half not started |
| WP02-M3, WP12, WP13, WP14, MB, QA2, WP12-B, WP13-B | not started |

Merge order from `scripts/to-release.js` (`ORDER`): WP04, WP10 (done), WP08, WP06, WP02-M2, WP03, WP05, WP07-A, WP11,
WP02-M3, WP14, MB, QA2, WP07-B, WP07-C, WP12, WP13, WP12-B, WP13-B. `STATUS.md` §2 gives WP04 → WP03 → WP06 →
WP02-M2 → WP08 → WP05 for the Android path. Both put WP04 first; the WP03-after-WP04 conflicts have a tested
resolution in `282dd3c`.

---

## 7. Where other notes disagree with git (corrections)

| Note | What it says | What git and the logs show |
|---|---|---|
| `FABLE-FINDINGS.md` §1 "Still in progress" | "Phone measurement finished on Oct 16 at 10:27 … compares HEAD, fixed, floor" | `out/WP04-perf/logs/measure2.log`: HEAD runs 1–2 complete, run 3 exit 2 (`uiautomator could not dump the screen`); "fixed" exit 2 (`device 9a7c74f4 is not attached`); "floor" never run. Only the HEAD comparison exists |
| `H/WP05-glass` last line | "WP05: merge wp/WP05-glass into wp/WP05 … the orchestrator verified it" | `wp/WP05-glass` has no commit of its own; all its work is uncommitted. WP05 run 13 correctly refused to merge it |
| `SESSION-2026-10-16.md`, task brief | WP04-perf has 5 dirty files | 4 at 11:40 (`WIPE-READINESS.md` §3.1 agrees) |
| `STATUS.md` header | "Oct 10, 2026, 16:05" and package table of Oct 10 | stale below its resume pointer; use `SESSION-2026-10-16.md` and this file |
| `qa/shimmer/REPORT.md` §0 | WP04's K3/K6/K11/D9/D12 "uncommitted" | committed on `wp/WP04` at Oct 15 16:16 (`7a50e56`, `fe182db`, `dc5c26c`, `3ff25f8`) |
| `H/WP04` Measurements | "Phase 3 (the final tree): pending the phone" | phase3c/3d exist and are summarised in its own "Phase 3 on the phone" section; phase3e stopped after run 1 (run 2 has no `metrics.json`); phase3f never ran |
| `REQUIREMENTS-STATUS.md` (Oct 14) | WP02 38 ahead, WP04 83, WP05 14, WP06 30, WP08 64 | now 44, 95, 15, 32, 65 (§0.3) |
| `REQUIREMENTS.md` R34 | `qa/shimmer/REPORT.md` and `VERIFY.md` | REPORT.md now exists (Oct 15 20:55); VERIFY.md still does not |

---

## 8. One-page summary

| # | Requirement (short) | Status | Owner of what remains | Main blocker |
|---|---|---|---|---|
| N0 | Smooth, light apps on the Redmi | in progress | WP04, WP13 | first frame after idle (R19) |
| 1 | Navigation glitches (tabs, push/pop, rapid Back) | done-on-branch | WP04 | merge; R19 gates |
| 2 | No ripple; iOS press | done-on-branch | WP03, WP02 | WP03 sheet WIP blocks merge |
| 3 | Optional, fast liquid glass; PeriFlow too | in progress | WP05, WP11 | no phone numbers |
| 4 | Apple-level components | in progress | WP03 | sheet uncommitted |
| 5 | Better floating bars | done-on-branch | WP04 | "after" shots |
| 6 | Multi-module architecture | M1 merged; M2 on branch; M3 not started | WP02 | WP04/WP06 merges |
| 7 | No AI slop | in progress | all, WP13 | reviews of unmerged packages |
| 8 | Draggable devtools panel | done-on-branch | WP06 | K13 after WP04 |
| 9 | All build options tested | in progress | WP13 | Gradle matrix, CI rewrite |
| 10 | Useful generator features | in progress | WP14 (+ merges) | WP14 not started |
| 11 | Compose Multiplatform | in progress (phase B) | WP07 | WP04 merge, phase B |
| 12 | SwiftUI, hidden | done-on-branch | WP08, WP13-B | no Mac compile |
| 13 | Web, Python, repo docs, picker | web/Python merged; docs not started | WP12, WP13 | WP10 release blockers (licence, privacy page, CI claim, `ENABLE_TARGETS`) |
| 14 | Anonymous admin telemetry | done-and-merged | — | signed-in admin check |
| 15 | Agent docs in repo and projects | in progress (partial) | WP12 | WP12 not started |
| 16 | Document the project | not started | WP12 | — |
| 17 | No opaque block under the bar | done-on-branch | WP04, WP03 | merges, phone shots |
| 18 | Continuous visual defect hunting | 4 named defects done-on-branch; QA2 not started | QA2 | merges |
| 19 | iOS-level smoothness | in progress | WP04, WP13 | **gate decision (§9)**; fixed build unmeasured |
| 20 | Before/after report | in progress | WP13, WP11 | final runs, PeriFlow after |
| 21 | Full iOS animation set | in progress (most on branch; zoom, card stack missing) | WP04, WP03, WP02-M3 | sheet, WP02-M3 |
| 22 | Recomposition | in progress | WP02-M3, WP03 | feature tests |
| 23 | Adaptive UI | in progress | WP03, WP04, WP02-M3 | goldens, feed grid, postures |
| 24 | Gmail predictive back | done-on-branch | WP04 | gesture janky 2.1–2.26 % vs 2 % |
| 25 | Smoothness docs | not started (drafts) | WP12 | — |
| 26 | Google skills (v1.0.13) | done-on-branch | WP02, WP07 | merge |
| 27 | AOSP-quality code | in progress | all | reviews, `explicitApi` |
| 28 | Tab-switch animation | done-on-branch | WP04 | 1.15 % vs 1 % gate |
| 29 | Pull to refresh | done-on-branch | WP03 | merge |
| 30 | DEV badge, crowded pill | done-on-branch | WP06, WP04 | K13 |
| 31 | Material policy; `materialbridge` | rule on branch; MB not started | WP02, MB | — |
| 32 | Back swipe during a push | done-on-branch | WP04 | merge |
| 33 | Bottom sheet system | in progress (spec done; code uncommitted, not compiling) | WP03, WP04, WP05, WP06 | WP03 compile; WP04 §19.2 |
| 34 | Shimmer and cut content | shimmer done-on-branch; K5/K8 not started | WP03, WP04, WP02-M3 | `VERIFY.md`, K3 after shots |
| 35 | Lens on tap | generator in progress; **PeriFlow blocked** | WP05, WP11 | **user decision (§9 item 1)** |
| 36 | Android first | in progress | WP13, release gate | everything above |
| 37 | Full glass, playground parity | in progress (uncommitted) | WP05-glass | commit, phone, review |
| 38 | Skills v1.0.15 + compose-performance | digest done; vendoring and audit not started | skills workflow, WP02 | — |
| MP | Master plan §1 goals 1–14 | 2, 11 merged; 1, 3, 8 partly merged; 4, 5, 7, 10 on branch; 6, 9, 12, 13 in progress; 14 not started | §6.1 | |

**Counts (1–38):** done-and-merged 1 (R14); done-on-branch 12 (R1, R2, R5, R8, R12, R17, R24, R26, R28, R29, R30,
R32); in progress or partly done 23 (R3, R4, R6, R7, R9, R10, R11, R13, R15, R18, R19, R20, R21, R22, R23, R27, R31,
R33, R34, R35, R36, R37, R38; among them R13's web part and R6's M1 are merged, R35's PeriFlow part is blocked on the
user); not started 2 (R16, R25). The Android release is **not ready**: no UI package has merged into the integration
branch yet.

**Shortest path to the Android release gate** (from `SESSION-2026-10-14.md` §4 and the current state): WP04 round-2
measurement and the R19 decision → merge WP04 → WP03 compiles and commits the sheet, merges (reuse `282dd3c`'s
resolution) → WP06 (K13, §19.8) → WP02-M2 (with skills v1.0.15) → WP05 (+ WP05-glass, CP2) → WP02-M3 → WP14, MB, QA2
→ WP12 → WP13 (matrix, final gates, report) → `RELEASE-GATE-android.md` → the user's go-ahead → merge to `main`,
push, Vercel. The ETA of "about Oct 17" (`SESSION-2026-10-15.md`) assumed no crashes and does not hold any more.

---

## 9. Decisions the user must make

1. **PeriFlow lens deviation (R35, WP11).** The tap lens fails the PeriFlow `glass_live`-based gate: tab switch janky
   **3.62 % against a 2.5 % gate, with PeriFlow's own baseline at 3.41 %** (lens-5, Oct 14); the latest attempt
   (lens-6, `45b91a2`, Oct 15) measured 3.76 % against a 3.44 % baseline and pushed the visited-tab first frame max to
   29.65 ms (gate 20). The lens adds about 0.2–0.3 points; the baseline already fails. MASTER-PLAN B.4 (WP11) lets the
   user decide PeriFlow-only costs. Options: (a) accept a recorded PeriFlow-only deviation with lens-5's behaviour;
   (b) one more traced attempt at the per-lift cost (the 2 uncommitted files in `wt/WP11` are such an attempt);
   (c) keep the lens-free tap on PeriFlow (contradicts R35 there). PeriFlow is in neither deploy, so this does not
   block the generator's release.
2. **What "smooth enough" means on this phone (R19, WP04 escalation).** Tab switch janky (1.15 % vs 1 %), push/pop p99
   (31 vs 25 ms), back-gesture janky (2.1–2.26 % vs 2 %) and lean push/pop janky (2.24 % vs 2 %) fail because of one
   late frame at the start of each transition, caused by the phone's clocks at idle (no app API raises them). Options
   from `H/WP04` Open issues: (a) after WP04-perf's round 2 (fixed vs floor) shows the fixed build at the floor, gate
   what the user sees instead (hitch ratio, first-frame UI time, janky frames after the first:
   `late_first_frames/transitions`) and keep the old rows as ratchets; (b) a CP1-level change (the tab icon's
   down-leg on press, so the GPU is awake when the finger lifts); (c) keep the gates and keep working. The standing
   instruction "never compromise quality" makes this the user's call, not an agent's.
3. **Scroll janky 0.5 % gate** (0.81–0.88 % on `everything`, 0.52 % on `standard`): decide after re-measuring with
   WP03's `AppOverscroll` merged.
4. **PSS and cold start on `everything`.** App PSS 52–54 MB vs 48 (five tabs kept warm; `standard` passes at 41.7);
   cold start as installed 419–440 ms vs 400 (`standard`/`lean` pass). Options: a per-preset limit, warm fewer tabs
   (`MAX_WARM_TABS`, trading first-visit speed), or keep working. The plan's 70 MB total is unreachable on this phone
   (WP04 Deviation 20, accept or reject).
5. **Shimmer sweep janky 0.69 % vs 0.5 %** (R34): accept if the attribution shows wake-from-pause frames, as the
   report proposes.
6. **CP2, glass feel** (blur and tint variant, tier policy), once WP05 records the per-tier strips. Earlier the user
   let a decision agent pick CP1; say whether CP2 may be decided the same way.
7. **Release model after the wipe (merge-all).** REQUIREMENTS 36 says: release gate first, then the user's go-ahead,
   then merge to `main`. On Oct 16 (11:11, confirmed 11:17) he asked to merge every branch into `main` with Vercel
   deploys paused (`scripts/merge-all-to-main.js`). **It did not run** (checked 12:45: no `archive/*` tags, every
   branch present, `main` = `99c4edf`). **It is pending his decision; do not run it without asking him.** Ask which
   model holds. Recommendation: keep the gate model and merge once, reviewed, at the release; the script's
   `vercel.json` "pause" pauses nothing (CI's deploy job ships `main`; `HANDOVER.md` §2.8), so if he wants it now,
   disable `ci.yml` first with his OK. If the merge-all ever happens, re-enabling the deploy is the release step and
   needs the gate first.
8. **Utility style's default bar: Docked → Floating** (K3 (e)). A product-visible default; the edit is in WP03's
   uncommitted tree. Confirm it.
9. **Licence wording.** The site says "MIT" but the repository has no `LICENSE` file, and generated projects will
   vendor Apache-2.0 skills. Add MIT `LICENSE` and adjust the copy (WP10 release blocker).
10. **Deploy settings only the user can set:** `ENABLE_TARGETS=android` on Vercel for phase A; production
    `ADMIN_TOKEN` (`openssl rand`), `DATABASE_URL`, `TRUSTED_PROXY=vercel`.
11. **Approvals recorded as waiting:** the Gradle 9.8.1 distribution download (`H/WP02-M2` Deviation 1); `protobuf` for
    the r8-analyzer scoring; display-size overrides on the Redmi for the `large` gate column (ADAPTIVE §0.7).
12. **The Kyant playground APK** for side-by-side glass strips (R37): the user installs it on the phone; agents may not.
13. **The release itself:** the explicit go-ahead in chat after `RELEASE-GATE-android.md` says go.
14. **The new machine and the offline backup** (added in the handover fix round): accept the WSL2 setup and do its
    admin steps (WSL with the user `crazyhusir`, `.wslconfig`, power and lid, Intel DTT, usbipd, `gh auth login`;
    `RESUME-ON-WINDOWS.md` §2.2, §8.4); confirm that the Drive folder `backup-2026-10-16` was uploaded and that
    `01-secrets.zip` (not encrypted, by his choice) is stored safely.
15. **The raw-data draft release.** It was not created by 12:45 on Oct 16. If it still does not exist, say whether it
    is still wanted (the Drive backup holds the same files) or whether the Drive copy is enough.
16. **The public workspace branch's history: resolved on Oct 17.** With the user's OK, the branch's 7 commits were
    rewritten in place, so no commit holds the full `WIPE-READINESS.md` or the other leftovers (`PUSHED.md` §8).


## REQUIREMENTS 39, added Oct 16 (12:10) after this document was written: Cronet / HTTP/3 transport (opt-in)
Status: **not started.** The full text is in `.overhaul/REQUIREMENTS.md` item 39. It is an opt-in generator feature: Cronet installed into the shared OkHttpClient through the cronet-okhttp bridge (HttpEngine on API 34+, else Play Services Cronet, else plain OkHttp), WebSockets kept on OkHttp, certificate pins mirrored into Cronet, and perf-proven before it is recommended. Owners: WP06, WP02, WP12, WP13. Scheduled after the Android release gate.
