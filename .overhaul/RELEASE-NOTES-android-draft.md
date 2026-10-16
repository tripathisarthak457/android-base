# android-base: Android-first release (DRAFT release notes)

Status: DRAFT for user review before deploying. Not published. Requirement: REQUIREMENTS 36.
Drafted 2026-10-14 from .overhaul/ sources. Every claim cites its evidence; anything not verified is marked "unverified".
Legend: [MERGED] = on android-base `overhaul/multiplatform-v2`. [BRANCH] = only on a work-package branch/worktree. [PLANNED] = not started.

## 1. Highlights

(pending)

## 2. Generator and website

All four packages in this section are **[MERGED]** into `overhaul/multiplatform-v2` (tags `merged/WP02-M1`,
`merged/WP01`, `merged/WP09`, `merged/WP10`; checked with `git tag -l 'merged/*'` on 2026-10-14). The branch is
233 commits ahead of `main` (`git rev-list --count main..overhaul/multiplatform-v2`), and **nothing is on `main` or
pushed yet** (STATUS.md header; `main` head is still `99c4edf`).

| Package | Merge commit | Date (IST) | Gate at merge (merges.md) |
|---|---|---|---|
| WP02-M1 core and MVI | `54d078d` | Oct 9, 07:36 | generator unittest 117 OK; `:core:common:test`, `:core:mvi:testDebugUnitTest`, `:app:assembleDevDebug` green |
| WP01 engine v2 | `709612d` | Oct 9, 08:38 | 249 OK / 1 skipped; `genkit.tokens --check`, `genkit.core.catalogue --check`; `assembleDevDebug` green |
| WP09 API and telemetry | `8672003` | Oct 9 | same generator checks; `go vet` and `go test` on all six packages ok |
| WP10 website | `a402d87` | Oct 14, ~18:10 | same generator checks; `tsc --noEmit` clean; 220/220 frontend tests; `next build` passed in WP10's own acceptance (`out/WP10/logs/accept-run14.log`), not rerun at merge |

### 2.1 Generator engine v2 (WP01) [MERGED `709612d`]

- **One engine, many targets.** The old `render.py` is split into a target-agnostic engine with an Android plugin
  (`genkit/core/target.py`, `pipeline.py`), a manifest-driven spec v2 and catalogue v2, and one token source that
  emits Kotlin and Swift design tokens (`python3 -m genkit.tokens`). Evidence: merge message of `709612d`;
  `handoff/WP01.md` "What changed", "Tokens".
- **Old specs still load.** Schema-1 specs are upgraded on read; the record is always written as schema 2
  (`handoff/WP01.md` "Schema-1 upgrade", golden list line `*/generator-spec.json`).
- **CLI.** `create_project.py` gains `--target`, repeatable `--set KEY=VALUE` (`--set key=null` removes a field;
  `--set schema` is refused), `--json`, `--dry-run`; an existing `--out` is replaced only after the new project is
  complete (`46a62a5`, `7eb684c`; `handoff/WP01.md` "CLI", "`--set` rules").
- **Audited generator bugs fixed** (golden comparison, `handoff/WP01.md` "Allowed golden differences", 36 baseline
  specs, `GOLDEN OK`):
  - the double rename (`com.base.apple` became `com.base.applele`; app `Xbaseapp` became `Xxbaseapp`), audit 3.1;
  - packages such as `com.base.baseapp` that failed with "AppConfig.kt not found" now generate; Python-keyword
    segments (`global`, `pass`) are no longer refused, Kotlin/Java keywords are;
  - `keystore.properties` written as ISO-8859-1 with `\uXXXX` escapes (audit 4.3); PKCS12 keystores with a single
    password (`keystores.py`), addressing the audit's "signing breaks when the store and key passwords differ"
    (PROGRESS.md "Audit headlines"). The end-to-end signing fix is **unverified** here beyond the handoff's file list;
  - `--icon` monochrome XML rewrite fixed (`icons.py` regex). Full `--icon` generation is tested only where Pillow
    is installed, and it was skipped in acceptance (`handoff/WP01.md` "Open issues", line 695). **Partly unverified.**
  - The audit's `["auth"]`-alone crash is not named in WP01's handoff. **Unverified** whether it is fixed.
- **Tests.** 249 generator tests pass, 1 skipped (Pillow); `accept.sh` ACCEPT OK (`out/WP01/logs/accept.log`).

### 2.2 Pure-Kotlin core and non-blocking MVI (WP02-M1) [MERGED `54d078d`]

