# WP12-DOCS handoff (stage 4 of the release pipeline: the docs base)

## Progress

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
- Found and fixed: `CodeRules.kt`'s failure message sent readers to `docs/MOTION-AND-PERFORMANCE.md` and
  `docs/ADAPTIVE.md`, neither in any project; it now names `AGENTS.md` and `docs/PERFORMANCE.md`.
  `test_docs.py` gained `test_every_guide_the_code_points_to_is_in_the_project`.
- Tests: `generator/tests/test_docs.py`, 10 tests. On staging's tree (`git archive staging` into
  `out/WP12-DOCS/st`, this file copied in): red (`logs/WP12-DOCS/r2-test_docs-on-staging.log`: the guide is
  missing, ARCHITECTURE.md's `#data-and-the-network` link breaks without `network`, and the code sends
  readers to the two missing guides). On the branch: 10 OK (`logs/WP12-DOCS/r2-test_docs-2.log`).
  Full generator suite and `:build-logic:convention:test` + `validatePlugins`: see "Verification".
- Note: one `git status` was run in `wt/sw-WP02` while checking WP02-CONT's progress (read-only intent; it
  can refresh that worktree's index stat cache). Nothing else outside this lane was touched.

## Verification

(filled at the end of run 2)

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
   spring, path, CamelCase name or link the guide or section 6 cites and a generated project lacks, on
   workshop words (`overhaul`, `WP..`, `REQUIREMENTS`, private app names) and on `{braces}` left unfilled,
   and on a `docs/*.md` the project's code names but does not ship. "Enforced by." paragraphs and the
   part after " — " in section 6 are parsed: name real ids there, in backticks.
4. **"Where the template stands"** (in "Screen transitions") holds the last measured release rows of the
   Everything preset. A stage that re-measures them on merged code (HUD-O1, REG, a train's phone gate,
   WP13) updates the table in the same change; a row that goes green moves to "within".
5. **Exceptions to "no whole screen offscreen"** are listed in four places that must agree: the guide
   (rule 3, "How the shell moves screens", "No alpha on a full-screen layer", "The back gesture"),
   section 6, `ARCHITECTURE.md`'s `verifyCodeRules` row and the comment above `layer-alpha-fill` in
   `ComposeRules.kt`. Today: the back card's texture and `HostCrossfade`. P6/P10T add theirs to all four.
6. **Spring numbers are the Standard style's.** Say so wherever a response time is quoted; Calm and
   Snappy change the travel times of `screen`, `sheet`, `dialog` and `layout`.
7. **Section numbers of `AGENTS.md`.** Section 6 is "Smooth and light"; "Google's Android skills" is 9.
   Tests and the `.claude/skills` stubs find sections by name; "Before you say it is done" cites §6 and §9.

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
- **WP12-KNOW (AD-70):** `AGENTS.md`'s `testing-setup` row points at `docs/ADAPTIVE.md`, which does not
  exist until AD-70 writes it; the build's code-rules message names `AGENTS.md` and `docs/PERFORMANCE.md`
  only, so add `docs/ADAPTIVE.md` to it (and `test_docs.py` will check it) when the file lands.
- **WP02-CONT or WP06-CARRY:** `ManifestRules.CONFIG_CHANGES` lacks `uiMode`, so the "theme never
  recreates the activity" rule rests on the manifest alone (the guide says so).
