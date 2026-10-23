# WP12-DOCS handoff (stage 4 of the release pipeline: the docs base)

## Progress

Run 3 (fix #1, the first review's three blocking findings), 2026-10-24 01:55-02:30 IST. Staging still at
`d429cbfbf` (the merge at the start was a no-op). The fix is two commits on top of `0ec84b634`; their shas
are in "Verification".

- **Private names in `test_docs.py` (blocking 1): fixed.** `WORKSPACE_WORDS` keeps only the workshop words
  (`overhaul`, `staging-wp`, `WP\d\d`, `REQUIREMENTS`). No guard for private names stays in the test: the
  combined review already blocks them and the orchestrator's pre-push scan covers them, and any form in
  the public tree (plain or hashed: six lower-case letters brute-force in seconds) would leak them. The
  commit message and this handoff stay generic. **Needs the orchestrator:** commit `0ec84b634` still
  carries the three names in its tree, and section 3.2's `git merge --no-ff` would put that commit into
  staging's history and, at release, main's. This run may not rewrite history. The branch was never
  pushed (no `origin/staging-wp/WP12-DOCS`), so either merge this stage with `git merge --squash` (one
  commit, whose tree has no names) or, with the user's OK, recut the branch as one commit. `git log --all
  -S<name> -- generator/tests/test_docs.py` finds only `0ec84b634`.
- **Done-condition that no change could meet (blocking 2): fixed, with a test.** The three places
  (`AGENTS.md` section 6 "Measuring" and section 10 item 7, the guide's "Before you merge UI work" item 8)
  now say, in one wording: on a release build, `--gate solid` fails on no limit but those "Where the
  template stands" lists as over, none of those is worse than it lists there, and against the previous
  build the same day `--baseline … --gate smoke` passes (and, in two of them, no other row got worse).
  "Where the template stands" gained a `performance`-fenced paragraph: the exception exists only while a
  row is over, a change that brings one within updates the table, and the exception goes when every row
  reads "within". `PENDING.md`'s rule on that table says the same. New test
  `test_being_done_excuses_only_the_rows_the_guide_lists_as_over`: red on `0ec84b634`'s docs
  (`logs/WP12-DOCS/f1-test_docs-red-final.log`, all three conditions), green after; it also fails once every
  row is "within" and the exception is still there, and when the three wordings differ.
- **`gridMinCellWidth` in `ARCHITECTURE.md` (blocking 3): fixed at the root.** Line 586 names
  `listPaneWidth`, `gridMinCardWidth` and `gridMinTileWidth`. The miss came from the name check, which only
  caught a name the template declares and a project lacks. New test `test_every_name_the_docs_cite_exists`
  checks every CamelCase name in backticks on every page of the template (`AGENTS.md`, `README.md`,
  `docs/*.md`) against the template's identifiers, its code files' names, its variants' task names and
  two short lists in the test (`LIBRARY_NAMES`: platform and library names; `EXAMPLE_NAMES`: made-up
  names in examples). Red on `0ec84b634` with `gridMinCellWidth`, `userMessage` (twice) and `isOffline`;
  green after. Names still stale on pages this stage does not own are pinned in `STALE_NAMES`
  (`README.md`: `entryPoint`; `ARCHITECTURE.md`: `AppIndication`, `AppNavHost`, `DeepLinkResolver`,
  `Paged`, `debugEnabled`, `surfaceRaised`), which must equal what the page cites, so it only shrinks:
  WP12-KNOW rewrites those paragraphs and empties it (routed below).
- Non-blocking, fixed: `failure.userMessage(...)` in `AGENTS.md` and `ARCHITECTURE.md` now
  `AppError.toUiText(fallback)` (`:core:ui`), with `ARCHITECTURE.md`'s "Failures are classified" bullets
  rewritten to the real `AppError` cases (Offline, Timeout, Http with `status`, `code`, `message`,
  `fieldErrors`, the `Retry-After` wait; the monitor counts any network that claims internet, not a
  validated one); `showMessage(text, kind = MessageKind.Error)` and its own `messages` channel, shown as
  toasts (`MessageHost`); the root `AGENTS.md` now says what the name check covers; `check_gates.py`
  re-gating two runs on disk in "The perf harness"; every `####` directly under a `##` is now `###` (39
  headings; slugs unchanged), rule 9 links the heading's slug and the inline `<a id>` is gone; the image
  loader's heading moved inside the `network` fence, with "One image loader" as the `opt-else` heading
  (rendered for a coil-only project); the display override "restored after", not the workshop's phrasing, in the guide and in
  `gates.toml`'s header comment (no row touched);
  section 6 says `LocalBottomChrome.current.value`. `PENDING.md`: a note on heading levels for pasted
  entries, and the Coil entry points at both headings.
- Non-blocking, routed, not done here: the `HostCrossfade` KDoc (`layer-alpha-fill`, its per-frame
  offscreen cost) belongs to its owner's next change; the other routing notes below are unchanged.

Run 2 (impl), 2026-10-24 01:14 IST. Staging still at `d429cbfbf` (the merge at the start was a no-op).

- Run 1's uncommitted work reviewed claim by claim against staging `d429cbfbf` and kept: the guide
  `templates/android/docs/PERFORMANCE.md`, `templates/android/AGENTS.md` section 6 "Smooth and light" (sections
  6-10 renumbered 7-11; the only numbered reference, "(§8)" in "Before you say it is done", now reads §9),
  the two checklist lines, the README pointer, the generator repository's `AGENTS.md` pointer, run 1's
  stale-line fixes in `AGENTS.md` and `docs/ARCHITECTURE.md`, and the `layer-alpha-fill` comment in
  `ComposeRules.kt` naming both shell exceptions.
- Checked on staging (grep and reading the code; every number below matched): every gate limit the guide
  quotes against `tools/perf/gates.toml`; `perf.py`'s scenarios, 37 °C wait, three runs, `perf.hud=off`
  default, the hitch ratio (12 intervals, 1.5 intervals), `gesture_first_frame_ms` (slowest of the first
  three frames), `record.sh` at 720x1600; the Standard springs (the test also checks them), fades
  120/200 ms, press 0.96/0.98/0.90, dim 0.45, highlight 220 ms; the shell (parallax 0.333, scrims
  0.10/0.24, 8 dp edge shadow at 0.04, Back applied at 99 %, tab nudge 12 dp on a 0.28 s flat spring,
  indicator 0.22/0.32 s and 1.5 slots, icon 0.84, `PARKED_DEPTH = 3`, five warmed tabs with
  `createPausedPrecomposition`, tab lifecycle lowered one frame after hiding and raised two frames after
  showing, stack caps RESUMED/STARTED/CREATED, `FrameRateCategory.High` only while sliding or the card is
  up); the back card (0.90, 8 dp, 24 dp, 28 dp, scrim 0.32 in 50 ms, commit fade 100 ms, cancel 100 ms,
  `BackCardState.textures`); `HostCrossfade` (200 ms, `AnimatedContent`, both hosts composed, one drawn
  background, Back swallowed); sheets (400 dp/s, 2 heights/s, 48 %, 0.099 s, 0.008, 0.9165, 10 dp);
  glass (tiers and API levels, 120-frame windows at 15 %, two in a row, `LocalGlassQuality` default
  Frosted, nothing outside `foundation/glass` calls `GlassStage` or `rememberGlassQuality()`); the
  scopes of `scroll-fling`, `subcompose-image`, `adaptive-box-constraints`, `layer-alpha-fill`,
  `state-param`, `uistate-mutable`, `api-immutable`, `scroll-chrome`, `nav-fade`, `sheet-*`, `shell-seam`;
  `checkComposeStability` on the three modules, the baseline format, `-PcomposeReports`; the
  recomposition harness in `:core:testing:compose`; `PlayStorePrompts` after the first frame and only
  while resumed; `AppFontPreload.run` called only by `CatalogActivity`; `AuthTokenStore.isAuthenticated`;
  `DataStoreFiles`; WorkManager's initializer removed under `offlinequeue`; Glance and Lottie in
  `removed.toml`; `runtime-tracing` as `devImplementation`; `-PabiSplits=true`; the 4 GB heap; the
  checked-in profiles in `app`, `core/navigation`, `core/designsystem`, `core/ui` in every preset, with
  the package renamed; "Where the template stands" against `staging/TEST-REPORT.md` (test phase and fix
  loop).
- Corrected in this run: the `screen` spring is 0.344 s in the Standard style only (Calm 0.3954, Snappy
  0.2991; none bounces); the fling's carried momentum (a new flick within 500 ms of a stopped fling, at
  least half as fast, carries part of its speed); Rapid Back's after-the-first row (over 1.0 in one
  session); the sheet rule now says what `sheet-when-else` and `shell-seam` hold (outside the sheet
  system), in the guide and in section 6.
- Stale lines fixed besides (the draft's merge note 3, last item): `AGENTS.md` section 4 named
  `updateState`, `emitEffect`, `persistState` and `launchLatest(key, debounceMillis)`, none of which
  exists (`MviViewModel` has `setState`, `emit`, `showMessage`, `launchLatest(key, debounce)`; typed input
  survives through `savedStateHandle.saveable(…, TextFieldState.Saver)`); `ARCHITECTURE.md`'s "Surviving
  process death" the same; two over-long lines rewrapped.
- Also stale and fixed: the template README's "Events are queued, not launched" and `ARCHITECTURE.md`'s
  section of that name (`onEvent` calls `handleEvent` at once on the main thread; there is no channel
  and no `launchWork`), and its "Messages and errors" (`onUnexpected`, called for launched and observed
  work and doing nothing by default, not `onError` with a generic message).
- Found and fixed: `CodeRules.kt`'s failure message sent readers to `docs/MOTION-AND-PERFORMANCE.md` and
  `docs/ADAPTIVE.md`, neither in any project; it now names `AGENTS.md` and `docs/PERFORMANCE.md`.
  `test_docs.py` gained `test_every_guide_the_code_points_to_is_in_the_project`.
- Tests: `generator/tests/test_docs.py`, 10 tests. On staging's tree (`git archive staging` into
  `out/WP12-DOCS/st`, this file copied in): red (`logs/WP12-DOCS/r2-test_docs-on-staging.log`: the guide is
  missing, ARCHITECTURE.md's `#data-and-the-network` link breaks without `network`, and the code sends
  readers to the two missing guides). On the branch: 10 OK (`logs/WP12-DOCS/r2-test_docs-2.log`).
  Full generator suite and `:build-logic:convention:test` + `validatePlugins`: see "Verification".
- **State: implementation done at `0ec84b634`, waiting for the combined review (WP12's accuracy lens).**
  Before the merge (after INTAKE), merge staging's newest head and re-check: "Where the template stands"
  against the newest phone numbers (staging's fix is still measuring), every shell constant the guide
  quotes if staging's fix touched `core/navigation`, then `tests.test_docs`.
- Note: one `git status` was run in `wt/sw-WP02` while checking WP02-CONT's progress (read-only intent; it
  can refresh that worktree's index stat cache). Nothing else outside this lane was touched.

## Verification

Run 3 (fix #1), at staging `d429cbfbf` + this branch. Logs under `D:/projects/tools/.overhaul/logs/WP12-DOCS/`.

| Check | Result | Log |
|---|---|---|
| The final `test_docs` against `0ec84b634`'s three docs (stashed in place, the new test kept) | red, as intended: the three done-conditions; `userMessage` in `AGENTS.md`; `gridMinCellWidth`, `isOffline`, `userMessage` in `ARCHITECTURE.md` | `f1-test_docs-red-final.log` (`f1-test_docs-red.log` is the first try, which also tripped on a placeholder bug in the test itself, fixed before the final run) |
| `tests.test_docs` + `tests.test_android_target`, final tree | 47 tests OK | `f1-test_docs-2.log` |
| Generator suite (`unittest discover -s tests -t .`, memwait) | 489 run, OK, 9 skipped (run 2's 487 plus the two new tests) | `f1-generator-suite.log` |
| `tools/perf/test_perf.py` (the `gates.toml` header comment changed) | 54 tests OK | `f1-test_perf.log` |
| Rendered docs for a coil-only project, `none` and `everything` | coil-only shows "### One image loader" with the no-client rule; the stands note and the new done-condition only with `performance`; `opt-else` wording without it | `f1-render.log` |

Not run: Gradle (no Kotlin, resource or build-script change; the gates.toml change is a comment), the
phone (docs and a test only).

Run 2, all at staging `d429cbfbf` + this branch's work, committed as **`0ec84b634`** on
`staging-wp/WP12-DOCS` ("Give every project a performance guide, and the short rules that point to it";
the worktree is clean). Logs under `D:/projects/tools/.overhaul/logs/WP12-DOCS/`.

| Check | Result | Log |
|---|---|---|
| `tests.test_docs` on staging's tree (`git archive staging`, the test copied in) | red, as intended: no guide, the broken `#data-and-the-network` link, two guides the code names and no project has | `r2-test_docs-on-staging.log` |
| `tests.test_docs` + `tests.test_android_target` on the branch, after the last edit | 45 tests OK | `r2-test_docs-3.log` |
| Generator suite (`unittest discover -s tests -t .`, memwait) | 487 run, OK, 9 skipped (staging's 477 and the same 9 skips, plus `test_docs`' 10) | `r2-generator-suite.log` |
| `:build-logic:convention:compileKotlin`, `validatePlugins`, `test` (`--rerun-tasks`, locked gradle, low priority) | BUILD SUCCESSFUL; 77 tests, 0 failures | `r2-buildlogic-test.log` |
| Rendered docs read for `none`, `lean`, `standard`, `everything` | fences resolve, no text runs into a heading, renamed application class in the coil line | (generated under the session scratchpad) |

Not run, with reasons: the template's full `./gradlew build` and the preset builds (the only code change
is a comment and a string in `:build-logic:convention`, which compiled and passed its tests;
`verifyCodeRules` logic is unchanged), `consistency --quick` (the generator suite renders the presets
that bracket the catalogue; the desktop gate runs it at the merge), the phone (docs only; nothing
measured changed). The desktop gate's dex comparison should find every measured APK identical.

## Measurements

None of its own: a docs stage takes no phone measurement. Every number the guide quotes comes from a
handoff, a perf run or `staging/TEST-REPORT.md`; the guide changes no code a phone check measures.

## Rules and gotchas the docs must state

For every later stage that writes into the docs base (`docs-draft/PENDING.md` says the same at its top):

1. **Fence by feature.** The guide ships in every project. Anything about `tools/perf` (`perf.sh`,
   `gates.toml`, `record.sh`, `split_profile.py`, the benchmark build, `devBenchmarkRelease`) goes inside
   `<!-- <opt:performance> -->` with an `opt-else` wording; the overlay inside `perfhud`; dev tooling inside
   `devtools`; Coil inside `coil` (and the client inside `network`); glass inside `liquidglass`; the
   sample feature inside `sample`; Play inside `playstore`; WorkManager inside `offlinequeue`; the
   catalog inside `catalog`. Markdown markers are not tidied (`MARKDOWN.collapse = False`): put the blank
   line inside the fence, as the existing blocks do, or the text runs into the next heading.
2. **The `standard` preset has `perfhud` without `performance`.** Overlay text must not lean on
   `perf.sh` (it does in `PERFORMANCE-OVERLAY.md`, see the findings).
3. **`test_docs.py` is the accuracy check.** It fails on a rule id, lint id, gate row, scenario, Standard
   spring, path or link the guide or section 6 cites and a generated project lacks; on a template name
   either cites that a generated project lacks; on a CamelCase name in backticks on any page of the
   template (`AGENTS.md`, `README.md`, `docs/*.md`) that the template's code, file names and variant
   tasks do not have, unless the test lists it (`LIBRARY_NAMES` for a platform or library name,
   `EXAMPLE_NAMES` for a made-up one, `STALE_NAMES` for the old ones still to rewrite, which only
   shrink); on workshop words (`overhaul`, `WP..`, `REQUIREMENTS`) and `{braces}` left unfilled in the
   guide and `AGENTS.md`; on a `docs/*.md` the project's code names but does not ship; and on a
   done-condition that does not excuse exactly the rows "Where the template stands" lists as over. It
   has no check for private names: the review and the pre-push scan hold that, never the public tree.
   "Enforced by." paragraphs and the part after " — " in section 6 are parsed: name real ids there, in
   backticks.
4. **"Where the template stands"** (in "Screen transitions") holds the last measured release rows of the
   Everything preset. A stage that re-measures them on merged code (HUD-O1, REG, a train's phone gate,
   WP13) updates the table in the same change; a row that goes green moves to "within". The
   done-condition excuses exactly the rows marked over, in one wording in three places (section 6,
   section 10 item 7, the guide's "Before you merge UI work" item 8); the stage that brings the last row
   within removes the exception from all three (the test fails until it does).
5. **Exceptions to "no whole screen offscreen"** are listed in four places that must agree: the guide
   (rule 3, "How the shell moves screens", "No alpha on a full-screen layer", "The back gesture"),
   section 6, `ARCHITECTURE.md`'s `verifyCodeRules` row and the comment above `layer-alpha-fill` in
   `ComposeRules.kt`. Today: the back card's texture and `HostCrossfade`. P6/P10T add theirs to all four.
6. **Spring numbers are the Standard style's.** Say so wherever a response time is quoted; Calm and
   Snappy change the travel times of `screen`, `sheet`, `dialog` and `layout`.
7. **Section numbers of `AGENTS.md`.** Section 6 is "Smooth and light"; "Google's Android skills" is 9.
   Tests and the `.claude/skills` stubs find sections by name; "Before you say it is done" cites §6 and §9.
8. **Heading levels in the guide.** A rule directly under a `##` section is `###`; under `### Start-up`
   or `### Rules for screens`, `####`. `PENDING.md`'s entries are written with `####`: match the section.

## Findings for routing (code or docs outside this stage; INTAKE routes them)

The guide describes the code as it is, so none of these is a false claim; each is a gap a project
copying the template inherits.

- **WP03 or the sample's owner:** `SampleDetailViewModel` starts empty, loads on `Appeared` and loads the
  related rows with the item, although `SampleRepository.peek(id)` exists (the guide's `sample` note says
  so; remove the note when fixed).
- **FREEZE (Telegram P1):** the pull-to-refresh spinner ignores `LocalTabVisible` (`AppPullToRefresh.kt`,
  `SpinStepMillis`); the guide says "does not yet" in "Nothing animates while idle".
- **WP03-CARRY:** downloaded fonts never apply in the app (only `CatalogActivity` calls
  `AppFontPreload.run`); `AppFonts.kt`'s KDoc says "the startup gate runs it". The guide's "Fonts" says
  what is true today; update it with the preload.
- **WP04 lane (shared navigation files, in the file order):** `BackCardState.textures`' KDoc
  (`back/BackCard.kt`) still says "The one exception to A11.3 rule 1" and "the screen under it" (it is the
  screen the card shows, and `HostCrossfade` is a second exception); `TabReveal.kt`'s KDoc justifies "no
  veil" by the GPU's idle clock, which the one-fill measurement contradicts (the measured reasons: the
  veil's GPU p90 12 ms against 8, and the display list re-recorded per reveal frame); `AppMotion.kt`
  still documents and exposes `revealVeilAlpha` (`DesignTokens.Motion.tabRevealVeilAlpha`), which nothing
  draws.
- **WP05-BAR:** `GlassQuality`'s KDoc names "a tier switcher (the catalog, a benchmark)" that staging
  lacks; when glass chrome lands, move the guide's "Lessons from glass chrome built outside this
  template" into its rules.
- **HUD:** `docs/PERFORMANCE-OVERLAY.md` cites `tools/perf/perf.sh` and `tools/perf/gates.toml` unfenced,
  and the `standard` preset ships the overlay without `performance`, so its readers get two paths that do
  not exist (found by checking every backticked path of every generated doc).
- **WP12-KNOW (stale names, found by the fix run's name check):** `test_docs.STALE_NAMES` lists the names
  the template's pages cite that no code has: `README.md`'s `MainActivity.entryPoint()` ("Where the app
  starts"), and in `ARCHITECTURE.md` `AppIndication` (the design system's indication), `AppNavHost` (line
  ~208, the one file that uses Navigation 3: today the stack and presentation hosts in
  `:core:navigation`), `DeepLinkResolver.resolve` (~301; links now go through `PendingDeepLink`),
  `Paged` (`:core:model`'s example type), `AppLogger.debugEnabled` (~660) and `surfaceRaised` (a colour
  role). Rewrite each paragraph from the code and remove the name from the set; the knowledge pass is
  done only with the set empty. The same pass should read `ARCHITECTURE.md` beyond names: this run found
  "Failures are classified" wrong in substance (an `isOffline` flag, a raw body, a queued message), not
  only in its one call.
- **WP12-KNOW (AD-70):** `AGENTS.md`'s `testing-setup` row points at `docs/ADAPTIVE.md`, which does not
  exist until AD-70 writes it; the build's code-rules message names `AGENTS.md` and `docs/PERFORMANCE.md`
  only, so add `docs/ADAPTIVE.md` to it (and `test_docs.py` will check it) when the file lands.
- **WP02-CONT or WP06-CARRY:** `ManifestRules.CONFIG_CHANGES` lacks `uiMode`, so the "theme never
  recreates the activity" rule rests on the manifest alone (the guide says so).
