# NOUI handoff (REQUIREMENTS 59)

## Orchestrator note (Oct 25, 00:42), read before running the API or the generator

Never run the API or the generator with `PYTHON_BIN=python` (or any bare `python`) under a minimal environment on this machine: the Python install manager's alias then installs and registers a new Python in the working directory (incident 1 in ## WEB). Use the real interpreter `C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe`, and make sure `75887a654` (the child environment keeps LOCALAPPDATA) is in the tree you run. Do not touch `wt/NOUI-WEB/generator/Python` or its log: the user's registry points at it until they approve the repair. `go test` binaries are blocked by Smart App Control (incident 2): record that row as 'not runnable here, CI is the evidence', do not try to bypass it.

Lanes: GEN, WEB, DOCS (wt/NOUI-*), integration in wt/NOUI (staging-wp/NOUI). Plan: plans/2026-10-24/noui.md.

## GEN

GEN runs as one implementer in `wt/NOUI-GEN` (branch `staging-wp/NOUI-GEN`); the GEN-kt work is done in the same
worktree, so there is no `wt/NOUI-GEN-KT` branch to merge.

- **First commit (engine and option metadata): `83c2dd10f`** (23:08 IST). WEB and DOCS: `git merge 83c2dd10f`.
  It adds `templates/android/options/ui_components.toml` (metadata, no `owns` or `variant` yet), the option in the
  manifest, the spec field `ui_components` (absent = true, recorded only when false, non-bool = SpecError on the
  field), the dropped-feature notes, `"options"` in the dry-run plan, and the catalogue's `options` per target
  (contract 1.4, `locked: null`), with `web/frontend/lib/catalogue.json` rewritten by `--write`.
- Later commits: `b7af6520b` (CLI flag, wizard, `--list-features`, scaffold/tabs/add_feature No-UI), `0993ebaf3`
  (`tools/compare_generations.py`), `da27da32d` (the template: owns, fences, variants under `generator/variants/noui/`,
  No-UI static checks, `test_android_noui.py`), `388b054f7` (CI rows `noui-none`, `noui-everything`; catalogue and
  tokens `--check` in the generator job).
- **State at 01:15: P0 and P1 done, except `DesignSystemDocTest` (it reads DOCS's `docs/DESIGN-SYSTEM.md`, which is
  not on this branch; see "For the integrator" below). Head `d3c5b818c`.** Commits after `388b054f7`: `2d79e15b1`
  (No-UI shapes in the consistency matrix), `6e6524c76` (`add_feature --domain` reads the architecture from the
  record), `1a32c8801` (the code-rule baseline's screen entries and the theme's words fenced), `da02f9bff`
  (`SharedDestinationsTest` without a feature module), `15b6c72a9` (DOCS's two findings: `RealTemplatesTest` renders
  with `manifest.marker_keys` on and off, `compare_generations.py` writes through `write_text`), `a3b26df3c` (P1:
  `ComponentRules`, `ComponentRuleCasesTest`, `@AppComponentPreviews`), `01827c81c` (P1: the F1.4 advice-name test),
  `d3c5b818c` (P1: the No-UI startup profile recorded on the Redmi, split per module).