- `:core:common` is pure Kotlin; a new `:core:mvi` whose `MviViewModel` no longer queues every event behind network
  I/O, so Back and typing stay live during loads (`launchLatest`/`launchExclusive`). `:core:model` and
  `:core:coroutines` are deleted (`handoff/WP02-M1.md` commit `c3895f8`; merge message of `54d078d`).
- Every feature and data module gets an `api/` package; an enforced `ModuleGraph` and `verifyApiPurity` task in
  build-logic (`7406029`).
- Review round 1's two blocking bugs fixed (`efe9246`..`e9ed6a6`; `reviews/WP02-M1-round1.json`).

### 2.3 Web API per target and anonymous telemetry v2 (WP09) [MERGED `8672003`]

- **Routes:** `GET /api/v1/targets`, `/api/v1/catalogue`, `/api/v1/health`, `POST /api/v1/{target}/generate`,
  `/api/v1/events`, `/api/v1/feedback`, `/api/v1/client-error`, plus admin routes behind `ADMIN_TOKEN`. Legacy
  aliases (`/api/generate` = android, `/api/options`, `/api/health`, `/api/track` for one release) are kept
  (`handoff/WP09.md` "Public API").
- **Safer generation:** one generator per target with its own semaphore and timeout; the spec bytes are passed
  unchanged after an opaque precheck; stdout/stderr capped; the work directory removed on every path (the old API
  leaked the staging tree; merge message of `8672003`). A shutdown mid-run is `abandoned_running`, not an internal
  error (`6125111`).
- **Telemetry v2, anonymous only** (STATUS.md §3 "Telemetry: anonymous only"): bounded write queue off the response
  path, retention job that also strips addresses, deep links and Apple team ids from stored specs (`f9fe5ad`),
  failure fingerprints by signature (`2c19817`), daily rollups (`migrations/0003_telemetry_v2.sql`,
  `0004_rollups.sql`).
- Acceptance ACCEPT OK with Postgres 17 store tests, the real generator, the container build and `-selftest`
  (`out/WP09/logs/accept.log`).

### 2.4 Website (WP10) [MERGED `a402d87`]

- **Configurator** rebuilt on catalogue v2 and the v1 API: validation in the browser matches the generator's
  (`features/configurator/validation/`, shared fixture `generator/tests/fixtures/validation_cases.json`), draft
  kept with Start over and Undo, keystore signing panel (`handoff/WP10.md` "What changed").
- **Previews:** live phone preview of the chosen look, motion cards with Pause/Play, a styles showcase
  (`handoff/WP10.md` Progress, reviews 4 to 6). The REQUIREMENTS 28 tab-switch transition is not previewed yet
  (WP04 not merged; `handoff/WP10.md` "Open issues"). No Glass style is in the catalogue yet (WP05).
- **Admin metrics** at `/admin`: generations with warnings, ignored fields, notes and the queue/generator/stream
  time split (`d80a3b9`), failures grouped by cause, daily and weekly tables, CSV export. **Unverified in a
  browser behind sign-in**: nothing after the admin sign-in has been clicked through; checked by curl, `tsc` and the
  build only (`handoff/WP10.md` "Open issues", first item).
- **Android-only picker (REQUIREMENTS 36):** the site's own `OFFERED` set is `android`; Compose Multiplatform and
  Swift read "coming soon" (`ddc533f`, `7bde1b1`, tests in `platforms.test.ts`).
- **Accessibility and reflow:** nav fits 320 to 768 px, reduced motion honoured without a hydration error,
  focus kept on self-disabling controls, `aria-describedby` on field errors, AA contrast fixes (reviews 5 to 8,
  `handoff/WP10.md` Progress).
- Eleven review rounds; the eleventh came back clean (PROGRESS.md, Oct 14 ~18:10). Bundle: `/` 56.3 kB / 203 kB
  first load, `/admin` 14.4 kB / 161 kB (`out/WP10/logs/accept-run14.log`).

## 3. Android template

**Nothing in this section is merged yet except the WP02-M1 core (2.2).** Every item below is **[BRANCH]** unless
marked otherwise. Branch heads on 2026-10-14 (`git -C wt/<x> rev-list --count overhaul/multiplatform-v2..HEAD`,
`git status --porcelain`):