- Acceptance at `d3c5b818c` (logs in `logs/NOUI/GEN/`):
  1. `memwait python -I -m unittest discover -s tests -t .`: 553 tests OK, 9 skipped (`unittest-final-3.log`); the
     base is 477 OK, 9 skipped.
  2. `catalogue --check` and `tokens --check`: current.
  3. `consistency --quick`: 81 projects, 0 new issues, 188 known (`consistency-quick-3.log`).
  4. `compare_generations.py --before out/NOUI/base --after wt/NOUI-GEN`: 19 specs identical outside the exceptions
     (`compare-final-2.log`).
  5. The `create_project.py --preset everything --no-ui-components ...` command, wizard answered by Enter:
     `out/NOUI/gen-everything` (regenerated from the committed head: only the record's commit stamp differs).
  6. `check assembleDevDebug assembleProdRelease koverVerify --continue` there (`build-everything-5.log`): every task
     passes except `:spotlessKotlinCheck` (180 Kotlin files) and `:spotlessKotlinGradleCheck`
     (`benchmark/build.gradle.kts`), the WP02 formatting carry-over (`staging/CARRY-OVER.md`): the UI everything
     project at the base fails the same two tasks (`spotless-check-ui-base-2.log`: 359 Kotlin files, the same
     Gradle file). Not fixed here: the one reformat changes every UI project's bytes, which this lane must not. Both
     APKs built; the prod release carries `assets/dexopt/baseline.prof`. No-UI lean (`build-lean-4.log`) and none
     (`build-none-3.log`), `check assembleDevDebug assembleProdRelease`: BUILD SUCCESSFUL.

### Where reality differed from the plan, and why

- `--list-features --json` stays the list of features it always printed: scripts read it as a list (README, AGENTS.md,
  `test_cli`), and an object would break them. The options are in the catalogue (`catalogue --json`) and in the
  dry-run plan (`"options"`); the human `--list-features` prints the option, its default, what it leaves out and
  any lock note.
- The wizard asks the option right after the preset question, as a yes/no on its checkbox label ("No UI components?",
  default no), so "choose individually" never offers a disabled feature; the look questions skip the hidden fields.
- `core/ui/form` is not owned: `ValidationError.toUiText()` is the auth ViewModels' validation copy, not UI.
- `PostureTest` is kept with its sheet case fenced (it imports `AppSheet`); `AppLockGateTest` keeps its cover tests
  and fences the three that drive overlays or the Unlock button. `SheetRules.kt` and `SheetRuleCasesTest.kt` are owned
  (dead code once `CodeRules.ALL` drops them); `CodeRulesTest`'s sheet fixtures are fenced.
- The two sample routes are variant files rather than fences: their only No-UI difference is the content call plus
  two imports, which a fence could only carry with an empty `opt` block before the `opt-else`.
- The tooling kit (`core/devtools/.../kit/ToolingKit.kt`) has the 17 names the tools import, not 12:
  `AppText`, `AppMonoText`, `AppButton`, `ButtonVariant`, `ButtonSize`, `AppCard`, `AppDivider`, `AppSwitch`,
  `AppToggleRow`, `AppRow`, `AppStatusPill`, `AppStateView`, `AppSegmentedControl`, `AppScrollDefaults`,
  `Modifier.pressable`, `PressFeedback`, `LocalBottomChrome`. Press feedback is a draw-time scale, never a ripple.
- The app lock's No-UI cover is `PlaceholderScreen(R.string.applock_title)` that asks for the prompt again on a tap
  (with the Unlock label as its click label), because without the Unlock button a dismissed prompt would leave the
  person locked out; the system's failure message is its state description. The app-status placeholders have no
  actions until components exist (update and retry are what the user's screen calls).
- `core:applock` and `core:playstore` depend on `:core:ui` instead of `:core:designsystem` without UI components
  (PlaceholderScreen, LocalMessagePresenter; `core:ui` exposes the theme). `core:ui`'s No-UI test dependency on
  `:core:testing:compose` is a live `opt:!ui_components` line: in the raw template it only adds an unused test
  dependency to a module with no tests, and no generated UI project contains it.
- No-UI code never sits live in the raw template: every No-UI line is a `//~` fallback or a variant file, so the
  template's own `./gradlew build` is the UI project it always was.
- The consistency checks count a No-UI project's unused strings and plurals as intended (D14: each screen's copy stays,
  translated, for the screen built over its placeholder; release builds shrink what stays unused). Other resource
  kinds are still checked.
- `test_manifest.OrphanedTestTest` now runs for options too (a kept test whose main file the option owns fails).
- The acceptance command `create_project.py --preset everything --no-ui-components --set ... --out ... --no-zip` has no
  `--spec`, so it runs the wizard; it was run with every question answered by Enter (`yes '' | python create_project.py
  ...`), which keeps the defaults.
- For the integrator (not changed: it would change UI output): the template's `app/src/main/baseline-prof.txt` names
  its application and Hilt component classes `EverythingApplication`/`DaggerEverythingApplication_...`, the names of
  the project it was recorded from, so in a generated project those entries match nothing. The No-UI variant profile
  was the template's filtered to the classes a No-UI everything project has (3337 of 3520 entries), with the same
  quirk; since `d3c5b818c` it is recorded (below), still under the name Everything.
- The lean and none presets have no feature module without UI components (no home tab is scaffolded), so
  `SharedDestinationsTest`'s sanity check found no keys and failed `:app:testDevDebugUnitTest`; it now waits, as a
  skipped test, for the first feature module (`assumeTrue(File("../feature").isDirectory)`), and holds as before once
  one exists. It uses the inline `// <opt:!ui_components>CODE` form, since an `opt-else` needs an `opt` block before it.
- `stateless-params` leaves a feature's ViewModel parameter to the template's `vm-forwarding`, which already reports
  it (`CodeRulesTest` caught the double report); in the design system it reports any ViewModel parameter.
- `ComponentRules`' two pattern rules carry their regexes inline: an `object`'s `private val` declared after `ALL` is
  still null while `ALL` is built.
- The F1.4 test checks every `AppTheme` member, annotation and code-shaped name in the advice of the rules in each No-UI
  shape's `CodeRules.ALL` against the project's main sources, with a reviewed list of library names
  (`ADVICE_LIBRARY_NAMES`); `docs/...` paths are left to DOCS's `test_docs.py`.
- The No-UI startup profile (P9) was recorded from `out/NOUI/profile-everything-2` (No-UI everything generated as
  "Everything" / `com.base.app`, the names the test checks it under) with `locked adb` around `locked gradle
  ./gradlew :app:generateBaselineProfile` (`profile-record.log`: 25496 rules, 01:01), then split with the template's
  `tools/perf/split_profile.py` as today: app 1727 lines, core:navigation 143, core:designsystem 444, core:ui 17. The
  three module profiles were owned at P0 (absent in No-UI); they are variant files now, so `owns` lost those paths.
  The phone has no `com.base.app` package afterwards (the plugin uninstalls what it installed) and no setting was
  changed; it was woken before and sent HOME after. The app profile keeps the `EverythingApplication` quirk below.

### For the integrator (GEN)

- **P1 rule rows in DESIGN-SYSTEM.md.** `ComponentRules` landed, so DOCS's block (the five rows `designsystem-imports`,
  `stateless-params`, `raw-type`, `feature-primitive`, `component-preview` in `<!-- <opt:!ui_components> -->`, quoted
  under ## DOCS) goes into the rule index, and "Previews are render tests" (No-UI block) names `@AppComponentPreviews`.
  The ids match `ComponentRules.ALL` exactly; `test_docs.py` fails until the rows are in.
- **`DesignSystemDocTest` (P1, not done).** After DOCS is merged: a variant test
  `generator/variants/noui/build-logic/convention/src/test/kotlin/com/base/app/buildlogic/guard/DesignSystemDocTest.kt`
  that reads `../../docs/DESIGN-SYSTEM.md` (the build-logic test runs in `build-logic/convention`), takes the
  ```` ```kotlin component-template ```` block and asserts `CodeRules.check(<a core/designsystem/.../component/ path>,
  block)` is empty. The plan's second half (a "don't" block with exact rule ids) needs DOCS to add that block; there is
  none at `a76edea35`. On GEN's branch alone the doc is absent, so the test would fail every No-UI build there.
- **DOCS's NavDisplay recommendation is not taken.** The No-UI host still uses Navigation 3's default transitions
  (fades); the variant host and DOCS's sentence that the defaults fade must change together, so it is left as one
  decision for integration.

## DOCS

DOCS runs in `wt/NOUI-DOCS` (branch `staging-wp/NOUI-DOCS`). **State at 00:17: P0 and P1 done, head `a76edea35`.**
Acceptance: `python -m unittest tests.test_docs tests.test_vendored_skills -v` green on that head
(`logs/NOUI/DOCS/accept-2.log`, 39 tests, the one skip is the multiplatform-skills check that skips while that
template carries none); the full suite's only failures are GEN's (below). GEN was last merged at `388b054f7`; GEN's
later commits (from `2d79e15b1`) are not in DOCS: the integrator merges both lanes, and D1 runs test_docs on the
result. Merged `83c2dd10f` (GEN's plumbing) at 23:15 as
`7a5e0df0c` (its message is git's default, without the why and the trailer: a slip, left as is because history is
never rewritten).

Done (commits on `staging-wp/NOUI-DOCS`):

- `7af23196f` skills: android/skills **v1.0.17** (`f88c691a8a94`, checked with `git ls-remote` at 22:4x; v1.0.17 is the
  newest tag). 30 skills in every project (the official 33 minus `styles`,
  `migrate-xml-views-to-jetpack-compose`, `leanback-to-compose-tv-migration`); `auth.toml`/`media.toml` own no
  skill path now. LICENSE.txt read: Apache-2.0, no NOTICE file at v1.0.17. The clone went to `%TEMP%skills` and
  was sent to the Recycle Bin. Copies come from git objects (`ls-tree` + `cat-file`), not the working tree.
  New `templates/android/tools/android_skills.py` (P1 done): the project-local refresh,
  `python3 tools/android_skills.py --tag <tag>`; the repository's `tools/update_android_skills.py` now imports
  its copying, README, manifest and pointer code, so both write the same files.
- `cdab379b3` docs: `docs/ARCHITECTURE.md` (T1 fixes, No-UI blocks), `docs/PERFORMANCE.md` (new, condensed from
  WP12-KNOW's guide via `git show`, never merged), `docs/DESIGN-SYSTEM.md` (new), README.md.
- `10f24c9df` `generator/tests/test_docs.py`.
- CLAUDE.md (rules, done-checklist, skills table, refresh command, three `@docs/…` imports) and AGENTS.md
  (the same text, imports as links) in `7af23196f` and the No-UI blocks in the commit after `10f24c9df`.

Where reality differed from the plan, and why:

- **The multiplatform template gets no skills.** `templates/multiplatform/template.toml` has no `verbatim` rule, so
  the generator would rename and marker-parse upstream's text (`BaseApplication` in the testing skill, for one).
  The update tool now skips a template that does not copy `.agents/skills/**` verbatim and says so; a test holds
  "skills only where they are copied verbatim". Ship them there together with that rule when the multiplatform
  template is worked on.
- **PERFORMANCE.md uses the gate values at `8fdcba741`**, not WP12-KNOW's: REQUIREMENTS 58 moved the janky
  ceilings to 1.0 % (scroll and motion) and 6.0 % (navigation stress) after WP12-KNOW branched. WP12's "Where the
  template stands" table is not carried over: it goes stale with every measurement; the guide quotes limits and the
  measured numbers that explain each rule.
- **The doc test checks project-declared names per project, library names only at template level.** A No-UI project
  has no `LazyColumn` call, yet its PERFORMANCE.md must name Compose APIs; so per project the test fails on a name the
  template or a variant *declares* that the project lacks (its own API), and against the raw template every cited
  name must exist in the template, a variant, or a short reviewed allowlist.
- **README.md "The look"** is UI-only (the No-UI `AppTheme` takes no design or motion style); No-UI gets its own
  paragraph. The `readme-styles` and `readme-theme-call` regions sit inside the UI block.

For GEN (the names the No-UI docs cite; the doc test fails until each exists in the No-UI project, so either match
them or tell DOCS):

- `StartKey`, `StartScreen` (the blank start screen), `AppDestinations.start` names it.
- `PlaceholderScreen` (core:ui), `MessagePresenter` and `LocalMessagePresenter` (core:ui).
- `NavDisplayHostTest` (core:navigation tests), `AppNavigationHost` still the host's name, `OpenDeepLinks`,
  `PendingDeepLink`, `NavPane`, `Presentation` kept.
- `preview/ThemePreview.kt` (file name), `@AppComponentPreviews` (P1).
- `CodeRules.kt` failure message: "AGENTS.md and docs/PERFORMANCE.md say why" in both modes (the doc test's
  `test_every_doc_the_code_points_to_is_in_the_project` fails on today's `docs/MOTION-AND-PERFORMANCE.md` and
  `docs/ADAPTIVE.md` in every project).
- The rule index in DESIGN-SYSTEM.md must list exactly `CodeRules.ALL`'s rules per project (the test compares). No-UI:
  everything but `SheetRules`, plus the P1 `ComponentRules`.
- `generator/tests/test_markers.py` (`RealTemplatesTest`, GEN's file) renders every template file with
  `known = frozenset(manifest.features)`, so any `<opt:ui_components>` marker is "unknown feature" there: 12 errors
  on DOCS's branch today (CLAUDE.md, AGENTS.md, README.md, docs/*.md, both ways), and every Kotlin fence will hit it
  too. It needs `manifest.marker_keys`, and the option on and off. Run finished 23:53 on DOCS's branch
  (`logs/NOUI/DOCS/suite-1.log`): 539 tests, the only failures are those 12 and test_docs.py's checks waiting for
  GEN (names, CodeRules pointer, P1 rules).
- After merging GEN's head `388b054f7` (00:00) the DOCS branch runs 569 tests (`logs/NOUI/DOCS/suite-2.log`, 00:15):
  test_docs and test_vendored_skills green; the failures are GEN's only: `test_markers.RealTemplatesTest` 96 errors
  (the `known = frozenset(manifest.features)` above, now hit by every fenced Kotlin file too) and
  `test_host.test_text_is_written_only_through_write_text` (`tools/compare_generations.py:98` writes text without
  `genkit.core.copy.write_text`).

For the integrator:

- **P1 rows (changed at 00:05, after merging GEN's `da27da32d`).** The P1 rule rows are *not* in the docs now,
  because GEN's P1 has not landed and the doc test requires the rule index to equal `CodeRules.ALL` exactly. If
  `ComponentRules` lands, whoever lands it adds this block to the rule index in
  `templates/android/docs/DESIGN-SYSTEM.md`, right after the UI-only sheet rows (the doc test fails until they do):

  ```
  <!-- <opt:!ui_components> -->
  | `designsystem-imports` | a design system that cannot reach data | import only Compose and the theme |
  | `stateless-params` | stateless components | take values and callbacks, never a ViewModel, repository or `Flow` |
  | `raw-type` | one type scale | `AppTheme.typography.x`, `.copy()` for a one-off |
  | `feature-primitive` | features arrange, the design system draws | draw it once in `:core:designsystem` |
  | `component-preview` | every component rendered and tested | a `@Preview` on the component's file |
  <!-- </opt:!ui_components> -->
  ```

  and, if `AppComponentPreviews` lands, names it in "Previews are render tests" (No-UI block).
- **For GEN, a recommendation:** the No-UI `AppNavigationHost` passes no transitions, so `NavDisplay` uses Navigation 3's
  Android defaults, which fade whole screens (`fadeIn`/`fadeOut`, `scaleOut` on predictive pop; read from
  navigation3-ui 1.2.0's bytecode). That is what the template's own rule forbids (no cross-fade of full screens). The
  docs now say the defaults fade and must be replaced when a shell is built; a slide (or no motion) in the variant
  would make the project follow its own rule from the start. Not changed by DOCS (GEN's file).
- **Root `AGENTS.md`** (no lane owns it) still says a skill that ships with a feature is owned by that feature's
  TOML; every project now ships the whole set. Suggested replacement for that sentence: "Every project ships the
  whole set except the skills `LEFT_OUT` names in `templates/android/tools/android_skills.py`; a skill added to the
  set needs its row in that template's CLAUDE.md and AGENTS.md table." and the refresh command is
  `python3 tools/update_android_skills.py <checkout> --tag <tag>`.
## WEB

WEB runs in `wt/NOUI-WEB` (branch `staging-wp/NOUI-WEB`, from `8fdcba741`). Merged GEN's `83c2dd10f` at 23:3x as
`5536e7b6d` (git's default message, no why and no trailer: the same slip DOCS logged, left as is because history is
never rewritten). Head at 00:04 IST: `314b0fd15`. Nothing pushed.

Commits (all P0 and P1 of the lane):

- `77b0480c7` state: `Option`/`OptionLock`/`Target.options` types, `features/catalogue/options.ts` (one accessor
  per question: the option, the effective value, disabled features, hidden fields, setup-only, the Android-only note),
  `uiComponents` and `skipped` in the state, presets/toggles/drafts subtract the disabled features, `ui_components:
  false` sent only when ticked, the browser validator takes option keys as booleans. Tests: `options.test.ts`,
  `uiComponents.test.ts`, two in `request.test.ts`.
- `b5265c554` Go API: `options` parsed from the catalogue (a missing boolean default, a lock without a boolean value
  or a note, a key twice: boot fails), `TargetEntry.CheckOptions` (non-bool 400 `invalid_spec` on `ui_components`;
  lock mismatch 403 `{code: option_locked, field: ui_components, error: <note>}`), checked after the precheck and
  before `gen.Run` (no slot); selftest sends `LockedValues()` and runs the route's checks on its own spec first.
- `efcad37de` steps: the box first on Features (option's label/headline/description; locked = ticked, fixed,
  the note), disabled cards greyed with their reason (and "Coming with the Android release" when locked), preset
  counts without disabled features, Look/Motion show the `hides` reason where the style choice was (colours, font,
  haptics stay), Review "UI components" row, hidden rows dropped, "Skipped, and why"; Platform cards for multiplatform
  and iOS carry the Android-only note, which also stands in for the box on those targets.
- `bcf072658` the contract stand-in in `testing.ts` removed once the real catalogue had options; the locked catalogue
  stays a fixture (`lockedCatalogue()`), since only the release branch carries a lock.
- `e1727bf8d` integration test (runs only with `GENERATOR_DIR`): the ticked box on the largest preset passes the API
  checks, generates, drops `catalog` and notes it.
- `71dd17c23` `web/README.md`: the option, the lock (where, how it reaches site and API, the 403, fail-closed boot,
  selftest, how the Android release removes it), `ENABLE_TARGETS` and `TRUSTED_PROXY` rows; `Dockerfile.api` ->
  `Dockerfile`.
- `fe8e633b2` P1: theme-slots preview (colour swatches, type scale, blank start screen; light and dark) beside Look
  and Motion and in Review when ticked; landing badges in locked mode (design-styles showcase; the four "included"
  items that need components).
- `75887a654` the API's child environment passes `LOCALAPPDATA` on Windows (see incident 1).
- `314b0fd15` locked mode: the hero shows the lock's note under its promise (beyond the plan; the hero says "pick a
  look", which a setup-only site cannot honour).

Where reality differed from the plan, and why:

- **Design and motion style reset to the target's defaults when the box is ticked**, and stay there when it is
  unticked; the request always carries the effective (default) styles. A remembered style would need every reader
  (request, preview, Review, glass note, features' "needed by the X look") to pick the effective value; resetting
  keeps `state` the truth. Unticking shows the style choices again.
- **`skipped` lives in the state** (features the selection or the chosen preset had, in the option's order), because
  after subtraction the selection cannot say what it lost; it is recomputed on tick, preset, platform switch and draft
  restore, and persisted with the draft (draft version unchanged; older drafts read as UI on).
- **Disabled cards stay focusable** (`aria-disabled` plus `aria-describedby` on the reason) instead of the native
  `disabled`, so a keyboard or screen-reader visitor meets the reason. `ChoiceCard` gained an `unavailable` state for
  it; per its existing rule the text is not faded (dashed edge, no fill).
- **A locked refusal is recorded as outcome `invalid`** with the note as reason and `ui_components` as field, and the
  spec described (features, preset): a new outcome would need a migration and admin changes for no gain tonight.
- **`null` counts as absent** in the API check, as the generator strips nulls; so a locked deployment refuses
  `"ui_components": null` with the 403 too.
- **Lock route tests run on every platform**: the platform-neutral helpers (`testCatalogue`, `fakeSink`, `newServer`,
  `decodeError`, ...) moved from the unix-only test files into `httpapi/helpers_test.go`; `httpapi/options_test.go`
  uses a generator whose interpreter does not exist, so only a refusal before the slot can pass. The "accepted" cases
  (locked + false, unlocked: absent/true/false) need `/bin/sh` and are in the unix-only `generate_test.go` (CI).
- The site's `stepOf("ui_components")` is the Features step, so the API's 403 links there.

Checks (logs under `logs/NOUI/WEB/`):

| Check | Result | Log |
|---|---|---|
| `memwait npm ci` | ok | `accept-npm-ci.log` |
| `npm run typecheck && npm test` | ok, 246 tests, 0 fail | `accept-typecheck-test.log` |
| `memwait npx next build` | ok | `accept-next-build.log` |
| `gofmt -l .` empty, `go vet ./...` (also `GOOS=linux go vet ./...` for the unix-only tests), `go build ./...` | ok | `accept-api.log` |
| `go test ./...` at the head | **blocked**: Smart App Control refuses every freshly linked test binary since ~23:55 (incident 2); clip, config, store, telemetry ok | `accept-api.log` |
| `go test ./...` at `b5265c554` (lock, options, selftest, route tests) | ok, every package | `api-1.log` |
| integration with the real generator (`GENERATOR_DIR`, real interpreter path) | android, resolution, **no UI components**, v1 ok; multiplatform fails on Windows MAX_PATH in the temp dir (pre-existing, not this lane) | `api-integration.log` |
| site in the browser (`next start`, Browser pane) | box, greyed cards with reasons, `aria-disabled` + reason ids, Standard 17 still the preset, Look/Motion notes, theme slots light/dark, Review rows and "Skipped, and why", Platform notes; draft cleared afterwards, server stopped | `next-start.log` |

Not run or not verified here: the unix-only route tests (CI runs them); `TestChildEnvPassesOnlyWhatPythonNeeds`
(written, blocked by incident 2; its Windows behaviour was checked by hand: with `LOCALAPPDATA` the alias resolves
`%LOCALAPPDATA%\Python\pythoncore-3.15-64\python.exe` and writes nothing); the locked site in a browser (the checked-in
catalogue is never hand-edited; the locked behaviour is covered by `lockedCatalogue()` tests).

**Incident 1 (system change, needs the user): a stray Python registered for the user account.** At 23:27 the
integration run was started with `PYTHON_BIN=python`. The API runs the generator with a minimal environment (no
`LOCALAPPDATA`); the Python install manager's `python` alias then downloaded Python 3.15.1 from python.org
(signature and hash verified by the manager) into its working directory and registered it:
- files: `wt/NOUI-WEB/generator/Python/` (181 MB, untracked) and `wt/NOUI-WEB/generator/python_install_20261024232739_10016.log`;
- registry, checked from a Terminal tab (real, not the app's virtualised view):
  `HKCU\Software\Python\PythonCore\3.15` (`InstallPath` and its values) and the Apps-list entry
  `HKCU\...\Uninstall\pymanager-pythoncore-3.15-64` now point at that folder instead of
  `%LOCALAPPDATA%\Python\pythoncore-3.15-64`;
- Start menu: `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Python\Python 3.15\*.lnk` point at it;
- unchanged: the real install (3.15.0 in `%LOCALAPPDATA%\Python`, runs), its `bin` aliases, user and system PATH.
Cause fixed in `75887a654` (the child environment keeps `LOCALAPPDATA` on Windows). Repair, for the user to approve
(not done: registry and Start menu are the user's): in a Terminal tab, `py install --refresh` (re-registers the
installs the manager owns, which rewrites the PEP 514 key, the Apps entry and the shortcuts to the real install),
check with `reg query "HKCU\Software\Python\PythonCore\3.15\InstallPath"`, then send `generator\Python` and the log
file to the Recycle Bin. Undo: none needed; the stray copy is not used by `python`/`py`.
**Gate D16 on this machine**: run the API smoke only with `75887a654` in, or with `PYTHON_BIN` set to the real
interpreter (`C:\Users\crazyhusir\AppData\Local\Python\pythoncore-3.15-64\python.exe`); with the alias and an older
API it repeats incident 1.

**Incident 2 (environment): Smart App Control is On and blocks new unsigned executables.** Code Integrity event 3077
for every `go-build...\*.test.exe` and a hello-world from 23:55; earlier the same evening fresh test binaries ran.
Not changed (a security setting is the user's). Gate D13 on Windows needs the user to allow it, or rely on CI's API
job for `go test`.

For the integrator:

- Lane order GEN, DOCS, WEB: WEB's only generator-side file is `lib/catalogue.json` from the merge of `83c2dd10f`;
  take GEN's (re-run `catalogue --write` if stale). WEB adds no hand edit to it.
- The user's relayed request also asked for a manual Vercel deploy when done. Not done from this lane: lane commits
  are not integrated or gated, and plan section 7 puts the deploy after the gate and the user's go; the Vercel plugin
  also needs `/mcp` authorisation.

## Pause (Oct 25, 01:21), usage 94 % of the window that resets 02:40

Stopped at a clean point: the three lanes had returned (GEN done d3c5b818c, WEB partial 314b0fd15, DOCS done a76edea35; their results are saved with the plan in plans/2026-10-24/noui-resume-args.json under 'built'), and the integrator had just started: staging-wp/NOUI at 39f71cca7, clean tree. NOUI-WEB's two untracked entries are the stray Python folder and its log from incident 1 (leave them). The relaunch at 02:43 skips the architecture reviews, the plan and the lanes, and starts at the integration, which continues from 39f71cca7.

## Integration

Integrator in `wt/NOUI` (`staging-wp/NOUI`). **State at 03:18: done; head `4b169b8d2`.** Nothing pushed; `main`,
branches, worktrees and history untouched; no stash.

Merges (made by the first integration run before the 01:21 pause, checked by this one): `cb9560dfa` GEN (`d3c5b818c`),
`0a51c82a5` DOCS (`a76edea35`), `7c1129a1c` WEB (`314b0fd15`), each `--no-ff` with a why-message and the trailer, in
the plan's order. None had a conflict (no hand-resolved hunk in `git diff-tree --cc`), so nothing was resolved by
taking both sides. Emitted files: `39f71cca7` ran `catalogue --write` after the merges; content was already current
and only `generator_commit` moved (to `7c1129a1c645`). Nothing this run changed is emitted, so `--check` stays
current at the head and the stamp was not moved again (`--check` ignores it).

Cross-lane items finished (commits on `staging-wp/NOUI`):

- `77fb9371e` The five ComponentRules rows (`designsystem-imports`, `stateless-params`, `raw-type`,
  `feature-primitive`, `component-preview`) in DESIGN-SYSTEM.md's No-UI rule index; `component-preview`'s fix names
  `@AppComponentPreviews` (the rule's own advice) rather than DOCS's draft "a `@Preview`". The No-UI component template
  carries `@AppComponentPreviews` (UI keeps its four `@Preview`s), and "Previews are render tests" names it.
  `test_docs.active_rule_ids` read `CodeRules.ALL` from one line; GEN's No-UI sum wraps over three, so every No-UI
  case errored (`logs/NOUI/INTEG/test-docs-1.log`). It now follows lines ending in `=` or `+`.
- `9a804b1cf` `DesignSystemDocTest` (P1, F3), a No-UI variant test in build-logic: the component template and the
  test template break no rule, and a new No-UI "what the build refuses" block (`kotlin component-dont`, an `AppBadge`
  whose lines carry `// state-param`, `// stateless-params`, `// animated-delegate, raw-tween`, `// raw-type`,
  `// raw-dp`, `// raw-color`) yields exactly those findings at exactly those lines. That block is the "don't" block
  the plan's 4.2 asked for and DOCS had not written. The test reads the project's page from outside build-logic, so
  the No-UI `build-logic/convention/build.gradle.kts` declares the page as an input of `tasks.test` (a
  `<opt:!ui_components>` block: live but harmless in the raw template like GEN's `core:ui` line, absent from every UI
  output; D5 below proves it).
- `dbadd6ab2` + `4b169b8d2` **Decision (logged in `decisions/LOG.md`):** the No-UI `AppNavigationHost` passes no
  motion (`EnterTransition.None togetherWith ExitTransition.None`) for push, pop and predictive pop instead of
  Navigation 3's defaults (fade whole screens, scale one under predictive back), which broke the template's own rule
  in every No-UI project. Options were no motion, a slide, or the defaults with the docs' warning; no motion because
  (c) takes the shell's visuals out and No-UI `:core:navigation` has no design system (no springs or reduce-motion to
  slide with); the docs already say to add a slide with the shell. ARCHITECTURE.md and PERFORMANCE.md changed with
  it. New `NavDisplayHostTest` case: six paused frames (0.1 s) after a push only the new screen is composed. The first
  form (one 0.1 s advance) failed in the build because the paused clock never let the looper hand the change to
  Compose (`build-everything-1.log`); fixed in `4b169b8d2`. Checked both ways in the generated project: green with
  the host (`navhost-test-2.log`), red with the defaults put back (`navhost-test-3-defaults.log`, the start screen
  still drawn), then restored.
- `9c25389d9` Root `AGENTS.md`: DOCS's replacement for the feature-owned-skill sentence, `--tag` on the refresh
  command, and the Compose Multiplatform template described as carrying no skills (it never did; its
  `template.toml` has no verbatim rule).

Checks (logs under `logs/NOUI/INTEG/`):

| Check | Result | Log |
|---|---|---|
| Docs, skills and markers tests after the edits | 80 tests OK, 1 skip | `test-docs-2.log` |
| D1 generator suite, `memwait python -I -m unittest discover -s tests -t .` | 576 OK, 9 skipped, at `9c25389d9` and again at `4b169b8d2` | `unittest-1.log`, `unittest-2.log` |
| D2 `catalogue --check`, `tokens --check` | current, at both heads | `catalogue-tokens-check.log`, `-2.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI` | 19 specs identical outside 1.6's globs; the `CodeRules.kt` hunk is the message only | `compare-1.log` |
| No-UI everything (`--preset everything --no-ui-components`, wizard by Enter, `out/NOUI/integ-everything`), `locked gradle ./gradlew check assembleDevDebug --continue` at `4b169b8d2` | every task green except `:spotlessKotlinCheck` (178 files) and `:spotlessKotlinGradleCheck` (`benchmark/build.gradle.kts`): the WP02 carry-over, exactly GEN's result. `DesignSystemDocTest` 3/3 (in `build-everything-1.log`'s run; cache hit at head), `NavDisplayHostTest` green, 31 `verifyCodeRules`, lint, Kover, `assembleDevDebug` | `build-everything-2.log` (first run at `9c25389d9`: `build-everything-1.log`) |
| The four files this run added or changed, in the generated project, through Spotless's IDE hook (nothing written) | all `IS CLEAN` | `spotless-ide-hook.log` |
| D12 website: `memwait npm ci`, `npm run typecheck`, `npm test`, `memwait npx next build` | ok; 246 tests, 0 fail | `npm-ci.log`, `web-typecheck-test.log`, `next-build.log` |

Not run here: D13 `go test` (Smart App Control, WEB's incident 2), D3/D4 consistency, D7-D11, D15-D17 and the phone
rows: they are the gate's.

Open, for the orchestrator and the gate:

- **`next` 15.5.27 has a critical advisory** (`npm audit`: GHSA-2464-fhr4-w9xw, remote code execution in the Image
  Optimization API with SVG; GHSA-p3m5-59mp-jv5r, a Server Actions DoS); fixed in 15.5.28, while `package.json` pins
  exactly `15.5.27`. Present since before this work (`package.json` unchanged since `8fdcba741`; WEB's `npm ci` log shows it
  too). The site uses neither `next/image` nor server actions, but `/_next/image` is served by default. For D15 and the
  deploy: a one-line bump of `next` (and the lock file) is WEB's to make and gate; not done here.
- Spotless carry-over (both modes): the planned one-time `spotlessApply` after the last UI change.
- `EverythingApplication`/`DaggerEverythingApplication_*` names in the app profiles (GEN's note): unchanged.
- WEB's incident 1 (the stray Python registration) and incident 2 (Smart App Control) still need the user.
- Flags for the morning report (plan 1.6, risk 9): README.md and the CodeRules failure message change in UI projects.
- The Vercel deploy waits for the gate and the user's go; the Vercel plugin still needs `/mcp` authorisation.
- Housekeeping: `%TEMP%\nav3ui` (navigation3-ui's classes, extracted to read `NavDisplay`'s signature) was sent to the
  Recycle Bin; one scratch formatter output in `%TEMP%` was deleted with `rm`. Handoff backup before this section:
  `C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/NOUI.md.before-integration`.

## Fix round 1

Fixer in `wt/NOUI` (`staging-wp/NOUI`), from `4b169b8d2`, for the 13 blocking findings of the generator, template
and web/API reviews (two of them the same private-name finding). **Head `b7eeb6c9a`.** One commit per cause, each
with a test where the cause is code. Nothing pushed; `main`, other branches, worktrees and history untouched; no
stash; `wt/STAGING` not touched; the phone not used (no finding needed it). Logs under `logs/NOUI/FIX1/`.

| # | Finding | Commit | What changed, and the proof |
|---|---|---|---|
| 1 | Private names in `test_docs.py` (`WORKSHOP_WORDS`), reported twice | `e00ea8a3d` | The three names are gone from the repository's tree (`git grep -i` over HEAD finds none); the test keeps the workshop words. Private names are the workspace pre-push scan's job (see Open). `test_docs` green (`test-docs-1.log`). |
| 2 | `tools/android_skills.py` refresh deleted a project's own skills | `268ca81ed`, `36a924385` (drops the constant the old manifest read) | The tool's `MANIFEST.sha256` is the record of what it vendored: only those skills are replaced or removed, the manifest hashes only them, and a skill that would land on a folder or pointer it did not write is refused before anything changes (`SkillsError`). New `ToolTest` cases: a user's `.claude/skills/my-own-skill` and `.agents/skills/my-team-skill` survive a refresh and an upstream drop, byte for byte, and are not called dropped; `--add` over a user folder exits 1 and changes nothing. Both new cases fail on the old tool and pass on the new one (run with the old file put back and then restored). The template's README, pointers and manifest are unchanged (the template-copy tests compare them). |
| 3 | No-UI app profile named `EverythingApplication` and its Hilt/Dagger classes | `39215bd49` | The variant profile names them in the identity form (`BaseAppApplication`, `Hilt_BaseAppApplication`, `DaggerBaseAppApplication_…`, `injectBaseAppApplication`; 222 occurrences), and `template.toml` gains three identity tokens (`Hilt_BaseApp`, `DaggerBaseApp`, `injectBaseApp` → the `{pascal}` forms, prefix boundary), data rather than engine special cases; no UI file contains them (git grep), and D5 below is identical. `test_rename` covers the three forms and their boundaries; the profile test now generates as "Acme NoUi" / `com.acme.noui` and asserts the profile names `AcmeNoUiApplication`, `Hilt_AcmeNoUiApplication` and `DaggerAcmeNoUiApplication_HiltComponents_SingletonC`. With the old profile put back it fails (DaggerEverything… lines unmatched, the three classes absent). The UI template's own profile has the same quirk; not changed (UI output), see Open. |
| 4 | `DevToolsOverlayTest` press imports inside `<opt:flags>` | `6dbd13dd6` | The UI/No-UI import pair sits above `// <opt:flags>`; the generator sorts imports, so UI bytes equal the base again. `compare_generations.py`'s default set gains `only-<key>` and `allbut-<key>` for every selectable feature (105 specs). D5 (`compare-1.log` before the fix commit was made, `compare-2.log` at the head): identical outside 1.6's globs, `only-devtools`, `only-perfhud`, `allbut-flags` included. Gradle: a UI project with `features: ["devtools"]`, `locked gradle ./gradlew :core:devtools:testDebugUnitTest`: BUILD SUCCESSFUL, `DevToolsOverlayTest` 3/3 (`build-ui-devtools-only-test.log`; the review's log of the same spec at the old head fails with `Unresolved reference 'pressable'`). No-UI lean + devtools, no flags: the same test 3/3 (row 5's build). |
| 5 | add then remove a feature left No-UI lean/none red | `51031084e` | `remove_feature.py` deletes a tier (`feature/`, `data/`) it leaves empty; `SharedDestinationsTest`'s No-UI guard needs a module folder (`File("../feature").listFiles(File::isDirectory).orEmpty().isNotEmpty()`), not the tier; the test helper `snapshot` records folders too, so `test_remove_undoes_add` compares the folder set (it fails with the old `remove_feature.py`, passes now). Gradle: No-UI lean + devtools generated, `add_feature orders`, `remove_feature orders --yes` (both tiers gone afterwards), then an empty `feature/` made by hand to recreate the review's state, `locked gradle ./gradlew check assembleDevDebug --continue`: BUILD SUCCESSFUL in 3m 51s, `SharedDestinationsTest` 2 tests, the scan check skipped (`build-noui-lean-removed.log`). |
| 6 | The consistency matrix read the skills' 45 Kotlin samples | `f169cf3a1`, `b7eeb6c9a` | First `Project` skipped the manifest's `verbatim` paths; the quick run then still failed every project with 88 `RES` issues from the glance skill's sample widget XML (`consistency-quick-1.log`: 81 projects, 7128 new issues, all under `.agents/skills`), because the resource, manifest, settings, translation, empty-file and leftovers checks each walked the tree themselves. The root fix is one walker, `project_files(root, pattern)` in `project.py`, which every check and `Project` use. `VendoredFilesTest`: vendored Kotlin and Gradle are not read, and `_resources`, `_translations` and `_hollow` report nothing under `.agents/skills` (fails on `f169cf3a1`'s walkers, passes now). D3 `consistency-quick-2.log`: 81 projects, 0 new issues, 188 known. |
| 7 | Setup-only site still promised slides and predictive back | `888951c47` | The landing items moved to `features/landing/included.ts` (`includedItems(catalogue)`), so they are testable with `node --test`; on a setup-only catalogue "Smooth by rule" drops "screens slide rather than fade" and "Navigation nobody has to coordinate on" says deep links open on the back stack, which survives process death, instead of predictive back. The unlocked copy is the same text as before. `included.test.ts`: in locked mode no unbadged item promises slides, predictive back, a tab bar or a floating bar; unlocked keeps both promises; both list the same items. |
| 8 | FAQ and landing said AGENTS.md with CLAUDE.md pointing at it | `52c7720e2` | Both now say CLAUDE.md and AGENTS.md hold the same rules, with the three detailed guides CLAUDE.md imports and Google's Android skills pinned to a release (`.agents/skills`). |
| 9 | No-UI docs promised render tests features do not run | `dea19fad0` | Option 2 of the finding, keeping T4 (a placeholder feature would pay Robolectric resource merging and a preview scan for nothing). No-UI only: ARCHITECTURE.md Testing (its bullet split per mode), DESIGN-SYSTEM.md "Previews are render tests" and "First things to build" step 4, and the `testing-setup` row in CLAUDE.md and AGENTS.md say the convention runs in `:core:designsystem`, and a feature adds `id("<package>.android.screenshots")` to its own `plugins { }` with its first screen and `@AppScreenPreviews` (per module, so features still on placeholders are not scanned), moving it into `AndroidFeatureConventionPlugin` once every feature has screens. UI text unchanged. Rendered in `out/NOUI/fix1-noui-lean/docs/ARCHITECTURE.md` with the project's own plugin id. |
| 10 | No-UI scroll-chrome accepted only padding the project never has | `d47dbb02e` | Option 2. In No-UI (`opt-else` fences, UI lines unchanged) the rule also accepts insets inside the scroll: `contentPadding = WindowInsets.<x>[.only(…)…].asPaddingValues()` on a list, and after `.verticalScroll(…)` `.windowInsetsPadding(…)`, `.safeDrawingPadding()`, `.safeContentPadding()`, `.systemBarsPadding()` or `.padding(WindowInsets.<x>.asPaddingValues())`; the pass-through `contentPadding` stays for a shell the user builds. The advice names the edge-to-edge forms; PERFORMANCE.md (No-UI block, with an example) and DESIGN-SYSTEM.md's rule row say the same. New No-UI variant test `ScrollChromeCasesTest` (six forms pass; insets on the list's modifier, insets before the scroll and a fixed `PaddingValues` are flagged at lines 1, 8, 10); the shared `CodeRuleCasesTest` pads with insets in No-UI. The F1.4 advice test's library list gains `WindowInsets`, `asPaddingValues`, `safeDrawing`, `windowInsetsPadding` (Compose foundation names). Gradle (row 5's No-UI build): `:build-logic:convention:test` ran, `ScrollChromeCasesTest` 2/2, `CodeRuleCasesTest` 22/22, `DesignSystemDocTest` passed. |
| 11 | PERFORMANCE-OVERLAY.md pointed at `tools/perf` everywhere | `7a703f571` | Every `perf.sh` / `gates.toml` / `devBenchmarkRelease` line is in `<opt:performance&ui_components>`; the other branch gives `dumpsys gfxinfo`, `am start -W` and Macrobenchmark's `StartupTimingMetric`, with the thresholds as plain numbers (0.5 % janky, 380 ms cold start, 58.4 MB PSS). `test_docs` checks the page (`FEATURE_DOCS`) like the other docs in every case that has the overlay: with the old page it fails on `tools/perf/perf.sh` and `tools/perf/gates.toml` in noui-standard, noui-everything and noui-everything-modular; green now. `run-as` joined the tool words the rule-id check skips (an adb command the page names). |
| 12 | D7: UI generation +16 % | none | Not fixable in code without dropping files from (g); the user decides (see Open). Not re-measured; the review's numbers stand and D7 is not reported as passing. |

Checks at the head (logs in `logs/NOUI/FIX1/`):

| Check | Result | Log |
|---|---|---|
| D1 generator suite, `memwait python -I -m unittest discover -s tests -t .` | 581 tests OK, 9 skipped, at `b7eeb6c9a` (580 OK at `7a703f571`) | `unittest-2.log`, `unittest-1.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI` (105 specs) | 105 specs identical outside 1.6's globs; the reviewed `CodeRules.kt` hunk is the message only. Run at `7a703f571`: the two later commits touch only `tools/android_skills.py` (an exception path) and the generator's tests | `compare-2.log` |
| D2 `catalogue --check`, `tokens --check` | both current | `catalogue-tokens-check.log` |
| D3 `memwait python -m tests.consistency --quick` | 81 projects in 268 s, 0 new issues, 188 known (`-1.log` is the run between the two commits of row 6) | `consistency-quick-2.log` |
| D4 `memwait python -m tests.consistency` | 223 projects in 497 s, 0 new issues, 374 known | `consistency-full-2.log` |
| No-UI lean + devtools, add/remove, empty `feature/`, `locked gradle ./gradlew check assembleDevDebug --continue` | BUILD SUCCESSFUL | `build-noui-lean-removed.log` |
| UI `features: ["devtools"]`, `locked gradle ./gradlew :core:devtools:testDebugUnitTest` | BUILD SUCCESSFUL, 3/3 | `build-ui-devtools-only-test.log` |
| D12 `npm run typecheck`, `npm test`, `memwait npx next build` | ok; 249 tests, 0 fail | `web-typecheck-test.log`, `web-next-build.log` |

Not run: D7 (row 12), D8-D11 beyond the two builds above, D13 (no Go change), the phone rows.

Process note: a full matrix run on `f169cf3a1`'s walkers (started about 04:54, before row 6's second commit) was
stopped as a task, but its Python process and four workers kept running; stopping those PIDs was refused by the
permission classifier, so it ran to its end at 05:05 (`consistency-full-1.log`: 223 projects, 19624 new issues, which
is 223 x 88, the skill-XML `RES` class alone). It is evidence that nothing but that class was left; D4 is
`consistency-full-2.log`.

Open, for the orchestrator and the user:

- **The private names are already public.** `10f24c9df` (the commit that added them) is on `origin/staging-wp/NOUI-DOCS`
  and `origin/staging-wp/NOUI` (`git branch -r --contains 10f24c9df`). The fix removes them from the tree; only a
  history rewrite and a force push would take them out of those branches, which the rules forbid without the user.
  The user's call: leave them in that history, or rewrite and force-push those two branches (and keep them out of
  any release branch, which a squash would do). Also: the orchestrator's pre-push scan should carry the private names
  (case-insensitive) beside PUSHED.md §5's token patterns, so a later slip is caught before a push.
- **D7, generation time (REQUIREMENTS 59 g against m).** The review measured UI generation at 2.14 s against 1.84 s
  at the base (+16 %, about 0.3 s per project), almost all of it the 459 vendored skill files against 169 (about
  0.9 ms each on this machine; `shutil.copyfile` is no faster). Options, recommended first:
  (a) accept the cost of the full skills set in the gate report: (g) as the user asked, 0.3 s per project; catch: (m)'s
  "as fast as before" is then not literally true, and D7's +10 % bar has to be waived by the user;
  (b) ship fewer files, for example without glance-canonical-layouts' 45 reference `.kt` files: fewer files to copy;
  catch: the vendored copies stop being upstream's unmodified set (the manifest, the README's "unmodified" and the
  verbatim policy change), the skill points at files it does not have, and it may still not reach +10 %;
  (c) leave a skill out entirely (`LEFT_OUT`): catch: (g) asks for the latest official set.
- The UI template's own `app/src/main/baseline-prof.txt` still names `EverythingApplication` and its Hilt/Dagger classes,
  so in every UI project not named "Everything" those rules match nothing. With the identity tokens of row 3 in, the fix is
  the same rewrite of that file to the identity form; it changes UI output (D5), so it waits for the UI release work.
- Housekeeping: this round left generated projects in `out/NOUI/fix1-noui-lean`, `out/NOUI/fix1-ui-devtools` and
  `out/NOUI/fix1-read-std` (specs `out/NOUI/fix1-noui-lean.json`, `fix1-ui-devtools.json`), kept as evidence; nothing was
  written outside the workspace, the worktree and the scratchpad, and no setting or system state was changed.

## Fix round 2

Fixer in `wt/NOUI` (`staging-wp/NOUI`), from `b7eeb6c9a`, for the eight blocking findings of the second generator,
template and web reviews. **Head `ccd9c3c9b`.** Eight commits, one per cause: finding 5 turned out to be two causes,
finding 8 needs the user, not code. Every cause that is code has a test. The new app-lock, harness, tier, held-folder
and include tests were seen failing on the old code; the status-cover and copy tests were not run against it (the old
cover has no action to call, and the old option no tables to read). Nothing pushed; `main`, other branches, worktrees and history untouched; no stash; `wt/STAGING` not touched; the
phone not used (no finding needed it). Logs under `logs/NOUI/FIX2/`.

| # | Finding | Commit | What changed, and the proof |
|---|---|---|---|
| 1 | No-UI status cover swallowed every tap, never called `onRetry` | `48669ade4` | Without components the cover is `PlaceholderScreen(...).clickable(onClickLabel = <the UI button's string>)`: maintenance calls `onRetry`, an update opens the store listing as the UI's Update button does (both strings were already translated). `openStoreListing`, `LocalContext` and the four imports it needs are shared instead of UI-only; the generator sorts imports, so the UI file renders byte for byte as before (D5 below). In `core:ui` the `bare-clickable` rule applies, so the line carries `// allow(bare-clickable): the cover is the whole screen, which no press may scale or dim` (the template forbids scaling or fading whole screens; the No-UI theme's indication draws nothing). KDoc's `@param onRetry` is now true in both modes. New No-UI variant test `core/ui/.../appstatus/AppStatusGateTest.kt` (4 tests): maintenance tap calls `onRetry` once under the retry label and reaches nothing underneath; update tap starts `ACTION_VIEW market://details?id=<package>` and never retries; `Available` composes no cover and the content takes taps; the cover goes when the status clears. The file is fenced whole by `<opt:appstatus>`, so it is dropped as hollow with that feature like the code it tests; `test_every_variant_file_is_there` learned such files (`whole_file_fence`). 4/4 green in `build-noui-everything-6-targeted.log` (and in `-5.log` before the constants were renamed). |
| 2 | Perf tab advice and KDoc named `tools/perf/perf.sh`, which no No-UI project has | `c17a931ce` | `perfhud_fix_cold_start` and `perfhud_fix_baseline_profile` (XML `opt`/`opt-else`, the `<!--~ ... -->` fallback form) now give Macrobenchmark's `StartupTimingMetric` or the median `TotalTime` of `adb shell am start -W` on devRelease, and `adb shell cmd package compile -f -m speed-profile` (what PERFORMANCE-OVERLAY.md's No-UI branch says). Whole KDoc blocks fenced, as `StartupBenchmark.kt` already does: `PerformanceRecorder`, `OptimisationScore` (two), `FrameIntake`, `MemorySummary.appPssMb`, `PerformanceRecorderTest`, and `LaunchHook.kt`'s seam comment (also named tools/perf). UI lines unchanged. New `ProjectShapeTest.test_nothing_sends_a_reader_to_the_harness_the_option_leaves_out`: no generated No-UI file outside the manifest's `verbatim` paths names `tools/perf`, `tools/adaptive`, `perf.sh` or `gates.toml`, in all seven shapes. On `b7eeb6c9a` it fails in all seven: `LaunchHook.kt` in every one, the overlay's strings and KDoc wherever the overlay is (`new-tests-on-old-code.log`). UI projects without `performance` keep the same untrue advice; that waits for the UI release, as the review said. |
| 3 | `AppLockGateTest`'s focus test fenced out of No-UI with nothing in its place; the No-UI tap path untested | `cfccdf97b` | Three No-UI tests (`opt-else` blocks in the same file): Tab reaches the cover (found by its tag `placeholder`) and `clearFocus()` leaves it focused; the cover's `OnClick` action is labelled with `applock_action` and a tap reaches nothing underneath; and, with `Instrumentation.setInTouchMode(true)` (the test asserts Compose really is in `InputMode.Touch`), a field focused when the app locks loses focus to the cover. **The tests found a real defect:** against the old cover the first and the third fail (`build-noui-everything-2-targeted.log`: "The field kept focus, and the keyboard with it."). A bare `clickable` cannot take focus in touch mode, so on a phone a focused field kept focus behind the No-UI lock, the keyboard stayed up and typing reached the locked content; and with `onExit` on the only focus target, Tab took focus off the cover. Fix (No-UI branch only): `.focusProperties { onExit = cancel }.focusGroup().focusProperties { canFocus = true }` before the clickable, so focus sits inside a closed group as Unlock does in the drawn cover. AppLockGateTest 7/7 (`-4-applock.log`, `-5.log`, `-6-targeted.log`). Robolectric's touch mode only: not checked on the phone. |
| 4 | Kept features and presets promised tools/perf, tabs, the shell and core/ui's list component with the box ticked | `df1e6a008` | The words live in the option's data (59 m): `options/ui_components.toml` gains `[feature_copy.<feature>]` (description, optional headline) and `[preset_copy]`. Covered: `performance`, `paging`, `profile`, `search`, `deeplink` (each named a tab, the harness or the list component), `coil` (its skeleton and failure glyph are `AppNetworkImage`, which the option owns), `licenses` ("rendered by the design system"), `googlesignin` (a button); presets `lean` and `everything` ("every feature the template offers" is false with six left out). `manifest.py` loads them (presets now load before options), checks them like `[disables]` (a feature nobody can choose, a feature the option leaves out, an unknown preset, a blank text, an unknown key: each a `TemplateError`, `BrokenOptionTest`), and offers `feature_copy(feature, values)` and `preset_description(preset, values)`; the catalogue option emits `feature_copy` and `preset_copy`; `catalogue.json` rewritten with `--write`. Site: `featureCopy`/`presetDescription` in `options.ts`; FeaturesStep (cards, preset tooltip and line) and Review's tooltips use them. CLI: `--list-features --no-ui-components` (human and `--json`) lists the features as such a project has them (the left-out ones gone, the option's words); plain `--list-features` and its JSON are unchanged except one dim hint line under the option. Wizard: the option is now asked **before** the preset, as the site shows its box above the presets, so presets and per-feature questions read as the project will have them. Tests: `copy.test.ts` (with the box ticked and on `lockedCatalogue()`, no preset or feature text names tools/perf, tools/adaptive, a tab (a browser's Custom Tab and "Performance tab in the inspector" excepted), the navigation shell, a home screen or a list component; unticked every text is the catalogue's own), `test_manifest` (load and resolve, seven broken files), `test_catalogue` (contract keys, normalised text, only kept features and real presets), `test_cli` (both listings, refusal for multiplatform), `test_prompts` (option first; presets and features in the option's words). The Go API reads the catalogue with `encoding/json`, which ignores the new fields: no Go change. |
| 5a | `SharedDestinationsTest`'s No-UI guard counted any folder; a tier left holding only `build/` | `4e5c7446f` | The guard reads `../settings.gradle.kts` (`^\s*include\(":feature:`): a module is what Gradle builds. Not "a folder with a build file", the review's first option: the repro below left `feature/orders/` with its `build.gradle.kts` and sources but no include, which that rule would still count. `remove_feature.emptied_tiers` takes a tier whose only other entry is a `build` folder with its last module, and the plan lists it ("delete feature/ (no module left in it)") before asking. Tests: `test_a_tier_left_with_only_its_build_output_goes_with_its_last_module` (kover artifacts in both tiers, dry-run names both, the project is byte-identical to before the add) and `test_a_tier_keeps_its_other_modules_and_their_build_output`; the first fails on `b7eeb6c9a` (`new-tests-on-old-code.log`). |
| 5b | (found while reproducing 5) remove_feature died halfway on Windows | `608c61941` | Repro on `out/NOUI/fix2-lean-cov` (No-UI lean + coverage, `add_feature orders`, `check assembleDevDebug` green in `build-lean-cov-added.log`, then `remove_feature orders --yes`): `shutil.rmtree` stopped with WinError 145 at the first `.dex` under `feature/orders/build/.transforms` that a process still holds (the idle Gradle daemon, most likely), after deleting `data/orders` and before touching `settings.gradle.kts` or `app/build.gradle.kts`: a half-deleted module the build still included. Now the edits come first, deletion goes on past a held file (`ignore_errors`), the folders still there are named with what usually holds them, and the run exits 1; tiers are counted over every named module, so the next run finishes a cut-short one. Real runs: `remove-lean-cov-2.log` (edits made, held folders named), `-3.log` (the next run took the `data/` tier the crash had left), `-4.log` (only 23 held `.dex` files remain). Tests: a mocked held folder leaves the module out of settings and the app's dependencies, exits 1 naming it, and a second run restores the project exactly; and a removal cut short (one module gone, `data/build` left) is finished. Both fail on `4e5c7446f`'s script and on `b7eeb6c9a`'s (`new-tests-on-old-code.log`). |
| 5 | (proof for 5a with the real leftovers) | | With the includes gone and `feature/orders/` (build file, sources, held build output), `feature/build/` and `data/build/` still on disk, `locked gradle ./gradlew check assembleDevDebug --continue` in `out/NOUI/fix2-lean-cov`: BUILD SUCCESSFUL, `SharedDestinationsTest` 2 tests, the scan check skipped "No feature module yet" (`build-lean-cov-leftovers.log`). |
| 6 | First includes appended under `// ── tooling ──` | `07cf4b105` | `insert_include`: after the tier's last include as before, or, with none, right under the section's heading (`// ── data ──`, `// ── feature ──`), at the end only when the heading is gone too. `--domain` follows the last domain include, else the last data include, else the data heading. Seen in `out/NOUI/fix2-lean-cov/settings.gradle.kts` after `add_feature orders`. Tests: in `test_a_module_brings_...` each include is the line right after its heading; in `test_a_domain_module_joins_a_modular_project` the domain include opens the data section. Both fail on `b7eeb6c9a` (`new-tests-on-old-code.log`). |
| 7 | README.md and AGENTS.md never mentioned the option | `ccd9c3c9b` | README: `py create_project.py --preset lean --no-ui-components` in "Faster ways in" with what it leaves out and keeps; `--list-features --no-ui-components`; add_feature on a No-UI project (placeholder route, `--tab` refused); remove_feature taking an emptied tier. AGENTS.md: the `ui_components` spec field (Android only, what it drops, the flag, the listing); an **Options** paragraph under "Changing the template" (where options live, `disables`/`hides`/`owns`/`variant`/`feature_copy`/`preset_copy`, the `opt`/`opt-else` pair with the `//~` fallback, variants under `generator/variants/noui/`, `owns` for whole files, and the proofs: `test_android_noui.py`, the `noui-*` matrix runs, `compare_generations.py`). `web/README.md` names the copy tables (in `df1e6a008`). |
| 8 | D7, generation time | none | The user's decision (below); not re-measured, not reported as passing. |

Checks at the head (logs in `logs/NOUI/FIX2/`):

| Check | Result | Log |
|---|---|---|
| D1 generator suite, `memwait python -I -m unittest discover -s tests -t .` | 597 tests OK, 9 skipped (581 at `b7eeb6c9a`) | `unittest-2.log` (working tree before the commits), `unittest-3.log` (head) |
| Each commit's own tests, in an export of that commit (`git archive` to `out/NOUI/fix2-commits/<sha>`) | `test_android_noui` green at `48669ade4` (19), `c17a931ce` (20), `4e5c7446f` (22), `608c61941` (24); `test_manifest`, `test_catalogue`, `test_cli`, `test_prompts` green at `df1e6a008` | `per-commit.log` |
| HEAD's `test_android_noui.py` on `b7eeb6c9a`'s code (export), and the two held-folder tests on `4e5c7446f`'s script | 13 failures on `b7eeb6c9a`: the harness test in all seven shapes, the tier, held-folder, cut-short and include tests; both held-folder tests fail on `4e5c7446f` | `new-tests-on-old-code.log` |
| D2 `catalogue --check`, `tokens --check` | current | `catalogue-tokens-check.log`, `-2.log` |
| D3 `memwait python -m tests.consistency --quick` | 81 projects in 168 s, 0 new issues, 188 known | `consistency-quick-1.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI` (105 specs) | identical outside 1.6's globs; the `CodeRules.kt` hunk is the message only, as before | `compare-1.log` |
| No-UI everything (`out/NOUI/fix2-noui-everything`, spec `out/NOUI/fix2-noui.json` + `--preset everything --no-ui-components`), `locked gradle ./gradlew check assembleDevDebug --continue` | every task green except `:spotlessKotlinCheck` and `:spotlessKotlinGradleCheck`, the WP02 carry-over as in every earlier round; `AppStatusGateTest` 4/4, `AppLockGateTest` 7/7, `SharedDestinationsTest` 2/2, `PerformanceRecorderTest` 10/10, 31 `verifyCodeRules`, lint, `assembleDevDebug` | `build-noui-everything-5.log`; the first run (`-1.log`) failed on a missing `getOrNull` import and on `bare-clickable`, both fixed before the commits |
| The same project after the last edits (renamed test constants), `:core:ui` and `:core:applock` tests and code rules | BUILD SUCCESSFUL | `build-noui-everything-6-targeted.log` |
| No-UI lean + coverage add, build, remove, build | see rows 5, 5a, 5b, 6 | `build-lean-cov-added.log`, `remove-lean-cov-2..4.log`, `build-lean-cov-leftovers.log` |
| Spotless (IDE hook, nothing written) on the eleven Kotlin files this round touched, in the No-UI project | every reported change or lint is in a line this round did not write (the same long lines and `ContentTag`/`NotPrompted` constants in the base output, the carry-over); the new `AppStatusGateTest` is clean once its constants are `CONTENT_TAG`/`PLACEHOLDER_TAG` | `spotless-ide-hook.log`, `spotless-diff-1.log`, `spotless-diff-2.log` |
| D12 `npm run typecheck`, `npm test`, `memwait npx next build` | ok; 253 tests, 0 fail | `web-typecheck-test.log`, `web-next-build.log` |

Not run: D4 (the full matrix; the quick run covers the No-UI shapes), D7, D8-D11 beyond the builds above, D13 (no Go
change), the phone rows.

Where reality differed from the findings, and why:

- Finding 1 asked for the change "only in the `opt-else` branch". The No-UI branch needs the store helper and the
  context the UI branch already had, so those are shared instead of copied into a fallback; D5 proves the UI file is
  unchanged.
- Finding 3 asked only for tests; one of them found the touch-mode defect, fixed in the same commit since the tests are
  its proof. `AppLockGateTest` keeps its file's `PascalCase` constants for the new `PlaceholderTag` (ktlint flags them
  all, a carry-over); the new standalone test file follows ktlint.
- Finding 5's fix "count folders with build.gradle.kts" would not have held: see row 5a.
- The GEN section's note above ("The app-status placeholders have no actions until components exist") is superseded
  by row 1.

Open, for the orchestrator and the user:

- **D7, generation time (REQUIREMENTS 59 g against m): the user decides.** Unchanged since fix round 1: UI generation
  measured 2.14 s against 1.84 s at the base (+16 %, about 0.3 s per project), almost all of it the 459 vendored skill
  files. Options, recommended first: (a) accept the cost of the full skills set and waive D7's +10 % bar in the gate
  report (catch: "as fast as before" is then not literally true); (b) ship fewer skill files (catch: the vendored copies
  stop being upstream's unmodified set, and it may still miss +10 %); (c) leave a skill out (catch: (g) asks for the
  latest official set). The review's re-run beside two builds measured noise; a quiet-machine re-measurement is needed
  only if the user picks (b) or (c).
- The touch-mode focus fix of row 3 is proven under Robolectric's touch mode, not on the phone.
- 23 `.dex` files under `out/NOUI/fix2-lean-cov/feature/orders/build/.transforms` are still held open by a process this
  round did not start (most likely the shared Gradle daemon, which is never stopped by an agent); the folder is out of
  that project's build. They go when the daemon exits.
- Unchanged from earlier rounds: the Spotless carry-over; the UI template profile's `EverythingApplication` names; UI
  projects without `performance` still advise `tools/perf/perf.sh` (UI release); `next` 15.5.27's advisory; WEB's
  incidents 1 and 2; the private names in pushed history.
- Housekeeping: generated evidence kept in `out/NOUI/fix2-noui-everything`, `out/NOUI/fix2-lean-cov` (specs
  `out/NOUI/fix2-noui.json`, `fix2-lean-cov.json`) and the commit exports `out/NOUI/fix2-commits/`; edit scripts,
  staged file versions and commit messages in the scratchpad (`fix2/`). The handoff before this section:
  `C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/NOUI.md.before-fix2`.
  Nothing was written outside the workspace, the worktree and the scratchpad; no setting or system state was changed.

## Text fix round 1

Fixer in `wt/NOUI-FIX` (`staging-wp/NOUI-FIX`), from `ccd9c3c9b`, for the five blocking findings in
`release/noui-review3-blocking.json` (two of them the same cause: No-UI copy promising UI). **State at 09:38: done,
head `f5a773e74`.** Seven
commits, one per cause; every code cause has a test seen failing first. Nothing pushed; `main`, other branches,
worktrees and history untouched; no stash; `wt/NOUI` (the gate) not touched; Gradle and the phone not used. Logs under
`logs/NOUI/TEXTFIX1/`; generated evidence under `out/NOUI/textfix-1/`.

| # | Finding | Commit | What changed, and the proof |
|---|---|---|---|
| 5 | AGENTS.md: the No-UI theme "does not use design_style or motion_style", but a spec, `--set` or API request still gave it Playful/Bouncy | `e5158930c` | **Decision: the generator enforces the option's `[hides]`**, not the text. Evidence: only the clients enforced it (the wizard skips the questions, the site's `stylesFor` resets); AGENTS.md, both `[hides]` reasons and the No-UI README already promise neutral values; REQUIREMENTS 59 (b) and (e) ask for exactly that, and (m) wants the option in the generator's own mechanisms, which `[hides]` is. The other way (say a spec can still pick the style) would keep a hidden field that changes the output and a record saying "Style Playful" for a project whose UI is the user's. `spec.parse` now gives every field an off option hides the value a spec without the field gets (`manifest.hidden_fields(values)`, data from the option), with a note per field that held another value: `'design_style' was left at its default, Utility: <reason>`. The wizard reads the same method. UI specs are untouched. Tests (all failed first, `hides-tests-before-fix.log`): `UiComponentsOptionTest` (spec, schema-1 spec, `--set`, a default value needs no note, unticked styles kept), the mini-template golden case `option-off` with a hidden field (golden trees unchanged), `OptionTest.hidden_fields`. The review's repro (`cmpspecs/lean-style.json --no-ui-components`, with and without `--set design_style=Utility motion_style=Standard`) now generates two byte-identical projects (`repro-style.log`, `out/NOUI/textfix-1/repro-style-*`). AGENTS.md, README.md and web/README.md say so. |
| 1, 4 | Kept features' No-UI copy promised screens, rows, a walkthrough, a permission prompt at the end of onboarding, settings links | `00cfd3f63` | Every claim checked against a generated No-UI everything project (`out/NOUI/textfix-1/everything`). New `[feature_copy]` for applock, appstatus, auth, browser, language, media, onboarding, push, sample, settings, and also database ("on screen"), firebaseanalytics ("opts out in settings"), playstore (a restart the person can ask for; in No-UI the offer goes to `LocalMessagePresenter`, which only logs); profile's photo sentence corrected; `[preset_copy].standard` (no language picker). Each says the ViewModel/state, the placeholder route, and what the user's own screen must draw or call (push: nothing asks for `POST_NOTIFICATIONS` yet, `rememberNotificationPermission()` is ready, notifications stay off on Android 13+ until a screen asks). **Structural guard:** `ProjectShapeTest.test_every_feature_whose_ui_the_option_takes_reads_in_the_option_s_words` finds the features from the template (a fence needing the feature and `ui_components`; an option-owned path the feature owns or fences) and from the seven generated shapes (a feature-owned main file drawing `PlaceholderScreen`), and fails for any without `feature_copy`; before the copy it named exactly applock, appstatus, auth, browser, language, media, onboarding, push, sample, settings (`copy-guard-before-copy.log`). Site: `copy.test.ts`'s `MISSING` also refuses screens, rows, walkthrough, working screen, already drawn, language picker, end of onboarding, and a link held until onboarding or sign-in; run against the old catalogue it matches seven old texts (`copy-test-missing-on-old-catalogue.log`). `catalogue.json` rewritten with `--write`. |
| 3 | `[feature_copy.deeplink]` promised a link held until onboarding and sign-in; the No-UI `PendingDeepLink` KDoc said "after the gates are past" | `00cfd3f63` (copy), `1920b6829` (KDoc), `f5a773e74` (LinkActivity) | Copy: held until start-up has read the settings and the session, then opened on the back stack over the start screen whether or not anyone is signed in, with no entry gates until the project adds its own. KDoc (No-UI `opt-else` only): taken as soon as the root composes after start-up, beneath the app lock's and the API status's covers, signed in or not. `LinkActivity`'s KDoc said the same sign-in-gate thing ("opens it once the tabs are up, as it does for one that arrived before sign-in"); its No-UI branch is fenced the way `PendingDeepLink` is. Rendered in `out/NOUI/textfix-1/everything-head` (diff with the round's base: only these two KDocs and the record stamp). |
| 4 (wizard) | The wizard's Feature modules question said "Each produces a working screen" | `a23218316` | Data in the option, like its other copy: `module_copy` in `ui_components.toml` (manifest-checked, `manifest.module_description(values)`, emitted in the catalogue). The wizard passes the option values to `ask_feature_modules`; **the site's Identity-step hint**, which promised "a screen ... and a tab" (and tab overflow) to every setup-only visitor, uses `moduleCopy()` with the box ticked. Unticked, both keep their words. Tests: `test_manifest` (described while off, not while on; blank refused), `test_catalogue` (contract keys), `test_prompts` (both wordings), `copy.test.ts` (ticked, unticked, locked; the hint is held to `MISSING` too). Each failed before (`module-copy-tests-before.log`). The Go API ignores the new field (`encoding/json`). |
| 2 | web/README.md: "The boot selftest asks for the locked value"; selftest.go: "CI runs it against the built container" | `ea2cd386c` | Both false (the Dockerfile's CMD is `/app/server`, CI's api job runs gofmt, vet, build, test). README: at boot the API only refuses a lock it cannot read; `server -selftest` sends the locked value through the route's checks and the generator, but nothing runs it on its own: run it against an image before deploying. The comment says only `server -selftest` runs it. Not taken: the review's "(gate row D16)", because D16 is an API smoke run, not `-selftest`. `gofmt -l` clean. |
| - | (found while testing) the guard read `templates/android/tools/__pycache__` | `6513de150` | The first full suite failed with a `UnicodeDecodeError` (`unittest-1.log`): another test leaves android_skills' bytecode there. The guard now walks `genkit.core.copy.template_files`, the generator's own file list; green with the `__pycache__` present (`noui-markers-2.log`). |

Checks at the head (logs in `logs/NOUI/TEXTFIX1/`):

| Check | Result | Log |
|---|---|---|
| D1 generator suite, `memwait python -I -m unittest discover -s tests -t .` | 608 tests OK, 9 skipped (597 at `ccd9c3c9b`; 11 new) | `unittest-2.log` (`unittest-1.log`: the `__pycache__` error above) |
| D2 `catalogue --check`, `tokens --check` | both current | `catalogue-tokens-check.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI-FIX` (105 specs) | 105 specs identical outside 1.6's globs; the reviewed `CodeRules.kt` hunk is the message only, as before | `compare-1.log` |
| Website `npm run typecheck`, `npm test` (after `memwait npm ci`) | ok; 254 tests, 0 fail | `npm-ci.log`, `web-typecheck-test.log` |
| `gofmt -l` in `web/api` | clean | (run inline) |

Not run: `npx next build` (no page structure changed; one hint string and a helper), `go vet`/`go build`/`go test` (a comment changed; `go test` is blocked by Smart App Control anyway, and `memwait` sets no `GOPATH`, so a Go build could create `~\go\pkg`), D3/D4 consistency, Gradle builds (no Kotlin code changed, only two KDocs in `opt-else` branches), the phone.

Open, for the orchestrator:

- **Code comments in No-UI output that still describe the shell** (not in the findings; same class as the KDoc fixed above, each needs an `opt-else` KDoc in a shared template file): `StartupViewModel.kt` (EntryIdentity: "nothing pushes the sign-in screen into a tab"), `SignIn.kt` and `Onboarding.kt` route comments and `OnboardingNav.kt` ("moves on to sign-in or the tabs ... each host"; in No-UI the identity change starts a fresh stack on the start screen), `NotificationPermission.kt` ("onboarding asks once at its end, and settings offers it again"), `Shortcuts.kt` ("Ctrl+1...9 for the tabs"), `core/ui/browser/AppBrowser.kt` (KDoc example calls `AppRow`, which no No-UI `core:ui` has). The wizard's language question also says "the language picker in Settings lists them" in every mode.
- The copy guard is a lower bound: it finds UI the option takes by fences, owned paths and placeholder routes. A feature whose UI goes through a seam the option leaves empty (playstore's restart offer through `LocalMessagePresenter`) is only caught by reading the generated project, as this round did for playstore, database and firebaseanalytics; no
  test holds those three beyond the site's `MISSING` words.
- Unchanged from earlier rounds: D7 (the user's decision), the Spotless carry-over, the UI profile's `EverythingApplication` names, `next` 15.5.27's advisory, WEB's incidents 1 and 2, the private names in pushed history.
- Housekeeping: `web/frontend/node_modules` in this worktree is from `npm ci` (git-ignored); `templates/android/tools/__pycache__` (git-ignored) was left by a test; generated evidence in `out/NOUI/textfix-1/`; commit messages and scripts in the scratchpad's `textfix/`; the handoff
  before this section: `C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/NOUI.md.before-textfix1`. Nothing was written outside the workspace, the worktree and the scratchpad; no setting or system state was changed.

## Finish fix 1

Fixer in `wt/NOUI-FIX` (`staging-wp/NOUI-FIX`), from `f5a773e74`, for the one remaining finding (the No-UI push
sentence in `docs/ARCHITECTURE.md`). The earlier text fix round 2 was cut by the power cut after it had only generated
`out/NOUI/textfix-2/everything-before` and `none-before`: no commit and a clean tree, so nothing needed recovering and
this round started from the head. **State at 11:08: done, head `1f2fd6c85`.** Two commits, one per cause, each with a
test seen failing first. Nothing pushed; `main`, other branches, worktrees and history untouched; no stash; `wt/NOUI`
not touched; Gradle and the phone not used. Logs under `logs/NOUI/finish/`; generated evidence under
`out/NOUI/finish-fix-1/`.

| # | Finding | Commit | What changed, and the proof |
|---|---|---|---|
| 1 | A No-UI project with push said "The permission is asked at the end of onboarding and offered again in settings" (`docs/ARCHITECTURE.md`, fenced only by `push`), but nothing asks: the onboarding request is in `<opt:push&ui_components>`, `NotificationsRow.kt` is owned by the option, and `rememberNotificationPermission` has no caller | `af663958d` | The UI lines are fenced with `ui_components`, bytes unchanged. A No-UI project now says that nothing asks yet; that on Android 13 and up no notification is shown (`AppNotifications.post` logs and drops it) until one of the project's own screens calls `rememberNotificationPermission()` from `:core:notification` and `request()`s it where the person can see why; and that `request()` opens the notification settings once the system will not ask again. The helper's own KDoc, which the doc now sends readers to, made the same false claim ("onboarding asks once at its end, and settings offers it again"). Its No-UI `opt-else` now says nothing in the project calls it yet. Test: `GeneratedDocsTest.test_what_the_docs_say_asks_or_opens_is_what_the_code_does` checks each claim against the generated code. "asked at the end of onboarding" needs a call in `feature/onboarding/`, "offered again in settings" needs one in `feature/settings/`, and "Nothing asks ..." needs no call anywhere outside the declaring file. It failed on the old template for `noui-everything` and `noui-everything-modular` (`new-test-before-fix.log`) and passes after the fix (`new-tests-after-fix.log`). |
| - | Found while checking every onboarding claim, as the finding asked. Every No-UI README said "Onboarding, sign-in and every other screen are placeholder routes reachable by navigation and deep links", and ARCHITECTURE.md said "onboarding and sign-in are routes reachable by navigation and deep links". Both are false in a No-UI lean project: lean has neither feature, and it is a preset production generates. In every project, no `DeepLinkRoute` maps a link to either route | `1f2fd6c85` | ARCHITECTURE.md (No-UI block only) now has one sentence per pair (`onboarding&auth`, `onboarding&!auth`, `auth&!onboarding`). Each says that nothing opens onboarding, sign-in or both yet, that the routes are registered, and that a link reaches them only once a `DeepLinkRoute` maps it. With neither feature, the sentence is left out. The README now speaks of every destination a feature registers, which is true with any features, and points to the identity recipe for "once you have onboarding and sign-in screens". Tests: `test_the_docs_call_onboarding_and_sign_in_routes_only_where_the_project_has_them` failed on the old text in `noui-lean`, for both README and ARCHITECTURE (`route-test-before-fix.log`). The claims test also checks "Nothing opens onboarding / sign-in" against the code: the feature is there, and no main source outside it names `OnboardingKey` / `SignInKey`. |

Every push, notification and onboarding claim in the generated No-UI docs was read against the code of
`out/NOUI/finish-fix-1/everything` (No-UI, preset everything, generated at the head; before: `everything-before`, from
`f5a773e74`; the two differ only in README.md, docs/ARCHITECTURE.md and NotificationPermission.kt). To read each fenced
variant, `lean`, `onboarding-push` (onboarding and push only) and `auth` (auth only) were also generated:

- The `:core:notification` row ("Notification channels, the permission check and push-token registration") is true:
  `NotificationChannels`, `NotificationPermission`, `AppNotifications.hasPermission`, `PushRegistration`.
- The entry identity paragraph and the README's "Where the app starts" are fixed (`1f2fd6c85`). Each variant was read
  in `everything`, `onboarding-push`, `auth` and `lean`.
- "`@SettingsDataStore` holds ... onboarding progress" is true (`AppSettingsStore.onboardingCompleted`).
- The session-scoped stores do include "the push registration" (`bindPushRegistrationAsSessionScoped`).
- The Notifications paragraph is fixed (`af663958d`). Its other claims hold as before: channels are created at start-up
  in the application scope (`appScope.launch { notificationChannels.create() }`), and the token reaches
  `PushTokenSink` only when it changes.
- The KDoc of `rememberNotificationPermission` is fixed (`af663958d`).
- CLAUDE.md and AGENTS.md: the only match is the skills row ("touch the manifest, deep links, notifications or
  permissions"), a pointer, and true. README's feature list: names only. "push" in PERFORMANCE.md and
  PERFORMANCE-OVERLAY.md means a navigation push, not a notification.

Checks (logs in `logs/NOUI/finish/`):

| Check | Result | Log |
|---|---|---|
| Generator suite at the head `1f2fd6c85`, `memwait python -I -m unittest discover -s tests -t .` | 610 tests OK, 9 skipped (608 at `f5a773e74`; 2 new). An earlier run with the template fix and the first new test gave 609 OK. | `unittest-2.log`, `unittest-1.log` |
| `catalogue --check`, `tokens --check` | both current. No catalogue text changed, so the website's typecheck and tests were not needed. | `catalogue-tokens-check.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI-FIX` (105 specs; the template as committed) | identical outside 1.6's globs. The `CodeRules.kt` hunk is the reviewed message only, as before. | `compare-1.log` |
| The three changed template files, rendered at `f5a773e74` and now with UI components, for lean, standard, everything, none, push, onboarding, auth and settings alone, push+onboarding, and push+settings. Run because the compare's `docs/*` and `README.md` exceptions would hide a UI change there. | 30 renders, 0 differences. Control run without UI components: 30 of 30 differ. | `ui-render-identical.log`, `noui-render-differs.log` |

Not run: Gradle builds (only docs and one KDoc inside an `opt-else` comment changed, no code), D3/D4 consistency, the
website (no catalogue or site text changed), the phone.

Open, for the orchestrator:

- **A UI project with push but without onboarding or settings** has the same problem. No preset has that combination,
  but a custom selection can. Such a project still says "asked at the end of onboarding and offered again in settings"
  in `docs/ARCHITECTURE.md`, in the `rememberNotificationPermission` KDoc and in `features/push.toml`'s description
  (the catalogue and the site), and nothing asks there either. Not changed here: the KDoc and the catalogue text are UI
  bytes outside the compare's exceptions, so this is a UI decision. Fences on `onboarding` and `settings` would fix the
  doc and the KDoc; the catalogue description is a single text shared by every selection.
- Code comments in No-UI output that still describe the shell. This is Text fix round 1's list minus
  `NotificationPermission.kt`: `StartupViewModel.kt`, `SignIn.kt`, `Onboarding.kt` ("Both 'Get started' and 'Skip'",
  "The completed flag moves the app on to sign-in or the tabs"), `OnboardingNav.kt`, `Shortcuts.kt`,
  `core/ui/browser/AppBrowser.kt`.
- Unchanged from earlier rounds: D7 (the user's decision), the Spotless carry-over, the UI profile's
  `EverythingApplication` names, `next` 15.5.27's advisory, WEB's incidents 1 and 2, the private names in pushed history.
- Housekeeping: a test leaves `templates/android/tools/__pycache__` (git-ignored), as before. Generated evidence is in
  `out/NOUI/finish-fix-1/`. The commit messages, the step files used to split the two commits, and the render check are
  in the scratchpad's `finishfix1/`. The handoff before this section:
  `C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/finishfix1/NOUI.md.before-finishfix1`.
  Nothing was written outside the workspace, the worktree and the scratchpad, and no setting or system state was
  changed.

## Finish fix 2

Fixer in `wt/NOUI-FIX` (`staging-wp/NOUI-FIX`), from `1f2fd6c85`, for the three findings of the second finish review
(evidence `out/NOUI/finish-review-1/r2-*`). The earlier attempt at this round was cut by the power cut before it wrote
anything: a clean tree at `1f2fd6c85`, no commit since, no `out/NOUI/finish-fix-2`, no scratch files, so this round
started from the head. **State at 13:42: done, head `7779aab27`.** Three commits, one per finding, each with a test seen
failing first. Nothing pushed; `main`, other branches, worktrees and history untouched; no stash; `wt/NOUI` not touched;
Gradle and the phone not used. Logs under `logs/NOUI/finish/fix2-*`; generated evidence under `out/NOUI/finish-fix-2/`.

| # | Finding | Commit | What changed, and the proof |
|---|---|---|---|
| 2 | Every No-UI README said "Only `:app` and `:catalog` carry flavours", unfenced; no No-UI project has `:catalog` | `7a8835d64` | The sentence is fenced `<opt:ui_components>`, and an `<opt:!ui_components>` variant says "Only `:app` carries flavours. Every library module builds once, ...". Confirmed in the code first: only `app/build.gradle.kts` applies the flavours convention, `benchmark` uses `missingDimensionStrategy`. **Structural test:** `GeneratedDocsTest.test_every_module_the_docs_name_is_in_the_build` read only two-part module paths (`:core:ui`), so a root module (`:app`, `:catalog`, `:benchmark`) was never checked; it now reads them too. Before the fence it failed in exactly the four No-UI cases, on README's `:catalog` and nothing else (`fix2-module-test-before-fix.log`). The UI cases without the catalog (`ui-lean`, `ui-none`) carry the same untrue sentence; they get a named exception, `UI_STALE_MODULES = {("README.md", ":catalog")}`, for UI cases only, until the UI decision below. UI bytes: the README rendered at `1f2fd6c85` and now with UI components, for lean, standard, everything, none, push, onboarding, auth and settings alone, push+onboarding and push+settings: 10 renders, 0 differences (`fix2-readme-ui-render-identical.log`); without components all 10 differ (`fix2-readme-noui-render-differs.log`). |
| 1 | The site's Project-step panel "What that produces" always ended with the `planTabs` line: on the locked site the default answers showed "Tabs: Home · Settings", lean "Tabs: Home" and a scaffolded home module | `7610744f6` | The line is worked out in `producesNote(state)` in `features/configurator/uiComponents.ts`, beside the step's other No-UI wording. Without components it says "The app opens on a blank start screen, for your design system to build on." (the README's and the lean preset copy's own words; it names no tab bar, so the line can be held to `MISSING`). With components it is the old text, character for character. `IdentityStep.tsx` renders `{producesNote(state)}`. Tests in `copy.test.ts`: on `lockedCatalogue()` the line for the default answers, lean, everything with two modules and six modules says "blank start screen" and matches nothing in `MISSING`, which now also names `home module`; with components it is still `Tabs: Home · Settings` (standard) and the scaffolded-home sentence (lean). Run against the old panel the locked test fails on `'Tabs: Home · Settings'` (`fix2-copy-test-old-site.log`). The site at this commit alone: typecheck ok, 256 tests (`fix2-web-at-commit2.log`). |
| 3 | The Motion step's haptics text promised "a light tick when a selection changes, a switch flips or a pull reaches its threshold" with the box ticked; nothing in a No-UI project plays haptics | `7779aab27` | Confirmed in `out/NOUI/finish-review-1/r2-everything` and `r2-lean`: no `performHapticFeedback` anywhere; the value is `DesignTokens.Defaults.hapticsEnabled` → `AppTheme.defaults.hapticsEnabled`, which `AppRoot` ANDs with the preference store's `hapticsEnabled` into `AppTheme.hapticsEnabled`. **Option data, shared by the wizard and the site**, as the finding preferred: `[field_copy]` in `options/ui_components.toml` (spec field → what it does while the option is off; `haptics_enabled`: "The default behind AppTheme.hapticsEnabled, which the components you build read before they play a tick: nothing in the project plays one yet. A person who turns haptics off in the preference store turns it off too, and the phone's own setting still applies on top."). `manifest.py` loads and checks it (a field the spec does not have, a field `[hides]` leaves unasked, a blank text: each a `TemplateError`) and offers `field_descriptions(values)`; the catalogue option emits `field_copy` (a list of `{field, description}`, after `module_copy`); `catalogue.json` rewritten with `--write`. Wizard: `ask_look(features, hidden, described)` prints the option's words for the haptics question (it said "a light vibration when a control responds", the same class); site: `fieldCopy()` in `options.ts`, `hapticsHint()` in `uiComponents.ts`, used by `MotionStep.tsx`. With components both keep their words. Tests: `test_manifest` (described while off, `{}` while on; three broken files), `test_catalogue` (contract key order, normalised text, only fields still asked), `test_prompts` (each wording in its mode), `copy.test.ts` (ticked, unticked, locked; the haptics text joins `copyOf`, so it is held to `MISSING`, which now names `selection changes|switch flips|pull reaches`). Before the field existed: the mini manifest refused the fixture (46 manifest failures and errors), catalogue 2, prompts 2 (`fix2-field-copy-tests-before.log`); the site test crashed on the missing field (`fix2-copy-test-before-data.log`) and, with `fieldCopy` returning null, fails on the old words (`fix2-copy-test-old-site.log`). The Go API reads the catalogue with `encoding/json`, which ignores the field: no Go change. AGENTS.md and web/README.md name `field_copy`; web/README.md also says what the Project summary shows without components. |

Every push, notification and onboarding claim in the docs of `out/NOUI/finish-fix-2/everything` (No-UI, preset
everything, generated at `7779aab27` from `spec-noui.json`; `lean` beside it) was read against its code:

- README "Only `:app` carries flavours": true in both (`everything/README.md:92`, `lean/README.md:60`).
- ARCHITECTURE's `:core:notification` row ("channels, the permission check and push-token registration"): true
  (`NotificationChannels`, `AppNotifications.hasPermission`, `NotificationPermission`, `PushRegistration`).
- "Channels are the `NotificationChannelSpec` enum, created at start-up off the main thread": true (`enum class
  NotificationChannelSpec`; `appScope.launch { notificationChannels.create() }` in the Application, and `create()` runs
  in `withContext(dispatchers.io)`).
- "The push token is handed to `PushTokenSink` only when it changes, and sign-out unregisters it": true.
  `onRegistered` returns when the stored `submitted_installation_id` equals the new one, and `clear()` (a
  `SessionScopedStore`) calls `FirebaseMessaging.unregister()`. What it hands over is FCM's installation id, which the
  sink's own KDoc calls the install's push address.
- "Nothing asks for the permission ... `AppNotifications.post` logs and drops it ... call
  `rememberNotificationPermission()` ... `request()` ... opens the app's notification settings once the system will not
  ask again": true. No main source outside `NotificationPermission.kt` calls the helper. `post` returns after
  `Log.w(...)` while `hasPermission` is false, and `hasPermission` is true below API 33. `request()` starts
  `ACTION_APP_NOTIFICATION_SETTINGS` when access is `Blocked`.
- "the push registration" among the session-scoped stores: true (`bindPushRegistrationAsSessionScoped`).
- Entry identity: `EntryIdentity.Onboarding`, `SignedOut` and `SignedIn(generation)` exist. "Nothing opens onboarding
  or sign-in yet": no main source outside `feature/onboarding/` names `OnboardingKey`, and none outside `feature/auth/`
  names `SignInKey`. The only `DeepLinkRoute` is the sample's `items` route. Both placeholders are a bare
  `PlaceholderScreen(title)`, so "no way forward" holds.
- "`@SettingsDataStore` holds ... onboarding progress": true (`AppSettingsStore.onboardingCompleted`).
- README's pointer to the identity recipe, the feature list (names only) and CLAUDE.md's skills row (a pointer) make no
  other claim. "push" in PERFORMANCE.md means a navigation push.

Checks (logs in `logs/NOUI/finish/`):

| Check | Result | Log |
|---|---|---|
| Generator suite at `7779aab27`, `memwait python -I -B -m unittest discover -s tests -t .` | 616 tests OK, 9 skipped (610 at `1f2fd6c85`; 6 new). The same on the working tree before the commits. | `fix2-unittest-2.log`, `fix2-unittest-1.log` |
| `catalogue --check`, `tokens --check` at the head | both current | `fix2-catalogue-tokens-check-2.log` |
| Website `npm run typecheck`, `npm test` at the head (catalogue text changed) | ok; 257 tests, 0 fail (254 before; 3 new) | `fix2-web-typecheck-test-2.log` |
| The site at `7610744f6` alone (step files in place, then the head's files back, byte-checked) | typecheck ok; 256 tests, 0 fail | `fix2-web-at-commit2.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI-FIX` (105 specs) | identical outside 1.6's globs; the only hunk is the reviewed `CodeRules.kt` message, the same as in Finish fix 1 | `fix2-compare-1.log` |
| README rendered with and without UI components, `1f2fd6c85` against now | UI: 10 renders, 0 differences. No-UI: 10 of 10 differ | `fix2-readme-ui-render-identical.log`, `fix2-readme-noui-render-differs.log` |
| Generation of `out/NOUI/finish-fix-2/everything` and `lean` | exit 0 | `fix2-generate-everything.log`, `fix2-generate-lean.log` |

Not run: Gradle builds (no Kotlin, Gradle or resource file changed; one README paragraph and option data), `npx next
build` (no page structure changed: two texts now come from helpers), Go (no change), D3/D4 consistency, the phone.

Open, for the orchestrator and the user:

- **UI projects without the catalog** (lean, none, and any selection without `catalog`) still say "Only `:app` and
  `:catalog` carry flavours". Fencing it on `catalog` changes UI projects' bytes, which the gate holds identical, so
  this is the UI release's decision. When it is fenced, drop `UI_STALE_MODULES` in `test_docs.py`.
- **Same class, not in the findings:** the site's Languages hint on the Project step, shown to every setup-only
  visitor, says "Each one you pick translates every screen and joins the language picker"; a No-UI project draws no
  picker (`feature_copy.language`: "for the picker your settings screen draws"). The wizard's language question says
  "the language picker in Settings lists them" in every mode. `[field_copy].languages` would fit both, as
  `languages` is a field still asked.
- Comments, not docs: every No-UI `settings.gradle.kts` opens with "`:app / :catalog   composition roots`" (so do UI
  projects without the catalog), beside Finish fix 1's list of code comments that still describe the shell
  (`StartupViewModel.kt`, `SignIn.kt`, `Onboarding.kt`, `OnboardingNav.kt`, `Shortcuts.kt`, `AppBrowser.kt`).
  `ModuleGraph.kt`'s KDoc names `:catalog` as a rule of the guard, which stays true.
- `docs/PERFORMANCE-OVERLAY.md` (with `perfhud`) reports how the overlay's cost was measured on the template with
  components ("switching every tab and pushing and popping a screen"). It records that measurement and claims nothing
  about the project, so it was left alone.
- Unchanged from earlier rounds: D7 (the user's decision), a UI project with push but without onboarding or settings
  (Finish fix 1), the Spotless carry-over, the UI profile's `EverythingApplication` names, `next` 15.5.27's advisory,
  WEB's incidents 1 and 2, the private names in pushed history.
- Housekeeping: a test leaves `templates/android/tools/__pycache__` (git-ignored), as before. Generated evidence:
  `out/NOUI/finish-fix-2/` (`everything`, `lean`, `spec-noui.json`). The scratchpad's `finishfix2/` holds the commit
  messages, the step files used to split commits 2 and 3, the chain script, the old-site patch script and the render
  check. The handoff before this section is
  `C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/finishfix2/NOUI.md.before-finishfix2`.
  Nothing was written outside the workspace, the worktree and the scratchpad, and no setting or system state was
  changed.

## Merge after the power cut

Integrator in `wt/NOUI` (`staging-wp/NOUI`), from `ccd9c3c9b`, merging `staging-wp/NOUI-FIX` at `7779aab27`. **State
at 14:22: done, head `db2617930`.** Two commits. Nothing pushed; `main`, other branches, worktrees and history
untouched; no stash; `wt/NOUI-FIX` not touched; Gradle and the phone not used. Logs under `logs/NOUI/finish/merge-*`.

Starting state: a clean tree at `ccd9c3c9b` with no merge in progress (no `MERGE_HEAD`), so the power cut left nothing
to recover here. `locked status`: gradle and adb free, no `java.exe`; the gate's last log (`gate-1/verify`) is from
10:31, so nothing was running from this tree.

| Commit | What, and why |
|---|---|
| `079579f7e` | `git merge --no-ff staging-wp/NOUI-FIX`. The branch's 12 commits were made beside the gate, in `wt/NOUI-FIX`, so this branch's tree did not move while the gate ran from it. They are Text fix round 1's seven (`e5158930c`..`f5a773e74`, review 3's five blocking findings in `release/noui-review3-blocking.json`), Finish fix 1's two (`af663958d`, `1f2fd6c85`, the docs fix for the No-UI push sentence plus the onboarding and sign-in route claims) and Finish fix 2's three (`7a8835d64`, `7610744f6`, `7779aab27`, the second finish review's findings). The task named "the five review-3 findings and the last docs fix". The branch also carries Finish fix 2, so the message names all three rounds. No conflict: the merge base is `ccd9c3c9b` and this branch had nothing the fix branch lacked, so the merge's tree is `7779aab27`'s tree (`6cc480aa2`), byte for byte. |
| `db2617930` | `catalogue --write` and `tokens --write` after the merge (`merge-write.log`). The content was already current, because the fix branch wrote it, and the token copies did not change. Only the catalogue's `generator_commit` stamp moved, from `7610744f6` to the merge, which is the generator the catalogue now describes. Committed separately, as `39f71cca7` was after the lane merges. |

Checks at the head `db2617930` (logs in `logs/NOUI/finish/`):

| Check | Result | Log |
|---|---|---|
| `catalogue --check`, `tokens --check` | both current (3 token copies) | `merge-catalogue-tokens-check.log` |
| Generator suite, `memwait python -I -m unittest discover -s tests -t .` in `generator/` (the real interpreter `pythoncore-3.15-64/python.exe`) | 616 tests OK, 9 skipped, the same as `wt/NOUI-FIX` at `7779aab27` (`fix2-unittest-2.log`) | `merge-unittest.log` |
| Tree after the checks | clean. Only git-ignored `__pycache__` folders were added, because the suite was run without `-B`. | `merge-status-after.log` |

Not run: Gradle builds, the website's typecheck and tests, D5 compare and the phone. Apart from the one stamp line,
the tree is the one Finish fix 2 checked at `7779aab27`: website typecheck ok, 257 tests; D5 identical outside 1.6's
globs. The gate's own rows are the orchestrator's to re-run or carry over.

Open: unchanged from Finish fix 2's list (UI projects without the catalog and the flavours sentence, the Languages
hint and wizard question, the code comments that still describe the shell, D7, a UI project with push but without
onboarding or settings, the Spotless carry-over, the UI profile's `EverythingApplication` names, `next` 15.5.27's
advisory, WEB's incidents 1 and 2, the private names in pushed history). Housekeeping: the merge and stamp messages
and the two scripts are in the scratchpad's `mergefix/`. The handoff before this section is
`C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/mergefix/NOUI.md.before-merge`.
Nothing was written outside the workspace, the worktree and the scratchpad, and no setting or system state was
changed.