| Package | Branch | Ahead | Uncommitted files | Head |
|---|---|---|---|---|
| WP04 navigation, shell, perf | `wp/WP04` | 83 | 1 | `bdf61e8` (Oct 14 18:01) |
| WP03 design system | `wp/WP03` | 68 | 47 | `285275d` (Oct 14 18:08) |
| WP03-skel shimmer fix | `wp/WP03-skel` | 9 beyond `wp/WP03` | 13 | `e2cec54` (Oct 14 18:48) |
| WP06 data, network, security | `wp/WP06` | 30 | 2 | `1dcacd9` (Oct 14 12:50) |
| WP02-M2 architecture, build, skills | `wp/WP02` | 38 | 0 | `4d1a412` (Oct 13 16:32) |
| WP05 liquid glass, lens | `wp/WP05` | 14 | 0 | `30c03ad` (Oct 14 18:17) |

None of these branches contains `merged/WP10` yet (`git merge-base --is-ancestor`); each will merge the integration
branch before its own merge. Uncommitted counts change while agents run.

### 3.1 Smoothness (REQUIREMENTS 19, 20): measured numbers, INTERIM

All numbers: Redmi Note 10 Pro (`9a7c74f4`, 120 Hz), release or benchmark-release builds, speed-profile compiled,
3 runs, medians. **Interim: phase 3 is not finished, WP04 is not merged, and the gates do not all pass.** WP04's
handoff state is "escalated" (`handoff/WP04.md` Progress, Open issues first item).

Sources: old template `perf/WP04/baseline/summary.md` (Oct 9); phase 2 `perf/WP04/phase2/summary.md` (Oct 9) and
STATUS.md §4; phase 3 `perf/WP04/phase3c/summary.md` (everything preset, Oct 14 13:13, gate `solid`),
`perf/WP04/phase3c-standard/summary.md`, `perf/WP04/phase3d-lean/summary.md` (Oct 14 17:41).

| Metric (everything preset unless noted) | Old template | Phase 2 | Phase 3c (latest full run) | Gate limit | Phase 3c result |
|---|---|---|---|---|---|
| Rapid Back lands on the right screen | **no, exits the app** | yes | yes | yes | pass |
| Rapid Back, janky % | 5.32 | 2.62 | 2.16 | 4 | pass |
| Rapid Back, coalesced pop slowest frame median / max (ms) | n/a | 23.63 / 34.56 | 7.57 / 7.92 | 8 / 16 | pass |
| Tab switch to a visited tab, first frame median / max (ms) | 18.97 / 45.28 | 6.51 / 16.45 | 5.49 / 12.06 | 8 / 16 | pass |
| Tab switch, janky % | 1.58 | 1.90 | 1.15 | 1 | **fail** |
| Pop, first frame median / max (ms) | 19.09 / 27.64 | 15.01 / 36.93 | 5.09 / 11.03 | 8 / 16 | pass |
| Push/pop with Back key, janky % / p99 (ms) | 4.03 / 34 | 2.34 / 57 | 1.68 / 31 | 2 / 25 | janky pass, **p99 fail** |
| Push/pop with back gesture, janky % | 3.54 | 2.82 | 2.26 | 2 | **fail** |
| Back gesture, slowest of first three frames, median (ms) | n/a | n/a | 2.39 | 8 | pass |
| Back swipe during a push: lands on the list, no double exposure (REQ 32) | no (reproduced 3/3, PROGRESS.md Oct 9 13:10) | holds the swipe | yes / yes | yes | pass |
| Scroll a root list, janky % / GPU p90 (ms) | 0.22 / 6 | 0.15 / 6 | 0.88 / 6 | 0.5 / 7 | **janky fail** |
| Scroll with the bar minimizing, janky % | n/a | 0.30 | 0.89 | 0.5 | **fail** |
| Idle 5 s on every tab, frames rendered | 0 (608 in run 1) | 0 | 0 | 0 | pass |
| Process death: tab, stacks and presented sheet restored; Back pops the restored stack | unverified | unverified | yes / yes | yes | pass |
| Cold start, speed-profile, median (ms) | 336 | 355 | 390 | 380 | **fail** |
| Cold start as installed, median (ms) | n/a | 416 | 419 | 400 | **fail** |
| PSS after the journey (MB) | 109.8 | 93.8 | 107.3 total; 53.6 less window buffers | 48 (less buffers) | **fail** |

Standard and lean presets, phase 3 (`phase3c-standard`, `phase3d-lean`): cold start 285 ms (standard) and 287 ms
(lean) against the old template's 271 / 306 ms (`handoff/WP04.md` Measurements), both pass their 380 ms limit;
standard PSS 96.1 MB. Still failing: standard tab switch janky 1.15 % (limit 1), standard push/pop p99 27 ms (25),
standard scroll janky 0.52 % (0.5), lean push/pop janky 2.24 % (2) and p99 28 ms (25).

Note on the STATUS.md §4 table: its "new build" column is phase 2 (Oct 9) and is now superseded by phase 3c above.
STATUS.md §4's "below 336 ms" cold-start target and "≤ 70 MB" PSS target differ from the `solid` gate limits
(380 ms, 48 MB less buffers) in `phase3c/summary.md`; which one the release holds to is for WP13 to state
(**unverified** which is binding).

What the failing rows are made of (`handoff/WP04.md` "Phase 3 on the phone: what the failing rows are made of"):
- Tab switch and push/pop p99: one janky frame per transition, always the first, because the GPU is clocked down
  when a transition starts from idle (FrameTimeline deadline 13.67 ms; first-frame GPU 9–16 ms whatever is drawn).
  Excluding first frames, tab switch janky is 0.00 %, and tab-switch hitch time is 0 ms/s. The old template has the
  same floor (1.58 %).
- Scroll: late frames at 14–15 ms plus one-time shader compiles in the stretch overscroll layer, and a
  pull-to-refresh indicator `saveLayer`; the fix is WP03's `AppOverscroll`, not on WP04's branch.
- PSS: every tab is now pre-built and kept composed while idle (that is what makes first visits as fast as
  switches); the everything preset has five tabs. Trade-off knob: `MAX_WARM_TABS`.
- Cold start: baseline profiles split into modules shipped in `c14848d` (WP04 run 15); phase 3d (with the shipped
  profile) has no summary file (run 3 lost; `perf/WP04/phase3d/run1..run3`), so its cold-start number is
  **unverified** here. A phase 3e run was in progress at the time of writing (`perf/WP04/phase3e/` empty).
- Decision pending (the orchestrator, then the user): accept the first-frame rows as ratchets and gate what users
  see (hitch ratio, late first frames), change CP1's tab-icon timing, or wait for WP03's GPU savings
  (`handoff/WP04.md` Open issues, ESCALATION).

### 3.2 Navigation and Gmail-style predictive back (WP04) [BRANCH `wp/WP04`]

- **Shell rewrite**: Navigation 3 based stack scene, tabs kept composed and pre-warmed while idle, a bar that moves
  with the screens, Back handlers that turn off without recomposing (`227e1e9`), tabs composed in the frame of the
  tap (`e753cb0`). 120 navigation tests pass (`handoff/WP04.md` Progress, run 16).
- **CP1 motion** (`decisions/CP1.md`, STATUS.md §3): push/pop 0.344 s, no bounce, parallax 0.333; tab switch is a
  12 dp nudge with a background veil, a two-edge indicator, the icon dipping to 0.84; bar labels always shown,
  bar minimizes on scroll; BackGate releases at 99 %. The spring clock starts after the composing frame: first
  moving frame shows 3.6 % of travel (limit 4 %; phase 2 showed 16 %) (`handoff/WP04.md` Measurements,
  `perf/WP04/phase3-frames/push`).
- **Gmail-style predictive back (REQUIREMENTS 24)**: the page shrinks into a card that follows the finger; commit
  never returns toward rest before the pop; cancel springs back; back-to-home runs the system animation at the start
  tab's root. Recorded on the phone: `perf/WP04/predictive-back/{back_slow_commit, back_slow_cancel,
  back_fast_flick, gesture_pop, back_to_home, back_slow_commit_right, dark/back_slow_commit}`. `back_y_follow`
  recorded nothing on the phone (emulator only; **unverified on the phone**).
- **Back during a push (REQUIREMENTS 32)**: the swipe takes over from the sliding page, every frame opaque, lands
  on the list (`perf/WP04/back-during-push/{left,right}`; phase 3c gate rows pass).
- **Rapid Back**: bursts coalesce into one pop and land correctly; the old template left the app
  (STATUS.md §4, `phase3c` rows).
- **Tab switch (REQUIREMENTS 28)**: nudge and unveil on the incoming tab only, no blank frames
  (`perf/WP04/tab-switch/{light,dark,four-tabs}`). RTL clips came out left-to-right on the phone (app locale did not
  take) and 2x font is pending: **unverified for RTL and large font on the phone**.
- **Not done on this branch** (wait for WP03's merge; `followups.md` WP04): the sheet system's navigation half and
  the card stack (SHEET-SPEC §19.2), zoom/hero transitions (MOTION-PLAN M5), the first-swipe shader warm-up
  (SMOOTHNESS S6, up to 70 ms once per install).

### 3.3 Design system (WP03) [BRANCH `wp/WP03`, 47 files uncommitted]

- **Custom, foundation-only, no Material components.** The build refuses `import androidx.compose.material*` and the
  ripple (`verifyComposeUsage`, already true on the integration branch per `handoff/WP10.md` "Release blockers").
  `androidx.compose.material3.adaptive:*` is allowed (STATUS.md §3). `materialbridge` is optional and last
  (REQUIREMENTS 31; not started, STATUS.md §2).
- **Tokens drive the theme with the CP1 motion values** (`974d089`); one token source emits Kotlin and Swift
  (WP01). Press feedback replaces the grey 9 % overlay ripple the audit found (PROGRESS.md "Audit headlines";
  `handoff/WP03.md` "Press feedback as built").
- Done on the branch (`handoff/WP03.md` Progress, runs 3 to 6): call-site sweep (`1b7205f`), multi-line fields
  (`a276881`), segmented thumb (`c0b8e86`), adaptive layer v2 (`42d10b7`), large-title search accessory and
  inline-title fade (`48c9542`), scrolling, control motion, context menu, adaptive components (`6c040ba` to
  `75cb68a`), PascalCase constants (`e68bbfa`), link buttons with a full-size touch target (`9823262`), sheet
  tokens and a sheet material per design style (`285275d`).
- In progress (run 12, uncommitted at the time of writing): the sheet system, catalog Sheets page (19 demos), QA
  items D10, D11, D12, K3; then the ABI dump and `accept.sh`. **Unverified until committed and accepted.**
- Waits for WP04's merge: destination sheet demos (`AppSheetNavHost`), filled glyphs, `AppTheme.springs`.

### 3.4 Pull to refresh (REQUIREMENTS 29) [BRANCH `wp/WP03`]

Rebuilt as one node (`92045c7`), spinner in its own layer (`4a3f9e4`), the large title no longer overlaps scrolled
rows (`ec4dcb7`). Phone, release builds (`handoff/WP03.md` Measurements, data `perf/WP03/ptr/`):

| Build | Janky light / dark | GPU p90 |
|---|---|---|
| Old control (`measure-before/`) | 1.10 % / 1.08 % | 9 / 7 ms |
| First rebuild (Oct 9, recorded during `screenrecord`) | 16.81 % (light) | n/a |
| Current (`4a3f9e4`, `r8-after-2/`) | **0.54 % / 0.53 %** | **6 ms** |

Target ≤ 1 % janky and GPU p90 ≤ 7 ms: met in both themes. One round-1 light set read 5.34 % and was recorded as an
outlier (not reproduced in its own trace or the reverse-order round). Remaining janky frames are each pull's first
frame after idle. STATUS.md §4 cites `measure-head` 0.54 / 0.67 %; the handoff's later round 2 gives 0.54 / 0.53 %.
Not re-measured since WP04's phase 3 found a pull-to-refresh `saveLayer` in the scroll trace (3.1): **unverified**
whether that affects these numbers.

### 3.5 Shimmer and clipping fix (REQUIREMENTS 34) [BRANCH `wp/WP03-skel`, merged into `wp/WP03`'s line, not integrated]

- **One clock per window, no recomposition per frame.** `Modifier.skeleton`, `AppRedacted`, one `SkeletonNode` in
  its own layer; a single tilted band per window, translated rather than masked (no `clipPath`, no `saveLayer`).
  Sweep 1,200 ms then 800 ms rest; stops when no bone is on screen; honours reduce motion (`handoff/WP03-skel.md`
  "The shimmer").
- **Loading lists are the real row redacted**, in a list with the loaded list's chrome padding, so content no
  longer jumps or gets cut by the bars (K1, K2, K7, K9, K10).
- **Token contrast:** bone at least 7 L* from every surface; a BrandTint bug that failed `PaletteContrastTest` for
  33 of 200 golden accents was fixed in the second review.
- **State:** two fresh-context reviews done, blocking findings fixed; `wp/WP03` merged into `wp/WP03-skel` at
  `46ec781` (Oct 14 18:53). **Phone verification (report §5.4) not done**; `qa/shimmer/VERIFY.md` does not exist.
  `qa/shimmer/REPORT.md` was never written to disk (the harness refused the write; a copy is in the session
  scratchpad, `handoff/WP03-skel.md` "Source of truth").
- **Not done:** K3 (Utility style's bar Docked to Floating in `tokens/styles.json`): the auto-mode classifier
  refused it as a shared-resource edit; **needs the user's go-ahead** (`handoff/WP03-skel.md` "Not done").

### 3.6 Bottom sheet (REQUIREMENTS 33): spec final, build in progress

- **Spec [DONE, not code]:** `research/SHEET-SPEC.md` revision 2 (4,108 lines, Oct 13 16:30), from three competing
  designs, three judges and two critics; 95 findings landed (PROGRESS.md Oct 13 16:30). Headlines (§1): one
  `AppSheet` with detents `Large`, `Medium` (0.5 H), `FitContent`, `Peek`, `Fraction`, `Height`, `Custom`;
  floating at partial detents and attached at Large (iOS 26 style); card stack at Compact width; Solid, Frosted and
  Glass materials; Back closes in one gesture; no `Dialog` or `Popup` window anywhere; focus moves into modal
  sheets; reduce motion honoured.
- **Build [BRANCH, partly uncommitted]:** WP03's half (§19.1) is in `wt/WP03` run 12, uncommitted. WP04's half
  (§19.2: destination sheets, card stack, Back routing) starts after WP03 merges (`followups.md` WP04). WP06's
  DevTools panel moves onto `AppSheet` after WP03 (§19.8). Phone gates (§16) not run. **Not shippable today.**

### 3.7 Liquid glass and the travelling lens (REQUIREMENTS 35) [BRANCH `wp/WP05`]

- **Glass foundation** (Kyant backdrop library behind `<opt:liquidglass>`) committed (`handoff/WP05.md` Progress,
  run 3).
- **Generator lens** (`ds/foundation/glass/LiquidLens.kt`): every selection change (tap, Back, deep link, drag
  release) moves the lens on its settle spring; while it travels it swells, stretches along the path, magnifies
  the sampled scene and fades the resting tint, all driven from the spring's energy, read only in the lens's own
  layer and draw (no bar re-render, no dispersion, no inner shadow). At rest it samples nothing. Live tier only,
  off under reduce motion. Commits `904a2a8`, `095e1b1`, `8df970b`, `189749f`, `15b9f88`, `6c2c23a`, head
  `30c03ad`; 68 tests pass in run 8, plus mutation checks (`handoff/WP05.md` "Lens on tap", Progress).
- **Not on a device yet:** the generator has no `LiquidTabs` (step 6), which waits for WP03 and WP04, so nothing in
  a generated app shows this lens and the `glass_live` gate and CP2 tap-morph strips have not run
  (`handoff/WP05.md` Progress, runs 7 and 9). **Unverified on the phone.**
- PeriFlow's equivalent (WP11, local branch `overhaul/smooth-glass`, never pushed) has phone runs in
  `perf/WP11/lens-tap/`; it is **not part of this release** (REQUIREMENTS 36).
- The website has no Glass style or `liquidglass` feature in the catalogue yet (`handoff/WP10.md` Open issues).

### 3.8 Data, network and security (WP06) [BRANCH `wp/WP06`, 2 files uncommitted]

From `handoff/WP06.md` "What changed" and "Scope items":
- **Network:** `:core:network` rebuilt on Ktor with one shared `OkHttpClient` (HTTP cache, pins) behind every client
  and Coil; retry policy, in-flight de-duplication, connectivity monitor, WebSocket, optional offline queue.
- **Session safety:** tokens in one encrypted DataStore value with compare-and-set refresh, so a refresh landing
  after sign-out cannot restore the old user; one `SessionScopedStore` teardown list (WebSocket, push
  registration, flags, analytics user id). This addresses the audit's sign-in token leak (PROGRESS.md "Audit
  headlines"); a dedicated regression test for that leak is **unverified** here.
- **Database:** Room 3 (`androidx.room3` 3.0.3) with a generated-points scheme for scaffolded modules.
- **Push and Firebase split:** `push`, `crashlytics`, `firebaseanalytics`, `remoteconfig` are separate features over
  an implied `firebase`; `POST_NOTIFICATIONS` asked in context. The audit's "notifications never shown on Android
  13+" is addressed by the in-context permission (`rememberNotificationPermission`); **not verified on an Android
  13+ device** in the sources read.
- **Intent and permission security** (Google's skills): only `MainActivity` exported, explicit immutable
  `PendingIntent`s, no intent from extras launched, no `CAMERA` or storage permission, `FileProvider` not exported.
- **App lock** (`:core:applock`): the cover blocks touch, focus and accessibility (`AppLockGateTest` 3/3).
- **Sizes** (`perf/WP06/sizes.md`, devRelease arm64): standard APK 2,120,539 to 2,098,646 B (-1.0 %); everything
  4,606,428 to 4,024,923 B (-12.6 %).
- **Device checks:** badge drag OK on MIUI (`accept11`); QA-44 failure found and fixed (repository read-back);
  the final phone acceptance (`accept13`) was running at 17:22 Oct 14, result **unverified**. `followups.md` lists
  WP06's remaining device checks for QA2.

### 3.9 Adaptive UI [BRANCH `wp/WP04`, `wp/WP03`, `wp/WP02`]

- Bar on phones, rail on medium and expanded, sidebar on large windows; keyboard shortcuts and focus groups;
  multi-window. Emulator run `perf/WP04/adaptive/emulator/summary.md` (Oct 13): fold 673x841 dp Rail, tablets
  1024x640 Rail and 1280x800 Sidebar, 320x568 Bar, desktop 1920x1080 Sidebar, rotation and a second window, all ok.
- On `wp/WP02` (not merged): `verifyManifestRules` fails a merged manifest with an orientation or aspect-ratio lock
  or `resizeableActivity="false"`; `verifyCodeRules` has `adaptive-orientation`, `adaptive-screen-metrics` and
  `adaptive-device-type` rules; the orientation lint ids are errors (`handoff/WP02-M2.md` lines 142, 192, 517).
  The website's "designed for every screen" claim stays false on the integration branch until this merges
  (`handoff/WP10.md` Release blockers).
- **Not verified on a physical foldable or tablet**; posture/fold checks are emulator-only (`handoff/WP04.md`
  Progress table).

### 3.10 Architecture, build and Google's Android skills (WP02-M2) [BRANCH `wp/WP02`]

- Compact by default, modular `:api`/`:impl` as an option (`modes.py`); scaffolder in ktlint's layout at every
  module-name length; 40 `verifyCodeRules` rules (e.g. no `Modifier.composed`, no `collectAsState` in features,
  keys on lists), `verifyManifestRules`, lint hardening; `ci` and `formatting` features; Kotlin 2.4.21 and other
  bumps (STATUS.md §2; `handoff/WP02-M2.md` status table). Acceptance `ACCEPT OK` at `503c610`
  (`out/WP02/logs/accept-m2.log`; 290 generator tests then; 299 by run 11). Database scaffolding waits for WP06.
- **Google's Android skills vendored** into every generated project: `templates/android/.agents/skills/` on
  `wp/WP02`, from github.com/android/skills tag v1.0.13 (commit `42dc2270e9…`), Apache-2.0, unmodified with a
  `MANIFEST.sha256`. Always shipped: navigation-3, navigation-event, adaptive, edge-to-edge, testing-setup,
  android-profiler, r8-analyzer, android-intent-security, android-permissions-security, agp-9-upgrade; with
  `auth`: restore-credentials, verified-email (`wt/WP02/templates/android/.agents/skills/README.md`; commits
  `09ba713`, `ca5e6c9`). Pointers in `.claude/skills/`; `tools/update_android_skills.py` refreshes them.

## 4. Breaking changes and upgrade path

### 4.1 For people who generate projects (spec, CLI, API)

| Change | Status | Evidence |
|---|---|---|
| Spec schema 2. Schema-1 specs are upgraded on load and the record is always written as schema 2. Old specs keep working; saved records change shape | [MERGED] | `handoff/WP01.md` "Schema-1 upgrade"; golden list `*/generator-spec.json` |
| Removed feature keys are dropped with a note, not an error: `fastlane`, `architecturetests`, `screenshottests`, `staticanalysis`, `ci` | [MERGED] | `templates/android/removed.toml` on `overhaul/multiplatform-v2` |
| Further removed keys: `widget` (Glance), `workmanager` (now inside `offlinequeue`), `room` (use `database` or `offlinequeue`) | [BRANCH `wp/WP06`] | `wt/WP06` `templates/android/removed.toml` |
| `ci` and `formatting` return as new features | [BRANCH `wp/WP02`] | `handoff/WP02-M2.md` M2.7 to M2.9 |
| PKCS12 keystores use one password for store and key; the key-password field is dropped for PKCS12 with a warning | [MERGED] | MASTER-PLAN A1.10 "Keystores"; `handoff/WP01.md` `keystores.py` |
| Translations moved to one file per module per language (`generator/translations/android/<lang>/<module>.json`); contributors of strings must use the new layout | [MERGED] | `handoff/WP01.md` "Translations" |
| API: v1 routes under `/api/v1/...`; legacy `/api/generate`, `/api/options`, `/api/health` kept; `/api/track` kept for one release only | [MERGED] | `handoff/WP09.md` "Public API" |
| API visitors are no longer told when keystore creation was skipped (header never sent by the v2 generator) | [MERGED, regression] | `handoff/WP09.md` and `handoff/WP10.md` Open issues |

### 4.2 For code inside generated projects (what differs from an old project)

| Change | Status | Evidence |
|---|---|---|
| `:core:model` and `:core:coroutines` deleted; `:core:common` pure Kotlin; new `:core:mvi`; every ViewModel migrated; `api/` packages in every feature and data module, enforced by `ModuleGraph`/`verifyApiPurity` | [MERGED] | `handoff/WP02-M1.md` `c3895f8`, `c1a9fdd`, `7406029` |
| `AppBottomBar` and `AppNavigationRail` removed (replaced by `AppTabBar`, `TabRail`, `AppSidebar`) | [BRANCH `wp/WP04`] | `handoff/WP04.md` line 166 |
| `AppSkeletonLine(height)` and `AppSkeletonListItem` deprecated (WARNING); `LocalShimmer` now internal | [BRANCH `wp/WP03-skel`] | `handoff/WP03-skel.md` "The shimmer" |
| Old sheet API (`overlay/AppSheet.kt`, `AppSheetState.kt`, `core/navigation/SheetScene.kt`) replaced; `AppBottomSheet` and legacy `AppActionSheet` deprecated | [BRANCH `wp/WP03`, uncommitted] | `handoff/WP03.md` Progress, run 11 |
| `app/push`, `app/lock`, `app/playstore`, `app/widget` deleted; push, lock and Play Store move into `:core:notification`, `:core:applock`, `:core:playstore`; Ktor auth plugin removed | [BRANCH `wp/WP06`] | `handoff/WP06.md` "What changed" |
| Room 2 replaced by Room 3 (`androidx.room3` 3.0.3) | [BRANCH `wp/WP06`] | `handoff/WP06.md` |
| Material component imports and the ripple fail the build (`verifyComposeUsage`); 40 `verifyCodeRules` rules and `verifyManifestRules` will fail builds that break them | ripple/Material [MERGED]; rules [BRANCH `wp/WP02`] | `handoff/WP10.md` Release blockers; `handoff/WP02-M2.md` |

A complete list of every renamed or removed public API per package is in each handoff's "Public APIs added, changed,
removed" section; WP12 is meant to turn these into the migration notes. **This draft does not claim the list above
is complete.**

### 4.3 Upgrade path for projects generated before this release

- **There is no automatic upgrade for projects generated by the old generator (v1).** By design, every WP14 project
  tool reads `generator-spec.json` and refuses a v1 project (no record, or a record without `schema: 2`) with a
  message pointing at `docs/upgrading-v1-apps.md` (`design/wp/WP14.md` step 1; MASTER-PLAN A1.10, A11.2).
- **`docs/upgrading-v1-apps.md` [PLANNED, WP12]:** a by-hand guide covering module layout, MVI, features and
  navigation, adopting kept tabs, BackGate, chrome insets and press feedback, and why the tool refuses v1
  (MASTER-PLAN A11.2). It names WP06's DevTools badge fix as the patch to port for apps like VitFleet. Not written
  yet; `docs-draft/` holds SMOOTHNESS, MOTION, AGENTS-SECTION and ADAPTIVE drafts only.
- **WP14 project tools [PLANNED, not started]** (STATUS.md §2 "Not started"; no `wt/WP14`), for projects generated
  by the new generator (schema 2):
  - `upgrade_project.py <project> [--diff]`: regenerates the recorded spec at the recorded generator commit and at
    the current one, then three-way merges per file with `git merge-file`; leaves conflict markers and writes
    `upgrade-report.md`; never touches files the project added; `--diff` writes nothing; needs a clean git tree.
  - `make_modular.py` (compact to `:api`/`:impl`), `add_tab.py` (5-tab cap), `add_language.py`, and
    `add_feature.py`/`remove_feature.py` updated for both modes.
  - Waits for WP02-M2 and WP06 to merge (`design/wp/WP14.md` header). It is on the Android critical path
    (`SESSION-2026-10-14.md` §4 step 3).

## 5. Known gaps before release

(pending)

## 6. Coming later

(pending)

## Sources

(pending)
